#!/bin/bash
# Maven Build Script for Linux/Mac

echo "========================================"
echo "MicroService Project Build Script"
echo "========================================"

# 设置环境变量
source scripts/set-env.sh

# 清理并编译
echo "[INFO] Cleaning and building project..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "[SUCCESS] Build completed successfully!"
else
    echo "[ERROR] Build failed!"
    exit 1
fi

# 创建 lib 目录
mkdir -p target/lib

# 显示构建产物
echo "[INFO] Build artifacts:"
find target -name "*.jar" -type f

echo "========================================"
echo "Build process completed!"
echo "========================================"
