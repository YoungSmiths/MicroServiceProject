@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

set "LOG_DIR=%SCRIPT_DIR%logs"
set "LOG_FILE=%LOG_DIR%\start-all.log"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

set "JAVA_EXE=C:\Program Files\Java\jdk-17\bin\java.exe"
if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=java"
)

set "GATEWAY_JAR=%SCRIPT_DIR%micro-gateway\target\micro-gateway-1.0.0.jar"
set "USER_JAR=%SCRIPT_DIR%micro-user-service\target\micro-user-service-1.0.0.jar"
set "ORDER_JAR=%SCRIPT_DIR%micro-order-service\target\micro-order-service-1.0.0.jar"
set "REDIS_PASSWORD=123456"
set "SENTINEL_DASHBOARD=127.0.0.1:8181"
set "SEATA_ENABLED=false"
set "JM.LOG.PATH=%LOG_DIR%\nacos-client"
if not exist "%JM.LOG.PATH%" mkdir "%JM.LOG.PATH%"

echo [%date% %time%] start-all.bat begin > "%LOG_FILE%"
echo [%date% %time%] java executable: %JAVA_EXE% >> "%LOG_FILE%"
echo [%date% %time%] redis password configured for child processes >> "%LOG_FILE%"
echo [%date% %time%] nacos client log path: %JM.LOG.PATH% >> "%LOG_FILE%"

echo ========================================
echo Starting MicroService Project
echo ========================================
echo [INFO] SEATA_ENABLED = %SEATA_ENABLED%
echo [INFO] To enable Seata, set SEATA_ENABLED=true before running this script
echo ========================================

if not exist "%GATEWAY_JAR%" (
    echo [ERROR] Gateway jar not found: %GATEWAY_JAR%
    echo [ERROR] Please run build.bat first
    echo [%date% %time%] gateway jar missing >> "%LOG_FILE%"
    exit /b 1
)

if not exist "%USER_JAR%" (
    echo [ERROR] User Service jar not found: %USER_JAR%
    echo [ERROR] Please run build.bat first
    echo [%date% %time%] user-service jar missing >> "%LOG_FILE%"
    exit /b 1
)

if not exist "%ORDER_JAR%" (
    echo [ERROR] Order Service jar not found: %ORDER_JAR%
    echo [ERROR] Please run build.bat first
    echo [%date% %time%] order-service jar missing >> "%LOG_FILE%"
    exit /b 1
)

echo [INFO] Checking Docker...
docker --version >> "%LOG_FILE%" 2>&1
if errorlevel 1 (
    echo [ERROR] Docker not found or not running
    echo [%date% %time%] docker check failed >> "%LOG_FILE%"
    exit /b 1
)

echo [INFO] Starting infrastructure services (Docker)...
echo [%date% %time%] docker compose up -d >> "%LOG_FILE%"
docker compose up -d >> "%LOG_FILE%" 2>&1

echo [INFO] Waiting for infrastructure to start (15 seconds)...
timeout /t 15 /nobreak > nul

echo [INFO] Starting Gateway...
echo [%date% %time%] starting gateway >> "%LOG_FILE%"
start "Gateway" cmd /k "cd /d %SCRIPT_DIR% && %JAVA_EXE% -jar %GATEWAY_JAR%"

echo [INFO] Waiting for Gateway to start (5 seconds)...
timeout /t 5 /nobreak > nul

echo [INFO] Starting User Service...
echo [%date% %time%] starting user-service >> "%LOG_FILE%"
start "UserService" cmd /k "cd /d %SCRIPT_DIR% && set SEATA_ENABLED=%SEATA_ENABLED% && %JAVA_EXE% -jar %USER_JAR%"

echo [INFO] Waiting for User Service to start (5 seconds)...
timeout /t 5 /nobreak > nul

echo [INFO] Starting Order Service...
echo [%date% %time%] starting order-service >> "%LOG_FILE%"
start "OrderService" cmd /k "cd /d %SCRIPT_DIR% && set SEATA_ENABLED=%SEATA_ENABLED% && %JAVA_EXE% -jar %ORDER_JAR%"

echo ========================================
echo All services started
echo ========================================
echo Access URLs:
echo   Nacos:     http://127.0.0.1:8848/nacos
echo   Sentinel:  http://127.0.0.1:8181
echo   Gateway:   http://127.0.0.1:8080
echo   Mock Payment: http://127.0.0.1:8080/mock/payment/charge
echo ========================================
echo To stop all services, run: stop-all.bat
echo ========================================
echo [%date% %time%] start-all.bat end >> "%LOG_FILE%"
