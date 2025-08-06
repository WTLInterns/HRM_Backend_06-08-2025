# Device Mapping Test Script
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DEVICE MAPPING TEST SCRIPT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "https://api.managifyhr.com"
$subadminId = 1

# Function to make API calls and display results
function Test-ApiCall {
    param($url, $method = "GET", $body = $null, $contentType = $null)
    
    try {
        if ($method -eq "GET") {
            $response = Invoke-RestMethod -Uri $url -Method $method
        } else {
            $response = Invoke-RestMethod -Uri $url -Method $method -Body $body -ContentType $contentType
        }
        return $response
    } catch {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Function to display mapping summary
function Show-MappingSummary {
    param($status, $title)
    
    Write-Host $title -ForegroundColor Yellow
    if ($status) {
        Write-Host "  Total Employees: $($status.totalEmployees)" -ForegroundColor White
        Write-Host "  Total Mappings: $($status.totalMappings)" -ForegroundColor White
        Write-Host "  Coverage: $($status.summary.mappingCoverage)" -ForegroundColor White
        Write-Host "  Device Serial: $($status.deviceSerialNumber)" -ForegroundColor White
        Write-Host ""
    }
}

Write-Host "[STEP 1] Checking initial device mapping status..." -ForegroundColor Green
$initialStatus = Test-ApiCall "$baseUrl/api/debug/device-mapping/subadmin/$subadminId/status"
Show-MappingSummary $initialStatus "INITIAL STATUS:"

Write-Host "[STEP 2] Fixing any existing mapping issues..." -ForegroundColor Green
$fixResult = Test-ApiCall "$baseUrl/api/debug/device-mapping/subadmin/$subadminId/fix-mappings" "POST"
if ($fixResult) {
    Write-Host "Fix Result:" -ForegroundColor Yellow
    Write-Host "  Employees Fixed: $($fixResult.employeesFixed)" -ForegroundColor White
    Write-Host "  Mappings Created: $($fixResult.mappingsCreated)" -ForegroundColor White
    Write-Host "  Message: $($fixResult.message)" -ForegroundColor White
    Write-Host ""
}

Write-Host "[STEP 3] Checking status after fix..." -ForegroundColor Green
$afterFixStatus = Test-ApiCall "$baseUrl/api/debug/device-mapping/subadmin/$subadminId/status"
Show-MappingSummary $afterFixStatus "AFTER FIX STATUS:"

Write-Host "[STEP 4] Adding a new test employee..." -ForegroundColor Green
$employeeData = @{
    firstName = "TestDevice"
    lastName = "MappingEmployee"
    email = "testdevice$(Get-Random)@example.com"
    phone = "9999999999"
    address = "Test Address"
    position = "Test Position"
    department = "Test Department"
    salary = "50000"
}

# Create multipart form data
$boundary = [System.Guid]::NewGuid().ToString()
$bodyLines = @()
foreach ($key in $employeeData.Keys) {
    $bodyLines += "--$boundary"
    $bodyLines += "Content-Disposition: form-data; name=`"$key`""
    $bodyLines += ""
    $bodyLines += $employeeData[$key]
}
$bodyLines += "--$boundary--"
$body = $bodyLines -join "`r`n"

try {
    $addEmployeeResult = Invoke-RestMethod -Uri "$baseUrl/api/subadmin/$subadminId/add-employee" -Method POST -Body $body -ContentType "multipart/form-data; boundary=$boundary"
    Write-Host "Employee Added Successfully!" -ForegroundColor Green
    Write-Host "  Employee ID: $($addEmployeeResult.empId)" -ForegroundColor White
    Write-Host "  Name: $($addEmployeeResult.firstName) $($addEmployeeResult.lastName)" -ForegroundColor White
    $newEmpId = $addEmployeeResult.empId
} catch {
    Write-Host "Error adding employee: $($_.Exception.Message)" -ForegroundColor Red
    $newEmpId = $null
}
Write-Host ""

Write-Host "[STEP 5] Checking device mapping status after adding employee..." -ForegroundColor Green
$afterAddStatus = Test-ApiCall "$baseUrl/api/debug/device-mapping/subadmin/$subadminId/status"
Show-MappingSummary $afterAddStatus "AFTER ADDING EMPLOYEE:"

if ($newEmpId) {
    Write-Host "[STEP 6] Checking individual employee mapping..." -ForegroundColor Green
    $newEmployeeStatus = Test-ApiCall "$baseUrl/api/debug/device-mapping/employee/$newEmpId/status"
    if ($newEmployeeStatus) {
        Write-Host "NEW EMPLOYEE MAPPING:" -ForegroundColor Yellow
        Write-Host "  Employee ID: $($newEmployeeStatus.empId)" -ForegroundColor White
        Write-Host "  Name: $($newEmployeeStatus.name)" -ForegroundColor White
        Write-Host "  Device Serial: $($newEmployeeStatus.deviceSerialNumber)" -ForegroundColor White
        Write-Host "  Has Mappings: $($newEmployeeStatus.mappingCount -gt 0)" -ForegroundColor White
        Write-Host "  Mapping Count: $($newEmployeeStatus.mappingCount)" -ForegroundColor White
        Write-Host ""
    }
}

Write-Host "[STEP 7] Testing employee update..." -ForegroundColor Green
$updateData = @{
    firstName = "UpdatedTest"
    lastName = "Employee"
    email = "updated$(Get-Random)@example.com"
    phone = "8888888888"
    address = "Updated Address"
    position = "Updated Position"
    department = "Updated Department"
    salary = "55000"
}

# Create multipart form data for update
$updateBodyLines = @()
foreach ($key in $updateData.Keys) {
    $updateBodyLines += "--$boundary"
    $updateBodyLines += "Content-Disposition: form-data; name=`"$key`""
    $updateBodyLines += ""
    $updateBodyLines += $updateData[$key]
}
$updateBodyLines += "--$boundary--"
$updateBody = $updateBodyLines -join "`r`n"

try {
    $updateResult = Invoke-RestMethod -Uri "$baseUrl/api/subadmin/$subadminId/update-employee/504" -Method PUT -Body $updateBody -ContentType "multipart/form-data; boundary=$boundary"
    Write-Host "Employee Updated Successfully!" -ForegroundColor Green
} catch {
    Write-Host "Note: Employee update test skipped (employee 504 may not exist)" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "[STEP 8] Final device mapping status check..." -ForegroundColor Green
$finalStatus = Test-ApiCall "$baseUrl/api/debug/device-mapping/subadmin/$subadminId/status"
Show-MappingSummary $finalStatus "FINAL STATUS:"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "COMPARISON SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Compare results
Write-Host "MAPPING COVERAGE COMPARISON:" -ForegroundColor Magenta
if ($initialStatus) { Write-Host "  Initial:    $($initialStatus.summary.mappingCoverage)" -ForegroundColor White }
if ($afterFixStatus) { Write-Host "  After Fix:  $($afterFixStatus.summary.mappingCoverage)" -ForegroundColor White }
if ($afterAddStatus) { Write-Host "  After Add:  $($afterAddStatus.summary.mappingCoverage)" -ForegroundColor White }
if ($finalStatus) { Write-Host "  Final:      $($finalStatus.summary.mappingCoverage)" -ForegroundColor White }

Write-Host ""
Write-Host "EMPLOYEE COUNT COMPARISON:" -ForegroundColor Magenta
if ($initialStatus) { Write-Host "  Initial:    $($initialStatus.totalEmployees) employees" -ForegroundColor White }
if ($afterFixStatus) { Write-Host "  After Fix:  $($afterFixStatus.totalEmployees) employees" -ForegroundColor White }
if ($afterAddStatus) { Write-Host "  After Add:  $($afterAddStatus.totalEmployees) employees" -ForegroundColor White }
if ($finalStatus) { Write-Host "  Final:      $($finalStatus.totalEmployees) employees" -ForegroundColor White }

Write-Host ""
Write-Host "TEST RESULTS:" -ForegroundColor Magenta
if ($initialStatus -and $finalStatus) {
    $employeeIncrease = $finalStatus.totalEmployees - $initialStatus.totalEmployees
    $mappingIncrease = $finalStatus.totalMappings - $initialStatus.totalMappings
    
    if ($employeeIncrease -eq $mappingIncrease -and $employeeIncrease -gt 0) {
        Write-Host "  ✅ SUCCESS: Added $employeeIncrease employee(s) and $mappingIncrease mapping(s)" -ForegroundColor Green
        Write-Host "  ✅ Device mapping is working correctly!" -ForegroundColor Green
    } elseif ($employeeIncrease -gt $mappingIncrease) {
        Write-Host "  ❌ ISSUE: Added $employeeIncrease employee(s) but only $mappingIncrease mapping(s)" -ForegroundColor Red
        Write-Host "  ❌ Device mapping may have issues!" -ForegroundColor Red
    } else {
        Write-Host "  ℹ️  INFO: No new employees added during test" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST COMPLETE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Read-Host "Press Enter to exit"
