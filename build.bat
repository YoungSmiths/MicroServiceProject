@echo off
REM Maven Build Script for Windows

echo ========================================
echo MicroService Project Build Script
echo ========================================

REM 设置环境变量
call scripts\set-env.bat

REM 清理并编译
echo [INFO] Cleaning and building project...
call mvn clean install -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Build completed successfully!
) else (
    echo [ERROR] Build failed!
    exit /b 1
)

REM 复制 JAR 文件到 target 目录
echo [INFO] Copying JAR files...
if not exist "target\lib" mkdir target\lib

REM 显示构建产物
echo [INFO] Build artifacts:
dir /s /b target\*.jar

echo ========================================
echo Build process completed!
echo ========================================
