package com.kingzcheung.xime.ui

import com.kingzcheung.xime.keyboard.GestureAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 纯 JVM 验证仓颉/速成方案识别（决定是否显示仓颉字根）。 */
class CangjieDetectionTest {

    @Test
    fun detects_cangjie_and_sucheng_schemas() {
        assertTrue(isCangjieFamily("cangjie5"))
        assertTrue(isCangjieFamily("Cangjie"))
        assertTrue(isCangjieFamily("quick5"))      // 速成标准 id
        assertTrue(isCangjieFamily("sucheng"))
        assertTrue(isCangjieFamily("仓颉"))
        assertTrue(isCangjieFamily("速成"))
    }

    @Test
    fun ignores_non_cangjie_schemas() {
        assertFalse(isCangjieFamily("wubi86"))
        assertFalse(isCangjieFamily("pinyin_simp"))
        assertFalse(isCangjieFamily("luna_pinyin"))
    }

    @Test
    fun detects_wubi_schemas() {
        assertTrue(isWubiFamily("wubi86"))
        assertTrue(isWubiFamily("wubi86_pinyin"))
        assertTrue(isWubiFamily("五笔86"))
        assertFalse(isWubiFamily("cangjie5"))
        assertFalse(isWubiFamily("luna_pinyin"))
    }

    /** 「功能」模式下 a/c/v/x 下滑应映射到 全选/复制/粘贴/剪切；其它键无功能。 */
    @Test
    fun function_mode_maps_acvx_to_editing_actions() {
        assertEquals("全选" to GestureAction.SELECT_ALL, functionModeGestureFor("a"))
        assertEquals("复制" to GestureAction.COPY, functionModeGestureFor("c"))
        assertEquals("粘贴" to GestureAction.PASTE, functionModeGestureFor("v"))
        assertEquals("剪切" to GestureAction.CUT, functionModeGestureFor("x"))
        assertEquals("全选" to GestureAction.SELECT_ALL, functionModeGestureFor("A")) // 大小写无关
        assertNull(functionModeGestureFor("q"))
        assertNull(functionModeGestureFor("s"))
        assertNull(functionModeGestureFor("z"))
    }
}
