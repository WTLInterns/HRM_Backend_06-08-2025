# Device Mapping Test Guide

## 🚀 Quick Start

### Option 1: Automated PowerShell Test (Recommended)
```powershell
cd hrm_backend
.\test-device-mapping.ps1
```

### Option 2: Quick Batch Test
```cmd
cd hrm_backend
quick-test.bat
```

### Option 3: Manual API Testing (Step by Step)

## 📋 Manual Testing Steps

### Step 1: Check Current Status
```bash
curl https://api.managifyhr.com/api/debug/device-mapping/subadmin/1/status
```

**Look for:**
- `totalEmployees`: Number of employees
- `totalMappings`: Number of device mappings
- `mappingCoverage`: Should be "X/X" (all employees mapped)

### Step 2: Fix Existing Issues (if any)
```bash
curl -X POST https://api.managifyhr.com/api/debug/device-mapping/subadmin/1/fix-mappings
```

### Step 3: Add New Employee via API
```bash
curl -X POST https://api.managifyhr.com/api/subadmin/1/add-employee \
  -F "firstName=TestMapping" \
  -F "lastName=Employee" \
  -F "email=testmapping@example.com" \
  -F "phone=9876543210" \
  -F "address=Test Address" \
  -F "position=Test Position" \
  -F "department=Test Department" \
  -F "salary=50000"
```

### Step 4: Check Status After Adding
```bash
curl https://api.managifyhr.com/api/debug/device-mapping/subadmin/1/status
```

**Verify:**
- `totalEmployees` increased by 1
- `totalMappings` also increased by 1
- `mappingCoverage` still shows "X/X" (perfect coverage)

### Step 5: Check Individual Employee
```bash
# Replace 505 with the actual employee ID from step 3
curl https://api.managifyhr.com/api/debug/device-mapping/employee/505/status
```

**Verify:**
- `deviceSerialNumber` is set
- `mappingCount` is 1
- `deviceSerialMatch` is true

## 🎯 Expected Results

### ✅ SUCCESS Indicators:
```json
{
  "totalEmployees": 4,
  "totalMappings": 4,
  "summary": {
    "mappingCoverage": "4/4"
  }
}
```

### ❌ FAILURE Indicators:
```json
{
  "totalEmployees": 4,
  "totalMappings": 3,  // ❌ Missing mapping
  "summary": {
    "mappingCoverage": "3/4"  // ❌ Not all mapped
  }
}
```

## 🔧 Troubleshooting

### Issue: No Device Serial Number
```bash
# Check if subadmin has device serial assigned
curl https://api.managifyhr.com/api/debug/device-mapping/subadmin/1/status
```
Look for: `"hasDeviceSerial": false`

**Solution:** Assign device serial in master admin panel

### Issue: Mappings Not Created
```bash
# Force create mappings
curl -X POST https://api.managifyhr.com/api/debug/device-mapping/subadmin/1/fix-mappings
```

### Issue: Individual Employee Missing Mapping
```bash
# Create mapping for specific employee
curl -X POST https://api.managifyhr.com/api/debug/device-mapping/employee/504/create-mapping
```

## 📊 Test Scenarios

### Scenario 1: Fresh System
1. Check status (should show some unmapped employees)
2. Run fix-mappings
3. Add new employee
4. Verify all employees mapped

### Scenario 2: Existing System
1. Check current coverage
2. Add new employee
3. Verify coverage maintained
4. Update existing employee
5. Verify mappings preserved

### Scenario 3: Problem Recovery
1. Identify missing mappings
2. Run fix-mappings
3. Verify all fixed
4. Test adding new employee

## 🔍 Debug Endpoints

### Get All Mappings for Device
```bash
curl https://api.managifyhr.com/api/debug/device-mapping/device/DEVICE001/mappings
```

### Get Subadmin Status
```bash
curl https://api.managifyhr.com/api/debug/device-mapping/subadmin/1/status
```

### Get Employee Status
```bash
curl https://api.managifyhr.com/api/debug/device-mapping/employee/504/status
```

### Fix All Mappings
```bash
curl -X POST https://api.managifyhr.com/api/debug/device-mapping/subadmin/1/fix-mappings
```

### Create Individual Mapping
```bash
curl -X POST https://api.managifyhr.com/api/debug/device-mapping/employee/504/create-mapping
```

## 📝 Test Checklist

- [ ] Backend is running on port 8282
- [ ] Subadmin has device serial number assigned
- [ ] Initial status check shows current state
- [ ] Fix-mappings resolves any existing issues
- [ ] Adding employee preserves existing mappings
- [ ] New employee gets device serial automatically
- [ ] New employee gets device mapping automatically
- [ ] Final coverage is 100% (X/X)
- [ ] Individual employee check shows proper mapping
- [ ] Update employee doesn't break mappings

## 🎉 Success Criteria

**The fix is working if:**
1. ✅ Adding new employee increases both employee count AND mapping count
2. ✅ Existing employees keep their mappings when new ones are added
3. ✅ New employees automatically get device serial number
4. ✅ New employees automatically get device mapping
5. ✅ Coverage remains at 100% (X/X) after operations
6. ✅ No manual reassignment needed in admin panel
