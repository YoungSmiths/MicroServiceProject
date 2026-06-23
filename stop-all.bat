@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

set "LOG_DIR=%SCRIPT_DIR%logs"
set "LOG_FILE=%LOG_DIR%\stop-all.log"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

set "GATEWAY_PORT=8080"
set "USER_PORT=8081"
set "ORDER_PORT=8082"

echo ========================================
echo Stopping MicroService Project
echo ========================================
echo [%date% %time%] stop-all.bat begin > "%LOG_FILE%"

call :kill_port_process %GATEWAY_PORT% micro-gateway
call :kill_port_process %USER_PORT% micro-user-service
call :kill_port_process %ORDER_PORT% micro-order-service

echo [INFO] Stopping Docker containers...
docker compose down >> "%LOG_FILE%" 2>&1

echo ========================================
echo All services stopped
echo ========================================
echo [%date% %time%] stop-all.bat end >> "%LOG_FILE%"
exit /b 0

:kill_port_process
set "PORT=%~1"
set "SERVICE_NAME=%~2"
set "FOUND_PID="

echo [INFO] Stopping %SERVICE_NAME% on port %PORT%...
echo [%date% %time%] stopping %SERVICE_NAME% on port %PORT% >> "%LOG_FILE%"

for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
    set "FOUND_PID=1"
    echo [INFO] Killing PID %%p for %SERVICE_NAME%
    echo [%date% %time%] killing pid %%p for %SERVICE_NAME% >> "%LOG_FILE%"
    taskkill /F /PID %%p >> "%LOG_FILE%" 2>&1
)

if not defined FOUND_PID (
    echo [INFO] No listening process found on port %PORT%
    echo [%date% %time%] no process found on port %PORT% >> "%LOG_FILE%"
)

timeout /t 2 /nobreak > nul
exit /b 0
