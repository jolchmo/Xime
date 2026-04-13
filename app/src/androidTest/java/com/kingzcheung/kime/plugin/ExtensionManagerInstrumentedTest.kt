package com.kingzcheung.kime.plugin

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExtensionManagerInstrumentedTest {
    
    private lateinit var context: android.content.Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ExtensionManager.release()
    }
    
    @Test
    fun `initialize should succeed with valid context`() {
        val result = ExtensionManager.initialize(context)
        assertTrue("ExtensionManager should initialize successfully", result)
        assertTrue(ExtensionManager.isInitialized())
    }
    
    @Test
    fun `getPredictionPlugins should return list after initialization`() {
        ExtensionManager.initialize(context)
        val plugins = ExtensionManager.getPredictionPlugins()
        assertNotNull(plugins)
    }
    
    @Test
    fun `getEmojiPlugins should return list after initialization`() {
        ExtensionManager.initialize(context)
        val plugins = ExtensionManager.getEmojiPlugins()
        assertNotNull(plugins)
    }
    
    @Test
    fun `getSpeechPlugins should return list after initialization`() {
        ExtensionManager.initialize(context)
        val plugins = ExtensionManager.getSpeechPlugins()
        assertNotNull(plugins)
    }
    
    @Test
    fun `release should clear initialization state`() {
        ExtensionManager.initialize(context)
        assertTrue(ExtensionManager.isInitialized())
        
        ExtensionManager.release()
        assertFalse(ExtensionManager.isInitialized())
    }
    
    @Test
    fun `initialize should be idempotent`() {
        ExtensionManager.initialize(context)
        val result = ExtensionManager.initialize(context)
        assertTrue("Second initialization should return true", result)
        assertTrue(ExtensionManager.isInitialized())
    }
    
    @Test
    fun `reload should work after initialization`() {
        ExtensionManager.initialize(context)
        val result = ExtensionManager.reload(context)
        assertTrue("Reload should succeed", result)
        assertTrue(ExtensionManager.isInitialized())
    }
}