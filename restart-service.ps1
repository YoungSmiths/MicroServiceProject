param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("gateway", "user", "order")]
    [string]$Service,

    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

$ProjectRoot = "C:\Users\15321\Documents\trae_projects\MicroServiceProject"
$MavenExe = "D:\software\green\apache-maven-3.9.8\bin\mvn.cmd"
$JavaExe = "C:\Program Files\Java\jdk-17\bin\java.exe"

if (-not (Test-Path $MavenExe)) { $MavenExe = "mvn" }
if (-not (Test-Path $JavaExe)) { $JavaExe = "java" }

$serviceMap = @{
    gateway = @{
        Module = "micro-gateway"
        Port = 8080
        JarName = "micro-gateway-1.0.0.jar"
        JavaProps = @(
            "-DNACOS_SERVER=127.0.0.1:8848"
            "-DSENTINEL_DASHBOARD=127.0.0.1:8181"
            "-DREDIS_HOST=127.0.0.1"
            "-DREDIS_PORT=6379"
            "-DREDIS_PASSWORD=123456"
        )
    }
    user = @{
        Module = "micro-user-service"
        Port = 8081
        JarName = "micro-user-service-1.0.0.jar"
        JavaProps = @(
            "-DNACOS_SERVER=127.0.0.1:8848"
            "-DSENTINEL_DASHBOARD=127.0.0.1:8181"
            "-DREDIS_HOST=127.0.0.1"
            "-DREDIS_PORT=6379"
            "-DREDIS_PASSWORD=123456"
            "-DSEATA_ENABLED=false"
        )
    }
    order = @{
        Module = "micro-order-service"
        Port = 8082
        JarName = "micro-order-service-1.0.0.jar"
        JavaProps = @(
            "-DNACOS_SERVER=127.0.0.1:8848"
            "-DSENTINEL_DASHBOARD=127.0.0.1:8181"
            "-DREDIS_HOST=127.0.0.1"
            "-DREDIS_PORT=6379"
            "-DREDIS_PASSWORD=123456"
            "-DSEATA_ENABLED=false"
        )
    }
}

$config = $serviceMap[$Service]
$serviceDir = Join-Path $ProjectRoot $config.Module
$jarPath = Join-Path $serviceDir ("target\" + $config.JarName)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Restart Service: $($config.Module)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/4] 停止端口 $($config.Port) 上的旧进程..." -ForegroundColor Yellow
$connections = Get-NetTCPConnection -State Listen -LocalPort $config.Port -ErrorAction SilentlyContinue
if ($connections) {
    $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($procId in $pids) {
        try {
            Stop-Process -Id $procId -Force -ErrorAction Stop
            Write-Host "已停止 PID=$procId" -ForegroundColor Green
        } catch {
            Write-Host "停止 PID=$procId 失败: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
} else {
    Write-Host "未发现占用端口 $($config.Port) 的进程" -ForegroundColor Green
}

Start-Sleep -Seconds 2

Write-Host ""
Write-Host "[2/4] clean install $($config.Module)..." -ForegroundColor Yellow
Push-Location $ProjectRoot
try {
    $mavenArgs = @("-pl", $config.Module, "-am", "clean", "install")
    if ($SkipTests) {
        $mavenArgs += "-DskipTests"
        $mavenArgs += "-Dmaven.test.skip=true"
    }

    & $MavenExe @mavenArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Maven 构建失败"
    }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "[3/4] 校验 JAR 文件..." -ForegroundColor Yellow
if (-not (Test-Path $jarPath)) {
    throw "未找到 JAR 文件: $jarPath"
}
Write-Host "JAR 已生成: $jarPath" -ForegroundColor Green

Write-Host ""
Write-Host "[4/4] 启动服务..." -ForegroundColor Yellow
$argList = @()
$argList += $config.JavaProps
$argList += "-jar"
$argList += $jarPath

$process = Start-Process -FilePath $JavaExe -ArgumentList $argList -WorkingDirectory $ProjectRoot -WindowStyle Normal -PassThru

Write-Host "已启动 $($config.Module)，PID=$($process.Id)" -ForegroundColor Green
Write-Host "端口: $($config.Port)" -ForegroundColor Green
Write-Host ""
Write-Host "等待 10 秒后做一次端口检查..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

$listenCheck = Get-NetTCPConnection -State Listen -LocalPort $config.Port -ErrorAction SilentlyContinue
if ($listenCheck) {
    Write-Host "服务启动成功，端口 $($config.Port) 正在监听" -ForegroundColor Green
} else {
    Write-Host "端口 $($config.Port) 暂未监听，请查看新打开的服务窗口日志" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "完成。" -ForegroundColor Cyan
