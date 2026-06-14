package com.kingzcheung.xime.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.twotone.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingzcheung.xime.settings.KeysConfigHelper
import com.kingzcheung.xime.settings.SettingsPreferences
import com.kingzcheung.xime.ui.CANGJIE_RADICALS
import com.kingzcheung.xime.ui.FunctionKey
import com.kingzcheung.xime.ui.KeyboardLayout
import com.kingzcheung.xime.ui.LocalKeyInsetH
import com.kingzcheung.xime.ui.LocalKeyInsetV
import com.kingzcheung.xime.ui.LocalKeyboardSidePadding
import com.kingzcheung.xime.ui.SettingsSection
import com.kingzcheung.xime.ui.SettingsToggleItem
import com.kingzcheung.xime.ui.SwipeState

internal val PRESET_FUNCTION_TOKENS = listOf(
    "emoji", "mode_change", "ime_switch", "shift", "voice", "delete", "，", "。", "、", "."
)

internal fun functionTokenShortLabel(token: String): String = when (token) {
    "emoji" -> "表情"
    "mode_change" -> "数字"
    "ime_switch" -> "中英"
    "shift" -> "Shift"
    "voice" -> "语音"
    "delete" -> "删除"
    "，" -> "逗号"
    "。" -> "句号"
    "、" -> "顿号"
    "." -> "句点"
    else -> token
}

// 键帽三段标注示意用的字母（仓颉「日」字根明显）
private const val SAMPLE_KEY = "a"

// 功能键位：slot 索引 → (位置名, 默认 token)
private val FUNCTION_POSITIONS = listOf(
    Triple(0, "表情位", "emoji"),
    Triple(1, "左1", "mode_change"),
    Triple(2, "左2", "，"),
    Triple(4, "左3", "voice"),
    Triple(3, "右1", "ime_switch"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shadowDefault = remember { KeysConfigHelper.getKeyboardShadow() }
    // 高度/底距范围与工具栏「调整键盘」(KeyboardResizeOverlay) 保持一致：屏幕相对
    val screenHeightDp = configuration.screenHeightDp
    val minHeightDp: Int
    val maxHeightDp: Int
    if (isLandscape) {
        minHeightDp = screenHeightDp / 2
        maxHeightDp = ((screenHeightDp * 3) / 5).coerceAtLeast(minHeightDp + 1)
    } else {
        minHeightDp = 290.coerceAtMost(screenHeightDp / 2)
        maxHeightDp = (screenHeightDp / 2).coerceAtLeast(minHeightDp + 1)
    }
    val minBottomPaddingDp = -40
    val maxBottomPaddingDp = (maxHeightDp - minHeightDp).coerceAtLeast(1)

    var heightDp by remember { mutableStateOf(SettingsPreferences.getKeyboardHeightDp(context, isLandscape)) }
    var bottomPaddingDp by remember { mutableStateOf(SettingsPreferences.getKeyboardBottomPaddingDp(context)) }
    var sidePaddingDp by remember { mutableStateOf(SettingsPreferences.getKeyboardSidePaddingDp(context)) }
    var keyInsetH by remember { mutableStateOf(SettingsPreferences.getKeyHInsetDp(context)) }
    var keyInsetV by remember { mutableStateOf(SettingsPreferences.getKeyVInsetDp(context)) }
    var cornerRadius by remember { mutableStateOf(SettingsPreferences.getKeyCornerRadiusDp(context, shadowDefault.shapeRadius)) }
    var shadowEnabled by remember { mutableStateOf(SettingsPreferences.isKeyShadowEnabled(context, shadowDefault.enabled)) }
    var shadowElevation by remember { mutableStateOf(SettingsPreferences.getKeyShadowElevationDp(context, shadowDefault.elevation)) }
    var functionKeys by remember { mutableStateOf(SettingsPreferences.getFunctionKeys(context)) }
    var numberRowEnabled by remember { mutableStateOf(SettingsPreferences.isNumberRowEnabled(context)) }
    var showBottomButtons by remember { mutableStateOf(SettingsPreferences.showBottomButtons(context)) }
    // 键帽三段标注：上=上滑提示(符号) / 中=键面主字(字母·字根) / 下=下滑提示(隐藏·功能·字根)
    var topMode by remember { mutableStateOf(if (SettingsPreferences.isSwipeUpHintsEnabled(context)) 1 else 0) }
    var midMode by remember { mutableStateOf(if (SettingsPreferences.isRadicalAsLabel(context)) 1 else 0) }
    var bottomMode by remember { mutableStateOf(SettingsPreferences.getSwipeDownMode(context)) }
    var bottomLeftCount by remember { mutableStateOf(SettingsPreferences.getBottomLeftCount(context)) }
    var showComments by remember { mutableStateOf(SettingsPreferences.showCandidateComments(context)) }
    var pageSize by remember { mutableStateOf(SettingsPreferences.getPageSize(context).let { if (it == 0) 20 else it }) }
    var selectedSlot by remember { mutableStateOf(1) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var testText by remember { mutableStateOf("") }
    var previewCollapsed by remember { mutableStateOf(SettingsPreferences.isEditorPreviewCollapsed(context)) }

    fun assign(token: String) {
        if (selectedSlot < 0) return
        val defaults = listOf("emoji", "mode_change", "，", "ime_switch", "voice")
        val updated = functionKeys.toMutableList()
        while (updated.size <= selectedSlot) updated.add(defaults.getOrElse(updated.size) { "，" })
        updated[selectedSlot] = token
        functionKeys = updated
        SettingsPreferences.setFunctionKeys(context, updated)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("布局与显示", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            ),
            windowInsets = WindowInsets(0.dp)
        )

        // 悬浮区：内嵌真实键盘做实时预览（固定，不随下方滚动）。下方所有调节都会即时反映在这里。
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "实时预览（示意）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        previewCollapsed = !previewCollapsed
                        SettingsPreferences.setEditorPreviewCollapsed(context, previewCollapsed)
                    }) {
                        Icon(
                            imageVector = if (previewCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                            contentDescription = if (previewCollapsed) "展开预览" else "折叠预览",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (previewCollapsed) {
                    Text(
                        "预览已折叠 — 点下方「测试输入框」用真实键盘查看实际效果。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                } else {
                    RealKeyboardPreview(
                        heightDp = heightDp,
                        sidePaddingDp = sidePaddingDp,
                        keyInsetH = keyInsetH,
                        keyInsetV = keyInsetV,
                        cornerRadius = cornerRadius,
                        shadowEnabled = shadowEnabled,
                        shadowElevation = shadowElevation
                    )
                    Text(
                        "示意预览仅供参考；实际高度受键盘尺寸调整等影响，精确效果请用下方测试框验证。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                // 测试输入框：点此用真实键盘输入，逐键检验实际手感
                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    label = { Text("👇 在此输入，实地测试键盘") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )
            }
            item {
                SettingsSection(title = "按键大小") {
                    SliderRow(
                        title = "键盘高度", subtitle = "范围与工具栏「调整键盘」一致",
                        valueLabel = "${heightDp}dp",
                        value = heightDp.toFloat().coerceIn(minHeightDp.toFloat(), maxHeightDp.toFloat()),
                        valueRange = minHeightDp.toFloat()..maxHeightDp.toFloat(),
                        onValueChange = { heightDp = it.toInt() },
                        onValueChangeFinished = { SettingsPreferences.setKeyboardHeightDp(context, heightDp, isLandscape) }
                    )
                    SliderRow(
                        title = "底部边距", subtitle = "与工具栏「调整键盘」一致；负值缩减底部空白",
                        valueLabel = "${bottomPaddingDp}dp",
                        value = bottomPaddingDp.toFloat().coerceIn(minBottomPaddingDp.toFloat(), maxBottomPaddingDp.toFloat()),
                        valueRange = minBottomPaddingDp.toFloat()..maxBottomPaddingDp.toFloat(),
                        onValueChange = { bottomPaddingDp = it.toInt() },
                        onValueChangeFinished = { SettingsPreferences.setKeyboardBottomPaddingDp(context, bottomPaddingDp) }
                    )
                    SliderRow(
                        title = "按键宽度（左右边距）", subtitle = "边距越大，按键越窄",
                        valueLabel = "${sidePaddingDp}dp",
                        value = sidePaddingDp.toFloat(), valueRange = 0f..48f,
                        onValueChange = { sidePaddingDp = it.toInt() },
                        onValueChangeFinished = { SettingsPreferences.setKeyboardSidePaddingDp(context, sidePaddingDp) }
                    )
                    SliderRow(
                        title = "按键背景宽度（横向留白）", subtitle = "留白越大，按键背景越窄",
                        valueLabel = "${keyInsetH}dp",
                        value = keyInsetH.toFloat(), valueRange = 0f..16f,
                        onValueChange = { keyInsetH = it.toInt() },
                        onValueChangeFinished = { SettingsPreferences.setKeyHInsetDp(context, keyInsetH) }
                    )
                    SliderRow(
                        title = "按键背景高度（纵向留白）", subtitle = "留白越大，按键背景越矮",
                        valueLabel = "${keyInsetV}dp",
                        value = keyInsetV.toFloat(), valueRange = 0f..16f,
                        onValueChange = { keyInsetV = it.toInt() },
                        onValueChangeFinished = { SettingsPreferences.setKeyVInsetDp(context, keyInsetV) }
                    )
                }
            }
            item {
                SettingsSection(title = "按键外观") {
                    SliderRow(
                        title = "圆角", valueLabel = "${cornerRadius}dp",
                        value = cornerRadius.toFloat(), valueRange = 0f..24f,
                        onValueChange = { cornerRadius = it.toInt() },
                        onValueChangeFinished = { SettingsPreferences.setKeyCornerRadiusDp(context, cornerRadius) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Filled.Dialpad, title = "按键阴影",
                        subtitle = "关闭后按键扁平无投影",
                        checked = shadowEnabled,
                        onCheckedChange = {
                            shadowEnabled = it
                            SettingsPreferences.setKeyShadowEnabled(context, it)
                        }
                    )
                    SliderRow(
                        title = "阴影强度", subtitle = if (shadowEnabled) null else "需先开启「按键阴影」",
                        valueLabel = "${shadowElevation}dp",
                        value = shadowElevation.toFloat(), valueRange = 0f..6f,
                        onValueChange = { shadowElevation = it.toInt() },
                        onValueChangeFinished = { SettingsPreferences.setKeyShadowElevationDp(context, shadowElevation) }
                    )
                }
            }
            item {
                SettingsSection(title = "显示选项") {
                    SettingsToggleItem(
                        icon = Icons.Filled.Dialpad, title = "数字行",
                        subtitle = "在字母键上方显示一行 1-0（中英文键盘同步）",
                        checked = numberRowEnabled,
                        onCheckedChange = {
                            numberRowEnabled = it
                            SettingsPreferences.setNumberRowEnabled(context, it)
                        }
                    )
                    SettingsToggleItem(
                        icon = Icons.TwoTone.Straighten, title = "显示底部按钮",
                        subtitle = "显示收回键盘和切换输入法按钮（部分系统自带）",
                        checked = showBottomButtons,
                        onCheckedChange = {
                            showBottomButtons = it
                            SettingsPreferences.setShowBottomButtons(context, it)
                        }
                    )
                }
            }
            item {
                SettingsSection(title = "底排控制行") {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        SegmentedRow(
                            label = "左侧功能键数（接近 Gboard）",
                            options = listOf("2 键", "3 键"),
                            selectedIndex = (bottomLeftCount - 2).coerceIn(0, 1),
                            onSelect = { bottomLeftCount = it + 2; SettingsPreferences.setBottomLeftCount(context, it + 2) }
                        )
                    }
                    Text(
                        "选「3 键」时空格左侧多一个键（默认语音）。逗号长按出表情、句号长按出更多符号。默认与原版一致（2 键）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                }
            }
            item {
                SettingsSection(title = "功能键位（位置 → 功能）") {
                    FunctionKeyEditor(
                        functionTokens = functionKeys,
                        bottomLeftCount = bottomLeftCount,
                        selectedSlot = selectedSlot,
                        onSelectSlot = { selectedSlot = it },
                        onPick = { assign(it) },
                        onCustom = { showCustomDialog = true }
                    )
                }
            }
            item {
                SettingsSection(title = "键帽标注（上 · 中 · 下）") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        KeycapAnnotationSample(
                            top = if (topMode == 1) (KeysConfigHelper.getSwipeUpText(SAMPLE_KEY)?.ifBlank { "1" } ?: "1") else null,
                            middle = if (midMode == 1) (CANGJIE_RADICALS[SAMPLE_KEY] ?: "A") else "A",
                            bottom = when {
                                midMode == 1 -> null
                                bottomMode == 2 -> CANGJIE_RADICALS[SAMPLE_KEY]
                                else -> null
                            }
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SegmentedRow(
                                label = "上 · 上滑提示",
                                options = listOf("隐藏", "符号"),
                                selectedIndex = topMode,
                                onSelect = { topMode = it; SettingsPreferences.setSwipeUpHintsEnabled(context, it == 1) }
                            )
                            SegmentedRow(
                                label = "中 · 键面主字",
                                options = listOf("字母", "字根"),
                                selectedIndex = midMode,
                                onSelect = { midMode = it; SettingsPreferences.setRadicalAsLabel(context, it == 1) }
                            )
                            SegmentedRow(
                                label = "下 · 下滑提示",
                                options = listOf("隐藏", "字根"),
                                selectedIndex = if (bottomMode == 0) 0 else 1,
                                onSelect = { val m = if (it == 1) 2 else 0; bottomMode = m; SettingsPreferences.setSwipeDownMode(context, m) }
                            )
                        }
                    }
                    Text(
                        "「字根」：仓颉/速成在键面静态显示字根（如 A→日）；五笔等以下滑气泡显示字根。中选「字根」时下方不再重复。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                }
            }
            item {
                SettingsSection(title = "候选词") {
                    SettingsToggleItem(
                        icon = Icons.Filled.Code, title = "显示编码注释",
                        subtitle = "在候选词旁显示对应的编码（如五笔字根）",
                        checked = showComments,
                        onCheckedChange = {
                            showComments = it
                            SettingsPreferences.setShowCandidateComments(context, it)
                        }
                    )
                    SliderRow(
                        title = "每页候选词数", subtitle = "修改后需到方案设置中点击部署才生效",
                        valueLabel = "${pageSize} 个",
                        value = pageSize.toFloat(), valueRange = 20f..50f,
                        onValueChange = { pageSize = it.toInt() },
                        onValueChangeFinished = { SettingsPreferences.setPageSize(context, pageSize) }
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showCustomDialog) {
        CustomCharDialog(
            onConfirm = { ch ->
                if (ch.isNotEmpty()) assign(ch)
                showCustomDialog = false
            },
            onDismiss = { showCustomDialog = false }
        )
    }
}

/** 内嵌真实 KeyboardLayout 作为实时预览：完全复用线上渲染，所见即所得。点按不触发输入。 */
@Composable
private fun RealKeyboardPreview(
    heightDp: Int,
    sidePaddingDp: Int,
    keyInsetH: Int,
    keyInsetV: Int,
    cornerRadius: Int,
    shadowEnabled: Boolean,
    shadowElevation: Int
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val cfgVer by KeysConfigHelper.configVersion.collectAsState()
    val kbColors = remember(isDark, cfgVer) { KeysConfigHelper.getKeyboardColors() }
    fun c(l: Long) = Color(0xFF000000 or l)
    val keyboardBg = if (isDark) c(kbColors.keyboardBgColorDark) else c(kbColors.keyboardBgColor)
    val keyBg = if (isDark) c(kbColors.keyBgColorDark) else c(kbColors.keyBgColor)
    val keyText = if (isDark) c(kbColors.keyTextColorDark) else c(kbColors.keyTextColor)
    val specialBg = (if (isDark) kbColors.specialKeyBgColorDark else kbColors.specialKeyBgColor)
        ?.let { c(it) } ?: MaterialTheme.colorScheme.secondaryContainer
    val schemaId = remember { SettingsPreferences.getCurrentSchema(context) }
    val previewH = heightDp.coerceIn(200, 300)
    CompositionLocalProvider(
        LocalKeyInsetH provides keyInsetH,
        LocalKeyInsetV provides keyInsetV,
        LocalKeyboardSidePadding provides sidePaddingDp
    ) {
        KeyboardLayout(
            onKeyPress = {},
            isShifted = false,
            isLandscape = false,
            schemaName = "预览",
            currentSchemaId = schemaId,
            isDarkTheme = isDark,
            keyBackgroundColor = keyBg,
            keyTextColor = keyText,
            specialKeyBackgroundColor = specialBg,
            keyboardBackgroundColor = keyboardBg,
            shadowEnabled = shadowEnabled,
            shadowElevation = shadowElevation.dp,
            shadowShapeRadius = cornerRadius.dp,
            isSttEnabled = false,
            modifier = Modifier.fillMaxWidth().height(previewH.dp)
        )
    }
}

/** 功能键位编辑：先选位置（含 2 键时也可预配置的「左3」），再从两排里选功能。 */
@Composable
private fun FunctionKeyEditor(
    functionTokens: List<String>,
    bottomLeftCount: Int,
    selectedSlot: Int,
    onSelectSlot: (Int) -> Unit,
    onPick: (String) -> Unit,
    onCustom: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("① 选位置", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FUNCTION_POSITIONS.forEach { (slot, name, default) ->
                val cur = functionTokens.getOrElse(slot) { default }
                val hint = if (slot == 4 && bottomLeftCount < 3) "$name(需3键)" else name
                PositionChip(
                    posName = hint,
                    curLabel = functionTokenShortLabel(cur),
                    selected = slot == selectedSlot,
                    onClick = { onSelectSlot(slot) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("② 选功能", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        val current = functionTokens.getOrElse(selectedSlot) { "" }
        TwoRowPicker(current = current, onPick = onPick, onCustom = onCustom)
    }
}

@Composable
private fun PositionChip(posName: String, curLabel: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            posName, fontSize = 10.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            curLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** 可选功能：分两排展示。 */
@Composable
private fun TwoRowPicker(current: String, onPick: (String) -> Unit, onCustom: () -> Unit) {
    val rows = PRESET_FUNCTION_TOKENS.chunked((PRESET_FUNCTION_TOKENS.size + 1) / 2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { ri, row ->
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { token -> PickerChip(token = token, selected = token == current, onClick = { onPick(token) }) }
                if (ri == rows.lastIndex) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable(onClick = onCustom),
                            contentAlignment = Alignment.Center
                        ) { Text("字+", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) }
                        Text("自定义", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerChip(token: String, selected: Boolean, onClick: () -> Unit) {
    val keyBg = MaterialTheme.colorScheme.surface
    val specialBg = MaterialTheme.colorScheme.secondaryContainer
    val keyText = MaterialTheme.colorScheme.onSurface
    val noSwipe: (SwipeState, Rect) -> Unit = { _, _ -> }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp))
                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
        ) {
            FunctionKey(
                token = token, modifier = Modifier.fillMaxSize(),
                onKeyPress = { onClick() }, onKeyPressDown = null,
                keyBackgroundColor = keyBg, specialKeyBackgroundColor = specialBg,
                keyTextColor = keyText, onSwipeStateChange = noSwipe
            )
        }
        Text(functionTokenShortLabel(token), fontSize = 10.sp, color = keyText)
    }
}

/** 单个示意键帽：上(符号)/中(字母·字根)/下(功能·字根)三段实时显示。 */
@Composable
private fun KeycapAnnotationSample(top: String?, middle: String, bottom: String?) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        if (top != null) {
            Text(
                top, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 7.dp)
            )
        }
        Text(
            middle, fontSize = 24.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.Center)
        )
        if (bottom != null) {
            Text(
                bottom, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp)
            )
        }
    }
}

/** 多选一分段控件。 */
@Composable
private fun SegmentedRow(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, opt ->
                val sel = i == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelect(i) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        opt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    subtitle: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = valueLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, onValueChangeFinished = onValueChangeFinished)
    }
}

@Composable
private fun CustomCharDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var customText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义字符") },
        text = {
            OutlinedTextField(
                value = customText,
                onValueChange = { customText = it.take(2) },
                label = { Text("输入要上屏的字符") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(customText) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
