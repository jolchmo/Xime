package com.kingzcheung.kime.plugin

import android.content.Context
import com.kingzcheung.kime.plugin.api.EmojiItem
import com.kingzcheung.kime.plugin.api.EmojiPlugin
import com.kingzcheung.kime.plugin.api.PluginMetadata
import com.kingzcheung.kime.plugin.api.PluginType
import com.kingzcheung.kime.plugin.api.PredictionPlugin
import com.kingzcheung.kime.plugin.api.PredictionCandidate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExtensionManagerTest {
    
    @Before
    fun setup() {
        try {
            ExtensionManager.release()
        } catch (e: Exception) {
        }
    }
    
    @Test
    fun `getExtensionPlugins should return empty list when not initialized`() {
        try {
            ExtensionManager.release()
        } catch (e: Exception) {
        }
        val plugins = ExtensionManager.getPredictionPlugins()
        assertTrue(plugins.isEmpty())
    }
    
    @Test
    fun `isInitialized should return false before initialization`() {
        try {
            ExtensionManager.release()
        } catch (e: Exception) {
        }
        assertFalse(ExtensionManager.isInitialized())
    }
    
    @Test
    fun `getPluginById should return null for unknown plugin`() {
        val plugin = ExtensionManager.getPluginById("unknown_id")
        assertNull(plugin)
    }
    
    @Test
    fun `release should clear all plugins`() {
        try {
            ExtensionManager.release()
        } catch (e: Exception) {
        }
        assertFalse(ExtensionManager.isInitialized())
        assertTrue(ExtensionManager.getPredictionPlugins().isEmpty())
        assertTrue(ExtensionManager.getEmojiPlugins().isEmpty())
        assertTrue(ExtensionManager.getSpeechPlugins().isEmpty())
    }
}

class MockPredictionPlugin(
    override val id: String = "test_prediction",
    override val name: String = "Test Prediction",
    override val description: String = "Test prediction plugin",
    override val version: String = "1.0.0",
    override val type: PluginType = PluginType.PREDICTION
) : PredictionPlugin {
    
    override fun initialize(context: Context): Boolean = true
    
    override suspend fun predict(inputText: String, topK: Int): List<PredictionCandidate> {
        return listOf(
            PredictionCandidate("${inputText}预测1", 0.9f),
            PredictionCandidate("${inputText}预测2", 0.8f)
        )
    }
    
    override fun release() {}
}

class MockEmojiPlugin(
    override val id: String = "test_emoji",
    override val name: String = "Test Emoji",
    override val description: String = "Test emoji plugin",
    override val version: String = "1.0.0",
    override val type: PluginType = PluginType.EMOJI
) : EmojiPlugin {
    
    override fun initialize(context: Context): Boolean = true
    
    override suspend fun getEmojis(
        category: String?,
        searchText: String?,
        topK: Int
    ): List<EmojiItem> {
        return listOf(
            EmojiItem("smile", "😀", "😀", null, "face"),
            EmojiItem("joy", "😂", "😂", null, "face")
        )
    }
    
    override suspend fun getCategories(): List<String> {
        return listOf("face", "animal", "food")
    }
    
    override fun release() {}
}