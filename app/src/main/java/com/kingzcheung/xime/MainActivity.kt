package com.kingzcheung.xime

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kingzcheung.xime.rime.RimeConfigHelper
import com.kingzcheung.xime.rime.RimeEngine
import com.kingzcheung.xime.settings.SettingsPreferences
import com.kingzcheung.xime.ui.KeysConfigHelper
import com.kingzcheung.xime.ui.SettingsScreen
import com.kingzcheung.xime.ui.theme.XimeTheme
import com.kingzcheung.xime.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "麦克风权限已授权", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "麦克风权限被拒绝，无法使用语音输入", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
    
    private val prewarmScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private fun prewarmRimeEngine() {
        if (RimeEngine.isInitialized()) return
        prewarmScope.launch {
            try {
                Log.d(TAG, "Pre-warming Rime engine...")
                KeysConfigHelper.loadConfig(this@MainActivity)
                val (userDataDir, sharedDataDir) = RimeConfigHelper.initializeRimeDataAsync(this@MainActivity)
                RimeEngine.getInstance().initialize(userDataDir, sharedDataDir)
                Log.d(TAG, "Rime engine pre-warmed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Rime engine pre-warm failed, will init on demand", e)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prewarmRimeEngine()
        
        val requestPermission = intent?.getStringExtra("request_permission")
        if (requestPermission == PermissionHelper.PERMISSION_RECORD_AUDIO) {
            if (!PermissionHelper.hasRecordAudioPermission(this)) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                Toast.makeText(this, "麦克风权限已授权", Toast.LENGTH_SHORT).show()
                finish()
            }
            return
        }
        
        enableEdgeToEdge()
        val openFragment = intent?.getStringExtra("open_fragment")
        setContent {
            val context = this
            var darkMode by remember { mutableIntStateOf(SettingsPreferences.getDarkMode(context)) }
            var keyboardTheme by remember { mutableStateOf(SettingsPreferences.getKeyboardTheme(context)) }
            
            XimeTheme(darkTheme = darkMode == 1, themeId = keyboardTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SettingsScreen(
                            initialRoute = openFragment,
                            onThemeChanged = {
                                darkMode = SettingsPreferences.getDarkMode(context)
                                keyboardTheme = SettingsPreferences.getKeyboardTheme(context)
                            }
                        )
                    }
                }
            }
        }
    }
}