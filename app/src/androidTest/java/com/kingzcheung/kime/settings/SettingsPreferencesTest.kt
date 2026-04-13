package com.kingzcheung.kime.settings

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SettingsPreferencesTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("kime_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
    
    @Test
    fun `test default settings`() {
        val defaultSchema = SettingsPreferences.getDefaultSchema(context)
        assertEquals("wubi86", defaultSchema)
    }
    
    @Test
    fun `test set and get schema`() {
        SettingsPreferences.setSchema(context, "wubi98")
        val schema = SettingsPreferences.getSchema(context)
        assertEquals("wubi98", schema)
    }
    
    @Test
    fun `test plugin enabled status`() {
        val pluginId = "test_plugin"
        
        val defaultEnabled = SettingsPreferences.isPluginEnabled(context, pluginId)
        assertFalse("插件默认应该是禁用状态", defaultEnabled)
        
        SettingsPreferences.setPluginEnabled(context, pluginId, true)
        val enabled = SettingsPreferences.isPluginEnabled(context, pluginId)
        assertTrue("插件应该被启用", enabled)
        
        SettingsPreferences.setPluginEnabled(context, pluginId, false)
        val disabled = SettingsPreferences.isPluginEnabled(context, pluginId)
        assertFalse("插件应该被禁用", disabled)
    }
    
    @Test
    fun `test theme settings`() {
        SettingsPreferences.setTheme(context, "dark")
        val theme = SettingsPreferences.getTheme(context)
        assertEquals("dark", theme)
    }
    
    @Test
    fun `test keyboard height settings`() {
        val testHeight = 300
        SettingsPreferences.setKeyboardHeight(context, testHeight)
        val height = SettingsPreferences.getKeyboardHeight(context)
        assertEquals(testHeight, height)
    }
    
    @Test
    fun `test vibration settings`() {
        SettingsPreferences.setVibrationEnabled(context, true)
        assertTrue(SettingsPreferences.isVibrationEnabled(context))
        
        SettingsPreferences.setVibrationEnabled(context, false)
        assertFalse(SettingsPreferences.isVibrationEnabled(context))
    }
    
    @Test
    fun `test sound settings`() {
        SettingsPreferences.setSoundEnabled(context, true)
        assertTrue(SettingsPreferences.isSoundEnabled(context))
        
        SettingsPreferences.setSoundEnabled(context, false)
        assertFalse(SettingsPreferences.isSoundEnabled(context))
    }
}