package com.kingzcheung.xime.ui

import org.junit.Assert.assertFalse
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
}
