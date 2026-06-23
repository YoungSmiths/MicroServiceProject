$sourceDir = 'C:\Users\15321\GolandProjects'
$targetDir = 'D:\Users\15321\GolandProjects'

$projects = @(
    'nft-market-fe',
    'okx-test',
    'Pake',
    'pancake-farm',
    'pancake-smart-contracts',
    'perpetual-contract',
    'prictice',
    'ProjectBreakdown-Pledge',
    'roryhe-web3-record',
    'Solidity-Mission2',
    'Solidity-Mission3',
    'solidity_lesson',
    'squid-sdk',
    'study-web3',
    'studyGo',
    'study_record',
    'swapbased',
    'syncswap-core-contracts',
    'v2-core',
    'v2-periphery',
    'v3-core',
    'v3-periphery',
    'web3-interview-sharing-main',
    'web3-lesson',
    'web3study',
    'web3_exercise',
    'week2-zjl-solidity',
    'zbs_homework',
    'zc_study_web3',
    'zhaowenxiang',
    'zwx-web3-study'
)

Write-Host '========================================' -ForegroundColor Cyan
Write-Host '  Start Moving Projects' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan
Write-Host ('Source: {0}' -f $sourceDir) -ForegroundColor Yellow
Write-Host ('Target: {0}' -f $targetDir) -ForegroundColor Yellow
Write-Host

$successCount = 0
$failCount = 0

foreach ($project in $projects) {
    $sourcePath = Join-Path $sourceDir $project
    $targetPath = Join-Path $targetDir $project

    if (Test-Path $sourcePath) {
        Write-Host ('Moving: {0}' -f $project) -ForegroundColor Green

        try {
            if (Test-Path $targetPath) {
                Remove-Item -Path $targetPath -Recurse -Force
            }

            Move-Item -Path $sourcePath -Destination $targetPath -Force
            Write-Host ('  Done: {0}' -f $project) -ForegroundColor Green
            $successCount++
        } catch {
            Write-Host ('  Failed: {0} - {1}' -f $project, $_.Exception.Message) -ForegroundColor Red
            $failCount++
        }
    } else {
        Write-Host ('  Skip: {0} (not found)' -f $project) -ForegroundColor Gray
    }
}

Write-Host
Write-Host '========================================' -ForegroundColor Cyan
Write-Host '  Summary' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan
Write-Host ('Success: {0}' -f $successCount) -ForegroundColor Green
Write-Host ('Fail: {0}' -f $failCount) -ForegroundColor Red
Write-Host
