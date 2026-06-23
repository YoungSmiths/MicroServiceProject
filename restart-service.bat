@echo off
setlocal enabledelayedexpansion

if "%~1"=="" goto :usage

set "SERVICE=%~1"
set "SKIP_TESTS=%~2"

set "PROJECT_ROOT=C:\Users\15321\Documents\trae_projects\MicroServiceProject"
set "MAVEN_EXE=D:\software\green\apache-maven-3.9.8\bin\mvn.cmd"
set "JAVA_EXE=C:\Program Files\Java\jdk-17\bin\java.exe"

if not exist "%MAVEN_EXE%" set "MAVEN_EXE=mvn"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"

if /I "%SERVICE%"=="gateway" (
    set "MODULE_DIR=micro-gateway"
    set "PORT=8080"
    set "JAR_NAME=micro-gateway-1.0.0.jar"
    set "JAVA_OPTS=-DNACOS_SERVER=127.0.0.1:8848 -DSENTINEL_DASHBOARD=127.0.0.1:8181 -DREDIS_HOST=127.0.0.1 -DREDIS_PORT=6379 -DREDIS_PASSWORD=123456"
    goto :main
)

if /I "%SERVICE%"=="user" (
    set "MODULE_DIR=micro-user-service"
    set "PORT=8081"
    set "JAR_NAME=micro-user-service-1.0.0.jar"
    set "JAVA_OPTS=-DNACOS_SERVER=127.0.0.1:8848 -DSENTINEL_DASHBOARD=127.0.0.1:8181 -DREDIS_HOST=127.0.0.1 -DREDIS_PORT=6379 -DREDIS_PASSWORD=123456 -DSEATA_ENABLED=false"
    goto :main
)

if /I "%SERVICE%"=="order" (
    set "MODULE_DIR=micro-order-service"
    set "PORT=8082"
    set "JAR_NAME=micro-order-service-1.0.0.jar"
    set "JAVA_OPTS=-DNACOS_SERVER=127.0.0.1:8848 -DSENTINEL_DASHBOARD=127.0.0.1:8181 -DREDIS_HOST=127.0.0.1 -DREDIS_PORT=6379 -DREDIS_PASSWORD=123456 -DSEATA_ENABLED=false"
    goto :main
)

echo [ERROR] Unsupported service: %SERVICE%
goto :usage

:main
set "SERVICE_DIR=%PROJECT_ROOT%\%MODULE_DIR%"
set "JAR_PATH=%SERVICE_DIR%\target\%JAR_NAME%"

echo ========================================
echo Restart Service: %MODULE_DIR%
echo ========================================
echo.
echo [1/4] Stop old process on port %PORT%...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
    echo Stopping PID=%%p
    taskkill /PID %%p /F >nul 2>nul
)

timeout /t 2 /nobreak >nul

echo.
echo [2/4] Clean install %MODULE_DIR%...
pushd "%PROJECT_ROOT%"

if /I "%SKIP_TESTS%"=="skipTests" (
    call "%MAVEN_EXE%" -pl "%MODULE_DIR%" -am clean install -DskipTests -Dmaven.test.skip=true
) else (
    call "%MAVEN_EXE%" -pl "%MODULE_DIR%" -am clean install
)

if errorlevel 1 (
    echo [ERROR] Maven build failed
    popd
    exit /b 1
)

popd

echo.
echo [3/4] Check JAR...
if not exist "%JAR_PATH%" (
    echo [ERROR] JAR not found: %JAR_PATH%
    exit /b 1
)
echo [OK] JAR exists: %JAR_PATH%

echo.
echo [4/4] Start service...
start "%MODULE_DIR%" cmd /k "cd /d %PROJECT_ROOT% && %JAVA_EXE% %JAVA_OPTS% -jar %JAR_PATH%"

echo.
echo Waiting 10 seconds...
timeout /t 10 /nobreak >nul

netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo [WARN] Port %PORT% is not listening yet. Check service window logs.
) else (
    echo [OK] Service started, port %PORT% is listening.
)

echo.
echo Done.
exit /b 0

:usage
echo Usage:
echo   restart-service.bat gateway
echo   restart-service.bat user
echo   restart-service.bat order
echo.
echo Skip tests:
echo   restart-service.bat gateway skipTests
exit /b 1
