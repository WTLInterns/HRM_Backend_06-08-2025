# 🚀 HRM Backend Deployment Guide

## 📋 **Overview**
This guide provides step-by-step instructions for building and deploying the HRM Backend with biometric integration.

## 🔧 **Prerequisites**
- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Git

## 📦 **Building the Application**

### **Step 1: Clone the Repository**
```bash
git clone https://github.com/WTLInterns/HRM_Backend_Biometric.git
cd HRM_Backend_Biometric
```

### **Step 2: Build the JAR File**
```bash
# Clean and build the project
./mvnw clean package -DskipTests

# Or on Windows
mvnw.cmd clean package -DskipTests
```

### **Step 3: Verify Build**
```bash
# Check if JAR file is created
ls -la target/demo-0.0.1-SNAPSHOT.jar

# On Windows
dir target\demo-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
target/demo-0.0.1-SNAPSHOT.jar (approximately 240MB)
```

## 🗄️ **Database Setup**

### **Step 1: Create Databases**
```sql
-- Main HRM Database
CREATE DATABASE new_hrm;

-- EasyTimePro Integration Database (if using biometric middleware)
CREATE DATABASE easywdms;
```

### **Step 2: Configure Database Connection**
Update `src/main/resources/application.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/new_hrm?allowMultiQueries=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## 🚀 **Deployment Options**

### **Option 1: Local Development**
```bash
# Run directly with Maven
./mvnw spring-boot:run

# Or run the JAR file
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### **Option 2: Production Server**
```bash
# Copy JAR to server
scp target/demo-0.0.1-SNAPSHOT.jar user@server:/opt/hrm/

# Run on server
java -jar -Dspring.profiles.active=production /opt/hrm/demo-0.0.1-SNAPSHOT.jar
```

### **Option 3: Docker Deployment**
```bash
# Build Docker image
docker build -t hrm-backend .

# Run with Docker Compose
docker-compose up -d
```

### **Option 4: Hostinger Deployment**
1. **Upload JAR file** to your Hostinger server
2. **Configure Java environment** on Hostinger
3. **Set up database** connection
4. **Run application** with production profile

## 🔧 **Configuration**

### **Environment Variables**
```bash
# Database
export DATABASE_URL=jdbc:mysql://localhost:3306/new_hrm
export DATABASE_USERNAME=hrm_user
export DATABASE_PASSWORD=your_password

# Firebase (for notifications)
export FIREBASE_PROJECT_ID=your_project_id
export FIREBASE_PRIVATE_KEY="your_private_key"
export FIREBASE_CLIENT_EMAIL=your_client_email

# Email Configuration
export MAIL_USERNAME=your_email@gmail.com
export MAIL_PASSWORD=your_app_password

# Google Maps API
export GOOGLE_MAPS_API_KEY=your_api_key
```

### **Application Profiles**
- **Development**: `application.properties`
- **Production**: `application-production.properties`

## 🔍 **Health Checks**

### **Verify Deployment**
```bash
# Check application health
curl http://localhost:8181/api/multi-device/health

# Expected Response:
{
  "status": "OK",
  "service": "MultiDeviceManagementController",
  "database_connection": "OK",
  "checked_at": "2025-07-12T10:30:00"
}
```

### **Test Endpoints**
```bash
# Test employee API
curl http://localhost:8181/api/employee/1/list

# Test device management
curl http://localhost:8181/api/multi-device/devices

# Test subadmin API
curl http://localhost:8181/api/subadmin/all
```

## 📁 **Important Files**

### **JAR File Location**
```
target/demo-0.0.1-SNAPSHOT.jar (Main application JAR)
```

### **Configuration Files**
```
src/main/resources/application.properties
src/main/resources/application-production.properties
```

### **Build Files**
```
pom.xml (Maven configuration)
Dockerfile (Docker configuration)
docker-compose.yml (Docker Compose configuration)
```

## 🔧 **Troubleshooting**

### **Common Issues**

#### **1. JAR File Too Large for Git**
**Problem**: JAR files exceed GitHub's 100MB limit
**Solution**: Build locally using Maven commands above

#### **2. Database Connection Issues**
**Problem**: Cannot connect to database
**Solution**: 
- Check database credentials
- Verify database server is running
- Update connection URL in properties

#### **3. Port Already in Use**
**Problem**: Port 8181 is already in use
**Solution**:
```bash
# Kill process using port 8181
netstat -ano | findstr :8181
taskkill /PID <process_id> /F

# Or change port in application.properties
server.port=8182
```

#### **4. Memory Issues**
**Problem**: OutOfMemoryError
**Solution**:
```bash
# Increase JVM memory
java -Xmx2g -jar target/demo-0.0.1-SNAPSHOT.jar
```

## 📞 **Support**

### **Build Commands Summary**
```bash
# Full clean build
./mvnw clean package -DskipTests

# Quick build (skip tests)
./mvnw package -DskipTests

# Run application
java -jar target/demo-0.0.1-SNAPSHOT.jar

# Run with production profile
java -jar -Dspring.profiles.active=production target/demo-0.0.1-SNAPSHOT.jar
```

### **Deployment Checklist**
- [ ] Java 17+ installed
- [ ] Maven build successful
- [ ] Database configured and accessible
- [ ] Environment variables set
- [ ] JAR file created (target/demo-0.0.1-SNAPSHOT.jar)
- [ ] Application starts without errors
- [ ] Health check endpoint responds
- [ ] Frontend can connect to backend APIs

**Your HRM Backend with biometric integration is ready for deployment!** 🎉
