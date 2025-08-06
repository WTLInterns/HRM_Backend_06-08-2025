@echo off
echo ========================================
echo DEVICE MAPPING TEST SCRIPT
echo ========================================
echo.

set BASE_URL=https://api.managifyhr.com
set SUBADMIN_ID=1

echo [STEP 1] Checking initial device mapping status...
echo.
curl -s "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" > initial_status.json
echo Initial Status:
type initial_status.json
echo.
echo.

echo [STEP 2] Fixing any existing mapping issues...
echo.
curl -s -X POST "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/fix-mappings" > fix_result.json
echo Fix Result:
type fix_result.json
echo.
echo.

echo [STEP 3] Checking status after fix...
echo.
curl -s "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" > after_fix_status.json
echo Status After Fix:
type after_fix_status.json
echo.
echo.

echo [STEP 4] Adding a new test employee...
echo.
curl -s -X POST "%BASE_URL%/api/subadmin/%SUBADMIN_ID%/add-employee" ^
  -H "Content-Type: multipart/form-data" ^
  -F "firstName=TestDevice" ^
  -F "lastName=MappingEmployee" ^
  -F "email=testdevice@example.com" ^
  -F "phone=9999999999" ^
  -F "address=Test Address" ^
  -F "position=Test Position" ^
  -F "department=Test Department" ^
  -F "salary=50000" > add_employee_result.json
echo Add Employee Result:
type add_employee_result.json
echo.
echo.

echo [STEP 5] Checking device mapping status after adding employee...
echo.
curl -s "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" > after_add_status.json
echo Status After Adding Employee:
type after_add_status.json
echo.
echo.

echo [STEP 6] Getting the new employee ID and checking individual mapping...
echo.
for /f "tokens=2 delims=:" %%a in ('findstr "empId" add_employee_result.json') do (
    set NEW_EMP_ID=%%a
    set NEW_EMP_ID=!NEW_EMP_ID:,=!
    set NEW_EMP_ID=!NEW_EMP_ID: =!
)

if defined NEW_EMP_ID (
    echo Checking individual employee mapping for ID: %NEW_EMP_ID%
    curl -s "%BASE_URL%/api/debug/device-mapping/employee/%NEW_EMP_ID%/status" > new_employee_status.json
    echo New Employee Mapping Status:
    type new_employee_status.json
    echo.
    echo.
)

echo [STEP 7] Testing employee update (if employee exists)...
echo.
curl -s -X PUT "%BASE_URL%/api/subadmin/%SUBADMIN_ID%/update-employee/504" ^
  -H "Content-Type: multipart/form-data" ^
  -F "firstName=UpdatedTest" ^
  -F "lastName=Employee" ^
  -F "email=updated@example.com" ^
  -F "phone=8888888888" ^
  -F "address=Updated Address" ^
  -F "position=Updated Position" ^
  -F "department=Updated Department" ^
  -F "salary=55000" > update_employee_result.json
echo Update Employee Result:
type update_employee_result.json
echo.
echo.

echo [STEP 8] Final device mapping status check...
echo.
curl -s "%BASE_URL%/api/debug/device-mapping/subadmin/%SUBADMIN_ID%/status" > final_status.json
echo Final Status:
type final_status.json
echo.
echo.

echo [STEP 9] Comparing results...
echo.
echo ========================================
echo COMPARISON SUMMARY
echo ========================================

echo.
echo INITIAL STATUS:
findstr "totalEmployees\|totalMappings\|mappingCoverage" initial_status.json

echo.
echo AFTER FIX:
findstr "totalEmployees\|totalMappings\|mappingCoverage" after_fix_status.json

echo.
echo AFTER ADDING EMPLOYEE:
findstr "totalEmployees\|totalMappings\|mappingCoverage" after_add_status.json

echo.
echo FINAL STATUS:
findstr "totalEmployees\|totalMappings\|mappingCoverage" final_status.json

echo.
echo ========================================
echo TEST COMPLETE
echo ========================================
echo.
echo Check the JSON files for detailed results:
echo - initial_status.json
echo - after_fix_status.json  
echo - after_add_status.json
echo - final_status.json
echo - add_employee_result.json
echo - update_employee_result.json
echo.

pause
