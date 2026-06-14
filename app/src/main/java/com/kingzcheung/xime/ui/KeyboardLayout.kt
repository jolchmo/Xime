package com.kingzcheung.xime.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.twotone.EmojiEmotions
import androidx.compose.material.icons.twotone.KeyboardCapslock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.kingzcheung.xime.ui.LocalStretchFactor
import com.kingzcheung.xime.settings.SettingsPreferences
import com.kingzcheung.xime.settings.KeysConfigHelper
import com.kingzcheung.xime.keyboard.GestureAction
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingzcheung.xime.util.PermissionHelper
import com.kingzcheung.xime.util.CharInfo
import com.kingzcheung.xime.util.SubcharHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.TextUnit

/** 标准仓颉字根（A-Y）。仓颉与速成（简化仓颉）共用。 */
internal val CANGJIE_RADICALS = mapOf(
    "a" to "日", "b" to "月", "c" to "金", "d" to "木", "e" to "水", "f" to "火",
    "g" to "土", "h" to "竹", "i" to "戈", "j" to "十", "k" to "大", "l" to "中",
    "m" to "一", "n" to "弓", "o" to "人", "p" to "心", "q" to "手", "r" to "口",
    "s" to "尸", "t" to "廿", "u" to "山", "v" to "女", "w" to "田", "x" to "難",
    "y" to "卜"
)

/** 当前方案是否仓颉/速成系（需显示仓颉字根）。 */
internal fun isCangjieFamily(schemaId: String): Boolean {
    val s = schemaId.lowercase()
    return s.contains("cangjie") || s.contains("quick") || s.contains("sucheng") ||
        schemaId.contains("仓颉") || schemaId.contains("速成")
}

/** 数字行：1-0，开启「数字行」时渲染在 QWERTY 上方。中英文共用，透传阴影。 */
@Composable
internal fun NumberRow(
    onKeyPress: (String) -> Unit,
    onKeyPressDown: ((String) -> Unit)?,
    keyBackgroundColor: Color,
    keyTextColor: Color,
    shadowEnabled: Boolean = true,
    shadowElevation: Dp = 1.dp,
    shadowShapeRadius: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { d ->
            KeyButton(
                text = d, onClick = { onKeyPress(d) },
                backgroundColor = keyBackgroundColor, textColor = keyTextColor,
                modifier = Modifier.weight(1f), onPress = { onKeyPressDown?.invoke(d) },
                shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
            )
        }
    }
}

/**
 * 可换位功能键：按 token 渲染（emoji/mode_change(?123)/ime_switch(中·英)/shift/voice/delete/标点/自定义），
 * 透传阴影参数以与维护者按键一致；mode_change 保留长按切符号。
 */
@Composable
internal fun FunctionKey(
    token: String,
    modifier: Modifier,
    onKeyPress: (String) -> Unit,
    onKeyPressDown: ((String) -> Unit)?,
    keyBackgroundColor: Color,
    specialKeyBackgroundColor: Color,
    keyTextColor: Color,
    onSwipeStateChange: (SwipeState, Rect) -> Unit,
    isShifted: Boolean = false,
    isEnglish: Boolean = false,
    shadowEnabled: Boolean = true,
    shadowElevation: Dp = 1.dp,
    shadowShapeRadius: Dp = 8.dp
) {
    when (token) {
        "emoji" -> IconKeyButton(
            icon = rememberVectorPainter(Icons.TwoTone.EmojiEmotions),
            onClick = { onKeyPress("emoji") }, backgroundColor = specialKeyBackgroundColor,
            iconColor = keyTextColor, modifier = modifier, onPress = { onKeyPressDown?.invoke("emoji") },
            shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
        )

        "mode_change" -> KeyButton(
            text = "?123", onClick = { onKeyPress("mode_change") },
            onLongClick = { onKeyPress("mode_change_symbol") },
            backgroundColor = specialKeyBackgroundColor, textColor = keyTextColor, modifier = modifier,
            onPress = { onKeyPressDown?.invoke("mode_change") },
            shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
        )

        "ime_switch" -> KeyButton(
            text = if (isEnglish) "英" else "中", onClick = { onKeyPress("ime_switch") },
            backgroundColor = specialKeyBackgroundColor, textColor = keyTextColor, modifier = modifier,
            onPress = { onKeyPressDown?.invoke("ime_switch") },
            shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
        )

        "shift" -> IconKeyButton(
            icon = rememberVectorPainter(Icons.TwoTone.KeyboardCapslock),
            onClick = { onKeyPress("shift") }, backgroundColor = specialKeyBackgroundColor,
            iconColor = keyTextColor, modifier = modifier, isHighlighted = isShifted,
            onPress = { onKeyPressDown?.invoke("shift") },
            shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
        )

        "voice" -> IconKeyButton(
            icon = rememberVectorPainter(Icons.Default.Mic),
            onClick = { onKeyPress("voice") }, backgroundColor = specialKeyBackgroundColor,
            iconColor = keyTextColor, modifier = modifier, onPress = { onKeyPressDown?.invoke("voice") },
            shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
        )

        "delete" -> IconKeyButton(
            icon = rememberVectorPainter(Icons.AutoMirrored.Filled.Backspace),
            onClick = { onKeyPress("delete") }, backgroundColor = specialKeyBackgroundColor,
            iconColor = keyTextColor, modifier = modifier, onPress = { onKeyPressDown?.invoke("delete") },
            shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
        )

        "，", "。", "、" -> {
            // 逗号长按 → 表情；句号长按 → 更多符号（接近 Gboard）
            val punctLongClick: (() -> Unit)? = when (token) {
                "，" -> { { onKeyPress("emoji") } }
                "。" -> { { onKeyPress("mode_change_symbol") } }
                else -> null
            }
            SwipeableKeyButton(
                text = token, onClick = { onKeyPress(token) },
                backgroundColor = keyBackgroundColor, textColor = keyTextColor, modifier = modifier,
                swipeText = if (token == "，") "。" else "，",
                onSwipe = { swiped -> onKeyPress(swiped) }, onSwipeStateChange = onSwipeStateChange,
                onPress = { onKeyPressDown?.invoke(token) },
                onLongClick = punctLongClick,
                shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
            )
        }

        else -> KeyButton(
            text = token, onClick = { onKeyPress(token) },
            backgroundColor = keyBackgroundColor, textColor = keyTextColor, modifier = modifier,
            onPress = { onKeyPressDown?.invoke(token) },
            shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
        )
    }
}

@Composable
fun KeyboardLayout(
    onKeyPress: (String) -> Unit,
    isShifted: Boolean,
    isLandscape: Boolean = false,
    schemaName: String = "",
    enterKeyText: String = "发送",
    currentSchemaId: String = "",
    isDarkTheme: Boolean = false,
    keyBackgroundColor: Color,
    keyTextColor: Color,
    specialKeyBackgroundColor: Color,
    keyboardBackgroundColor: Color = Color.Transparent,
    shadowEnabled: Boolean = true,
    shadowElevation: Dp = 1.dp,
    shadowShapeRadius: Dp = 8.dp,
    onVoiceModeChange: ((Boolean) -> Unit)? = null,
    onCommitText: ((String) -> Unit)? = null,
    isSttEnabled: Boolean = true,
    isVoiceMode: Boolean = false,
    modifier: Modifier = Modifier,
    onKeyPressDown: ((String) -> Unit)? = null,
    onCursorMove: ((Int) -> Unit)? = null,
    onGestureAction: ((GestureAction, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var swipeUpHintsEnabled by remember {
        mutableStateOf(
            SettingsPreferences.isSwipeUpHintsEnabled(
                context
            )
        )
    }
    var swipeDownHintsEnabled by remember {
        mutableStateOf(SettingsPreferences.getSwipeDownMode(context) != 0)
    }
    // 下滑提示三态：true=字根优先(mode 2)；false=强制功能(mode 1)。隐藏由 swipeDownHintsEnabled=false 表示。
    var swipeDownRadicalEnabled by remember {
        mutableStateOf(SettingsPreferences.getSwipeDownMode(context) == 2)
    }
    // 布局编辑器：键宽边距 / 功能键 / 数字行 / 字根代替字母 / 底排左侧键数
    var sidePaddingDp by remember { mutableStateOf(SettingsPreferences.getKeyboardSidePaddingDp(context)) }
    var functionTokens by remember { mutableStateOf(SettingsPreferences.getFunctionKeys(context)) }
    var numberRowEnabled by remember { mutableStateOf(SettingsPreferences.isNumberRowEnabled(context)) }
    var radicalAsLabel by remember { mutableStateOf(SettingsPreferences.isRadicalAsLabel(context)) }
    var bottomLeftCount by remember { mutableStateOf(SettingsPreferences.getBottomLeftCount(context)) }

    // 监听设置变化
    DisposableEffect(context) {
        val prefs = SettingsPreferences.getPrefsPublic(context)
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    SettingsPreferences.KEY_SWIPE_UP_HINTS_ENABLED ->
                        swipeUpHintsEnabled = SettingsPreferences.isSwipeUpHintsEnabled(context)

                    SettingsPreferences.KEY_SWIPE_DOWN_HINTS_ENABLED,
                    SettingsPreferences.KEY_SWIPE_DOWN_MODE -> {
                        swipeDownHintsEnabled = SettingsPreferences.getSwipeDownMode(context) != 0
                        swipeDownRadicalEnabled = SettingsPreferences.getSwipeDownMode(context) == 2
                    }

                    SettingsPreferences.KEY_BOTTOM_LEFT_COUNT ->
                        bottomLeftCount = SettingsPreferences.getBottomLeftCount(context)

                    SettingsPreferences.KEY_KEYBOARD_SIDE_PADDING_DP ->
                        sidePaddingDp = SettingsPreferences.getKeyboardSidePaddingDp(context)
                    SettingsPreferences.KEY_FUNCTION_KEYS ->
                        functionTokens = SettingsPreferences.getFunctionKeys(context)
                    SettingsPreferences.KEY_NUMBER_ROW_ENABLED ->
                        numberRowEnabled = SettingsPreferences.isNumberRowEnabled(context)
                    SettingsPreferences.KEY_RADICAL_AS_LABEL ->
                        radicalAsLabel = SettingsPreferences.isRadicalAsLabel(context)
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LaunchedEffect(Unit) {
        SubcharHelper.init(context)
    }

    var swipeState by remember { mutableStateOf(SwipeState()) }
    var keyboardBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
    var lastKeyBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    // 监听手势配置版本号，部署后强制刷新键帽显示
    val cfgVer by KeysConfigHelper.configVersion.collectAsState()

    fun processSwipeState(state: SwipeState, bounds: Rect) {
        val newState = if (state.isSwipeDown && state.swipeText != null) {
            state.copy(charInfos = SubcharHelper.parseSwipeDownText(state.swipeText))
        } else {
            state
        }
        swipeState = newState

        lastKeyBounds = Rect(
            left = bounds.left - keyboardBounds.left,
            top = bounds.top - keyboardBounds.top,
            right = bounds.right - keyboardBounds.left,
            bottom = bounds.bottom - keyboardBounds.top
        )
    }

    // 仓颉/速成：字根（同时用 id 与显示名判断，市场方案 id 可能不规范）
    val isCangjieSchema = isCangjieFamily(currentSchemaId) || isCangjieFamily(schemaName)
    val cangjieRadicalFor: (String) -> String? = { k -> if (isCangjieSchema) CANGJIE_RADICALS[k.lowercase()] else null }
    val mainLabelFor: (String) -> String? = { k -> if (radicalAsLabel && isCangjieSchema) CANGJIE_RADICALS[k.lowercase()] else null }

    Box(
        modifier = modifier
            .background(keyboardBackgroundColor)
            .onGloballyPositioned { coordinates ->
                keyboardBounds = coordinates.boundsInRoot()
            }
    ) {
        if (isLandscape) {
            LandscapeKeyboardContent(
                onKeyPress = onKeyPress,
                isShifted = isShifted,
                schemaName = schemaName,
                enterKeyText = enterKeyText,
                isDarkTheme = isDarkTheme,
                keyBackgroundColor = keyBackgroundColor,
                keyTextColor = keyTextColor,
                specialKeyBackgroundColor = specialKeyBackgroundColor,
                keyboardBackgroundColor = keyboardBackgroundColor,
                onKeyPressDown = onKeyPressDown,
                onGestureAction = onGestureAction,
                swipeUpHintsEnabled = swipeUpHintsEnabled,
                swipeDownHintsEnabled = swipeDownHintsEnabled,
                onCommitText = onCommitText,
                onSwipeStateChange = { state, bounds -> processSwipeState(state, bounds) },
                shadowEnabled = shadowEnabled,
                shadowElevation = shadowElevation,
                shadowShapeRadius = shadowShapeRadius,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(keyboardBackgroundColor)
                    .padding(vertical = 8.dp, horizontal = (LocalKeyboardSidePadding.current ?: sidePaddingDp).dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 数字行（可选）
                    if (numberRowEnabled && !isVoiceMode) {
                        Box(modifier = Modifier.weight(0.8f)) {
                            NumberRow(
                                onKeyPress = onKeyPress,
                                onKeyPressDown = onKeyPressDown,
                                keyBackgroundColor = keyBackgroundColor,
                                keyTextColor = keyTextColor,
                                shadowEnabled = shadowEnabled,
                                shadowElevation = shadowElevation,
                                shadowShapeRadius = shadowShapeRadius
                            )
                        }
                    }
                    // 第一行
                    if (isVoiceMode) {
                        Box(modifier = Modifier.weight(1f)) {
                            DummyKeyboardRow(
                                keysCount = 10,
                                keyBackgroundColor = keyBackgroundColor.copy(alpha = 0.5f),
                                keyboardBackgroundColor = keyboardBackgroundColor
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            KeyboardRowWithConfig(
                                keys = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                                onKeyPress = onKeyPress,
                                keyBackgroundColor = keyBackgroundColor,
                                keyTextColor = keyTextColor,
                                isShifted = isShifted,
                                keyboardBackgroundColor = keyboardBackgroundColor,
                                onSwipeStateChange = { state, bounds ->
                                    processSwipeState(
                                        state,
                                        bounds
                                    )
                                },
                                onKeyPressDown = onKeyPressDown,
                                swipeDownHintsEnabled = swipeDownHintsEnabled,
                                swipeDownRadicalEnabled = swipeDownRadicalEnabled,
                                swipeUpHintsEnabled = swipeUpHintsEnabled,
                                onCommitText = onCommitText,
                                configVersion = cfgVer,
                                shadowEnabled = shadowEnabled,
                                shadowElevation = shadowElevation,
                                shadowShapeRadius = shadowShapeRadius,
                                radicalFor = cangjieRadicalFor,
                                mainLabelOverride = mainLabelFor,
                            )
                        }
                    }

                    // 第二行
                    if (isVoiceMode) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            DummyKeyboardRow(
                                keysCount = 9,
                                keyBackgroundColor = keyBackgroundColor.copy(alpha = 0.5f),
                                keyboardBackgroundColor = keyboardBackgroundColor
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            KeyboardRowWithConfig(
                                keys = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                                onKeyPress = onKeyPress,
                                keyBackgroundColor = keyBackgroundColor,
                                keyTextColor = keyTextColor,
                                isShifted = isShifted,
                                keyboardBackgroundColor = keyboardBackgroundColor,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onSwipeStateChange = { state, bounds ->
                                    processSwipeState(
                                        state,
                                        bounds
                                    )
                                },
                                onKeyPressDown = onKeyPressDown,
                                swipeDownHintsEnabled = swipeDownHintsEnabled,
                                swipeDownRadicalEnabled = swipeDownRadicalEnabled,
                                swipeUpHintsEnabled = swipeUpHintsEnabled,
                                onCommitText = onCommitText,
                                configVersion = cfgVer,
                                shadowEnabled = shadowEnabled,
                                shadowElevation = shadowElevation,
                                shadowShapeRadius = shadowShapeRadius,
                                radicalFor = cangjieRadicalFor,
                                mainLabelOverride = mainLabelFor,
                            )
                        }
                    }

                    // 第三行
                    if (isVoiceMode) {
                        Box(modifier = Modifier.weight(1f)) {
                            DummyBottomRow(
                                keyBackgroundColor = keyBackgroundColor.copy(alpha = 0.5f),
                                specialKeyBackgroundColor = specialKeyBackgroundColor.copy(alpha = 0.5f),
                                keyboardBackgroundColor = keyboardBackgroundColor
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(keyboardBackgroundColor),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FunctionKey(
                                token = functionTokens.getOrElse(0) { "emoji" },
                                modifier = Modifier.width(40.dp).fillMaxHeight(),
                                onKeyPress = onKeyPress,
                                onKeyPressDown = onKeyPressDown,
                                keyBackgroundColor = keyBackgroundColor,
                                specialKeyBackgroundColor = specialKeyBackgroundColor,
                                keyTextColor = keyTextColor,
                                onSwipeStateChange = { state, bounds -> processSwipeState(state, bounds) },
                                isShifted = isShifted,
                                shadowEnabled = shadowEnabled,
                                shadowElevation = shadowElevation,
                                shadowShapeRadius = shadowShapeRadius
                            )

                            Row(
                                modifier = Modifier
                                    .weight(7f)
                                    .fillMaxHeight()
                                    .background(keyboardBackgroundColor),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val bottomKeys = listOf("z", "x", "c", "v", "b", "n", "m")
                                bottomKeys.forEach { key ->
                                    val rawSwipeUpText = KeysConfigHelper.getSwipeUpText(key)
                                    val swipeUpText =
                                        if (swipeUpHintsEnabled) rawSwipeUpText else null
                                    val swipeDownRaw =
                                        KeysConfigHelper.getKeyGesture(key)?.swipeDown
                                    val swipeDownLabel =
                                        swipeDownRaw?.label?.takeIf { it.isNotEmpty() }
                                    val swipeDownAction = swipeDownRaw?.action
                                    val swipeDownValue = swipeDownRaw?.value
                                    val swipeDownDisplay = swipeDownRaw?.display ?: "key"
                                    val radicalOverride = cangjieRadicalFor(key)
                                    val swipeDownBubbleText =
                                        if (radicalOverride == null && swipeDownDisplay != "key") swipeDownLabel else null
                                    // 仓颉/速成字根静态显示在键面；字根代替模式则字根作主标签、不再小字
                                    val swipeDownStaticLabel = when {
                                        mainLabelFor(key) != null -> null
                                        !swipeDownHintsEnabled -> null
                                        radicalOverride != null && swipeDownRadicalEnabled -> radicalOverride
                                        swipeDownDisplay == "key" -> swipeDownLabel
                                        // 功能模式(swipeDownRadicalEnabled=false)：键面静态显示该键下滑标签
                                        else -> if (swipeDownRadicalEnabled) null else swipeDownLabel
                                    }

                                    val longPressConfig =
                                        KeysConfigHelper.getKeyGesture(key)?.longPress
                                    val longPressDisplay = longPressConfig?.display ?: "key"
                                    val longPressLabels = if (longPressDisplay == "bubble") {
                                        longPressConfig?.values?.map { it.label }
                                            ?.filter { it.isNotEmpty() }?.ifEmpty { null }
                                    } else null
                                    val longPressGestureMap = if (longPressDisplay == "bubble") {
                                        longPressConfig?.values?.associateBy { it.label }
                                    } else null

                                    // 键帽显示文本：字根代替模式显示字根，否则大写字母
                                    val displayText = mainLabelFor(key) ?: key.uppercase()

                                    SwipeableKeyButton(
                                        text = displayText,
                                        onClick = { onKeyPress(key) },
                                        backgroundColor = keyBackgroundColor,
                                        textColor = keyTextColor,
                                        modifier = Modifier.weight(1f),
                                        swipeText = swipeUpText,
                                        swipeDownText = swipeDownBubbleText,
                                        swipeDownKeyLabel = swipeDownStaticLabel,
                                        onSwipe = if (swipeUpText != null) onKeyPress else null,
                                        onSwipeDown = if (swipeDownAction != null && swipeDownHintsEnabled && swipeDownLabel != null) {
                                            val label = swipeDownLabel
                                            { _ ->
                                                if (swipeDownAction == GestureAction.COMMIT) {
                                                    onKeyPress(key)
                                                } else {
                                                    onGestureAction?.invoke(
                                                        swipeDownAction,
                                                        swipeDownValue?.ifEmpty { label } ?: label)
                                                }
                                            }
                                        } else null,
                                        onSwipeStateChange = { state, bounds ->
                                            processSwipeState(
                                                state,
                                                bounds
                                            )
                                        },
                                        onPress = { onKeyPressDown?.invoke(key) },
                                        onLongPressSelect = { selectedLabel ->
                                            val gesture = longPressGestureMap?.get(selectedLabel)
                                            if (gesture != null && gesture.action != GestureAction.COMMIT) {
                                                onGestureAction?.invoke(
                                                    gesture.action!!,
                                                    gesture.value.ifEmpty { selectedLabel })
                                            } else {
                                                (onCommitText ?: onKeyPress)(selectedLabel)
                                            }
                                        },
                                        longPressItems = longPressLabels,
                                        shadowEnabled = shadowEnabled,
                                        shadowElevation = shadowElevation,
                                        shadowShapeRadius = shadowShapeRadius,
                                    )
                                }
                            }

                            SwipeableIconKeyButton(
                                icon = rememberVectorPainter(Icons.AutoMirrored.Filled.Backspace),
                                onClick = { onKeyPress("delete") },
                                backgroundColor = specialKeyBackgroundColor,
                                iconColor = keyTextColor,
                                modifier = Modifier
                                    .width(48.dp)
                                    .fillMaxHeight(),
                                swipeText = "清空",
                                onSwipe = { onKeyPress("clear_composition") },
                                onLongClick = { onKeyPress("delete") },
                                onPress = { onKeyPressDown?.invoke("delete") },
                                swipeUpLabel = "上滑清空",
                                swipeDownLabel = "下滑撤回",
                                onSwipeUp = { onKeyPress("clear_all") },
                                onSwipeDown = { onKeyPress("undo_clear") },
                                onSwipeStateChange = { state, bounds ->
                                    processSwipeState(
                                        state,
                                        bounds
                                    )
                                },
                                shadowEnabled = shadowEnabled,
                                shadowElevation = shadowElevation,
                                shadowShapeRadius = shadowShapeRadius,
                            )
                        }
                    }

                    // 第四行（控制行）- 包含空格键
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(keyboardBackgroundColor),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 123 / 英中 键
                        if (isVoiceMode) {
                            DummyKeyButton(
                                backgroundColor = specialKeyBackgroundColor.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1.2f)
                            )
                            DummyKeyButton(
                                backgroundColor = specialKeyBackgroundColor.copy(alpha = 0.5f),
                                modifier = Modifier.weight(0.8f)
                            )
                        } else {
                            FunctionKey(
                                token = functionTokens.getOrElse(1) { "mode_change" },
                                modifier = Modifier.weight(1.2f),
                                onKeyPress = onKeyPress, onKeyPressDown = onKeyPressDown,
                                keyBackgroundColor = keyBackgroundColor,
                                specialKeyBackgroundColor = specialKeyBackgroundColor,
                                keyTextColor = keyTextColor,
                                onSwipeStateChange = { state, bounds -> processSwipeState(state, bounds) },
                                isShifted = isShifted,
                                shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
                            )

                            FunctionKey(
                                token = functionTokens.getOrElse(2) { "，" },
                                modifier = Modifier.weight(0.8f),
                                onKeyPress = onKeyPress, onKeyPressDown = onKeyPressDown,
                                keyBackgroundColor = keyBackgroundColor,
                                specialKeyBackgroundColor = specialKeyBackgroundColor,
                                keyTextColor = keyTextColor,
                                onSwipeStateChange = { state, bounds -> processSwipeState(state, bounds) },
                                isShifted = isShifted,
                                shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
                            )

                            // 第三个左侧功能键（仅当底排左侧键数=3，接近 Gboard）
                            if (bottomLeftCount >= 3) {
                                FunctionKey(
                                    token = functionTokens.getOrElse(4) { "voice" },
                                    modifier = Modifier.weight(0.8f),
                                    onKeyPress = onKeyPress, onKeyPressDown = onKeyPressDown,
                                    keyBackgroundColor = keyBackgroundColor,
                                    specialKeyBackgroundColor = specialKeyBackgroundColor,
                                    keyTextColor = keyTextColor,
                                    onSwipeStateChange = { state, bounds -> processSwipeState(state, bounds) },
                                    isShifted = isShifted,
                                    shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
                                )
                            }
                        }

                        // 空格键 - 支持左右滑动控制光标、长按语音
                        val currentOnKeyPress by rememberUpdatedState(onKeyPress)
                        val currentOnKeyPressDown by rememberUpdatedState(onKeyPressDown)
                        val currentOnVoiceModeChange by rememberUpdatedState(onVoiceModeChange)
                        val currentOnCursorMove by rememberUpdatedState(onCursorMove)
                        val scope = rememberCoroutineScope()
                        Box(
                            modifier = Modifier
                                .weight(3f)
                                .fillMaxHeight()
                                // 与字母键一致地应用按键背景留白(inset)，否则空格会比其它键大
                                .padding(horizontal = LocalKeyInsetH.current.dp, vertical = LocalKeyInsetV.current.dp)
                                .then(
                                    if (shadowEnabled) Modifier.shadow(shadowElevation, RoundedCornerShape(shadowShapeRadius), ambientColor = Color(0x80000000), spotColor = Color(0x80000000))
                                    else Modifier
                                )
                                .clip(RoundedCornerShape(shadowShapeRadius))
                                .background(keyBackgroundColor)
                                .pointerInput(isSttEnabled) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        currentOnKeyPressDown?.invoke("space")

                                        // 启动长按检测
                                        var longPressTriggered = false
                                        val longPressJob = scope.launch {
                                            delay(400)
                                            longPressTriggered = true

                                            if (isSttEnabled) {
                                                // 检查麦克风权限
                                                if (!PermissionHelper.hasRecordAudioPermission(
                                                        context
                                                    )
                                                ) {
                                                    Toast.makeText(
                                                        context,
                                                        "需要麦克风权限才能使用语音输入",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    PermissionHelper.requestRecordAudioPermission(
                                                        context
                                                    )
                                                } else {
                                                    // 触发语音模式切换，外部状态变化后会显示 VoiceKeyboardLayout
                                                    currentOnVoiceModeChange?.invoke(true)
                                                }
                                            } else {
                                                // STT 关闭：长按空格连续输出空格，手指抬起后 longPressJob.cancel() 会取消此协程
                                                while (true) {
                                                    currentOnKeyPress("space")
                                                    delay(80)
                                                }
                                            }
                                        }

                                        // 跟踪水平滑动控制光标
                                        var isHorizontalSwipe = false
                                        val cursorThreshold = 60f
                                        var totalDx = 0f

                                        // 使用 drag 检测水平滑动，drag 会在手指抬起后自动结束
                                        drag(down.id) { change ->
                                            val dx = change.position.x - down.position.x
                                            val dy = change.position.y - down.position.y
                                            totalDx = dx

                                            // 只要水平位移超过阈值就视为滑动意图，防止误触上屏空格
                                            if (kotlin.math.abs(dx) > cursorThreshold) {
                                                if (!isHorizontalSwipe) {
                                                    isHorizontalSwipe = true
                                                    longPressJob.cancel()
                                                }
                                                // 光标移动需要更严格的条件：水平远大于垂直
                                                if (kotlin.math.abs(dx) > kotlin.math.abs(dy) * 2f) {
                                                    val steps = (dx / cursorThreshold).toInt()
                                                    if (steps != 0) {
                                                        currentOnCursorMove?.invoke(if (steps > 0) 1 else -1)
                                                    }
                                                }
                                            }
                                        }

                                        longPressJob.cancel()

                                        // 非滑动操作视为点击空格
                                        if (!longPressTriggered && !isHorizontalSwipe) {
                                            currentOnKeyPress("space")
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isVoiceMode) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "语音输入",
                                    tint = keyTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = schemaName,
                                    color = keyTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )

                                if (isSttEnabled) {
                                    Icon(
                                        painter = painterResource(com.kingzcheung.xime.R.drawable.voice),
                                        contentDescription = "语音输入",
                                        tint = keyTextColor.copy(alpha = 0.3f),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.BottomStart)
                                            .padding(start = 6.dp, bottom = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = "空格",
                                        color = keyTextColor.copy(alpha = 0.3f),
                                        fontSize = 10.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(start = 6.dp, bottom = 2.dp)
                                    )
                                }
                            }
                        }

                        // 逗号 / 回车 键
                        if (isVoiceMode) {
                            DummyKeyButton(
                                backgroundColor = keyBackgroundColor.copy(alpha = 0.5f),
                                modifier = Modifier.weight(0.8f)
                            )
                            DummyKeyButton(
                                backgroundColor = specialKeyBackgroundColor.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1.2f)
                            )
                        } else {
                            FunctionKey(
                                token = functionTokens.getOrElse(3) { "ime_switch" },
                                modifier = Modifier.weight(0.8f),
                                onKeyPress = onKeyPress, onKeyPressDown = onKeyPressDown,
                                keyBackgroundColor = keyBackgroundColor,
                                specialKeyBackgroundColor = specialKeyBackgroundColor,
                                keyTextColor = keyTextColor,
                                onSwipeStateChange = { state, bounds -> processSwipeState(state, bounds) },
                                isShifted = isShifted,
                                shadowEnabled = shadowEnabled, shadowElevation = shadowElevation, shadowShapeRadius = shadowShapeRadius
                            )

                            KeyButton(
                                text = enterKeyText,
                                onClick = { onKeyPress("enter") },
                                backgroundColor = specialKeyBackgroundColor,
                                textColor = keyTextColor,
                                modifier = Modifier.weight(1.2f),
                                onPress = { onKeyPressDown?.invoke("enter") },
                                shadowEnabled = shadowEnabled,
                                shadowElevation = shadowElevation,
                                shadowShapeRadius = shadowShapeRadius,
                            )
                        }
                    }
                }

            }
        }

        // 语音模式中央麦克风图标
        if (isVoiceMode) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "语音输入",
                    tint = keyTextColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        SwipeBubble(
            swipeState = swipeState,
            keyBounds = lastKeyBounds,
            isDarkTheme = isDarkTheme,
            keyWidth = if (swipeState.isSwiping || swipeState.isPressed) lastKeyBounds.width else 0f,
            keyboardWidth = keyboardBounds.width
        )
    }
}

@Composable
private fun DummyKeyboardRow(
    keysCount: Int,
    keyBackgroundColor: Color,
    keyboardBackgroundColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(keyboardBackgroundColor),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(keysCount) {
            DummyKeyButton(
                backgroundColor = keyBackgroundColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DummyBottomRow(
    keyBackgroundColor: Color,
    specialKeyBackgroundColor: Color,
    keyboardBackgroundColor: Color = Color.Transparent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(keyboardBackgroundColor),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DummyKeyButton(
            backgroundColor = specialKeyBackgroundColor,
            modifier = Modifier.weight(1.2f)
        )
        Row(
            modifier = Modifier
                .weight(7f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(7) {
                DummyKeyButton(
                    backgroundColor = keyBackgroundColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        DummyKeyButton(
            backgroundColor = specialKeyBackgroundColor,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun DummyKeyButton(
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
    )
}

@Composable
fun KeyboardRowWithConfig(
    keys: List<String>,
    onKeyPress: (String) -> Unit,
    keyBackgroundColor: Color,
    keyTextColor: Color,
    isShifted: Boolean,
    keyboardBackgroundColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    onSwipeStateChange: ((SwipeState, Rect) -> Unit)? = null,
    onKeyPressDown: ((String) -> Unit)? = null,
    swipeDownHintsEnabled: Boolean = true,
    swipeDownRadicalEnabled: Boolean = true,
    swipeUpHintsEnabled: Boolean = true,
    onCommitText: ((String) -> Unit)? = null,
    onGestureAction: ((GestureAction, String) -> Unit)? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    swipeFontSize: androidx.compose.ui.unit.TextUnit = 9.sp,
    configVersion: Int = 0,
    shadowEnabled: Boolean = true,
    shadowElevation: Dp = 1.dp,
    shadowShapeRadius: Dp = 8.dp,
    radicalFor: ((String) -> String?)? = null,
    mainLabelOverride: ((String) -> String?)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(keyboardBackgroundColor),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            val rawSwipeUpText = KeysConfigHelper.getSwipeUpText(key)
            val swipeUpText = if (swipeUpHintsEnabled) rawSwipeUpText else null
            val swipeDownRaw = KeysConfigHelper.getKeyGesture(key)?.swipeDown
            val swipeDownLabel = swipeDownRaw?.label?.takeIf { it.isNotEmpty() }
            val swipeDownAction = swipeDownRaw?.action
            val swipeDownValue = swipeDownRaw?.value
            val swipeDownDisplay = swipeDownRaw?.display ?: "key"
            val radicalOverride = radicalFor?.invoke(key)
            val swipeDownBubbleText =
                if (radicalOverride == null && swipeDownDisplay != "key" && swipeDownHintsEnabled) swipeDownLabel else null
            // 仓颉/速成字根静态显示在键面；字根代替模式则字根作主标签
            val swipeDownStaticLabel = when {
                mainLabelOverride?.invoke(key) != null -> null
                !swipeDownHintsEnabled -> null
                radicalOverride != null && swipeDownRadicalEnabled -> radicalOverride
                swipeDownDisplay == "key" -> swipeDownLabel
                else -> if (swipeDownRadicalEnabled) null else swipeDownLabel
            }

            // 长按选项
            val longPressConfig = KeysConfigHelper.getKeyGesture(key)?.longPress
            val longPressDisplay = longPressConfig?.display ?: "key"
            val longPressLabels = if (longPressDisplay == "bubble") {
                longPressConfig?.values?.map { it.label }?.filter { it.isNotEmpty() }
                    ?.ifEmpty { null }
            } else null
            val longPressGestureMap = if (longPressDisplay == "bubble") {
                longPressConfig?.values?.associateBy { it.label }
            } else null

            // 键帽显示文本：字根代替模式显示字根，否则大写字母
            val displayText = mainLabelOverride?.invoke(key) ?: key.uppercase()
            val commitValue = key

            SwipeableKeyButton(
                text = displayText,
                onClick = { onKeyPress(commitValue) },
                backgroundColor = keyBackgroundColor,
                textColor = keyTextColor,
                modifier = Modifier.weight(1f),
                swipeText = swipeUpText,
                swipeDownText = swipeDownBubbleText,
                swipeDownKeyLabel = swipeDownStaticLabel,
                onSwipe = if (swipeUpText != null) onKeyPress else null,
                onSwipeDown = if (swipeDownAction != null && swipeDownHintsEnabled && swipeDownLabel != null) {
                    val label = swipeDownLabel
                    { _ ->
                        if (swipeDownAction == GestureAction.COMMIT) {
                            onKeyPress(key)
                        } else {
                            onGestureAction?.invoke(
                                swipeDownAction,
                                swipeDownValue?.ifEmpty { label } ?: label)
                        }
                    }
                } else null,
                onSwipeStateChange = onSwipeStateChange,
                onPress = { onKeyPressDown?.invoke(key) },
                onLongPressSelect = { selectedLabel ->
                    val gesture = longPressGestureMap?.get(selectedLabel)
                    if (gesture != null && gesture.action != GestureAction.COMMIT) {
                        onGestureAction?.invoke(
                            gesture.action!!,
                            gesture.value.ifEmpty { selectedLabel })
                    } else {
                        (onCommitText ?: onKeyPress)(selectedLabel)
                    }
                },
                longPressItems = longPressLabels,
                fontSize = fontSize,
                swipeFontSize = swipeFontSize,
                shadowEnabled = shadowEnabled,
                shadowElevation = shadowElevation,
                shadowShapeRadius = shadowShapeRadius,
            )
        }
    }
}

/**
 * 横屏分体键盘内容 — 当 [KeyboardLayout.isLandscape] 为 true 时渲染。
 * 将键盘拆分为左右两个面板，紧贴屏幕左右边缘，中间留空方便双手持机拇指操作。
 */
@Composable
private fun LandscapeKeyboardContent(
    onKeyPress: (String) -> Unit,
    isShifted: Boolean,
    schemaName: String,
    enterKeyText: String,
    isDarkTheme: Boolean,
    keyBackgroundColor: Color,
    keyTextColor: Color,
    specialKeyBackgroundColor: Color,
    keyboardBackgroundColor: Color,
    onKeyPressDown: ((String) -> Unit)?,
    onGestureAction: ((GestureAction, String) -> Unit)?,
    swipeUpHintsEnabled: Boolean,
    swipeDownHintsEnabled: Boolean,
    onCommitText: ((String) -> Unit)?,
    onSwipeStateChange: ((SwipeState, Rect) -> Unit)? = null,
    shadowEnabled: Boolean = true,
    shadowElevation: Dp = 1.dp,
    shadowShapeRadius: Dp = 8.dp,
) {
    val staggerStep = 10.dp
    val landscapeFontSize = 12.sp
    val landscapeSwipeFontSize = 7.sp

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 6.dp, horizontal = 50.dp)
    ) {
        // ========== 左面板 ==========
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.42f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CompactKeyboardRowWithConfig(
                    keys = listOf("q", "w", "e", "r", "t"),
                    onKeyPress = onKeyPress,
                    keyBackgroundColor = keyBackgroundColor,
                    keyTextColor = keyTextColor,
                    isShifted = isShifted,
                    keyboardBackgroundColor = keyboardBackgroundColor,
                    fontSize = landscapeFontSize,
                    swipeFontSize = landscapeSwipeFontSize,
                    onKeyPressDown = onKeyPressDown,
                    swipeDownHintsEnabled = swipeDownHintsEnabled,
                    swipeUpHintsEnabled = swipeUpHintsEnabled,
                    onCommitText = onCommitText,
                    onGestureAction = onGestureAction,
                    onSwipeStateChange = onSwipeStateChange,
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = staggerStep)
            ) {
                CompactKeyboardRowWithConfig(
                    keys = listOf("a", "s", "d", "f", "g"),
                    onKeyPress = onKeyPress,
                    keyBackgroundColor = keyBackgroundColor,
                    keyTextColor = keyTextColor,
                    isShifted = isShifted,
                    keyboardBackgroundColor = keyboardBackgroundColor,
                    fontSize = landscapeFontSize,
                    swipeFontSize = landscapeSwipeFontSize,
                    onKeyPressDown = onKeyPressDown,
                    swipeDownHintsEnabled = swipeDownHintsEnabled,
                    swipeUpHintsEnabled = swipeUpHintsEnabled,
                    onCommitText = onCommitText,
                    onGestureAction = onGestureAction,
                    onSwipeStateChange = onSwipeStateChange,
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = staggerStep * 2)
            ) {
                CompactKeyboardRowWithConfig(
                    keys = listOf("z", "x", "c", "v"),
                    onKeyPress = onKeyPress,
                    keyBackgroundColor = keyBackgroundColor,
                    keyTextColor = keyTextColor,
                    isShifted = isShifted,
                    keyboardBackgroundColor = keyboardBackgroundColor,
                    fontSize = landscapeFontSize,
                    swipeFontSize = landscapeSwipeFontSize,
                    onKeyPressDown = onKeyPressDown,
                    swipeDownHintsEnabled = swipeDownHintsEnabled,
                    swipeUpHintsEnabled = swipeUpHintsEnabled,
                    onCommitText = onCommitText,
                    onGestureAction = onGestureAction,
                    onSwipeStateChange = onSwipeStateChange,
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconKeyButton(
                    icon = rememberVectorPainter(Icons.Default.EmojiEmotions),
                    onClick = { onKeyPress("emoji") },
                    backgroundColor = specialKeyBackgroundColor,
                    iconColor = keyTextColor,
                    modifier = Modifier.weight(1.2f),
                    onPress = { onKeyPressDown?.invoke("emoji") },
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
                CompactSwipeableKeyButton(
                    text = "，",
                    onClick = { onKeyPress("，") },
                    backgroundColor = keyBackgroundColor,
                    textColor = keyTextColor,
                    modifier = Modifier.weight(0.8f),
                    swipeText = "。",
                    swipeFontSize = landscapeSwipeFontSize,
                    onSwipe = { onSwipeText -> onKeyPress(onSwipeText) },
                    onPress = { onKeyPressDown?.invoke("。") },
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
                SplitSpaceKey(
                    onClick = { onKeyPress("space") },
                    backgroundColor = keyBackgroundColor,
                    textColor = keyTextColor,
                    schemaName = schemaName,
                    modifier = Modifier.weight(3f),
                    onPress = { onKeyPressDown?.invoke("space") },
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
            }
        }

        // 中间留空
        Spacer(modifier = Modifier.weight(0.16f))

        // ========== 右面板 ==========
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.42f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CompactKeyboardRowWithConfig(
                    keys = listOf("y", "u", "i", "o", "p"),
                    onKeyPress = onKeyPress,
                    keyBackgroundColor = keyBackgroundColor,
                    keyTextColor = keyTextColor,
                    isShifted = isShifted,
                    keyboardBackgroundColor = keyboardBackgroundColor,
                    fontSize = landscapeFontSize,
                    swipeFontSize = landscapeSwipeFontSize,
                    onKeyPressDown = onKeyPressDown,
                    swipeDownHintsEnabled = swipeDownHintsEnabled,
                    swipeUpHintsEnabled = swipeUpHintsEnabled,
                    onCommitText = onCommitText,
                    onGestureAction = onGestureAction,
                    onSwipeStateChange = onSwipeStateChange,
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = staggerStep)
            ) {
                CompactKeyboardRowWithConfig(
                    keys = listOf("g", "h", "j", "k", "l"),
                    onKeyPress = onKeyPress,
                    keyBackgroundColor = keyBackgroundColor,
                    keyTextColor = keyTextColor,
                    isShifted = isShifted,
                    keyboardBackgroundColor = keyboardBackgroundColor,
                    fontSize = landscapeFontSize,
                    swipeFontSize = landscapeSwipeFontSize,
                    onKeyPressDown = onKeyPressDown,
                    swipeDownHintsEnabled = swipeDownHintsEnabled,
                    swipeUpHintsEnabled = swipeUpHintsEnabled,
                    onCommitText = onCommitText,
                    onGestureAction = onGestureAction,
                    onSwipeStateChange = onSwipeStateChange,
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(end = staggerStep * 2),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.weight(4f)) {
                    CompactKeyboardRowWithConfig(
                        keys = listOf("v", "b", "n", "m"),
                        onKeyPress = onKeyPress,
                        keyBackgroundColor = keyBackgroundColor,
                        keyTextColor = keyTextColor,
                        isShifted = isShifted,
                        keyboardBackgroundColor = keyboardBackgroundColor,
                        fontSize = landscapeFontSize,
                        swipeFontSize = landscapeSwipeFontSize,
                        onKeyPressDown = onKeyPressDown,
                        swipeDownHintsEnabled = swipeDownHintsEnabled,
                        swipeUpHintsEnabled = swipeUpHintsEnabled,
                        onCommitText = onCommitText,
                        onGestureAction = onGestureAction,
                        onSwipeStateChange = onSwipeStateChange,
                        shadowEnabled = shadowEnabled,
                        shadowElevation = shadowElevation,
                        shadowShapeRadius = shadowShapeRadius,
                    )
                }
                SwipeableIconKeyButton(
                    icon = rememberVectorPainter(Icons.AutoMirrored.Filled.Backspace),
                    onClick = { onKeyPress("delete") },
                    backgroundColor = specialKeyBackgroundColor,
                    iconColor = keyTextColor,
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight(),
                    swipeText = "",
                    onSwipe = { onKeyPress("clear_composition") },
                    onLongClick = { onKeyPress("delete") },
                    onPress = { onKeyPressDown?.invoke("delete") },
                    swipeUpLabel = "上滑清空",
                    swipeDownLabel = "下滑撤回",
                    onSwipeUp = { onKeyPress("clear_all") },
                    onSwipeDown = { onKeyPress("undo_clear") },
                    onSwipeStateChange = onSwipeStateChange,
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SplitSpaceKey(
                    onClick = { onKeyPress("space") },
                    backgroundColor = keyBackgroundColor,
                    textColor = keyTextColor,
                    schemaName = "",
                    modifier = Modifier.weight(2f),
                    onPress = { onKeyPressDown?.invoke("space") },
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
                KeyButton(
                    text = "?123",
                    onClick = { onKeyPress("mode_change") },
                    onLongClick = { onKeyPress("mode_change_symbol") },
                    backgroundColor = specialKeyBackgroundColor,
                    textColor = keyTextColor,
                    modifier = Modifier.weight(1.2f),
                    onPress = { onKeyPressDown?.invoke("mode_change") },
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
                KeyButton(
                    text = "中",
                    onClick = { onKeyPress("ime_switch") },
                    backgroundColor = specialKeyBackgroundColor,
                    textColor = keyTextColor,
                    modifier = Modifier.weight(0.8f),
                    onPress = { onKeyPressDown?.invoke("ime_switch") },
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
                KeyButton(
                    text = enterKeyText,
                    onClick = { onKeyPress("enter") },
                    backgroundColor = specialKeyBackgroundColor,
                    textColor = keyTextColor,
                    modifier = Modifier.weight(1.2f),
                    onPress = { onKeyPressDown?.invoke("enter") },
                    shadowEnabled = shadowEnabled,
                    shadowElevation = shadowElevation,
                    shadowShapeRadius = shadowShapeRadius,
                )
            }
        }
    }
}

/**
 * 横屏紧凑版按键 — 主字符和上滑字符垂直堆叠居中
 */
@Composable
fun CompactSwipeableKeyButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    swipeText: String? = null,
    swipeDownText: String? = null,
    onSwipe: ((String) -> Unit)? = null,
    onSwipeDown: ((String) -> Unit)? = null,
    onPress: (() -> Unit)? = null,
    onLongPressSelect: ((String) -> Unit)? = null,
    longPressItems: List<String>? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    swipeFontSize: androidx.compose.ui.unit.TextUnit = 8.sp,
    onSwipeStateChange: ((SwipeState, Rect) -> Unit)? = null,
    shadowEnabled: Boolean = true,
    shadowElevation: Dp = 1.dp,
    shadowShapeRadius: Dp = 8.dp,
) {
    var isPressed by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var hasTriggeredSwipeUp by remember { mutableStateOf(false) }
    var hasTriggeredSwipeDown by remember { mutableStateOf(false) }
    var isSwiping by remember { mutableStateOf(false) }
    var isSwipeDown by remember { mutableStateOf(false) }
    var buttonBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    val currentText by rememberUpdatedState(text)
    val currentSwipeText by rememberUpdatedState(swipeText)
    val currentSwipeDownText by rememberUpdatedState(swipeDownText)
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnLongPressSelect by rememberUpdatedState(onLongPressSelect)
    val currentLongPressItems by rememberUpdatedState(longPressItems)
    val currentOnSwipeStateChange by rememberUpdatedState(onSwipeStateChange)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current
    val chaiPuaFontFamily = remember {
        FontFamily(Font("ChaiPUA-0.2.7-snow.ttf", context.assets))
    }

    val density = LocalDensity.current
    val swipeUpThreshold = with(density) { (-15).dp.toPx() }
    val swipeDownThreshold = with(density) { 15.dp.toPx() }
    val bubbleShowThresholdUp = swipeUpThreshold * 0.3f
    val bubbleShowThresholdDown = swipeDownThreshold * 0.3f

    val shadowShape = remember(shadowShapeRadius) { RoundedCornerShape(shadowShapeRadius) }
    val shadowModifier = remember(shadowEnabled, shadowElevation, shadowShapeRadius) {
        if (shadowEnabled) Modifier.shadow(shadowElevation, shadowShape) else Modifier
    }

    fun darkenColor(color: Color, factor: Float = 0.15f): Color {
        return Color(
            red = (color.red * (1 - factor)).coerceIn(0f, 1f),
            green = (color.green * (1 - factor)).coerceIn(0f, 1f),
            blue = (color.blue * (1 - factor)).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(shadowModifier)
            .onGloballyPositioned { coordinates ->
                buttonBounds = coordinates.boundsInRoot()
            }
            .clip(shadowShape)
            .background(if (isPressed) darkenColor(backgroundColor) else backgroundColor)
            .pointerInput(currentLongPressItems, currentOnLongPressSelect) {
                if (currentLongPressItems.isNullOrEmpty() || currentOnLongPressSelect == null) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            currentOnSwipeStateChange?.invoke(SwipeState(isPressed = true, pressedText = currentText), buttonBounds)
                            currentOnPress?.invoke()
                            tryAwaitRelease()
                            isPressed = false
                            currentOnSwipeStateChange?.invoke(SwipeState(), buttonBounds)
                        },
                        onTap = {
                            if (!hasTriggeredSwipeUp && !hasTriggeredSwipeDown) currentOnClick()
                        }
                    )
                } else {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        var localLongPressTriggered = false
                        var selectedIdx = 0
                        val downX = down.position.x
                        val items = currentLongPressItems ?: return@awaitEachGesture

                        currentOnSwipeStateChange?.invoke(SwipeState(isPressed = true, pressedText = currentText), buttonBounds)
                        currentOnPress?.invoke()

                        val longPressJob = scope.launch {
                            delay(400L)
                            localLongPressTriggered = true
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            currentOnSwipeStateChange?.invoke(
                                SwipeState(
                                    isPressed = true,
                                    isLongPress = true,
                                    longPressItems = items,
                                    selectedLongPressIndex = 0
                                ),
                                buttonBounds
                            )
                        }

                        val cancelThresholdPx = with(density) { 5.dp.toPx() }
                        val downY = down.position.y
                        var swipeDetected = false

                        try {
                            var lastReportedIdx = -1
                            var completed = false
                            while (!completed) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break

                                if (change.isConsumed) continue

                                if (!localLongPressTriggered) {
                                    val deltaX = change.position.x - downX
                                    val deltaY = change.position.y - downY
                                    if (kotlin.math.abs(deltaX) > cancelThresholdPx || kotlin.math.abs(deltaY) > cancelThresholdPx) {
                                        swipeDetected = true
                                        longPressJob.cancel()
                                    }
                                }

                                if (localLongPressTriggered) {
                                    val deltaX = change.position.x - downX
                                    val itemWidth = buttonBounds.width / items.size
                                    selectedIdx = ((deltaX / itemWidth) + if (items.size > 1) 0.5f else 0f).toInt()
                                        .coerceIn(0, items.size - 1)

                                    if (selectedIdx != lastReportedIdx) {
                                        lastReportedIdx = selectedIdx
                                        currentOnSwipeStateChange?.invoke(
                                            SwipeState(
                                                isPressed = true,
                                                isLongPress = true,
                                                longPressItems = items,
                                                selectedLongPressIndex = selectedIdx
                                            ),
                                            buttonBounds
                                        )
                                    }
                                    change.consume()
                                }

                                if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Release) {
                                    completed = true
                                    if (localLongPressTriggered) {
                                        val selected = items.getOrNull(selectedIdx)
                                        if (selected != null) {
                                            currentOnLongPressSelect?.invoke(selected)
                                        }
                                    } else if (!swipeDetected) {
                                        currentOnClick()
                                    }
                                }
                            }
                        } finally {
                            longPressJob.cancel()
                            isPressed = false
                            currentOnSwipeStateChange?.invoke(SwipeState(), buttonBounds)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isPressed = true
                        dragOffsetY = 0f
                        hasTriggeredSwipeUp = false
                        hasTriggeredSwipeDown = false
                        isSwiping = false
                        isSwipeDown = false
                        currentOnSwipeStateChange?.invoke(SwipeState(isPressed = true, pressedText = currentText), buttonBounds)
                    },
                    onDragEnd = {
                        if (!hasTriggeredSwipeUp && !hasTriggeredSwipeDown && dragOffsetY > swipeUpThreshold && dragOffsetY < swipeDownThreshold) {
                            onClick()
                        }
                        isPressed = false
                        dragOffsetY = 0f
                        hasTriggeredSwipeUp = false
                        hasTriggeredSwipeDown = false
                        isSwiping = false
                        isSwipeDown = false
                        currentOnSwipeStateChange?.invoke(SwipeState(), buttonBounds)
                    },
                    onDragCancel = {
                        isPressed = false
                        dragOffsetY = 0f
                        hasTriggeredSwipeUp = false
                        hasTriggeredSwipeDown = false
                        isSwiping = false
                        isSwipeDown = false
                        currentOnSwipeStateChange?.invoke(SwipeState(), buttonBounds)
                    },
                    onDrag = { _: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Offset ->
                        dragOffsetY += dragAmount.y

                        val swipeTextValue = currentSwipeText
                        val swipeDownTextValue = currentSwipeDownText
                        val onSwipeAction = currentOnSwipe
                        val onSwipeDownAction = currentOnSwipeDown
                        val onSwipeStateChangeAction = currentOnSwipeStateChange

                        if (dragOffsetY < 0) {
                            val shouldShowBubble = swipeTextValue != null && dragOffsetY < bubbleShowThresholdUp
                            if (shouldShowBubble != isSwiping) {
                                isSwiping = shouldShowBubble
                                isSwipeDown = false
                                onSwipeStateChangeAction?.invoke(
                                    SwipeState(isSwiping = shouldShowBubble, swipeText = swipeTextValue, isSwipeDown = false),
                                    buttonBounds
                                )
                            }
                        } else if (dragOffsetY > 0) {
                            val shouldShowBubble = swipeDownTextValue != null && dragOffsetY > bubbleShowThresholdDown
                            if (shouldShowBubble != isSwipeDown) {
                                isSwipeDown = shouldShowBubble
                                isSwiping = shouldShowBubble
                                onSwipeStateChangeAction?.invoke(
                                    SwipeState(isSwiping = shouldShowBubble, swipeText = swipeDownTextValue, isSwipeDown = true),
                                    buttonBounds
                                )
                            }
                        }

                        if (dragOffsetY < 0 && !hasTriggeredSwipeUp && swipeTextValue != null && onSwipeAction != null) {
                            if (dragOffsetY < swipeUpThreshold) {
                                hasTriggeredSwipeUp = true
                                onSwipeAction(swipeTextValue)
                            }
                        } else if (dragOffsetY > 0 && !hasTriggeredSwipeDown && swipeDownTextValue != null && onSwipeDownAction != null) {
                            if (dragOffsetY > swipeDownThreshold) {
                                hasTriggeredSwipeDown = true
                                onSwipeDownAction(swipeDownTextValue)
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopStart
    ) {


        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) fontSize else 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                lineHeight = TextUnit.Unspecified
            )
        }

        if (swipeText != null) {
            Text(
                text = swipeText,
                color = textColor.copy(alpha = 0.5f),
                fontSize = swipeFontSize,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.End,
                maxLines = 1,
                lineHeight = 8.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
        }
        if (swipeDownText != null) {
            Text(
                text = swipeDownText,
                color = textColor.copy(alpha = 0.5f),
                fontSize = swipeFontSize,
                fontWeight = FontWeight.Normal,
                fontFamily = chaiPuaFontFamily,
                textAlign = TextAlign.Start,
                maxLines = 1,
                lineHeight = 8.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 4.dp, bottom = 2.dp)
            )
        }
    }
}

/**
 * 横屏紧凑版键盘行 — 使用 [CompactSwipeableKeyButton] 替代 [SwipeableKeyButton]
 */
@Composable
fun CompactKeyboardRowWithConfig(
    keys: List<String>,
    onKeyPress: (String) -> Unit,
    keyBackgroundColor: Color,
    keyTextColor: Color,
    isShifted: Boolean,
    keyboardBackgroundColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    onKeyPressDown: ((String) -> Unit)? = null,
    swipeDownHintsEnabled: Boolean = true,
    swipeUpHintsEnabled: Boolean = true,
    onCommitText: ((String) -> Unit)? = null,
    onGestureAction: ((GestureAction, String) -> Unit)? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    swipeFontSize: androidx.compose.ui.unit.TextUnit = 9.sp,
    onSwipeStateChange: ((SwipeState, Rect) -> Unit)? = null,
    configVersion: Int = 0,
    shadowEnabled: Boolean = true,
    shadowElevation: Dp = 1.dp,
    shadowShapeRadius: Dp = 8.dp,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(keyboardBackgroundColor),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            val rawSwipeUpText = KeysConfigHelper.getSwipeUpText(key)
            val swipeUpText = if (swipeUpHintsEnabled) rawSwipeUpText else null
            val swipeDownRaw = KeysConfigHelper.getKeyGesture(key)?.swipeDown
            val swipeDownLabel = swipeDownRaw?.label?.takeIf { it.isNotEmpty() }
            val swipeDownAction = swipeDownRaw?.action
            val swipeDownValue = swipeDownRaw?.value
            val swipeDownDisplay = swipeDownRaw?.display ?: "key"
            val swipeDownBubbleText =
                if (swipeDownDisplay != "key" && swipeDownHintsEnabled) swipeDownLabel else null

            val longPressConfig = KeysConfigHelper.getKeyGesture(key)?.longPress
            val longPressDisplay = longPressConfig?.display ?: "key"
            val longPressLabels = if (longPressDisplay == "bubble") {
                longPressConfig?.values?.map { it.label }?.filter { it.isNotEmpty() }
                    ?.ifEmpty { null }
            } else null
            val longPressGestureMap = if (longPressDisplay == "bubble") {
                longPressConfig?.values?.associateBy { it.label }
            } else null

            CompactSwipeableKeyButton(
                text = key.uppercase(),
                onClick = { onKeyPress(key) },
                backgroundColor = keyBackgroundColor,
                textColor = keyTextColor,
                modifier = Modifier.weight(1f),
                swipeText = swipeUpText,
                swipeDownText = swipeDownBubbleText,
                    onSwipe = if (swipeUpText != null) onKeyPress else null,
                    onSwipeDown = if (swipeDownAction != null && swipeDownHintsEnabled && swipeDownLabel != null) {
                        { _ ->
                            if (swipeDownAction == GestureAction.COMMIT) {
                                onKeyPress(key)
                            } else {
                                onGestureAction?.invoke(
                                    swipeDownAction,
                                    swipeDownValue?.ifEmpty { swipeDownLabel } ?: swipeDownLabel!!)
                            }
                        }
                    } else null,
                    onSwipeStateChange = onSwipeStateChange,
                    onPress = { onKeyPressDown?.invoke(key) },
                onLongPressSelect = { selectedLabel ->
                    val gesture = longPressGestureMap?.get(selectedLabel)
                    if (gesture != null && gesture.action != GestureAction.COMMIT) {
                        onGestureAction?.invoke(
                            gesture.action!!,
                            gesture.value.ifEmpty { selectedLabel!! })
                    } else {
                        (onCommitText ?: onKeyPress)(selectedLabel)
                    }
                },
                longPressItems = longPressLabels,
                fontSize = fontSize,
                swipeFontSize = swipeFontSize,
                shadowEnabled = shadowEnabled,
                shadowElevation = shadowElevation,
                shadowShapeRadius = shadowShapeRadius,
            )
        }
    }
}

/**
 * 横屏分体键盘专用空格键（简化版，不支持语音/滑动光标）
 */
@Composable
private fun SplitSpaceKey(
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    schemaName: String = "",
    modifier: Modifier = Modifier,
    onPress: (() -> Unit)? = null,
    shadowEnabled: Boolean = true,
    shadowElevation: Dp = 1.dp,
    shadowShapeRadius: Dp = 8.dp,
) {
    val shadowShape = remember(shadowShapeRadius) { RoundedCornerShape(shadowShapeRadius) }
    val shadowModifier = remember(shadowEnabled, shadowElevation, shadowShapeRadius) {
        if (shadowEnabled) Modifier.shadow(shadowElevation, shadowShape) else Modifier
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(shadowModifier)
            .clip(shadowShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = schemaName,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = "空格",
            color = textColor.copy(alpha = 0.3f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 2.dp)
        )
    }
}