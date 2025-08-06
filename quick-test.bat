@echo off
echo ========================================
echo QUICK DEVICE MAPPING TEST
echo ========================================
echo.

set BASE_URL=https://api.managifyhr.com
set SUBADMIN_ID=1

echo [1] Current Status:
curl -s "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" | findstr "totalEmployees\|totalMappings\|mappingCoverage"
echo.

echo [2] Adding Test Employee...
curl -s -X POST "%BASE_URL%/api/subadmin/%SUBADMIN_ID%/add-employee" ^
  -F "firstName=QuickTest" ^
  -F "lastName=Employee" ^
  -F "email=quicktest@example.com" ^
  -F "phone=1111111111" ^
  -F "address=Quick Test Address" ^
  -F "position=Tester" ^
  -F "department=QA" ^
  -F "salary=40000" | findstr "empId\|firstName\|lastName"
echo.

echo [3] Status After Adding:
curl -s "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" | findstr "totalEmployees\|totalMappings\|mappingCoverage"
echo.

echo ========================================
echo QUICK TEST COMPLETE
echo ========================================
pause
