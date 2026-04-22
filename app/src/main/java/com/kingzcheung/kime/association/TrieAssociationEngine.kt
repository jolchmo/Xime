package com.kingzcheung.kime.association

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader

data class WordTrieNode(
    val children: MutableMap<Char, WordTrieNode> = mutableMapOf(),
    var word: String? = null,
    var frequency: Int = 0
)

class TrieAssociationEngine {
    private val root = WordTrieNode()
    private var isInitialized = false
    
    companion object {
        private const val TAG = "TrieAssociationEngine"
        private var instance: TrieAssociationEngine? = null
        
        fun getInstance(): TrieAssociationEngine {
            if (instance == null) {
                instance = TrieAssociationEngine()
            }
            return instance!!
        }
    }
    
    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return@withContext true
        }
        
        try {
            val startTime = System.currentTimeMillis()
            loadDictionary(context)
            isInitialized = true
            
            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "Trie engine initialized in $elapsed ms")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize trie engine", e)
            false
        }
    }
    
    private fun loadDictionary(context: Context) {
        var wordCount = 0
        context.assets.open("english.txt").bufferedReader().use { reader ->
            var lineNum = 0
            reader.lineSequence().forEach { line ->
                lineNum++
                val word = line.trim().lowercase()
                if (word.isNotEmpty()) {
                    insertWord(word, lineNum)
                    wordCount++
                }
            }
        }
        Log.d(TAG, "Loaded $wordCount words from dictionary")
    }
    
    private fun insertWord(word: String, frequency: Int) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { WordTrieNode() }
        }
        node.word = word
        node.frequency = frequency
    }
    
    suspend fun predict(prefix: String, topK: Int = 5): List<AssociationCandidate> = withContext(Dispatchers.Default) {
        if (!isInitialized || prefix.isEmpty()) {
            return@withContext emptyList()
        }
        
        val normalizedPrefix = prefix.lowercase()
        val startNode = findNode(normalizedPrefix)
        
        if (startNode == null) {
            return@withContext emptyList()
        }
        
        val candidates = mutableListOf<AssociationCandidate>()
        collectWords(startNode, normalizedPrefix, candidates)
        
        candidates.sortedBy { it.score }.take(topK)
    }
    
    private fun findNode(prefix: String): WordTrieNode? {
        var node = root
        for (char in prefix) {
            node = node.children[char] ?: return null
        }
        return node
    }
    
    private fun collectWords(node: WordTrieNode, currentPrefix: String, candidates: MutableList<AssociationCandidate>) {
        if (node.word != null) {
            candidates.add(AssociationCandidate(node.word!!, node.frequency.toFloat()))
        }
        
        for ((char, childNode) in node.children) {
            collectWords(childNode, currentPrefix + char, candidates)
        }
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    fun release() {
        root.children.clear()
        isInitialized = false
        Log.d(TAG, "Trie engine released")
    }
}