# Verify Device Mapping Changes Script
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFYING DEVICE MAPPING CHANGES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$changes = @()

# Check 1: SubAdminService.java changes
Write-Host "[1] Checking SubAdminService.java..." -ForegroundColor Green
$subAdminServicePath = "src\main\java\com\jaywant\demo\Service\SubAdminService.java"
if (Test-Path $subAdminServicePath) {
    $content = Get-Content $subAdminServicePath -Raw
    if ($content -match "handleNewEmployeeDeviceMapping" -and $content -match "Creating device mapping for new employee") {
        Write-Host "  ✅ SubAdminService.java has device mapping enhancements" -ForegroundColor Green
        $changes += "✅ SubAdminService.java - Enhanced addEmployee method"
    } else {
        Write-Host "  ❌ SubAdminService.java missing device mapping code" -ForegroundColor Red
        $changes += "❌ SubAdminService.java - Missing enhancements"
    }
} else {
    Write-Host "  ❌ SubAdminService.java not found" -ForegroundColor Red
    $changes += "❌ SubAdminService.java - File not found"
}

# Check 2: EmployeeDeviceMappingService.java changes
Write-Host "[2] Checking EmployeeDeviceMappingService.java..." -ForegroundColor Green
$mappingServicePath = "src\main\java\com\jaywant\demo\Service\EmployeeDeviceMappingService.java"
if (Test-Path $mappingServicePath) {
    $content = Get-Content $mappingServicePath -Raw
    if ($content -match "Update the existing mapping to ensure it's current" -and $content -match "setUpdatedAt") {
        Write-Host "  ✅ EmployeeDeviceMappingService.java has safe mapping logic" -ForegroundColor Green
        $changes += "✅ EmployeeDeviceMappingService.java - Safe mapping updates"
    } else {
        Write-Host "  ❌ EmployeeDeviceMappingService.java missing safe mapping code" -ForegroundColor Red
        $changes += "❌ EmployeeDeviceMappingService.java - Missing safe mapping logic"
    }
} else {
    Write-Host "  ❌ EmployeeDeviceMappingService.java not found" -ForegroundColor Red
    $changes += "❌ EmployeeDeviceMappingService.java - File not found"
}

# Check 3: DeviceMappingDebugController.java
Write-Host "[3] Checking DeviceMappingDebugController.java..." -ForegroundColor Green
$debugControllerPath = "src\main\java\com\jaywant\demo\Controller\DeviceMappingDebugController.java"
if (Test-Path $debugControllerPath) {
    $content = Get-Content $debugControllerPath -Raw
    if ($content -match "getSubadminMappingStatus" -and $content -match "fix-mappings") {
        Write-Host "  ✅ DeviceMappingDebugController.java exists with debug endpoints" -ForegroundColor Green
        $changes += "✅ DeviceMappingDebugController.java - Debug system added"
    } else {
        Write-Host "  ❌ DeviceMappingDebugController.java incomplete" -ForegroundColor Red
        $changes += "❌ DeviceMappingDebugController.java - Incomplete implementation"
    }
} else {
    Write-Host "  ❌ DeviceMappingDebugController.java not found" -ForegroundColor Red
    $changes += "❌ DeviceMappingDebugController.java - File not found"
}

# Check 4: Compilation status
Write-Host "[4] Checking compilation..." -ForegroundColor Green
try {
    $compileResult = & mvn compile -q 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Code compiles successfully" -ForegroundColor Green
        $changes += "✅ Compilation - Success"
    } else {
        Write-Host "  ❌ Compilation errors found" -ForegroundColor Red
        Write-Host "  Error: $compileResult" -ForegroundColor Yellow
        $changes += "❌ Compilation - Errors found"
    }
} catch {
    Write-Host "  ⚠️  Could not test compilation (Maven not in PATH?)" -ForegroundColor Yellow
    $changes += "⚠️  Compilation - Could not test"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICATION SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

foreach ($change in $changes) {
    Write-Host $change
}

Write-Host ""

# Count successful changes
$successCount = ($changes | Where-Object { $_ -match "✅" }).Count
$totalChecks = $changes.Count

if ($successCount -eq $totalChecks) {
    Write-Host "🎉 ALL CHANGES VERIFIED SUCCESSFULLY!" -ForegroundColor Green
    Write-Host "✅ Ready for testing and deployment" -ForegroundColor Green
} elseif ($successCount -gt ($totalChecks / 2)) {
    Write-Host "⚠️  MOST CHANGES VERIFIED ($successCount/$totalChecks)" -ForegroundColor Yellow
    Write-Host "🔧 Some issues found - check the details above" -ForegroundColor Yellow
} else {
    Write-Host "❌ MAJOR ISSUES FOUND ($successCount/$totalChecks)" -ForegroundColor Red
    Write-Host "🚨 Significant problems detected - review changes needed" -ForegroundColor Red
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. If all verified ✅ - Run: .\test-now.ps1" -ForegroundColor White
Write-Host "2. If issues found ❌ - Review and fix the problems" -ForegroundColor White
Write-Host "3. After testing ✅ - Deploy to production" -ForegroundColor White

Write-Host ""
Read-Host "Press Enter to exit"
