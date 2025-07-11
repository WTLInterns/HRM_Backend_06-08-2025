# 🚨 EMERGENCY MESSAGE DEBUG GUIDE

## 🔍 **ISSUE ANALYSIS**

You mentioned that the emergency message endpoint returns success but notifications don't reach users. Let me help you debug this step by step.

## 🎯 **IDENTIFIED ISSUES & SOLUTIONS**

### **1. .env File Configuration (FIXED)**
**Issue:** Your `.env` file had old individual Firebase variables instead of Base64 encoded credentials.
**Solution:** ✅ Updated to use `FIREBASE_CREDENTIALS_BASE64`

### **2. Potential Issues to Check**

#### **A. Employee FCM Token Registration**
**Problem:** Employees might not have FCM tokens registered in the database.

**Check this by:**
1. **Database Query:**
   ```sql
   SELECT empId, fullName, fcmToken, notificationsEnabled 
   FROM employee 
   WHERE subadmin_id = YOUR_SUBADMIN_ID;
   ```

2. **Expected Result:**
   - `fcmToken` should NOT be NULL or empty
   - `notificationsEnabled` should be TRUE or NULL

#### **B. Firebase Service Initialization**
**Check Backend Logs for:**
```
🔥 Initializing Firebase...
🔐 Using Base64 encoded Firebase credentials...
✅ Firebase credentials decoded from Base64!
✅ Firebase initialized successfully!
```

#### **C. Emergency Message Backend Logs**
**When you send emergency message, check for:**
```
🚨 EMERGENCY MESSAGE ALERT 🚨
📧 From: [Company Name] (Subadmin ID: [ID])
📧 Title: 🚨 EMERGENCY ALERT - [Company]
📧 Message: [Your Message]
👥 Target: [X] employees
📱 Sending emergency notification to: [Employee Name] (ID: [ID])
   ✅ FCM Message ID: [Message ID]
📊 EMERGENCY MESSAGE SUMMARY:
   📱 FCM notifications sent: [Count]
   📝 Database logs created: [Count]
   👥 Total employees: [Count]
```

## 🛠️ **DEBUGGING STEPS**

### **Step 1: Restart Backend with New .env**
```bash
# Stop current backend
# Restart with new .env configuration
mvn spring-boot:run
```

### **Step 2: Check Firebase Initialization**
Look for these logs on startup:
- ✅ `🔐 Using Base64 encoded Firebase credentials...`
- ✅ `✅ Firebase initialized successfully!`

### **Step 3: Test FCM Token Registration**
**Frontend Console Check:**
1. Open browser console
2. Go to Emergency Message page
3. Look for: `🔍 Subadmin FCM token for emergency message: [token]...`

### **Step 4: Send Test Emergency Message**
1. **Send message from frontend**
2. **Check backend logs immediately**
3. **Look for employee processing logs**

### **Step 5: Database Verification**
**Check if notifications are logged:**
```sql
SELECT * FROM notification_log 
WHERE notification_type = 'EMERGENCY_MESSAGE' 
ORDER BY created_at DESC 
LIMIT 10;
```

## 🔧 **COMMON ISSUES & FIXES**

### **Issue 1: No FCM Tokens for Employees**
**Symptoms:** Backend logs show "⚠️ No FCM token for employee: [Name] - logging only"

**Solution:**
1. **Employees need to login to frontend**
2. **FCM tokens auto-register on login**
3. **Check `firebaseService.registerTokenWithBackend()` calls**

### **Issue 2: Firebase Not Initialized**
**Symptoms:** Backend logs show "⚠️ Firebase not available - logging notification instead"

**Solution:**
1. **Check .env file is loaded correctly**
2. **Verify Base64 credentials are valid**
3. **Restart backend application**

### **Issue 3: Notifications Disabled**
**Symptoms:** Backend logs show "⚠️ Notifications disabled for employee: [Name]"

**Solution:**
```sql
UPDATE employee 
SET notificationsEnabled = true 
WHERE subadmin_id = YOUR_SUBADMIN_ID;
```

### **Issue 4: Invalid FCM Tokens**
**Symptoms:** Backend logs show "❌ FCM failed for [Employee]: [Error]"

**Solution:**
1. **Employees need to re-login to frontend**
2. **New FCM tokens will be generated**
3. **Old tokens expire and need refresh**

## 📋 **TESTING CHECKLIST**

### **Backend Verification:**
- [ ] ✅ .env file updated with Base64 credentials
- [ ] ✅ Backend restarted
- [ ] ✅ Firebase initialization logs show success
- [ ] ✅ Emergency endpoint accessible

### **Database Verification:**
- [ ] Employees exist under subadmin
- [ ] Employees have FCM tokens
- [ ] Notifications enabled for employees
- [ ] Notification logs are created

### **Frontend Verification:**
- [ ] Emergency message component loads
- [ ] FCM token generation works
- [ ] API call succeeds (200 response)
- [ ] Success message shows employee count

### **Notification Verification:**
- [ ] Backend logs show FCM messages sent
- [ ] Employees receive notifications
- [ ] Notification data is correct

## 🚀 **QUICK TEST COMMANDS**

### **1. Check Employee FCM Tokens:**
```sql
SELECT 
    empId, 
    CONCAT(firstName, ' ', lastName) as fullName,
    CASE 
        WHEN fcmToken IS NULL THEN 'NO TOKEN'
        WHEN fcmToken = '' THEN 'EMPTY TOKEN'
        ELSE CONCAT(LEFT(fcmToken, 20), '...')
    END as tokenStatus,
    notificationsEnabled,
    fcmTokenUpdatedAt
FROM employee 
WHERE subadmin_id = YOUR_SUBADMIN_ID;
```

### **2. Check Recent Notifications:**
```sql
SELECT 
    user_type,
    user_id,
    title,
    message,
    notification_type,
    fcm_message_id,
    created_at
FROM notification_log 
WHERE notification_type = 'EMERGENCY_MESSAGE'
ORDER BY created_at DESC 
LIMIT 5;
```

### **3. Test Firebase Endpoint:**
```bash
curl -X POST http://localhost:8080/api/fcm/send \
  -H "Content-Type: application/json" \
  -d '{
    "token": "test_token",
    "title": "Test Notification",
    "body": "This is a test",
    "data": {"type": "TEST"}
  }'
```

## 🎯 **EXPECTED RESULTS**

### **Successful Emergency Message:**
1. **Frontend:** Success toast with employee count
2. **Backend Logs:** FCM messages sent to all employees
3. **Database:** Notification logs created
4. **Mobile/Browser:** Employees receive notifications

### **If Still Not Working:**
1. **Share backend logs** from emergency message attempt
2. **Share database query results** for employee FCM tokens
3. **Check if employees are logged into frontend recently**

## 📞 **NEXT STEPS**

1. **Update .env file** ✅ (Already done)
2. **Restart backend** with new configuration
3. **Test emergency message** and share logs
4. **Check database** for FCM tokens and notification logs

The issue is likely that employees don't have FCM tokens registered. This happens when they haven't logged into the frontend recently or FCM token registration failed.
