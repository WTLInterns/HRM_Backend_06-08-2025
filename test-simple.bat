@echo off
echo ========================================
echo DEVICE MAPPING SIMPLE TEST
echo ========================================
echo.

set BASE_URL=https://api.managifyhr.com
set SUBADMIN_ID=1

echo [1] Testing if backend is running...
curl -s -f "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Backend is not running or debug endpoint not available
    echo Please start backend: cd hrm_backend ^& mvn spring-boot:run
    pause
    exit /b 1
)
echo ✅ Backend is running!
echo.

echo [2] Current device mapping status:
curl -s "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" | findstr "totalEmployees\|totalMappings\|mappingCoverage"
echo.

echo [3] Fixing any mapping issues...
curl -s -X POST "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/fix-mappings" | findstr "employeesFixed\|mappingsCreated\|message"
echo.

echo [4] Status after fix:
curl -s "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" | findstr "totalEmployees\|totalMappings\|mappingCoverage"
echo.

echo [5] Adding test employee...
curl -s -X POST "%BASE_URL%/api/subadmin/%SUBADMIN_ID%/add-employee" ^
  -F "firstName=TestDevice%RANDOM%" ^
  -F "lastName=MappingTest" ^
  -F "email=testdevice%RANDOM%@example.com" ^
  -F "phone=9999999999" ^
  -F "address=Test Address" ^
  -F "position=Tester" ^
  -F "department=QA" ^
  -F "salary=50000" | findstr "empId\|firstName\|deviceSerialNumber"
echo.

echo [6] Final status after adding employee:
curl -s "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" | findstr "totalEmployees\|totalMappings\|mappingCoverage"
echo.

echo ========================================
echo TEST COMPLETE
echo ========================================
echo.
echo Check if:
echo ✅ Employee count and mapping count both increased
echo ✅ Coverage shows "X/X" (all employees mapped)
echo ✅ New employee has deviceSerialNumber
echo.
pause
