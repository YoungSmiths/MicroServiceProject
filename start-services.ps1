# Only start the microservices (skip build and docker infrastructure)
# Requires: JAR files already built, infrastructure already running

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Starting Microservices Only" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$JAVA_EXE = "java"
if (Test-Path "C:\Program Files\Java\jdk-17\bin\java.exe") {
    $JAVA_EXE = "C:\Program Files\Java\jdk-17\bin\java.exe"
}

$SCRIPT_DIR = $PSScriptRoot
$GATEWAY_JAR = "$SCRIPT_DIR\micro-gateway\target\micro-gateway-1.0.0.jar"
$USER_JAR = "$SCRIPT_DIR\micro-user-service\target\micro-user-service-1.0.0.jar"
$ORDER_JAR = "$SCRIPT_DIR\micro-order-service\target\micro-order-service-1.0.0.jar"

# Check if JAR files exist
Write-Host "[1/3] Checking JAR files..." -ForegroundColor Yellow
if (-not (Test-Path $GATEWAY_JAR)) {
    Write-Host "[ERROR] Gateway JAR not found: $GATEWAY_JAR" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $USER_JAR)) {
    Write-Host "[ERROR] User Service JAR not found: $USER_JAR" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $ORDER_JAR)) {
    Write-Host "[ERROR] Order Service JAR not found: $ORDER_JAR" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] All JAR files found" -ForegroundColor Green

Write-Host ""
Write-Host "[2/3] Starting services..." -ForegroundColor Yellow

# Set environment variables
$env:NACOS_SERVER = "127.0.0.1:8848"
$env:SENTINEL_DASHBOARD = "127.0.0.1:8181"
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
$env:REDIS_PASSWORD = "123456"
$env:SEATA_ENABLED = "false"

Write-Host "Starting Gateway..." -ForegroundColor Cyan
$gatewayArgs = "-DNACOS_SERVER=127.0.0.1:8848 -DSENTINEL_DASHBOARD=127.0.0.1:8181 -DREDIS_HOST=127.0.0.1 -DREDIS_PORT=6379 -DREDIS_PASSWORD=123456 -jar `"$GATEWAY_JAR`""
$gatewayProc = Start-Process -FilePath $JAVA_EXE -ArgumentList $gatewayArgs -WindowStyle Normal -PassThru -WorkingDirectory $SCRIPT_DIR

Start-Sleep -Seconds 5

Write-Host "Starting User Service..." -ForegroundColor Cyan
$userArgs = "-DNACOS_SERVER=127.0.0.1:8848 -DSENTINEL_DASHBOARD=127.0.0.1:8181 -DREDIS_HOST=127.0.0.1 -DREDIS_PORT=6379 -DREDIS_PASSWORD=123456 -DSEATA_ENABLED=false -jar `"$USER_JAR`""
$userProc = Start-Process -FilePath $JAVA_EXE -ArgumentList $userArgs -WindowStyle Normal -PassThru -WorkingDirectory $SCRIPT_DIR

Start-Sleep -Seconds 5

Write-Host "Starting Order Service..." -ForegroundColor Cyan
$orderArgs = "-DNACOS_SERVER=127.0.0.1:8848 -DSENTINEL_DASHBOARD=127.0.0.1:8181 -DREDIS_HOST=127.0.0.1 -DREDIS_PORT=6379 -DREDIS_PASSWORD=123456 -DSEATA_ENABLED=false -jar `"$ORDER_JAR`""
$orderProc = Start-Process -FilePath $JAVA_EXE -ArgumentList $orderArgs -WindowStyle Normal -PassThru -WorkingDirectory $SCRIPT_DIR

Write-Host ""
Write-Host "[3/3] All services started!" -ForegroundColor Green
Write-Host ""
Write-Host "Process IDs:" -ForegroundColor Cyan
Write-Host "  Gateway: $($gatewayProc.Id)"
Write-Host "  User Service: $($userProc.Id)"
Write-Host "  Order Service: $($orderProc.Id)"
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Access URLs:" -ForegroundColor Yellow
Write-Host "  Nacos:     http://127.0.0.1:8848/nacos"
Write-Host "  Sentinel:  http://127.0.0.1:8181"
Write-Host "  Gateway:   http://127.0.0.1:8080"
Write-Host "  User Service: http://127.0.0.1:8081"
Write-Host "  Order Service: http://127.0.0.1:8082"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Waiting 10 seconds for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "Running health check..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://127.0.0.1:8080/mock/payment/charge" -Method POST -ContentType "application/json" -Body '{"scenario":"SUCCESS"}' -UseBasicParsing -TimeoutSec 5
    Write-Host "[OK] Gateway responding with HTTP $($response.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Gateway check failed: $($_.Exception.Message)" -ForegroundColor Yellow
}

try {
    $response = Invoke-WebRequest -Uri "http://127.0.0.1:8081/actuator/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "[OK] User Service responding with HTTP $($response.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "[WARN] User Service check failed: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Done!" -ForegroundColor Green
