package com.kingzcheung.xime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.kingzcheung.xime.settings.SettingsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KeyEffectUiState(
    val soundEnabled: Boolean = true,
    val soundVolume: Int = 50,
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Int = 50,
    val swipeDownShowRootsEnabled: Boolean = false
)

class KeyEffectSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    
    private val _uiState = MutableStateFlow(KeyEffectUiState(
        soundEnabled = SettingsPreferences.isSoundEnabled(context),
        soundVolume = SettingsPreferences.getSoundVolume(context),
        vibrationEnabled = SettingsPreferences.isVibrationEnabled(context),
        vibrationIntensity = SettingsPreferences.getVibrationIntensity(context),
        swipeDownShowRootsEnabled = SettingsPreferences.isSwipeDownShowRootsEnabled(context)
    ))
    val uiState: StateFlow<KeyEffectUiState> = _uiState.asStateFlow()
    
    fun setSoundEnabled(enabled: Boolean) {
        SettingsPreferences.setSoundEnabled(context, enabled)
        _uiState.update { it.copy(soundEnabled = enabled) }
    }
    
    fun setSoundVolume(volume: Int) {
        SettingsPreferences.setSoundVolume(context, volume)
        _uiState.update { it.copy(soundVolume = volume) }
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        SettingsPreferences.setVibrationEnabled(context, enabled)
        _uiState.update { it.copy(vibrationEnabled = enabled) }
    }
    
    fun setVibrationIntensity(intensity: Int) {
        SettingsPreferences.setVibrationIntensity(context, intensity)
        _uiState.update { it.copy(vibrationIntensity = intensity) }
    }
    
    fun setSwipeDownShowRootsEnabled(enabled: Boolean) {
        SettingsPreferences.setSwipeDownShowRootsEnabled(context, enabled)
        _uiState.update { it.copy(swipeDownShowRootsEnabled = enabled) }
    }
}