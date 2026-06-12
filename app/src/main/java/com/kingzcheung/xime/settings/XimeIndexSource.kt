package com.kingzcheung.xime.settings

import android.content.Context
import android.util.Log
import com.kingzcheung.xime.settings.KeysConfigHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 安装结果（带失败原因 + 未解决依赖，供 ViewModel 映射文案）。 */
data class InstallResult(
    val success: Boolean,
    val unresolvedDeps: List<String> = emptyList(),
    val failureReason: String? = null,
)

/** 方案列表拉取结果（含命中的来源主机名，供 UI 显示「从哪个端点拉的」）。 */
data class SchemesFetch(
    val schemes: List<MarketSchemeItem>,
    val source: String,
)

/**
 * 方案市场数据源：读取 ximeiorg/xime-index 精选索引（根→子→逐方案，CDN 优先 + raw 回退），
 * 并按版本 sha256 安装；安装后用 [RimeDependencyResolver] 按索引声明的 dependencies 补齐编译依赖。
 * 网络/Android 依赖集中在此层；解析/版本/路径/兼容性逻辑都在 [XimeIndexParser] 纯函数里。
 *
 * 端点列表通过 [xime.yaml] 的 `xime_index.base_urls` 配置，用户可自定义镜像列表。
 */
object XimeIndexSource {
    private const val TAG = "XimeIndexSource"
    // 默认端点，会被 xime.yaml 中的值覆盖
    private val defaultBaseUrls = listOf("https://index.ximei.me/")

    private var baseUrls: List<String> = defaultBaseUrls

    /** 构建完整镜像列表：直接使用用户配置的 base_urls。 */
    private fun buildMirrors(userUrls: List<String>): List<String> = userUrls

    /** 从 xime.yaml 加载 xime-index 配置。每次网络请求前调用以确保使用最新配置。 */
    private fun ensureConfigured(context: Context) {
        val cfg = KeysConfigHelper.loadXimeIndexConfig(context)
        if (cfg.baseUrls != baseUrls) {
            baseUrls = cfg.baseUrls
            mirrors = buildMirrors(baseUrls)
            Log.d(TAG, "XimeIndex configured: baseUrls=$baseUrls")
        }
    }

    private var mirrors: List<String> = buildMirrors(defaultBaseUrls)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** 镜像 base → 展示用主机名（如 index.ximei.me / fastly.jsdelivr.net）。 */
    private fun hostOf(base: String): String =
        base.substringAfter("://").substringBefore("/")

    /** 跟随索引跳转：根 → 子 → 逐方案（并行、部分失败容忍）。逐个镜像尝试直到获取到方案。 */
    suspend fun fetchSchemes(context: Context, appVersion: String): Result<SchemesFetch> =
        withContext(Dispatchers.IO) {
            ensureConfigured(context)
            try {
                // 遍历所有镜像，第一个成功获取到方案的返回
                for (base in mirrors) {
                    val result = tryFetchFromBase(base, appVersion)
                    if (result != null) return@withContext Result.success(result)
                }
                // 全部镜像都失败
                val lastUrl = mirrors.lastOrNull() ?: "未知"
                Result.failure(IOException("无法连接到方案市场（已尝试 ${mirrors.size} 个镜像）"))
            } catch (e: Exception) {
                Log.e(TAG, "fetchSchemes failed", e)
                Result.failure(e)
            }
        }

    /** 尝试从一个镜像基址获取完整方案列表。获取到空方案不计为成功，返回 null 让外层试下一个镜像。 */
    private fun tryFetchFromBase(base: String, appVersion: String): SchemesFetch? {
        val repoPath = "index.yaml"
        val host = hostOf(base)
        try {
            val rootText = fetchTextSingle(base, repoPath) ?: return null
            val root = XimeIndexParser.parseIndex(rootText)
            val subPath = XimeIndexParser.resolveRepoPath(
                repoPath, root.schemas?.from ?: "./rimes/index.yaml",
            )
            val subText = fetchTextSingle(base, subPath) ?: return null
            val sub = XimeIndexParser.parseSubIndex(subText)

            // 方案文件从所有镜像获取（按顺序轮询），避免单个 CDN 频率限制
            val schemeTexts = sub.schemas.mapNotNull { entry ->
                val p = XimeIndexParser.resolveRepoPath(subPath, entry.file)
                fetchTextAnyMirror(p)
            }
            Log.d(TAG, "tryFetchFromBase $host: 子索引共 ${sub.schemas.size} 条，获取到 ${schemeTexts.size} 个方案文件")
            val schemes = schemeTexts.mapNotNull { text ->
                runCatching { XimeIndexParser.parseScheme(text) }.getOrNull()
            }.distinctBy { it.id }
                .map { XimeIndexParser.toItem(it, appVersion) }
            Log.d(TAG, "tryFetchFromBase $host: 解析后 ${schemes.size} 个方案: ${schemes.map { it.scheme.id }}")

            if (schemes.isEmpty()) {
                Log.w(TAG, "tryFetchFromBase $host: 获取到 0 个方案，尝试下一个镜像")
                return null
            }
            Log.i(TAG, "tryFetchFromBase $host: 获取到 ${schemes.size} 个方案")
            return SchemesFetch(schemes, host)
        } catch (e: Exception) {
            Log.w(TAG, "tryFetchFromBase $host failed: ${e.message}")
            return null
        }
    }

    /** 从单一镜像基址获取文件内容，失败返回 null（不抛异常）。 */
    private fun fetchTextSingle(base: String, repoPath: String): String? {
        return try {
            client.newCall(Request.Builder().url(base + repoPath).build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.string()?.takeIf { it.isNotBlank() }
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchTextSingle $base$repoPath failed: ${e.message}")
            null
        }
    }

    /** 在所有镜像中查找文件，返回第一个成功获取的内容。 */
    private fun fetchTextAnyMirror(repoPath: String): String? {
        for (base in mirrors) {
            val result = fetchTextSingle(base, repoPath)
            if (result != null) return result
        }
        return null
    }

    /**
     * 安装一个方案：按版本 downloadUrl（+sha256）落盘，再按索引声明依赖补齐编译依赖。
     * @param resolveDepUrl 依赖包 id → 下载 URL（由调用方从已取的方案列表构造）。
     */
    suspend fun installScheme(
        context: Context,
        scheme: MarketScheme,
        resolveDepUrl: (String) -> String? = { null },
    ): InstallResult = withContext(Dispatchers.IO) {
        val v = scheme.resolvedVersion()
            ?: return@withContext InstallResult(false, failureReason = "无可用版本")
        val first = v.downloadUrls.firstOrNull()
        if (first == null || first.url.isBlank()) {
            return@withContext InstallResult(false, failureReason = "缺少下载地址")
        }

        val before = SchemaManager.discoverSchemas(context).map { it.schemaId }.toSet()
        val ok = SchemaManager.importFromUrl(context, first.url, first.sha256)
        if (!ok) return@withContext InstallResult(false, failureReason = "安装失败或文件校验失败")

        // 找到新落盘的真实 rime schema id（索引 id 不保证等于 rime schema_id / 文件名）
        val after = SchemaManager.discoverSchemas(context).map { it.schemaId }.toSet()
        val newSchemaId = (after - before).firstOrNull() ?: scheme.id

        // 依赖补齐（不剥离）：按索引声明的 dependencies 递归补齐，让方案带反查完整编译
        val completion = RimeDependencyResolver.complete(
            context = context,
            schemaId = newSchemaId,
            dependencies = scheme.dependencies,
            resolveUrl = resolveDepUrl,
        )
        val unresolved = (completion.unresolved + completion.stillMissingFiles).distinct()
        if (unresolved.isNotEmpty()) Log.w(TAG, "install ${scheme.id}: unresolved=$unresolved")
        InstallResult(success = true, unresolvedDeps = unresolved)
    }
}
