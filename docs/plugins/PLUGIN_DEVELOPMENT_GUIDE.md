# Kime 插件开发完整指南

## 插件系统架构

Kime 采用类似 Mihon/Tachiyomi 的动态加载插件架构：

```
┌─────────────────────────────────────────────┐
│          主应用 (Kime APK)                   │
│                                              │
│  ┌─────────────────────────────────────┐   │
│  │   ExtensionManager                   │   │
│  │   - 扫描已安装插件APK                 │   │
│  │   - 动态 ClassLoader 加载            │   │
│  │   - 管理插件生命周期                  │   │
│  └─────────────────────────────────────┘   │
│                    │                         │
│                    │ PathClassLoader        │
│                    ▼                         │
└─────────────────────────────────────────────┘
          │
          │ 加载插件APK
          ▼
┌─────────────────────────────────────────────┐
│       插件 APK (独立安装)                    │
│                                              │
│  ┌─────────────────────────────────────┐   │
│  │   MyPluginFactory                    │   │
│  │   - createPredictionPlugin()         │   │
│  │   - createEmojiPlugin()              │   │
│  │   - createSpeechPlugin()             │   │
│  └─────────────────────────────────────┘   │
│                    │                         │
│                    ▼                         │
│  ┌─────────────────────────────────────┐   │
│  │   MyPredictionPlugin                 │   │
│  │   - id, name, type, version          │   │
│  │   - initialize()                     │   │
│  │   - predict()                        │   │
│  │   - learn()                          │   │
│  │   - release()                        │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

## 核心概念

### 插件类型

Kime 插件系统支持三种独立插件类型：

| 类型 | 接口 | 用途 |
|------|------|------|
| PREDICTION | PredictionPlugin | 联想词预测 |
| SPEECH | SpeechPlugin | 语音转文字 |
| EMOJI | EmojiPlugin | 表情输入 |

### 插件架构组成

每个插件由以下核心组件组成：

1. **PluginFactory** - 插件工厂类，负责创建插件实例
2. **Plugin 实现** - 实现 `PredictionPlugin`/`EmojiPlugin`/`SpeechPlugin` 接口
3. **PluginDeclaration** - 空的 Activity，用于插件发现
4. **PluginSettingsActivity**（可选） - 插件设置界面

## 核心接口详解

### PluginMetadata - 插件元数据接口

所有插件都必须实现的基础接口：

```kotlin
interface PluginMetadata {
    val id: String              // 插件唯一标识符
    val name: String            // 插件显示名称
    val description: String    // 插件描述
    val version: String        // 插件版本号
    val type: PluginType        // 插件类型
    
    fun initialize(context: Context): Boolean
    fun initialize(context: Context, apkPath: String?): Boolean  // 可选，带 APK 路径
    fun release()
    
    fun hasSettings(): Boolean = false           // 是否有设置界面
    fun createSettingsIntent(context: Context): Intent? = null  // 创建设置 Intent
}
```

**重要方法说明**：
- `initialize()`: 初始化插件资源，返回 `true` 表示成功
- `initialize(context, apkPath)`: 重载方法，可获取插件 APK 路径用于加载资源
- `release()`: 释放资源，清理内存
- `hasSettings()`: 返回 `true` 表示插件有设置界面
- `createSettingsIntent()`: 创建跳转到设置 Activity 的 Intent

### PluginFactory - 插件工厂接口

```kotlin
interface PluginFactory {
    fun createPredictionPlugin(): PredictionPlugin?
    fun createEmojiPlugin(): EmojiPlugin?
    fun createSpeechPlugin(): SpeechPlugin?
}
```

**设计模式**：
- 一个插件 APK 可以实现多种插件类型
- 不支持的类型返回 `null`
- 工厂方法每次调用应返回**同一个实例**（单例模式）

### PredictionPlugin - 联想词插件接口

```kotlin
interface PredictionPlugin : PluginMetadata {
    suspend fun predict(inputText: String, topK: Int = 5): List<PredictionCandidate>
    
    fun learn(text: String) {}              // 学习用户输入（可选）
    suspend fun saveLearnedData() {}        // 保存学习数据（可选）
}

data class PredictionCandidate(
    val text: String,        // 候选词文本
    val score: Float = 1.0f  // 候选词得分（越高越好）
)
```

### EmojiPlugin - 表情插件接口

```kotlin
interface EmojiPlugin : PluginMetadata {
    suspend fun getEmojis(category: String?, searchText: String?, topK: Int = 100): List<EmojiItem>
    suspend fun getCategories(): List<String>
}

data class EmojiItem(
    val id: String,              // 表情唯一标识
    val displayText: String,     // 显示文本（如 "😊" 或 "[开心]"）
    val insertText: String,      // 插入文本
    val imageUrl: String? = null, // 图片 URL（可选，支持 file:///android_asset/）
    val category: String? = null  // 分类名称
)
```

### SpeechPlugin - 语音插件接口

```kotlin
interface SpeechPlugin : PluginMetadata {
    val supportsRealtime: Boolean     // 是否支持实时识别
    val requiresNetwork: Boolean      // 是否需要网络
    
    fun startRecognition(config: AudioConfig, onResult: (SpeechResult) -> Unit): Boolean
    fun sendAudioChunk(data: ByteArray)
    fun stopRecognition()
    fun cancelRecognition()
    suspend fun recognizeOnce(data: ByteArray, config: AudioConfig): String?
    fun getState(): RecognitionState
}
```

## 开发独立插件 APK

### 第一步：创建项目结构

```
my-kime-plugin/
├── build.gradle.kts
├── proguard-rules.pro          # ⚠️ 重要：混淆规则
└── src/main/
    ├── AndroidManifest.xml     # ⚠️ 重要：插件声明
    ├── java/com/example/plugin/
    │   ├── PluginDeclaration.kt          # 空的 Activity
    │   ├── MyPluginFactory.kt           # 插件工厂
    │   ├── MyPredictionPlugin.kt        # 插件实现
    │   └── PluginSettingsActivity.kt    # 设置界面（可选）
    └── res/
        └── values/strings.xml
```

### 第二步：配置 build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")  // 如果需要设置界面
}

android {
    namespace = "com.example.kime.plugin"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.example.kime.plugin.myplugin"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true              // ⚠️ 启用混淆
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    buildFeatures {
        compose = true   // 如果需要设置界面
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

// 自定义 APK 输出文件名
android.applicationVariants.all {
    val pluginName = "my-plugin"
    outputs.all {
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = 
            "$pluginName-$versionName.apk"
    }
}

dependencies {
    implementation(project(":plugin-api"))  // ⚠️ 必须依赖
    
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    
    // 如果需要设置界面
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
}
```

### 第三步：配置 AndroidManifest.xml（⚠️ 关键）

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true">
        
        <!-- 1. 插件声明 Activity - 必须声明 exported="true" -->
        <activity
            android:name=".PluginDeclaration"
            android:exported="true">
            <intent-filter>
                <!-- ⚠️ 必须匹配这个 action -->
                <action android:name="com.kingzcheung.kime.plugin.EXTENSION" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
        
        <!-- 2. 插件设置界面（可选） -->
        <activity
            android:name=".PluginSettingsActivity"
            android:exported="true"
            android:label="插件设置"
            android:theme="@android:style/Theme.Material.Light.NoActionBar">
        </activity>
        
        <!-- 3. 指定插件工厂类 - ⚠️ 必须是完整类名 -->
        <meta-data
            android:name="com.kingzcheung.kime.plugin.factory.class"
            android:value="com.example.plugin.MyPluginFactory" />
        
    </application>

</manifest>
```

### 第四步：配置 ProGuard 规则（⚠️ 最关键）

**必须保留插件相关类，否则动态加载会失败！**

```proguard
# plugin-api/consumer-rules.pro 会自动保留 API 接口
# -keep class com.kingzcheung.kime.plugin.api.** { *; }

# ⚠️ 但必须手动保留你的插件实现类
-dontobfuscate    # 禁用混淆
-dontoptimize     # 禁用优化

# Keep Kotlin standard library
-keep class kotlin.** { *; }
-keep class kotlin.jvm.** { *; }
-keep class kotlin.collections.** { *; }
-keep class kotlin.coroutines.** { *; }
-keep class kotlin.reflect.** { *; }

# CRITICAL: Keep plugin classes for discovery
# ⚠️ 必须保留这三个类！
-keep class com.example.plugin.PluginDeclaration { *; }
-keep class com.example.plugin.MyPluginFactory { *; }
-keep class com.example.plugin.MyPredictionPlugin { *; }

# 如果有其他内部类也需要保留
-keep class com.example.plugin.** { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
```

### 第五步：实现插件声明 Activity

```kotlin
package com.example.plugin

import android.app.Activity

/**
 * 空的 Activity，用于插件发现
 * 主应用通过 Intent Filter 查找这个 Activity 来识别插件
 */
class PluginDeclaration : Activity()
```

### 第六步：实现插件工厂

```kotlin
package com.example.plugin

import com.kingzcheung.kime.plugin.api.EmojiPlugin
import com.kingzcheung.kime.plugin.api.PluginFactory
import com.kingzcheung.kime.plugin.api.PredictionPlugin
import com.kingzcheung.kime.plugin.api.SpeechPlugin

/**
 * 插件工厂 - 负责创建插件实例
 * 
 * ⚠️ 注意：
 * 1. 必须在 AndroidManifest.xml 中通过 meta-data 声明
 * 2. 每个方法应返回同一个实例（单例模式）
 * 3. 不支持的插件类型返回 null
 */
class MyPluginFactory : PluginFactory {
    
    // 单例实例
    private val predictionPlugin by lazy { MyPredictionPlugin() }
    
    override fun createPredictionPlugin(): PredictionPlugin {
        return predictionPlugin
    }
    
    override fun createEmojiPlugin(): EmojiPlugin? = null
    
    override fun createSpeechPlugin(): SpeechPlugin? = null
}
```

### 第七步：实现插件接口

#### 联想词插件示例

```kotlin
package com.example.plugin

import android.content.Context
import android.content.Intent
import android.util.Log
import com.kingzcheung.kime.plugin.api.PluginType
import com.kingzcheung.kime.plugin.api.PredictionCandidate
import com.kingzcheung.kime.plugin.api.PredictionPlugin

class MyPredictionPlugin : PredictionPlugin {
    
    override val id = "my_prediction_plugin"
    override val name = "我的联想引擎"
    override val description = "基于 xxx 的联想插件"
    override val version = "1.0.0"
    override val type = PluginType.PREDICTION
    
    private var isInitialized = false
    
    companion object {
        private const val TAG = "MyPredictionPlugin"
    }
    
    override fun initialize(context: Context): Boolean {
        return initialize(context, null)
    }
    
    override fun initialize(context: Context, apkPath: String?): Boolean {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return true
        }
        
        Log.d(TAG, "Initializing plugin... apkPath=$apkPath")
        
        return try {
            // ⚠️ 加载模型、初始化资源
            // context: 主应用的 Context，可访问主应用私有目录
            // apkPath: 插件 APK 路径，可用于加载插件内资源
            
            isInitialized = true
            Log.d(TAG, "Plugin initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }
    
    override suspend fun predict(inputText: String, topK: Int): List<PredictionCandidate> {
        if (!isInitialized) {
            Log.e(TAG, "Plugin not initialized")
            return emptyList()
        }
        
        if (inputText.isEmpty()) return emptyList()
        
        return try {
            // 实现预测逻辑
            listOf("候选1", "候选2", "候选3")
                .take(topK)
                .map { PredictionCandidate(it, score = 1.0f) }
        } catch (e: Exception) {
            Log.e(TAG, "Prediction failed", e)
            emptyList()
        }
    }
    
    override fun learn(text: String) {
        if (isInitialized) {
            // 学习用户输入
        }
    }
    
    override suspend fun saveLearnedData() {
        if (isInitialized) {
            // 保存学习数据到持久化存储
        }
    }
    
    override fun release() {
        if (isInitialized) {
            // 释放资源
            isInitialized = false
            Log.d(TAG, "Plugin released")
        }
    }
    
    // ⚠️ 可选：如果插件有设置界面
    override fun hasSettings(): Boolean = true
    
    override fun createSettingsIntent(context: Context): Intent {
        val intent = Intent()
        intent.setClassName(
            "com.example.kime.plugin.myplugin",  // 插件包名
            "com.example.plugin.PluginSettingsActivity"  // 设置 Activity 全类名
        )
        return intent
    }
}
```

#### 表情插件示例

```kotlin
package com.example.plugin

import android.content.Context
import android.content.Intent
import com.kingzcheung.kime.plugin.api.EmojiItem
import com.kingzcheung.kime.plugin.api.EmojiPlugin
import com.kingzcheung.kime.plugin.api.PluginType

class MyEmojiPlugin : EmojiPlugin {
    
    override val id = "my_emoji_plugin"
    override val name = "自定义表情包"
    override val description = "我的表情集合"
    override val version = "1.0.0"
    override val type = PluginType.EMOJI
    
    private var emojiList: List<EmojiItem> = emptyList()
    
    override fun initialize(context: Context): Boolean {
        return try {
            // 加载表情数据
            emojiList = loadEmojis()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getEmojis(
        category: String?, 
        searchText: String?, 
        topK: Int
    ): List<EmojiItem> {
        var result = emojiList
        
        // 按分类过滤
        category?.let { cat ->
            result = result.filter { it.category == cat }
        }
        
        // 按搜索文本过滤
        searchText?.let { text ->
            if (text.isNotEmpty()) {
                result = result.filter { 
                    it.displayText.contains(text) || 
                    it.insertText.contains(text)
                }
            }
        }
        
        return result.take(topK)
    }
    
    override suspend fun getCategories(): List<String> {
        return emojiList.mapNotNull { it.category }.distinct()
    }
    
    override fun release() {
        emojiList = emptyList()
    }
    
    override fun hasSettings(): Boolean = true
    
    override fun createSettingsIntent(context: Context): Intent {
        val intent = Intent()
        intent.setClassName(
            "com.example.kime.plugin.myplugin",
            "com.example.plugin.PluginSettingsActivity"
        )
        return intent
    }
    
    private fun loadEmojis(): List<EmojiItem> {
        // 从 assets、数据库或资源文件加载
        return listOf(
            EmojiItem(
                id = "emoji_1",
                displayText = "😊",
                insertText = "😊",
                category = "表情"
            ),
            EmojiItem(
                id = "emoji_2",
                displayText = "[开心]",
                insertText = "[开心]",
                imageUrl = "file:///android_asset/emoji/happy.png",
                category = "图片表情"
            )
        )
    }
}
```

#### 语音插件示例

```kotlin
package com.example.plugin

import android.content.Context
import com.kingzcheung.kime.plugin.api.*

class MySpeechPlugin : SpeechPlugin {
    
    override val id = "my_speech_plugin"
    override val name = "离线语音识别"
    override val description = "基于 xxx 的语音识别"
    override val version = "1.0.0"
    override val type = PluginType.SPEECH
    
    override val supportsRealtime = true
    override val requiresNetwork = false
    
    private var state = RecognitionState.IDLE
    
    override fun initialize(context: Context): Boolean = true
    
    override fun startRecognition(config: AudioConfig, onResult: (SpeechResult) -> Unit): Boolean {
        state = RecognitionState.LISTENING
        // 启动识别，通过 onResult 回调结果
        return true
    }
    
    override fun sendAudioChunk(data: ByteArray) {
        // 发送音频数据块
    }
    
    override fun stopRecognition() {
        state = RecognitionState.IDLE
    }
    
    override fun cancelRecognition() {
        state = RecognitionState.IDLE
    }
    
    override suspend fun recognizeOnce(data: ByteArray, config: AudioConfig): String? {
        // 一次性识别
        return "识别结果"
    }
    
    override fun getState(): RecognitionState = state
    
    override fun release() {
        state = RecognitionState.IDLE
    }
}
```

### 第八步：实现设置界面（可选）

```kotlin
package com.example.plugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*

class PluginSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface {
                    SettingsScreen(
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    // 使用 Jetpack Compose 实现设置界面
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插件设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        // 设置内容
    }
}
```

## 安装和构建插件

### 构建插件 APK

```bash
# 构建插件
./gradlew assembleRelease

# 输出位置
# my-plugin/build/outputs/apk/release/my-plugin-1.0.0.apk
```


## 关键注意事项（⚠️ 必读）

### 1. ProGuard 混淆配置（最重要）

**为什么需要 ProGuard 规则？**

插件使用动态加载机制，主应用通过反射查找插件类。如果插件类被混淆或优化，会导致：
- 类名改变，无法通过 meta-data 中的类名找到工厂类
- 方法签名改变，无法调用插件接口方法
- 插件加载失败，抛出 `ClassNotFoundException` 或 `MethodNotFoundException`

**必须保留的类：**

1. **PluginDeclaration** - 用于插件发现的 Activity
2. **PluginFactory** - 插件工厂类
3. **Plugin 实现类** - 所有实现接口的类

**plugin-api 自带的 consumer-rules.pro：**

```proguard
# plugin-api/consumer-rules.pro
# Keep all public API classes - CRITICAL for plugin compatibility
-keep class com.kingzcheung.kime.plugin.api.** { *; }
-keep interface com.kingzcheung.kime.plugin.api.** { *; }
-keep enum com.kingzcheung.kime.plugin.api.** { *; }
```

这个文件会自动打包到 plugin-api 库中，并在插件构建时自动应用。它保留了所有 API 接口类。

**插件项目必须手动配置：**

```proguard
# 1. 禁用混淆和优化
-dontobfuscate
-dontoptimize

# 2. 保留 Kotlin 标准库
-keep class kotlin.** { *; }

# 3. ⚠️ 保留你的插件实现类（这是关键！）
-keep class com.example.plugin.PluginDeclaration { *; }
-keep class com.example.plugin.MyPluginFactory { *; }
-keep class com.example.plugin.MyPredictionPlugin { *; }

# 4. 如果有其他辅助类也需要保留
-keep class com.example.plugin.** { *; }
```

**配置方式：**

在 `build.gradle.kts` 中：

```kotlin
android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")  // plugin-api 自带的
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"  // 你的插件规则
            )
        }
    }
}
```

### 2. AndroidManifest 配置要点

**必须配置的三要素：**

1. **PluginDeclaration Activity** - 用于插件发现
   ```xml
   <activity android:name=".PluginDeclaration" android:exported="true">
       <intent-filter>
           <!-- ⚠️ 必须完全匹配 -->
           <action android:name="com.kingzcheung.kime.plugin.EXTENSION" />
           <category android:name="android.intent.category.DEFAULT" />
       </intent-filter>
   </activity>
   ```

2. **meta-data 工厂类声明**
   ```xml
   <meta-data
       android:name="com.kingzcheung.kime.plugin.factory.class"
       android:value="com.example.plugin.MyPluginFactory" />
   ```
   
   **注意：**
   - 必须使用完整类名（包含包名）
   - 类名不能被混淆（依赖 ProGuard 规则）
   - 类必须实现 `PluginFactory` 接口

3. **Settings Activity**（可选）
   ```xml
   <activity
       android:name=".PluginSettingsActivity"
       android:exported="true"
       android:theme="@android:style/Theme.Material.Light.NoActionBar">
   </activity>
   ```

### 3. Context 使用

插件获得的是**主应用的 Context**，特性如下：

- 可以访问主应用的私有目录：
  - `context.filesDir` → `/data/data/com.kingzcheung.kime/files`
  - `context.cacheDir` → `/data/data/com.kingzcheung.kime/cache`

- **不能**访问插件自己的资源：
  - 不能直接使用 `context.resources`
  - 不能直接加载 `R.xxx` 资源

- **如何加载插件资源：**
  
  使用 `initialize(context, apkPath)` 中的 `apkPath`：
  
  ```kotlin
  override fun initialize(context: Context, apkPath: String?): Boolean {
      apkPath?.let { path ->
          // 创建插件专用的 ClassLoader
          val pluginClassLoader = PathClassLoader(path, context.classLoader)
          
          // 加载插件资源
          val packageManager = context.packageManager
          val packageInfo = packageManager.getPackageArchiveInfo(path, 0)
          val applicationInfo = packageInfo.applicationInfo
          applicationInfo.sourceDir = path
          applicationInfo.publicSourceDir = path
          
          val pluginResources = packageManager.getResourcesForApplication(applicationInfo)
          
          // 现在可以加载插件资源了
          val asset = pluginResources.assets.open("model.onnx")
      }
      return true
  }
  ```

### 4. 生命周期管理

**插件生命周期流程：**

1. **加载阶段**：
   - 主应用扫描已安装应用
   - 查找 `com.kingzcheung.kime.plugin.EXTENSION` intent
   - 创建 `PathClassLoader(apkPath, parentClassLoader)`
   - 通过反射加载工厂类

2. **初始化阶段**：
   - 调用 `PluginFactory.createXxxPlugin()`
   - 调用 `plugin.initialize(context, apkPath)`
   - **⚠️ 必须检查是否已初始化，避免重复初始化**

3. **运行阶段**：
   - 根据插件类型调用相应方法
   - 使用 `suspend` 函数支持异步操作

4. **卸载阶段**：
   - 调用 `plugin.release()`
   - **⚠️ 必须释放所有资源，避免内存泄漏**

**正确实现：**

```kotlin
private var isInitialized = false

override fun initialize(context: Context, apkPath: String?): Boolean {
    if (isInitialized) return true  // ⚠️ 防止重复初始化
    
    try {
        // 初始化逻辑
        isInitialized = true
        return true
    } catch (e: Exception) {
        return false
    }
}

override fun release() {
    if (!isInitialized) return
    
    // ⚠️ 清理所有资源
    isInitialized = false
}
```

### 5. 线程与协程

- 插件在同一进程内运行，**无 IPC 开销**
- 使用 `suspend` 函数支持异步处理
- 建议使用 `Dispatchers.IO` 处理耗时任务：

```kotlin
override suspend fun predict(inputText: String, topK: Int): List<PredictionCandidate> {
    return withContext(Dispatchers.IO) {
        // 在 IO 线程执行耗时操作
        doPrediction(inputText, topK)
    }
}
```

### 6. 单例模式

**PluginFactory 应返回同一个实例：**

```kotlin
class MyPluginFactory : PluginFactory {
    private val plugin by lazy { MyPredictionPlugin() }
    
    override fun createPredictionPlugin(): PredictionPlugin = plugin
}
```

**原因：**
- 避免重复创建实例
- 保持状态一致性
- 避免资源浪费

### 7. 插件更新

- 更新插件 APK 后，需要重启主应用
- 主应用会重新扫描和加载插件
- 已保存的数据需要迁移到新版本

### 8. 调试技巧

**查看插件加载日志：**

```bash
adb logcat | grep -i "ExtensionManager"
adb logcat | grep -i "MyPlugin"
```

**常见错误：**

1. `ClassNotFoundException`
   - 原因：ProGuard 混淆了工厂类
   - 解决：添加 `-keep class YourFactory { *; }`

2. `NoSuchMethodException`
   - 原因：接口方法被优化删除
   - 解决：`-keep class YourPlugin { *; }` 或检查 `consumer-rules.pro`

3. 插件无法发现
   - 原因：AndroidManifest 配置错误
   - 解决：检查 intent-filter 和 meta-data

4. 资源加载失败
   - 原因：使用了主应用 Context 加载插件资源
   - 解决：使用 `apkPath` 创建专用 ClassLoader

## 现有插件示例

查看项目中的插件实现，学习最佳实践：

### 1. prediction-onnx - 联想词插件

**特点：**
- 使用 ONNX Runtime 进行联想词预测
- 支持 N-gram 模型融合
- 支持用户输入学习
- 有设置界面（Compose）

**关键文件：**
- `plugins/prediction-onnx/src/main/java/.../OnnxPredictionPlugin.kt` - 插件实现
- `plugins/prediction-onnx/src/main/java/.../PredictionPluginFactory.kt` - 工厂类
- `plugins/prediction-onnx/build.gradle.kts` - 构建配置（包含 ONNX 下载）
- `plugins/prediction-onnx/proguard-rules.pro` - 混淆规则

### 2. kaomoji - 颜文字插件

**特点：**
- 纯 Kotlin 实现，无 native 依赖
- 预定义的颜文字数据集
- 简单的分类和搜索功能
- 有设置界面（Compose）

**关键文件：**
- `plugins/kaomoji/src/main/java/.../KaomojiPlugin.kt` - 插件实现
- `plugins/kaomoji/src/main/java/.../KaomojiPluginFactory.kt` - 工厂类
- `plugins/kaomoji/src/main/java/.../KaomojiData.kt` - 数据定义
- `plugins/kaomoji/proguard-rules.pro` - 混淆规则

### 3. emoji-sticker - 表情贴纸插件

**特点：**
- 图片表情包支持
- 从 assets 加载图片
- 分类管理

**关键文件：**
- `plugins/emoji-sticker/src/main/java/.../EmojiStickerPlugin.kt` - 插件实现
- `plugins/emoji-sticker/src/main/java/.../EmojiPluginFactory.kt` - 工厂类

## 开发流程总结

完整的插件开发流程：

1. **创建项目**
   - 创建 Android Application 项目
   - 添加 plugin-api 依赖
   - 配置 Kotlin 和 Compose

2. **实现核心类**
   - PluginDeclaration（空 Activity）
   - PluginFactory（工厂类）
   - Plugin 实现（实现对应接口）

3. **配置 AndroidManifest**
   - 声明 PluginDeclaration Activity
   - 配置 intent-filter
   - 添加 meta-data 工厂类声明

4. **配置 ProGuard** ⚠️
   - 禁用混淆和优化
   - 保留插件类
   - 保留 Kotlin 标准库

5. **实现功能**
   - 实现 `initialize()` 和 `release()`
   - 实现业务逻辑方法
   - 实现设置界面（可选）

6. **构建和测试**
   - 构建 APK：`./gradlew assembleRelease`
   - 安装插件：`adb install plugin.apk`
   - 启动主应用测试

7. **调试问题**
   - 查看 logcat 日志
   - 检查 ProGuard 配置
   - 检查 AndroidManifest 配置

## 快速检查清单

发布插件前检查：

- [ ] ProGuard 规则保留了 PluginDeclaration、Factory、Plugin 类
- [ ] AndroidManifest 有正确的 intent-filter
- [ ] meta-data 中的类名是完整类名
- [ ] initialize() 防止重复初始化
- [ ] release() 清理所有资源
- [ ] PluginFactory 返回单例实例
- [ ] 测试插件加载和功能运行
- [ ] 查看日志无 ClassNotFoundException

## 参考

- [插件架构设计](./ASSOCIATION.md) - 联想词插件详细设计
- [plugin-api 源码](../../plugin-api/) - API 接口定义
- [现有插件实现](../../plugins/) - 学习最佳实践