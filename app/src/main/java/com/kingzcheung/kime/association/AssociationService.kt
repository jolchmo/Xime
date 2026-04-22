package com.kingzcheung.kime.association

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AssociationService {
    private const val TAG = "AssociationService"
    
    private var trieEngine: TrieAssociationEngine? = null
    private var isInitialized = false
    
    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return@withContext true
        }
        
        try {
            trieEngine = TrieAssociationEngine.getInstance()
            val trieInit = trieEngine!!.initialize(context)
            
            isInitialized = trieInit
            Log.i(TAG, "Association service initialized: trie=$trieInit")
            
            isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize association service", e)
            false
        }
    }
    
    suspend fun getAssociations(
        context: Context,
        inputText: String,
        isAsciiMode: Boolean,
        topK: Int = 5
    ): List<String> = withContext(Dispatchers.Default) {
        if (inputText.isEmpty()) {
            return@withContext emptyList()
        }
        
        try {
            val candidates = if (isAsciiMode) {
                getEnglishAssociations(inputText, topK)
            } else {
                getChineseAssociations(context, inputText, topK)
            }
            
            candidates.map { it.text }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get associations", e)
            emptyList()
        }
    }
    
    private suspend fun getEnglishAssociations(prefix: String, topK: Int): List<AssociationCandidate> {
        if (trieEngine == null || !trieEngine!!.isInitialized()) {
            Log.w(TAG, "Trie engine not initialized for English associations")
            return emptyList()
        }
        
        val candidates = trieEngine!!.predict(prefix, topK)
        Log.d(TAG, "English associations for '$prefix': ${candidates.map { it.text }}")
        
        return candidates
    }
    
    private suspend fun getChineseAssociations(
        context: Context,
        inputText: String,
        topK: Int
    ): List<AssociationCandidate> = withContext(Dispatchers.Default) {
        try {
            if (!AssociationManager.isInitialized()) {
                Log.d(TAG, "AssociationManager not initialized, initializing...")
                val initSuccess = withContext(Dispatchers.IO) {
                    AssociationManager.initialize(context)
                }
                if (!initSuccess) {
                    Log.e(TAG, "AssociationManager initialization failed")
                    return@withContext emptyList()
                }
            }
            
            val candidates = AssociationManager.predict(inputText, topK)
            Log.d(TAG, "Chinese associations for '$inputText': ${candidates.map { it.text }}")
            
            candidates
        } catch (e: Exception) {
            Log.e(TAG, "Chinese associations failed", e)
            emptyList()
        }
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    fun isTrieInitialized(): Boolean = trieEngine?.isInitialized() ?: false
    
    fun release() {
        trieEngine?.release()
        trieEngine = null
        isInitialized = false
        Log.d(TAG, "Association service released")
    }
}