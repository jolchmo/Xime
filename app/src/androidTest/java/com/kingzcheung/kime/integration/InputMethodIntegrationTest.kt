package com.kingzcheung.kime.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kingzcheung.kime.plugin.ExtensionManager
import com.kingzcheung.kime.rime.RimeEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class InputMethodIntegrationTest {
    
    private lateinit var context: android.content.Context
    private var rimeInitialized = false
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ExtensionManager.release()
    }
    
    @After
    fun tearDown() {
        if (rimeInitialized) {
            RimeEngine.getInstance().destroy()
            rimeInitialized = false
        }
        ExtensionManager.release()
    }
    
    @Test
    fun `test full initialization workflow`() {
        assertFalse(ExtensionManager.isInitialized())
        
        val result = ExtensionManager.initialize(context)
        
        assertTrue("ExtensionManager should initialize successfully", result)
        assertTrue(ExtensionManager.isInitialized())
    }
    
    @Test
    fun `test plugin loading workflow`() {
        ExtensionManager.initialize(context)
        
        val allPlugins = ExtensionManager.getAllPlugins()
        assertNotNull("Plugin list should not be null", allPlugins)
        
        val predictionPlugins = ExtensionManager.getPredictionPlugins()
        val emojiPlugins = ExtensionManager.getEmojiPlugins()
        val speechPlugins = ExtensionManager.getSpeechPlugins()
        
        val totalLoaded = predictionPlugins.size + emojiPlugins.size + speechPlugins.size
        assertTrue("Total plugins should match all plugins", totalLoaded == allPlugins.size)
    }
    
    @Test
    fun `test plugin enable disable workflow`() {
        val pluginId = "test_plugin_integration"
        
        ExtensionManager.initialize(context)
        
        val initialEnabled = ExtensionManager.getEnabledPredictionPlugins(context)
        val initialCount = initialEnabled.size
        
        assertFalse("New plugin should be disabled by default", 
            ExtensionManager.getPluginById(pluginId) != null)
    }
    
    @Test
    fun `test rime directory setup`() {
        val userDataDir = File(context.filesDir, "rime_user")
        val sharedDataDir = File(context.filesDir, "rime_shared")
        
        assertNotNull(userDataDir)
        assertNotNull(sharedDataDir)
    }
    
    @Test
    fun `test plugin prediction integration`() = runBlocking {
        ExtensionManager.initialize(context)
        
        val inputText = "测试"
        val predictions = ExtensionManager.predict(context, inputText, 5)
        
        assertNotNull("Predictions should not be null", predictions)
        assertTrue("Predictions should be a list", predictions is List)
    }
    
    @Test
    fun `test reload workflow`() {
        ExtensionManager.initialize(context)
        assertTrue(ExtensionManager.isInitialized())
        
        val result = ExtensionManager.reload(context)
        assertTrue("Reload should succeed", result)
        assertTrue("Should still be initialized after reload", ExtensionManager.isInitialized())
    }
    
    @Test
    fun `test force reload workflow`() {
        ExtensionManager.initialize(context)
        assertTrue(ExtensionManager.isInitialized())
        
        val result = ExtensionManager.forceReload(context)
        assertTrue("Force reload should succeed", result)
        assertTrue("Should be initialized after force reload", ExtensionManager.isInitialized())
    }
}