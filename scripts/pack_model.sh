#!/bin/bash

# 创建模型文件压缩包脚本
# 用于将模型文件打包并上传到 GitHub Release

set -e

MODEL_DIR="plugins/prediction-onnx/src/main/assets/association_model"
OUTPUT_FILE="association_model.tar.gz"

echo "=== 打包模型文件 ==="

if [ ! -d "$MODEL_DIR" ]; then
    echo "❌ 模型目录不存在: $MODEL_DIR"
    exit 1
fi

# 检查必要的文件
REQUIRED_FILES=(
    "model.onnx"
    "model.onnx.data"
    "model_int8_dynamic.onnx"
    "vocab.json"
)

ALL_FILES_EXIST=true
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$MODEL_DIR/$file" ]; then
        SIZE=$(du -h "$MODEL_DIR/$file" | cut -f1)
        echo "✓ $file ($SIZE)"
    else
        echo "❌ $file (缺失)"
        ALL_FILES_EXIST=false
    fi
done

if [ "$ALL_FILES_EXIST" = false ]; then
    echo "❌ 部分模型文件缺失，无法打包"
    exit 1
fi

# 打包文件
echo ""
echo "正在创建压缩包..."

# 创建临时目录结构
TMP_DIR=$(mktemp -d)
mkdir -p "$TMP_DIR/association_model"

# 复制文件
cp -v "$MODEL_DIR"/*.onnx "$TMP_DIR/association_model/"
cp -v "$MODEL_DIR"/*.onnx.data "$TMP_DIR/association_model/" || true
cp -v "$MODEL_DIR"/vocab.json "$TMP_DIR/association_model/"

# 创建压缩包
cd "$TMP_DIR"
tar -czf "$OUTPUT_FILE" association_model/

# 移动到脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mv "$OUTPUT_FILE" "$SCRIPT_DIR/"

# 清理临时目录
cd "$SCRIPT_DIR"
rm -rf "$TMP_DIR"

# 显示结果
echo ""
echo "=== 打包完成 ==="
ls -lh "$SCRIPT_DIR/$OUTPUT_FILE"

echo ""
echo "压缩包内容:"
tar -tzf "$SCRIPT_DIR/$OUTPUT_FILE"

echo ""
echo "下一步:"
echo "1. 上传此文件到 GitHub Release"
echo "2. 在 CI 构建时会自动下载此文件"
echo ""
echo "文件位置: $SCRIPT_DIR/$OUTPUT_FILE"