#!/bin/bash

# 从 Hugging Face 下载 ONNX 模型文件
# 模型仓库: https://huggingface.co/rkingzhong/predictive-text-small

set -e

MODEL_DIR="plugins/prediction-onnx/src/main/assets/association_model"
HF_MODEL="rkingzhong/predictive-text-small"
HF_BASE_URL="https://huggingface.co/${HF_MODEL}/resolve/main"

echo "=== 开始下载模型文件 ==="
echo "模型仓库: ${HF_MODEL}"
echo ""

# 创建模型目录
mkdir -p "$MODEL_DIR"

# 检查模型文件是否已存在
if [ -f "$MODEL_DIR/model_int8_dynamic.onnx" ] && [ -f "$MODEL_DIR/vocab.json" ]; then
    echo "✓ 模型文件已存在，跳过下载"
    ls -lh "$MODEL_DIR/"
    exit 0
fi

# 需要下载的文件列表
FILES=(
    "model.onnx"
    "model.onnx.data"
    "model_int8_dynamic.onnx"
    "vocab.json"
    "vocab.txt"
)

echo "下载文件列表:"
for file in "${FILES[@]}"; do
    echo "  - $file"
done
echo ""

# 下载文件
for file in "${FILES[@]}"; do
    target_file="$MODEL_DIR/$file"
    url="${HF_BASE_URL}/${file}"
    
    echo "下载: $file"
    
    # 使用 wget 或 curl
    if command -v wget >/dev/null 2>&1; then
        wget -q --show-progress "$url" -O "$target_file" || {
            echo "⚠ 警告: $file 下载失败，可能文件不存在"
            rm -f "$target_file"
        }
    elif command -v curl >/dev/null 2>&1; then
        curl -L --progress-bar "$url" -o "$target_file" || {
            echo "⚠ 警告: $file 下载失败，可能文件不存在"
            rm -f "$target_file"
        }
    else
        echo "❌ 错误: 需要 wget 或 curl"
        exit 1
    fi
done

echo ""
echo "=== 验证下载的文件 ==="

# 检查必需文件
REQUIRED_FILES=(
    "model_int8_dynamic.onnx"
    "vocab.json"
)

ALL_REQUIRED_EXIST=true
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$MODEL_DIR/$file" ]; then
        SIZE=$(du -h "$MODEL_DIR/$file" | cut -f1)
        echo "✓ $file ($SIZE)"
    else
        echo "❌ $file (缺失 - 必需)"
        ALL_REQUIRED_EXIST=false
    fi
done

# 检查可选文件
OPTIONAL_FILES=(
    "model.onnx"
    "model.onnx.data"
    "vocab.txt"
)

for file in "${OPTIONAL_FILES[@]}"; do
    if [ -f "$MODEL_DIR/$file" ]; then
        SIZE=$(du -h "$MODEL_DIR/$file" | cut -f1)
        echo "✓ $file ($SIZE) - 可选"
    else
        echo "○ $file (缺失 - 可选)"
    fi
done

echo ""

if [ "$ALL_REQUIRED_EXIST" = false ]; then
    echo "❌ 缺失必需文件，构建可能失败"
    exit 1
fi

echo "=== 模型文件下载完成 ==="
echo ""
echo "文件位置: $MODEL_DIR/"
ls -lh "$MODEL_DIR/"
echo ""

# 显示总大小
TOTAL_SIZE=$(du -sh "$MODEL_DIR" | cut -f1)
echo "总大小: $TOTAL_SIZE"