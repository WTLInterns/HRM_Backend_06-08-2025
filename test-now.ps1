# Device Mapping Live Test Script
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DEVICE MAPPING LIVE TEST" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$baseUrl = "https://api.managifyhr.com"
$subadminId = 1

# Test if backend is running
Write-Host "[CHECKING] Testing if backend is running..." -ForegroundColor Yellow
try {
    $healthCheck = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -TimeoutSec 5
    Write-Host "✅ Backend is running!" -ForegroundColor Green
} catch {
    try {
        # Try a simple endpoint
        $response = Invoke-WebRequest -Uri $baseUrl -TimeoutSec 5
        Write-Host "✅ Backend is running (no actuator endpoint)!" -ForegroundColor Green
    } catch {
        Write-Host "❌ Backend is NOT running!" -ForegroundColor Red
        Write-Host "Please start the backend first:" -ForegroundColor Yellow
        Write-Host "  cd hrm_backend" -ForegroundColor White
        Write-Host "  mvn spring-boot:run" -ForegroundColor White
        Read-Host "Press Enter to exit"
        exit
    }
}

Write-Host ""

# Test 1: Check current device mapping status
Write-Host "[TEST 1] Checking current device mapping status..." -ForegroundColor Green
try {
    $initialStatus = Invoke-RestMethod -Uri "$baseUrl/api/debug/device-mapping/subadmin/$subadminId/status"
    Write-Host "✅ Debug endpoint is working!" -ForegroundColor Green
    Write-Host "Current Status:" -ForegroundColor Yellow
    Write-Host "  Subadmin: $($initialStatus.subadminName)" -ForegroundColor White
    Write-Host "  Device Serial: $($initialStatus.deviceSerialNumber)" -ForegroundColor White
    Write-Host "  Total Employees: $($initialStatus.totalEmployees)" -ForegroundColor White
    Write-Host "  Total Mappings: $($initialStatus.totalMappings)" -ForegroundColor White
    Write-Host "  Coverage: $($initialStatus.summary.mappingCoverage)" -ForegroundColor White
    
    if ($initialStatus.summary.mappingCoverage -match "(\d+)/(\d+)") {
        $mapped = [int]$matches[1]
        $total = [int]$matches[2]
        if ($mapped -eq $total) {
            Write-Host "  ✅ All employees are mapped!" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️  Some employees are not mapped" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ Debug endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "This might mean the DeviceMappingDebugController is not loaded" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit
}

Write-Host ""

# Test 2: Fix any existing mapping issues
Write-Host "[TEST 2] Fixing any existing mapping issues..." -ForegroundColor Green
try {
    $fixResult = Invoke-RestMethod -Uri "$baseUrl/api/debug/device-mapping/subadmin/$subadminId/fix-mappings" -Method POST
    Write-Host "✅ Fix mappings endpoint works!" -ForegroundColor Green
    Write-Host "Fix Results:" -ForegroundColor Yellow
    Write-Host "  Employees Fixed: $($fixResult.employeesFixed)" -ForegroundColor White
    Write-Host "  Mappings Created: $($fixResult.mappingsCreated)" -ForegroundColor White
    Write-Host "  Message: $($fixResult.message)" -ForegroundColor White
} catch {
    Write-Host "❌ Fix mappings failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 3: Check status after fix
Write-Host "[TEST 3] Checking status after fix..." -ForegroundColor Green
try {
    $afterFixStatus = Invoke-RestMethod -Uri "$baseUrl/api/debug/device-mapping/subadmin/$subadminId/status"
    Write-Host "After Fix Status:" -ForegroundColor Yellow
    Write-Host "  Total Employees: $($afterFixStatus.totalEmployees)" -ForegroundColor White
    Write-Host "  Total Mappings: $($afterFixStatus.totalMappings)" -ForegroundColor White
    Write-Host "  Coverage: $($afterFixStatus.summary.mappingCoverage)" -ForegroundColor White
} catch {
    Write-Host "❌ Status check failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 4: Add a new employee
Write-Host "[TEST 4] Adding a new test employee..." -ForegroundColor Green
$randomId = Get-Random -Minimum 1000 -Maximum 9999
$employeeData = @{
    firstName = "TestDevice$randomId"
    lastName = "MappingEmployee"
    email = "testdevice$randomId@example.com"
    phone = "9999999999"
    address = "Test Address $randomId"
    position = "Test Position"
    department = "Test Department"
    salary = "50000"
}

# Create form data
$formData = @{}
foreach ($key in $employeeData.Keys) {
    $formData[$key] = $employeeData[$key]
}

try {
    $addResult = Invoke-RestMethod -Uri "$baseUrl/api/subadmin/$subadminId/add-employee" -Method POST -Form $formData
    Write-Host "✅ Employee added successfully!" -ForegroundColor Green
    Write-Host "New Employee:" -ForegroundColor Yellow
    Write-Host "  ID: $($addResult.empId)" -ForegroundColor White
    Write-Host "  Name: $($addResult.firstName) $($addResult.lastName)" -ForegroundColor White
    Write-Host "  Device Serial: $($addResult.deviceSerialNumber)" -ForegroundColor White
    $newEmpId = $addResult.empId
} catch {
    Write-Host "❌ Adding employee failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "This might mean the SubAdminController endpoint has issues" -ForegroundColor Yellow
    $newEmpId = $null
}

Write-Host ""

# Test 5: Check status after adding employee
Write-Host "[TEST 5] Checking status after adding employee..." -ForegroundColor Green
try {
    $afterAddStatus = Invoke-RestMethod -Uri "$baseUrl/api/debug/device-mapping/subadmin/$subadminId/status"
    Write-Host "After Adding Employee:" -ForegroundColor Yellow
    Write-Host "  Total Employees: $($afterAddStatus.totalEmployees)" -ForegroundColor White
    Write-Host "  Total Mappings: $($afterAddStatus.totalMappings)" -ForegroundColor White
    Write-Host "  Coverage: $($afterAddStatus.summary.mappingCoverage)" -ForegroundColor White
    
    # Compare with previous status
    if ($afterFixStatus -and $afterAddStatus) {
        $empIncrease = $afterAddStatus.totalEmployees - $afterFixStatus.totalEmployees
        $mappingIncrease = $afterAddStatus.totalMappings - $afterFixStatus.totalMappings
        
        Write-Host "Changes:" -ForegroundColor Yellow
        Write-Host "  Employee increase: $empIncrease" -ForegroundColor White
        Write-Host "  Mapping increase: $mappingIncrease" -ForegroundColor White
        
        if ($empIncrease -eq $mappingIncrease -and $empIncrease -gt 0) {
            Write-Host "  ✅ PERFECT! Employee and mapping counts match!" -ForegroundColor Green
        } elseif ($empIncrease -gt $mappingIncrease) {
            Write-Host "  ❌ PROBLEM! Added employee but mapping is missing!" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "❌ Status check failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 6: Check individual employee mapping
if ($newEmpId) {
    Write-Host "[TEST 6] Checking individual employee mapping..." -ForegroundColor Green
    try {
        $empStatus = Invoke-RestMethod -Uri "$baseUrl/api/debug/device-mapping/employee/$newEmpId/status"
        Write-Host "New Employee Mapping:" -ForegroundColor Yellow
        Write-Host "  Employee ID: $($empStatus.empId)" -ForegroundColor White
        Write-Host "  Name: $($empStatus.name)" -ForegroundColor White
        Write-Host "  Device Serial: $($empStatus.deviceSerialNumber)" -ForegroundColor White
        Write-Host "  Mapping Count: $($empStatus.mappingCount)" -ForegroundColor White
        Write-Host "  Device Match: $($empStatus.deviceSerialMatch)" -ForegroundColor White
        
        if ($empStatus.mappingCount -gt 0 -and $empStatus.deviceSerialMatch) {
            Write-Host "  ✅ Employee mapping is PERFECT!" -ForegroundColor Green
        } else {
            Write-Host "  ❌ Employee mapping has ISSUES!" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ Individual employee check failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Final comparison
if ($initialStatus -and $afterAddStatus) {
    Write-Host "BEFORE vs AFTER:" -ForegroundColor Magenta
    Write-Host "  Initial Coverage: $($initialStatus.summary.mappingCoverage)" -ForegroundColor White
    Write-Host "  Final Coverage:   $($afterAddStatus.summary.mappingCoverage)" -ForegroundColor White
    
    if ($afterAddStatus.summary.mappingCoverage -match "(\d+)/(\d+)") {
        $finalMapped = [int]$matches[1]
        $finalTotal = [int]$matches[2]
        if ($finalMapped -eq $finalTotal) {
            Write-Host ""
            Write-Host "🎉 SUCCESS! Device mapping is working correctly!" -ForegroundColor Green
            Write-Host "✅ All employees have device mappings" -ForegroundColor Green
            Write-Host "✅ New employees get mapped automatically" -ForegroundColor Green
            Write-Host "✅ Existing employees keep their mappings" -ForegroundColor Green
            Write-Host ""
            Write-Host "👍 READY FOR DEPLOYMENT!" -ForegroundColor Green
        } else {
            Write-Host ""
            Write-Host "❌ ISSUES FOUND! Device mapping needs attention!" -ForegroundColor Red
            Write-Host "⚠️  Not all employees are mapped" -ForegroundColor Yellow
            Write-Host "🔧 Check the logs and debug further" -ForegroundColor Yellow
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Read-Host "Press Enter to exit"
