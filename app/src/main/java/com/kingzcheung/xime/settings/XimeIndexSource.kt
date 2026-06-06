package com.kingzcheung.xime.settings

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

/**
 * 方案市场数据源：读取 ximeiorg/xime-index 精选索引（根→子→逐方案，CDN 优先 + raw 回退），
 * 并按版本 sha256 安装；安装后用 [RimeDependencyResolver] 按索引声明的 dependencies 补齐编译依赖。
 * 网络/Android 依赖集中在此层；解析/版本/路径/兼容性逻辑都在 [XimeIndexParser] 纯函数里。
 */
object XimeIndexSource {
    private const val TAG = "XimeIndexSource"
    private const val REPO = "ximeiorg/xime-index"
    private const val BRANCH = "main"

    // Xime 官方索引端点（代理 ximeiorg/xime-index，附正确 text/yaml + CORS，大陆可达性好）。
    // 仅服务索引 .yaml（方案 zip 仍走各 version 的 downloadUrl，直连上游）。
    private const val OFFICIAL_BASE = "https://index.ximei.me/"

    // 顺序即回退优先级：官方端点优先，失败回退 jsDelivr CDN，最后 raw。
    private val MIRRORS = listOf(
        OFFICIAL_BASE,
        "https://fastly.jsdelivr.net/gh/$REPO@$BRANCH/",
        "https://cdn.jsdelivr.net/gh/$REPO@$BRANCH/",
        "https://raw.githubusercontent.com/$REPO/$BRANCH/",
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** 按镜像优先级取一个 repo 相对路径的文本；首个 2xx 即返回，全失败返回 null。 */
    private fun fetchText(repoPath: String): String? {
        for (base in MIRRORS) {
            try {
                client.newCall(Request.Builder().url(base + repoPath).build()).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val s = resp.body?.string()
                        if (!s.isNullOrBlank()) return s
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetch $base$repoPath failed: ${e.message}")
            }
        }
        return null
    }

    /** 跟随索引跳转：根 → 子 → 逐方案（并行、部分失败容忍）。 */
    suspend fun fetchSchemes(appVersion: String): Result<List<MarketSchemeItem>> =
        withContext(Dispatchers.IO) {
            try {
                val rootText = fetchText("index.yaml")
                    ?: return@withContext Result.failure(IOException("根索引获取失败"))
                val root = XimeIndexParser.parseIndex(rootText)
                val subPath = XimeIndexParser.resolveRepoPath(
                    "index.yaml", root.schemas?.from ?: "./rimes/index.yaml",
                )
                val subText = fetchText(subPath)
                    ?: return@withContext Result.failure(IOException("方案子索引获取失败"))
                val sub = XimeIndexParser.parseSubIndex(subText)

                val schemes = coroutineScope {
                    sub.schemas.map { entry ->
                        async {
                            val p = XimeIndexParser.resolveRepoPath(subPath, entry.file)
                            val text = fetchText(p) ?: return@async null
                            runCatching { XimeIndexParser.parseScheme(text) }.getOrNull()
                        }
                    }.awaitAll()
                }.filterNotNull()
                    .distinctBy { it.id }
                    .map { XimeIndexParser.toItem(it, appVersion) }

                Result.success(schemes)
            } catch (e: Exception) {
                Log.e(TAG, "fetchSchemes failed", e)
                Result.failure(e)
            }
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
        if (v.downloadUrl.isBlank()) {
            return@withContext InstallResult(false, failureReason = "缺少下载地址")
        }

        val before = SchemaManager.discoverSchemas(context).map { it.schemaId }.toSet()
        val ok = SchemaManager.importFromUrl(context, v.downloadUrl, v.sha256)
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
