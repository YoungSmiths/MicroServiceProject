@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

set "LOG_DIR=%SCRIPT_DIR%logs"
set "LOG_FILE=%LOG_DIR%\stop-all.log"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo ========================================
echo Stopping MicroService Project
echo ========================================
echo [%date% %time%] stop-all.bat begin > "%LOG_FILE%"

echo [INFO] Stopping micro-gateway...
taskkill /F /FI "WINDOWTITLE eq Gateway*" >> "%LOG_FILE%" 2>&1
timeout /t 2 /nobreak > nul

echo [INFO] Stopping micro-user-service...
taskkill /F /FI "WINDOWTITLE eq UserService*" >> "%LOG_FILE%" 2>&1
timeout /t 2 /nobreak > nul

echo [INFO] Stopping micro-order-service...
taskkill /F /FI "WINDOWTITLE eq OrderService*" >> "%LOG_FILE%" 2>&1
timeout /t 2 /nobreak > nul

echo [INFO] Stopping Docker containers...
docker compose down >> "%LOG_FILE%" 2>&1

echo ========================================
echo All services stopped
echo ========================================
echo [%date% %time%] stop-all.bat end >> "%LOG_FILE%"
