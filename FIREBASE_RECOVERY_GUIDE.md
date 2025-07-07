# 🔥 Firebase Service Account Recovery Guide

## 🚨 **ISSUE:** Notifications stopped working after git operations

### **Root Cause:**
The `firebase-service-account.json` file was removed/corrupted during git force push operations.

### **Immediate Fix:**

#### **Step 1: Restore Firebase Service Account File**

**Option A - From Backup:**
```bash
# If you have a backup, copy it back:
cp /path/to/backup/firebase-service-account.json src/main/resources/
```

**Option B - Download New from Firebase Console:**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings (gear icon)
4. Click "Service Accounts" tab
5. Click "Generate new private key"
6. Download the JSON file
7. Rename to `firebase-service-account.json`
8. Place in `src/main/resources/`

#### **Step 2: Verify File Structure**
The file should look like this:
```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-...@your-project.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "..."
}
```

#### **Step 3: Restart Application**
```bash
# Kill existing process
taskkill /F /IM java.exe

# Rebuild and restart
mvn clean package -DskipTests
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

#### **Step 4: Test Notifications**
```bash
# Test FCM endpoint
curl -X POST "https://api.managifyhr.com/api/fcm/send" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "your-device-token",
    "title": "Test Notification",
    "body": "Testing Firebase recovery"
  }'
```

### **Prevention for Future:**

#### **1. Create Backup Script**
```bash
# backup-firebase.bat
copy src\main\resources\firebase-service-account.json backup\firebase-service-account-backup.json
echo Firebase backup created at %date% %time%
```

#### **2. Update .gitignore (Already Done)**
```
# Firebase and sensitive files
firebase-service-account.json
**/firebase-service-account.json
src/main/resources/firebase-service-account.json
```

#### **3. Environment Variable Alternative**
Instead of file, use environment variable:
```java
// In FirebaseConfig.java
String firebaseCredentials = System.getenv("FIREBASE_CREDENTIALS");
if (firebaseCredentials != null) {
    InputStream serviceAccount = new ByteArrayInputStream(firebaseCredentials.getBytes());
    // Use this instead of file
}
```

### **Quick Recovery Commands:**
```bash
# 1. Check if file exists
ls src/main/resources/firebase-service-account.json

# 2. If missing, restore from backup
cp backup/firebase-service-account.json src/main/resources/

# 3. Restart application
taskkill /F /IM java.exe
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### **Contact Support:**
If you don't have backup and can't access Firebase Console:
- Contact your Firebase project admin
- Check if file exists in other project copies
- Look for email with original Firebase setup

---
**Note:** Always backup sensitive files before git operations!
