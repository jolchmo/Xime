package com.kingzcheung.kime.plugin

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExtensionManagerInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ExtensionManager.release()
    }

    @After
    fun tearDown() {
        ExtensionManager.release()
    }

    @Test
    fun `initialize succeeds even when no plugins installed`() {
        val result = ExtensionManager.initialize(context)
        
        assertTrue("Initialize should succeed", result)
        assertTrue("Should be initialized", ExtensionManager.isInitialized())
    }

    @Test
    fun `getPredictionPlugins returns empty list initially`() {
        ExtensionManager.initialize(context)
        
        val plugins = ExtensionManager.getPredictionPlugins()
        
        assertTrue("Should return empty list when no plugins", plugins.isEmpty())
    }

    @Test
    fun `getAllPlugins combines all plugin types`() {
        ExtensionManager.initialize(context)
        
        val all = ExtensionManager.getAllPlugins()
        
        assertEquals(
            "Total should be sum of all types",
            ExtensionManager.getPredictionPlugins().size + 
            ExtensionManager.getEmojiPlugins().size +
            ExtensionManager.getSpeechPlugins().size,
            all.size
        )
    }

    @Test
    fun `getPluginById returns null for unknown plugin`() {
        ExtensionManager.initialize(context)
        
        val plugin = ExtensionManager.getPluginById("unknown_plugin_id")
        
        assertNull("Should return null for unknown plugin", plugin)
    }

    @Test
    fun `release clears initialization state`() {
        ExtensionManager.initialize(context)
        assertTrue(ExtensionManager.isInitialized())
        
        ExtensionManager.release()
        
        assertFalse("Should not be initialized after release", ExtensionManager.isInitialized())
    }

    @Test
    fun `reload works after initialization`() {
        ExtensionManager.initialize(context)
        assertTrue(ExtensionManager.isInitialized())
        
        val result = ExtensionManager.reload(context)
        
        assertTrue("Reload should succeed", result)
        assertTrue("Should still be initialized after reload", ExtensionManager.isInitialized())
    }
}
