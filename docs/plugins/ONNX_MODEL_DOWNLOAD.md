# ONNX 模型文件下载说明

## 问题背景

prediction-onnx 插件需要 ONNX 模型文件才能工作，但这些文件体积较大：

- `model.onnx.data` - 35MB（原始模型权重）
- `model_int8_dynamic.onnx` - 16MB（量化模型）
- `vocab.json` - 165KB（词表）
- `model.onnx` - 35KB（模型结构）

总计约 52MB，不适合直接提交到 Git 仓库。

## 解决方案

### 方案 1：自动下载（推荐）

在构建时从 Hugging Face 自动下载模型文件：

**模型仓库**: https://huggingface.co/rkingzhong/predictive-text-small

#### 本地构建

```bash
# 运行下载脚本
./scripts/download_model.sh

# 验证文件
ls -lh plugins/prediction-onnx/src/main/assets/
```

#### GitHub Actions 构建

在 CI 构建流程中已自动配置，会在构建前下载模型文件。

查看 `.github/workflows/release.yml` 配置：
```yaml
- name: Download ONNX Model Files from HuggingFace
  run: |
    chmod +x scripts/download_model.sh
    scripts/download_model.sh
```

### 方案 2：手动下载

如果自动下载失败，可以手动下载：

1. 访问模型仓库：https://huggingface.co/rkingzhong/predictive-text-small
2. 点击 "Files and versions" 标签
3. 下载以下文件：
   - `model.onnx`
   - `model.onnx.data`
   - `model_int8_dynamic.onnx`
   - `vocab.json`
   - `vocab.txt`
4. 放入目录：`plugins/prediction-onnx/src/main/assets/`

## 文件说明

### 必需文件

- `model_int8_dynamic.onnx` - INT8 量化模型（16MB），**必需**
- `vocab.json` - 词表文件（165KB），**必需**

### 可选文件

- `model.onnx` - 原始 ONNX 模型结构（35KB），可选
- `model.onnx.data` - 原始模型权重（35MB），可选
- `vocab.txt` - 词表文本格式（可选）

## 验证模型文件

运行脚本后会自动验证：

```bash
./scripts/download_model.sh
```

输出示例：
```
=== 验证下载的文件 ===
✓ model_int8_dynamic.onnx (16M)
✓ vocab.json (168K)
✓ model.onnx (36K) - 可选
✓ model.onnx.data (35M) - 可选
○ vocab.txt (缺失 - 可选)

=== 模型文件下载完成 ===
总大小: 52M
```

## 模型文件 Git 配置

模型文件已配置在 `.gitignore` 中，不会提交到 Git：

```
plugins/prediction-onnx/src/main/assets/.gitignore:
*.onnx.data    # 大文件（>50MB）
vocab.json     # 词表
vocab.txt      # 词表文本
```

## 插件构建大小对比

### 包含完整模型文件

- arm64-v8a: ~57MB
- universal: ~77MB

### 缺少模型文件

- arm64-v8a: ~11MB
- universal: ~31MB

**注意**: 缺少模型文件时，插件功能将受限或无法工作。

## 故障排查

### 下载失败

检查网络连接和 Hugging Face 访问：

```bash
# 测试连接
curl -I https://huggingface.co/rkingzhong/predictive-text-small

# 手动下载单个文件测试
wget https://huggingface.co/rkingzhong/predictive-text-small/resolve/main/vocab.json
```

### GitHub Actions 构建失败

1. 查看构建日志中的 "Download ONNX Model Files" 步骤
2. 如果下载失败，构建会继续但插件可能功能受限
3. 检查 Hugging Face 仓库是否可访问

### 本地构建缺少模型

1. 运行 `./scripts/download_model.sh`
2. 检查文件是否下载成功
3. 验证文件完整性

## 相关文档

- [插件开发指南](docs/plugins/PLUGIN_DEVELOPMENT_GUIDE.md)
- [模型仓库](https://huggingface.co/rkingzhong/predictive-text-small)
- [下载脚本](scripts/download_model.sh)