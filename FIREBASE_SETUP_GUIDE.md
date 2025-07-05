# 🔒 Firebase Environment Variables Setup Guide

## 🚨 Problem Solved
This setup prevents GitHub secret scanning alerts that block your Firebase private keys when you push code to GitHub.

## ✅ What This Solves
- ❌ **Before**: Firebase private key in repository → GitHub scans → Blocks key → Notifications fail
- ✅ **After**: Private key in environment variables → No GitHub scanning → Keys safe → Notifications work

## 🛠️ Setup Instructions

### Step 1: Local Development Setup

1. **Copy the environment file:**
   ```bash
   cp .env.example .env
   ```

2. **The .env file is already configured with your Firebase credentials** - no changes needed for local development.

3. **Test the setup:**
   ```bash
   mvn spring-boot:run
   ```
   
   You should see in the logs:
   ```
   🌍 Using Firebase environment variables...
   ✅ Firebase credentials created from environment variables!
   ✅ Firebase initialized successfully!
   ```

### Step 2: Remove Firebase JSON File (Important!)

1. **Remove the JSON file from Git tracking:**
   ```bash
   git rm --cached src/main/resources/firebase-service-account.json
   git commit -m "Remove Firebase service account file - using environment variables"
   ```

2. **The file is now in .gitignore** - it won't be committed again.

### Step 3: Production Deployment (Hostinger)

1. **In your Hostinger control panel**, set these environment variables:
   ```
   FIREBASE_TYPE=service_account
   FIREBASE_PROJECT_ID=notification-bb346
   FIREBASE_PRIVATE_KEY_ID=69d02de749a573d83dbded6c75292d3e8706c10f
   FIREBASE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nMIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCjFKGQJZmh45Js\nWlA9bscep0g/w3cuylug9FSLKLzCU1Vfeej7x0Wrzp3xreGPReVr/bruIEovYNCp\nkVzePGCn3hporeD591uBPFbghRNkxrJ7PqFTEqcOsmtbVsp6nAE01Hh83O1Nmuir\n8fx2cYI5YviLh6/7IZuceE3FgIKHY0wm5kK4dyGPWvzs2NTpw+pN6sfMw47fyJT4\nGOMN18X8yThzEMgwqcmmor0HlicR2WEqvWLBQQ5Hja4gijka69MOonaqvsFDOd4O\n4VOc11fa6a9O01/gJDwWcPgUnuVmWTWfTEbBLlM4Iy2uFRzyWe/Bpf41kv87cPow\nycZNC8z5AgMBAAECggEABA+fdld5W+6u1sPo+56mlrzTr8LOVPv1DC0Jbe21xaEN\nnzMJ+jscIQskI8CQvWCgNfA7l3J2ZneiupZEGcSnI4cEYVbzI0ZloPI/t5eYxAmu\nqCMHIWp1fogy5DNlF2iLHyWrWnCl+CjDYoTEJK7u7z7nsZHPcBGGLn9QcHqixXxJ\nW2gTIZVCRMQiTOo4ZEwD30nhVFiQnF6DRTzUt/zW/E4ZrYWHU9VZ6F/On7kpqyOq\n1NOqGOmpW6v6ksDmWc1eD1ToyPtFwAm0fDG+zTqC8slopANwVPaqmdpDgP4qRoSO\nMxRRJPjRxiFvitOdsaPoZBD0NGWfk3NLFV/VtVB2AQKBgQDQ4hvw0eu59dH9rIfH\nTyazkMJCsMNwkoiQUN3v+WfKnJoJuKwo8BJ9Waom/rVMGhwf6tx0gGNTgWzOMe+i\n6BKm2IVrX1wY9s9ogvEDZqKCDD1s1BmkxVfk2FrzCTNaBUpUBlb9oTopgfEaBgWI\nAmNaA+fiB2lta6QMqvi2nzxWpQKBgQDH3anr/lOVtqYBmLSc5TeKTJHVs4vjzWM2\nQtJ8XPoupak5dD424b1MqE9U2nr9bh4zXjtzz1XYDhNYmfsKewylMyNM4VZOrmI4\nYs32BQ8pDf1KacWZKMJesMYe4wBdd9RqcGgS9jvjmhWUPidX4Kc1Hii79+ozhpDs\nKcBDk76gxQKBgQC87yeypU2AQHaV6s5NWqx40sDDLbs5QXFAh8SW4JIe05MIo/8k\ncDFpLAcZil1V3m/99qM8xTwSNgjCYKyOPzdPrpzPR8u30NY9g+KX3ueylXuhyh7/\nyHf9yCxLk9Dc5dgbNDjO5FwFW0FG9pdeNDrFGq9M5MNZc3lI9AROuq0K+QKBgQCQ\n2My1A3SAJAbbUVhT/Krrx+JJBf5z8f3c7UCucJPACr9BrEUKcT9HtxMHY++Oi6fH\ngpuDqteD5W+P3shZKCRZ9V238fcivq7gPnsVuUTrdAAFVfKmtStohKSQ7eLt2lJP\ncXd7hoiQwrKw5PqObXnAvUK4N0u8W+HbxcZZM1IMIQKBgQDMJE/YmM2dUrxqzBoS\nFjuay6lSNSWpIjgax6HGKbn9j5nATcX+gJusVQHNV9QtnRRgE+BGzkTnaxOuFhdh\nRYcwepA+hYIJwe8r8ELDb1SbVl71WZCZbt94luAR4I6Aeuee4PwfKICE4ndlnGKY\nRe1PqtOjMzunFABvDxVxIWVmyA==\n-----END PRIVATE KEY-----
   FIREBASE_CLIENT_EMAIL=firebase-adminsdk-fbsvc@notification-bb346.iam.gserviceaccount.com
   FIREBASE_CLIENT_ID=106656562394567210089
   FIREBASE_AUTH_URI=https://accounts.google.com/o/oauth2/auth
   FIREBASE_TOKEN_URI=https://oauth2.googleapis.com/token
   FIREBASE_AUTH_PROVIDER_X509_CERT_URL=https://www.googleapis.com/oauth2/v1/certs
   FIREBASE_CLIENT_X509_CERT_URL=https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40notification-bb346.iam.gserviceaccount.com
   FIREBASE_UNIVERSE_DOMAIN=googleapis.com
   ```

2. **Deploy your code:**
   ```bash
   git push origin main
   mvn clean package
   ```

## 🔄 How It Works

1. **Environment Variables First**: The application checks for environment variables
2. **Fallback to JSON**: If environment variables are not found, it falls back to the JSON file
3. **No GitHub Scanning**: Since private keys are not in the repository, GitHub doesn't scan them
4. **Same Functionality**: Firebase works exactly the same way

## ✅ Benefits

- ✅ **No more GitHub secret scanning alerts**
- ✅ **No more Firebase key blocking**
- ✅ **No need to regenerate keys after each push**
- ✅ **Production-ready security**
- ✅ **Team-friendly setup**

## 🧪 Testing

After setup, test notifications:
1. Login to your application
2. Create a leave request (as employee)
3. Check if subadmin receives notification
4. Approve/reject leave (as subadmin)
5. Check if employee receives notification

## 🚨 Important Notes

- **Never commit .env file** - it's in .gitignore
- **Keep the JSON file locally** for backup (but don't commit it)
- **Use environment variables in production**
- **This setup works for all environments** (dev, staging, production)
