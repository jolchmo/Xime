package com.kingzcheung.xime.settings

import android.content.Context
import android.content.SharedPreferences
import com.kingzcheung.xime.plugin.core.runtime.PluginManager

object SettingsPreferences {
    private const val PREFS_NAME = "kime_settings"
    private const val KEY_CURRENT_SCHEMA = "current_schema"
    private const val KEY_DEPLOYMENT_DONE = "deployment_done"
    private const val KEY_DEPLOYMENT_HASH = "deployment_hash"
    private const val KEY_SETUP_COMPLETED = "setup_completed"
    private const val KEY_DARK_MODE = "dark_mode"
    
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_SOUND_VOLUME = "sound_volume"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_VIBRATION_INTENSITY = "vibration_intensity"
    private const val KEY_KEYBOARD_THEME = "keyboard_theme"
    private const val KEY_SHOW_BOTTOM_BUTTONS = "show_bottom_buttons"
    
    private const val KEY_SMART_PREDICTION_ENABLED = "smart_prediction_enabled"
    private const val KEY_PREDICTION_MODEL_REPO = "prediction_model_repo"
    
    private const val KEY_STT_ENABLED = "stt_enabled"
    private const val KEY_STT_PROVIDER = "stt_provider"
    private const val KEY_FUNASR_API_KEY = "funasr_api_key"
    private const val KEY_STT_USE_LOCAL = "stt_use_local"
    private const val KEY_STT_KEEP_MODEL_IN_RAM = "stt_keep_model_in_ram"
    
    private const val KEY_PUNCTUATION_MODEL_ENABLED = "punctuation_model_enabled"
    
    /** 默认主题 ID，可从 xime.yaml 的 style.color_scheme 初始化。 */
    @JvmStatic
    var defaultKeyboardTheme: String = "lavender_purple"
    
    const val KEY_SWIPE_UP_HINTS_ENABLED = "swipe_up_hints_enabled"
    const val KEY_SWIPE_DOWN_HINTS_ENABLED = "swipe_down_hints_enabled"
    
    private const val KEY_KEYBOARD_HEIGHT_DP = "keyboard_height_dp"
    private const val KEY_KEYBOARD_HEIGHT_DP_LANDSCAPE = "keyboard_height_dp_landscape"
    const val DEFAULT_KEYBOARD_HEIGHT_DP = 308
    
    private const val KEY_KEYBOARD_BOTTOM_PADDING_DP = "keyboard_bottom_padding_dp"
    private const val DEFAULT_KEYBOARD_BOTTOM_PADDING_DP = 0

    private const val KEY_TOOLBAR_BUTTONS = "toolbar_buttons"
    private val DEFAULT_TOOLBAR_BUTTONS = com.kingzcheung.xime.keyboard.ToolbarButton.DEFAULT_VISIBLE.joinToString(",") { it.id }

    fun getToolbarButtons(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY_TOOLBAR_BUTTONS, DEFAULT_TOOLBAR_BUTTONS) ?: DEFAULT_TOOLBAR_BUTTONS
        return raw.split(",").filter { it.isNotEmpty() }
    }

    fun setToolbarButtons(context: Context, buttons: List<String>) {
        getPrefs(context).edit().putString(KEY_TOOLBAR_BUTTONS, buttons.joinToString(",")).apply()
    }

    private const val KEY_WEBDAV_URL = "webdav_url"
    private const val KEY_WEBDAV_USERNAME = "webdav_username"
    private const val KEY_WEBDAV_PASSWORD = "webdav_password"
    private const val KEY_WEBDAV_PATH = "webdav_path"

    private const val KEY_SCHEMA_IMPORT_WARNING_DISMISSED = "schema_import_warning_dismissed"

    private const val KEY_INSTALLED_MARKET_IDS = "installed_market_ids"
    private const val KEY_COMPACT_MODE = "compact_mode"
    private const val KEY_SHOW_CANDIDATE_COMMENTS = "show_candidate_comments"
    private const val KEY_PAGE_SIZE = "page_size"
    const val DEFAULT_PAGE_SIZE = 0 // 0 表示使用 Rime schema 默认值

    fun isCompactModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_COMPACT_MODE, true)
    }

    fun setCompactModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_COMPACT_MODE, enabled).apply()
    }

    fun showCandidateComments(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_CANDIDATE_COMMENTS, true)
    }

    fun setShowCandidateComments(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_CANDIDATE_COMMENTS, show).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getPrefsPublic(context: Context): SharedPreferences {
        return getPrefs(context)
    }
    
    fun getCurrentSchema(context: Context): String {
        return getPrefs(context).getString(KEY_CURRENT_SCHEMA, "wubi86") ?: "wubi86"
    }
    
    fun setCurrentSchema(context: Context, schemaId: String) {
        getPrefs(context).edit().putString(KEY_CURRENT_SCHEMA, schemaId).apply()
    }
    
    fun isDeploymentDone(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DEPLOYMENT_DONE, false)
    }
    
    fun setDeploymentDone(context: Context, done: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEPLOYMENT_DONE, done).apply()
    }

    fun getDeploymentHash(context: Context): String {
        return getPrefs(context).getString(KEY_DEPLOYMENT_HASH, "") ?: ""
    }

    fun setDeploymentHash(context: Context, hash: String) {
        getPrefs(context).edit().putString(KEY_DEPLOYMENT_HASH, hash).apply()
    }

    fun isSetupCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun setSetupCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    fun getDarkMode(context: Context): Int {
        // 0 = 浅色, 1 = 深色, 2 = 跟随系统（默认）
        return getPrefs(context).getInt(KEY_DARK_MODE, 2)
    }
    
    fun setDarkMode(context: Context, mode: Int) {
        getPrefs(context).edit().putInt(KEY_DARK_MODE, mode).apply()
    }
    
    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)
    }
    
    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
    
    fun getSoundVolume(context: Context): Int {
        return getPrefs(context).getInt(KEY_SOUND_VOLUME, 50)
    }
    
    fun setSoundVolume(context: Context, volume: Int) {
        getPrefs(context).edit().putInt(KEY_SOUND_VOLUME, volume).apply()
    }
    
    fun isVibrationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VIBRATION_ENABLED, true)
    }
    
    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }
    
    fun getVibrationIntensity(context: Context): Int {
        return getPrefs(context).getInt(KEY_VIBRATION_INTENSITY, 50)
    }
    
    fun setVibrationIntensity(context: Context, intensity: Int) {
        getPrefs(context).edit().putInt(KEY_VIBRATION_INTENSITY, intensity).apply()
    }
    
    fun getKeyboardTheme(context: Context): String {
        return getPrefs(context).getString(KEY_KEYBOARD_THEME, defaultKeyboardTheme) ?: defaultKeyboardTheme
    }
    
    fun setKeyboardTheme(context: Context, themeId: String) {
        getPrefs(context).edit().putString(KEY_KEYBOARD_THEME, themeId).apply()
    }
    
    fun showBottomButtons(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_BOTTOM_BUTTONS, false)
    }
    
    fun setShowBottomButtons(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_BOTTOM_BUTTONS, show).apply()
    }
    
    fun isPluginEnabled(context: Context, pluginId: String): Boolean {
        val prefs = getPrefs(context)
        val key = "plugin_enabled_$pluginId"
        
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, false)
        }
        
        val pluginInfo = PluginManager.getAllInstallPlugins().find { it.id == pluginId }
        return pluginInfo?.enabled ?: true
    }
    
    fun setPluginEnabled(context: Context, pluginId: String, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("plugin_enabled_$pluginId", enabled).apply()
    }
    
    fun isSmartPredictionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SMART_PREDICTION_ENABLED, false)
    }
    
    fun setSmartPredictionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SMART_PREDICTION_ENABLED, enabled).apply()
    }
    
    fun getPredictionModelRepo(context: Context): String {
        return getPrefs(context).getString(KEY_PREDICTION_MODEL_REPO, "https://www.modelscope.cn/models/bikeand/predictive-text-small") 
            ?: "https://www.modelscope.cn/models/bikeand/predictive-text-small"
    }
    
    fun setPredictionModelRepo(context: Context, repo: String) {
        getPrefs(context).edit().putString(KEY_PREDICTION_MODEL_REPO, repo).apply()
    }
    
    fun isSttEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STT_ENABLED, false)
    }
    
    fun setSttEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STT_ENABLED, enabled).apply()
    }
    
    fun getSttProvider(context: Context): String {
        return getPrefs(context).getString(KEY_STT_PROVIDER, "funasr") ?: "funasr"
    }
    
    fun setSttProvider(context: Context, provider: String) {
        getPrefs(context).edit().putString(KEY_STT_PROVIDER, provider).apply()
    }
    
    fun getFunAsrApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_FUNASR_API_KEY, "") ?: ""
    }
    
    fun setFunAsrApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_FUNASR_API_KEY, apiKey).apply()
    }
    
    fun isSttUseLocal(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STT_USE_LOCAL, false)
    }
    
    fun setSttUseLocal(context: Context, useLocal: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STT_USE_LOCAL, useLocal).apply()
    }
    
    fun isSttKeepModelInRam(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STT_KEEP_MODEL_IN_RAM, true)
    }
    
    fun setSttKeepModelInRam(context: Context, keep: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STT_KEEP_MODEL_IN_RAM, keep).apply()
    }
    
    fun isPunctuationModelEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PUNCTUATION_MODEL_ENABLED, false)
    }
    
    fun setPunctuationModelEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PUNCTUATION_MODEL_ENABLED, enabled).apply()
    }
    
    fun isSwipeUpHintsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SWIPE_UP_HINTS_ENABLED, true)
    }
    
    fun setSwipeUpHintsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SWIPE_UP_HINTS_ENABLED, enabled).apply()
    }
    
    fun isSwipeDownHintsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SWIPE_DOWN_HINTS_ENABLED, true)
    }
    
    fun setSwipeDownHintsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SWIPE_DOWN_HINTS_ENABLED, enabled).apply()
    }

    // 下滑提示三态：0=隐藏 1=功能(复制/全选/粘贴…) 2=字根优先(无字根回退功能)。默认 2，与作者一致。
    const val KEY_SWIPE_DOWN_MODE = "swipe_down_mode"
    fun getSwipeDownMode(context: Context): Int {
        val p = getPrefs(context)
        return if (p.contains(KEY_SWIPE_DOWN_MODE)) p.getInt(KEY_SWIPE_DOWN_MODE, 2)
        else if (isSwipeDownHintsEnabled(context)) 2 else 0
    }
    fun setSwipeDownMode(context: Context, mode: Int) {
        getPrefs(context).edit()
            .putInt(KEY_SWIPE_DOWN_MODE, mode)
            .putBoolean(KEY_SWIPE_DOWN_HINTS_ENABLED, mode != 0)
            .apply()
    }
    
    fun getKeyboardHeightDp(context: Context): Int {
        return getPrefs(context).getInt(KEY_KEYBOARD_HEIGHT_DP, DEFAULT_KEYBOARD_HEIGHT_DP)
    }

    fun getKeyboardHeightDp(context: Context, isLandscape: Boolean): Int {
        val key = if (isLandscape) KEY_KEYBOARD_HEIGHT_DP_LANDSCAPE else KEY_KEYBOARD_HEIGHT_DP
        val alt = if (isLandscape) KEY_KEYBOARD_HEIGHT_DP else KEY_KEYBOARD_HEIGHT_DP_LANDSCAPE
        // 先读本方向的值，如果没有则用另一个方向的，最后用默认值
        return getPrefs(context).getInt(key, getPrefs(context).getInt(alt, DEFAULT_KEYBOARD_HEIGHT_DP))
    }

    fun setKeyboardHeightDp(context: Context, heightDp: Int) {
        getPrefs(context).edit().putInt(KEY_KEYBOARD_HEIGHT_DP, heightDp).apply()
    }

    fun setKeyboardHeightDp(context: Context, heightDp: Int, isLandscape: Boolean) {
        val key = if (isLandscape) KEY_KEYBOARD_HEIGHT_DP_LANDSCAPE else KEY_KEYBOARD_HEIGHT_DP
        getPrefs(context).edit().putInt(key, heightDp).apply()
    }
    
    fun getDefaultKeyboardHeightDp(): Int = DEFAULT_KEYBOARD_HEIGHT_DP

    // ── 键盘布局编辑器 ──（按键外观/阴影沿用维护者系统，这里只管尺寸/功能键/数字行/字根）
    const val KEY_KEYBOARD_SIDE_PADDING_DP = "keyboard_side_padding_dp"
    const val DEFAULT_KEYBOARD_SIDE_PADDING_DP = 4
    fun getKeyboardSidePaddingDp(context: Context): Int =
        getPrefs(context).getInt(KEY_KEYBOARD_SIDE_PADDING_DP, DEFAULT_KEYBOARD_SIDE_PADDING_DP)
    fun setKeyboardSidePaddingDp(context: Context, v: Int) { getPrefs(context).edit().putInt(KEY_KEYBOARD_SIDE_PADDING_DP, v).apply() }

    // 可换位功能键(CSV)：P0 emoji、P1 123、P2 逗号、P4 中；空格/回车结构性不在内。分隔符 ASCII 逗号≠中文「，」
    const val KEY_FUNCTION_KEYS = "keyboard_function_keys"
    const val DEFAULT_FUNCTION_KEYS = "emoji,mode_change,，,ime_switch"
    fun getFunctionKeys(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY_FUNCTION_KEYS, DEFAULT_FUNCTION_KEYS) ?: DEFAULT_FUNCTION_KEYS
        return raw.split(",").filter { it.isNotEmpty() }
    }
    fun setFunctionKeys(context: Context, keys: List<String>) { getPrefs(context).edit().putString(KEY_FUNCTION_KEYS, keys.joinToString(",")).apply() }

    // 行与行之间的间距(dp)，默认 6（与原硬编码一致）
    const val KEY_ROW_SPACING_DP = "key_row_spacing_dp"
    fun getRowSpacingDp(context: Context): Int = getPrefs(context).getInt(KEY_ROW_SPACING_DP, 6).coerceIn(0, 24)
    fun setRowSpacingDp(context: Context, v: Int) { getPrefs(context).edit().putInt(KEY_ROW_SPACING_DP, v.coerceIn(0, 24)).apply() }

    // 布局编辑器：预览区是否折叠（折叠后只用测试框看真实键盘）
    const val KEY_EDITOR_PREVIEW_COLLAPSED = "editor_preview_collapsed"
    fun isEditorPreviewCollapsed(context: Context): Boolean = getPrefs(context).getBoolean(KEY_EDITOR_PREVIEW_COLLAPSED, false)
    fun setEditorPreviewCollapsed(context: Context, v: Boolean) { getPrefs(context).edit().putBoolean(KEY_EDITOR_PREVIEW_COLLAPSED, v).apply() }

    // 底排控制行左侧功能键数：2（默认，与作者一致）或 3（接近 Gboard：?123 ， 语音）
    const val KEY_BOTTOM_LEFT_COUNT = "keyboard_bottom_left_count"
    fun getBottomLeftCount(context: Context): Int = getPrefs(context).getInt(KEY_BOTTOM_LEFT_COUNT, 2).coerceIn(2, 3)
    fun setBottomLeftCount(context: Context, v: Int) { getPrefs(context).edit().putInt(KEY_BOTTOM_LEFT_COUNT, v).apply() }

    const val KEY_NUMBER_ROW_ENABLED = "keyboard_number_row_enabled"
    fun isNumberRowEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_NUMBER_ROW_ENABLED, false)
    fun setNumberRowEnabled(context: Context, v: Boolean) { getPrefs(context).edit().putBoolean(KEY_NUMBER_ROW_ENABLED, v).apply() }

    // 字母键独立高/宽：内边距(变小留缝)，独立于整体键盘高度
    const val KEY_H_INSET_DP = "key_h_inset_dp"
    const val KEY_V_INSET_DP = "key_v_inset_dp"
    fun getKeyHInsetDp(context: Context): Int = getPrefs(context).getInt(KEY_H_INSET_DP, 0)
    fun setKeyHInsetDp(context: Context, v: Int) { getPrefs(context).edit().putInt(KEY_H_INSET_DP, v).apply() }
    fun getKeyVInsetDp(context: Context): Int = getPrefs(context).getInt(KEY_V_INSET_DP, 0)
    fun setKeyVInsetDp(context: Context, v: Int) { getPrefs(context).edit().putInt(KEY_V_INSET_DP, v).apply() }

    // 按键外观：阴影开关/强度、圆角。覆盖 xime.yaml 的 keyboard.shadow（未设置时回退到传入的 asset 默认值）
    const val KEY_SHADOW_ENABLED = "key_shadow_enabled"
    const val KEY_SHADOW_ELEVATION_DP = "key_shadow_elevation_dp"
    const val KEY_CORNER_RADIUS_DP = "key_corner_radius_dp"
    fun isKeyShadowEnabled(context: Context, default: Boolean): Boolean =
        getPrefs(context).getBoolean(KEY_SHADOW_ENABLED, default)
    fun setKeyShadowEnabled(context: Context, v: Boolean) { getPrefs(context).edit().putBoolean(KEY_SHADOW_ENABLED, v).apply() }
    fun getKeyShadowElevationDp(context: Context, default: Int): Int =
        getPrefs(context).getInt(KEY_SHADOW_ELEVATION_DP, default)
    fun setKeyShadowElevationDp(context: Context, v: Int) { getPrefs(context).edit().putInt(KEY_SHADOW_ELEVATION_DP, v).apply() }
    fun getKeyCornerRadiusDp(context: Context, default: Int): Int =
        getPrefs(context).getInt(KEY_CORNER_RADIUS_DP, default)
    fun setKeyCornerRadiusDp(context: Context, v: Int) { getPrefs(context).edit().putInt(KEY_CORNER_RADIUS_DP, v).apply() }

    // 字根代替键面字母：仓颉/速成下键面主字显示字根(日/月/金…)
    const val KEY_RADICAL_AS_LABEL = "key_radical_as_label"
    fun isRadicalAsLabel(context: Context): Boolean = getPrefs(context).getBoolean(KEY_RADICAL_AS_LABEL, false)
    fun setRadicalAsLabel(context: Context, v: Boolean) { getPrefs(context).edit().putBoolean(KEY_RADICAL_AS_LABEL, v).apply() }

    fun getOrientationDefaultKeyboardHeightDp(context: Context, isLandscape: Boolean): Int {
        val key = if (isLandscape) KEY_KEYBOARD_HEIGHT_DP_LANDSCAPE else KEY_KEYBOARD_HEIGHT_DP
        return getPrefs(context).getInt(key, DEFAULT_KEYBOARD_HEIGHT_DP)
    }
    
    fun getKeyboardBottomPaddingDp(context: Context): Int {
        return getPrefs(context).getInt(KEY_KEYBOARD_BOTTOM_PADDING_DP, DEFAULT_KEYBOARD_BOTTOM_PADDING_DP)
    }
    
    fun setKeyboardBottomPaddingDp(context: Context, paddingDp: Int) {
        getPrefs(context).edit().putInt(KEY_KEYBOARD_BOTTOM_PADDING_DP, paddingDp).apply()
    }
    
    fun getDefaultKeyboardBottomPaddingDp(): Int = DEFAULT_KEYBOARD_BOTTOM_PADDING_DP

    fun getWebDavUrl(context: Context): String {
        return getPrefs(context).getString(KEY_WEBDAV_URL, "") ?: ""
    }

    fun setWebDavUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_WEBDAV_URL, url).apply()
    }

    fun getWebDavUsername(context: Context): String {
        return getPrefs(context).getString(KEY_WEBDAV_USERNAME, "") ?: ""
    }

    fun setWebDavUsername(context: Context, username: String) {
        getPrefs(context).edit().putString(KEY_WEBDAV_USERNAME, username).apply()
    }

    fun getWebDavPassword(context: Context): String {
        return getPrefs(context).getString(KEY_WEBDAV_PASSWORD, "") ?: ""
    }

    fun setWebDavPassword(context: Context, password: String) {
        getPrefs(context).edit().putString(KEY_WEBDAV_PASSWORD, password).apply()
    }

    fun getWebDavPath(context: Context): String {
        return getPrefs(context).getString(KEY_WEBDAV_PATH, "xime") ?: "xime"
    }

    fun setWebDavPath(context: Context, path: String) {
        getPrefs(context).edit().putString(KEY_WEBDAV_PATH, path).apply()
    }

    fun isSchemaImportWarningDismissed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SCHEMA_IMPORT_WARNING_DISMISSED, false)
    }

    fun setSchemaImportWarningDismissed(context: Context, dismissed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SCHEMA_IMPORT_WARNING_DISMISSED, dismissed).apply()
    }

    fun getPageSize(context: Context): Int {
        return getPrefs(context).getInt(KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE)
    }

    fun setPageSize(context: Context, pageSize: Int) {
        getPrefs(context).edit().putInt(KEY_PAGE_SIZE, pageSize).apply()
    }

    // ── 方案市场「已安装」的持久记录 ──
    // 记录用户通过市场主动安装过的方案 id；与本地文件存在性解耦（方案可能仅作为依赖落盘，
    // 文件存在不代表用户装过它），且跨重启保持。
    fun getInstalledMarketIds(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_INSTALLED_MARKET_IDS, emptySet())?.toSet() ?: emptySet()

    fun addInstalledMarketId(context: Context, id: String) {
        val cur = getInstalledMarketIds(context).toMutableSet()
        if (cur.add(id)) {
            getPrefs(context).edit().putStringSet(KEY_INSTALLED_MARKET_IDS, cur).apply()
        }
    }

    fun removeInstalledMarketId(context: Context, id: String) {
        val cur = getInstalledMarketIds(context).toMutableSet()
        if (cur.remove(id)) {
            getPrefs(context).edit().putStringSet(KEY_INSTALLED_MARKET_IDS, cur).apply()
        }
    }
}