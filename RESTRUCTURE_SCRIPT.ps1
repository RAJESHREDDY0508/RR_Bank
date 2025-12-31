# ============================================================
# RR-Bank Microservices Restructuring Script
# ============================================================
# Run this script in PowerShell as Administrator
# ============================================================

$ProjectRoot = "C:\Users\rajes\Desktop\projects\RR-Bank"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "RR-Bank Microservices Restructuring" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# Step 1: Create backup
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = "$ProjectRoot\_backup_$timestamp"
Write-Host "`nStep 1: Creating backup at $backupDir" -ForegroundColor Green
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

# Backup important files
Copy-Item "$ProjectRoot\docker-compose.yml" -Destination $backupDir -Force -ErrorAction SilentlyContinue
Copy-Item "$ProjectRoot\.env.example" -Destination $backupDir -Force -ErrorAction SilentlyContinue
Copy-Item "$ProjectRoot\README.md" -Destination $backupDir -Force -ErrorAction SilentlyContinue

# Step 2: Delete old folders
Write-Host "`nStep 2: Removing old folders..." -ForegroundColor Green
$foldersToDelete = @(
    "rr-bank-microservices",
    "config-server",
    "discovery-server", 
    "shared-lib",
    "database",
    "banking-service",
    "ledger-service",
    "api-gateway"
)

foreach ($folder in $foldersToDelete) {
    $path = Join-Path $ProjectRoot $folder
    if (Test-Path $path) {
        Write-Host "  Deleting: $folder" -ForegroundColor Yellow
        Remove-Item -Path $path -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# Step 3: Delete old files
Write-Host "`nStep 3: Removing old files..." -ForegroundColor Green
$filesToDelete = @(
    "docker-compose.microservices.yml",
    "start-all.ps1",
    "start-phase1.ps1",
    "start.bat",
    "start-local.bat",
    "test-api.bat",
    "verify-system.bat",
    "PHASE0_BASELINE.md",
    "PHASE1_MICROSERVICES.md",
    "PHASE2_SECURITY.md",
    "PHASE3_TO_6_IMPLEMENTATION.md",
    "FRONTEND_INTEGRATION.md",
    "IMPLEMENTATION_SUMMARY.md",
    "ARCHITECTURE_REDESIGN.md",
    "STARTUP_GUIDE.md"
)

foreach ($file in $filesToDelete) {
    $path = Join-Path $ProjectRoot $file
    if (Test-Path $path) {
        Write-Host "  Deleting: $file" -ForegroundColor Yellow
        Remove-Item -Path $path -Force -ErrorAction SilentlyContinue
    }
}

# Step 4: Create new directory structure
Write-Host "`nStep 4: Creating new directory structure..." -ForegroundColor Green
$directories = @(
    "services\auth-service\src\main\java\com\rrbank\auth\config",
    "services\auth-service\src\main\java\com\rrbank\auth\controller",
    "services\auth-service\src\main\java\com\rrbank\auth\dto",
    "services\auth-service\src\main\java\com\rrbank\auth\entity",
    "services\auth-service\src\main\java\com\rrbank\auth\repository",
    "services\auth-service\src\main\java\com\rrbank\auth\service",
    "services\auth-service\src\main\java\com\rrbank\auth\security",
    "services\auth-service\src\main\java\com\rrbank\auth\event",
    "services\auth-service\src\main\resources",
    "services\customer-service\src\main\java\com\rrbank\customer\controller",
    "services\customer-service\src\main\java\com\rrbank\customer\entity",
    "services\customer-service\src\main\java\com\rrbank\customer\repository",
    "services\customer-service\src\main\java\com\rrbank\customer\service",
    "services\customer-service\src\main\resources",
    "services\account-service\src\main\java\com\rrbank\account\controller",
    "services\account-service\src\main\java\com\rrbank\account\dto",
    "services\account-service\src\main\java\com\rrbank\account\entity",
    "services\account-service\src\main\java\com\rrbank\account\repository",
    "services\account-service\src\main\java\com\rrbank\account\service",
    "services\account-service\src\main\resources",
    "services\transaction-service\src\main\java\com\rrbank\transaction\config",
    "services\transaction-service\src\main\java\com\rrbank\transaction\controller",
    "services\transaction-service\src\main\java\com\rrbank\transaction\dto",
    "services\transaction-service\src\main\java\com\rrbank\transaction\entity",
    "services\transaction-service\src\main\java\com\rrbank\transaction\repository",
    "services\transaction-service\src\main\java\com\rrbank\transaction\service",
    "services\transaction-service\src\main\java\com\rrbank\transaction\saga",
    "services\transaction-service\src\main\java\com\rrbank\transaction\event",
    "services\transaction-service\src\main\resources",
    "services\ledger-service\src\main\java\com\rrbank\ledger\config",
    "services\ledger-service\src\main\java\com\rrbank\ledger\controller",
    "services\ledger-service\src\main\java\com\rrbank\ledger\dto",
    "services\ledger-service\src\main\java\com\rrbank\ledger\entity",
    "services\ledger-service\src\main\java\com\rrbank\ledger\repository",
    "services\ledger-service\src\main\java\com\rrbank\ledger\service",
    "services\ledger-service\src\main\java\com\rrbank\ledger\event",
    "services\ledger-service\src\main\resources",
    "services\notification-service\src\main\java\com\rrbank\notification\controller",
    "services\notification-service\src\main\java\com\rrbank\notification\entity",
    "services\notification-service\src\main\java\com\rrbank\notification\repository",
    "services\notification-service\src\main\java\com\rrbank\notification\service",
    "services\notification-service\src\main\java\com\rrbank\notification\event",
    "services\notification-service\src\main\resources",
    "services\fraud-service\src\main\java\com\rrbank\fraud\controller",
    "services\fraud-service\src\main\java\com\rrbank\fraud\dto",
    "services\fraud-service\src\main\java\com\rrbank\fraud\service",
    "services\fraud-service\src\main\resources",
    "services\audit-service\src\main\java\com\rrbank\audit\controller",
    "services\audit-service\src\main\java\com\rrbank\audit\entity",
    "services\audit-service\src\main\java\com\rrbank\audit\repository",
    "services\audit-service\src\main\java\com\rrbank\audit\service",
    "services\audit-service\src\main\java\com\rrbank\audit\event",
    "services\audit-service\src\main\resources",
    "api-gateway\src\main\java\com\rrbank\gateway\config",
    "api-gateway\src\main\java\com\rrbank\gateway\filter",
    "api-gateway\src\main\resources",
    "docker",
    "scripts"
)

foreach ($dir in $directories) {
    $path = Join-Path $ProjectRoot $dir
    New-Item -ItemType Directory -Path $path -Force | Out-Null
}
Write-Host "  Created directory structure" -ForegroundColor Gray

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "Directory structure created!" -ForegroundColor Green
Write-Host "Now run the file creation commands..." -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
