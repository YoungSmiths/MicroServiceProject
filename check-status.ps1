# MicroService Project Status Check Script
# Detects Docker infrastructure and microservices status

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  MicroService Project Status Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Define services to check
$services = @(
    @{ Name = "Nacos"; Port = 8848; Type = "Docker"; Container = "nacos"; Url = "http://localhost:8848/nacos"; Description = "Registry Center" },
    @{ Name = "MySQL (user-db0)"; Port = 3306; Type = "Docker"; Container = "mysql-user-db0"; Url = $null; Description = "User DB 0" },
    @{ Name = "MySQL (user-db1)"; Port = 3307; Type = "Docker"; Container = "mysql-user-db1"; Url = $null; Description = "User DB 1" },
    @{ Name = "MySQL (order-db0)"; Port = 3308; Type = "Docker"; Container = "mysql-order-db0"; Url = $null; Description = "Order DB 0" },
    @{ Name = "MySQL (order-db1)"; Port = 3309; Type = "Docker"; Container = "mysql-order-db1"; Url = $null; Description = "Order DB 1" },
    @{ Name = "Redis"; Port = 6379; Type = "Docker"; Container = "redis"; Url = $null; Description = "Cache Service" },
    @{ Name = "Sentinel"; Port = 8181; Type = "Docker"; Container = "sentinel"; Url = "http://localhost:8181"; Description = "Circuit Breaker Dashboard" },
    @{ Name = "Seata"; Port = 8091; Type = "Docker"; Container = "seata-server"; Url = $null; Description = "Distributed Transaction Service" },
    @{ Name = "Gateway"; Port = 8080; Type = "Process"; Url = "http://localhost:8080/actuator/health"; Description = "API Gateway" },
    @{ Name = "User Service"; Port = 8081; Type = "Process"; Url = "http://localhost:8081/actuator/health"; Description = "User Service" },
    @{ Name = "Order Service"; Port = 8082; Type = "Process"; Url = "http://localhost:8082/actuator/health"; Description = "Order Service" }
)

$issues = @()
$allRunning = $true

Write-Host "--- 1. Check Docker ---" -ForegroundColor Yellow
Write-Host ""

# Check Docker availability
try {
    $dockerCheck = docker version --format '{{.Server.Version}}' 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Docker available (version: $dockerCheck)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Docker not running" -ForegroundColor Red
        $issues += "Docker not running"
        $allRunning = $false
    }
} catch {
    Write-Host "[FAIL] Docker not running" -ForegroundColor Red
    $issues += "Docker not running"
    $allRunning = $false
}

Write-Host ""
Write-Host "--- 2. Check Services ---" -ForegroundColor Yellow
Write-Host ""

# Check all services
foreach ($svc in $services) {
    Write-Host "- $($svc.Name) ($($svc.Description))"
    $status = "unknown"
    $statusColor = "White"
    $issueInfo = $null

    # Check if port is listening
    try {
        $portListen = Get-NetTCPConnection -State Listen -LocalPort $svc.Port -ErrorAction SilentlyContinue
        $isListening = ($portListen -ne $null)

        # For Docker services, check container status
        if ($svc.Type -eq "Docker") {
            try {
                $containerStatus = docker inspect -f '{{.State.Status}}' $svc.Container 2>&1
                if ($LASTEXITCODE -eq 0) {
                    if ($containerStatus -eq "running") {
                        $status = "Container running"
                        $statusColor = "Green"
                        if (-not $isListening) {
                            $status += " (port not listening)"
                            $statusColor = "Yellow"
                            $issueInfo = "$($svc.Name): container running but port $($svc.Port) not listening"
                        }
                    } else {
                        $status = "Container: $containerStatus"
                        $statusColor = "Red"
                        $issueInfo = "$($svc.Name): container not running"
                    }
                } else {
                    $status = "Container not found"
                    $statusColor = "Red"
                    $issueInfo = "$($svc.Name): container missing"
                }
            } catch {
                $status = "Container check failed"
                $statusColor = "Red"
                $issueInfo = "$($svc.Name): container status check error"
            }
        } else {
            # Process service
            if ($isListening) {
                $status = "Port listening"
                $statusColor = "Green"
            } else {
                $status = "Port not listening"
                $statusColor = "Red"
                $issueInfo = "$($svc.Name): not running"
            }
        }

        # Try HTTP request (if available)
        if ($svc.Url -and $isListening) {
            try {
                $response = Invoke-WebRequest -Uri $svc.Url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
                $status += " | HTTP $($response.StatusCode)"
                if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                    $statusColor = "Green"
                } else {
                    $statusColor = "Yellow"
                    $issueInfo = "$($svc.Name): HTTP $($response.StatusCode)"
                }
            } catch {
                $status += " | Not reachable"
                $statusColor = "Yellow"
                $issueInfo = "$($svc.Name): health check failed"
            }
        }

    } catch {
        $status = "Check error"
        $statusColor = "Red"
        $issueInfo = "$($svc.Name): detection error"
    }

    Write-Host "  - $status" -ForegroundColor $statusColor
    if ($statusColor -ne "Green") {
        $allRunning = $false
        if ($issueInfo) {
            $issues += $issueInfo
        }
    }
}

Write-Host ""
Write-Host "--- 3. Check JAR Files ---" -ForegroundColor Yellow
Write-Host ""
$jars = @(
    @{ Path = "micro-gateway\target\micro-gateway-1.0.0.jar"; Name = "Gateway JAR" },
    @{ Path = "micro-user-service\target\micro-user-service-1.0.0.jar"; Name = "User Service JAR" },
    @{ Path = "micro-order-service\target\micro-order-service-1.0.0.jar"; Name = "Order Service JAR" }
)
foreach ($jar in $jars) {
    if (Test-Path $jar.Path) {
        $size = (Get-Item $jar.Path).Length / 1MB
        Write-Host "[OK] $($jar.Name) exists ($([math]::Round($size, 2)) MB)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $($jar.Name) missing" -ForegroundColor Red
        $issues += "$($jar.Name) missing"
        $allRunning = $false
    }
}

Write-Host ""
Write-Host "--- 4. Summary ---" -ForegroundColor Yellow
Write-Host ""
if ($allRunning -and $issues.Count -eq 0) {
    Write-Host "[SUCCESS] All services running!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Access URLs:"
    Write-Host "  - Nacos: http://localhost:8848/nacos"
    Write-Host "  - Sentinel: http://localhost:8181"
    Write-Host "  - Gateway: http://localhost:8080"
} else {
    Write-Host "[WARNING] Found $($issues.Count) issues:" -ForegroundColor Red
    foreach ($issue in $issues) {
        Write-Host "  - $issue" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Recommended actions:"
    Write-Host "  1. Start infrastructure: docker compose up -d"
    Write-Host "  2. Build project: mvn clean package -DskipTests"
    Write-Host "  3. Start services: .\start-all.bat"
}

Write-Host ""
Write-Host "Check complete!" -ForegroundColor Cyan
