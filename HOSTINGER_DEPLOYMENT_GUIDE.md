# Hostinger VPS Deployment Guide

## 🎯 Overview
This guide will help you deploy your HRM Biometric Backend to Hostinger VPS without Docker, using direct JAR deployment.

## 🔧 Prerequisites

### 1. Hostinger VPS Requirements
- **VPS Plan**: Business VPS or higher (recommended)
- **OS**: Ubuntu 20.04 LTS or Ubuntu 22.04 LTS
- **RAM**: Minimum 2GB (4GB recommended)
- **Storage**: Minimum 20GB
- **Java**: OpenJDK 17 or higher

### 2. Domain Configuration
- Domain name pointed to your VPS IP
- SSL certificate (Let's Encrypt recommended)

## 🚀 Step-by-Step Deployment

### Step 1: Connect to Your Hostinger VPS

```bash
# SSH into your VPS
ssh root@your-vps-ip

# Or if you have a domain
ssh root@yourdomain.com
```

### Step 2: Install Required Software

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 17
sudo apt install openjdk-17-jdk -y

# Verify Java installation
java -version

# Install MySQL (if not already installed)
sudo apt install mysql-server -y

# Install Nginx (for reverse proxy)
sudo apt install nginx -y

# Install Git (for cloning repository)
sudo apt install git -y

# Install unzip (for extracting files)
sudo apt install unzip -y
```

### Step 3: Setup MySQL Database

```bash
# Secure MySQL installation
sudo mysql_secure_installation

# Login to MySQL
sudo mysql -u root -p

# Create databases
CREATE DATABASE new_hrm;
CREATE DATABASE easywdms;

# Create user for HRM application
CREATE USER 'hrm_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON new_hrm.* TO 'hrm_user'@'localhost';
GRANT ALL PRIVILEGES ON easywdms.* TO 'hrm_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Step 4: Clone and Deploy Application

```bash
# Clone your repository
cd /opt
sudo git clone https://github.com/WTLInterns/HRM_Backend_Biometric.git
cd HRM_Backend_Biometric

# Make deployment script executable
sudo chmod +x deploy-hostinger.sh

# Run deployment script
sudo ./deploy-hostinger.sh
```

### Step 5: Configure Environment Variables

```bash
# Edit environment file
sudo nano /opt/hrm-biometric-backend/.env
```

**Update these values in the .env file:**

```bash
# Database Configuration
DATABASE_URL=jdbc:mysql://localhost:3306/new_hrm?allowMultiQueries=true&useSSL=true&serverTimezone=UTC
DATABASE_USERNAME=hrm_user
DATABASE_PASSWORD=your_secure_password

# Firebase Configuration (Copy from your local .env)
FIREBASE_PROJECT_ID=your-actual-project-id
FIREBASE_PRIVATE_KEY_ID=your-actual-private-key-id
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nYour actual private key\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=your-actual-client-email
FIREBASE_CLIENT_ID=your-actual-client-id
FIREBASE_CLIENT_X509_CERT_URL=your-actual-client-cert-url

# Mail Configuration
MAIL_USERNAME=your-actual-email@gmail.com
MAIL_PASSWORD=your-actual-app-password

# Google Maps API
GOOGLE_MAPS_API_KEY=your-actual-maps-api-key

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

### Step 6: Start the Application

```bash
# Start the service
sudo systemctl start hrm-biometric-backend

# Check status
sudo systemctl status hrm-biometric-backend

# View logs
sudo journalctl -u hrm-biometric-backend -f
```

### Step 7: Configure Nginx Reverse Proxy

```bash
# Create Nginx configuration
sudo nano /etc/nginx/sites-available/hrm-backend
```

**Add this configuration:**

```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;

    # SSL Configuration (Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;

    # Proxy to Spring Boot application
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Static files (if needed)
    location /uploads/ {
        alias /opt/hrm-biometric-backend/uploads/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

```bash
# Enable the site
sudo ln -s /etc/nginx/sites-available/hrm-backend /etc/nginx/sites-enabled/

# Test Nginx configuration
sudo nginx -t

# Restart Nginx
sudo systemctl restart nginx
```

### Step 8: Setup SSL Certificate (Let's Encrypt)

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx -y

# Get SSL certificate
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Test auto-renewal
sudo certbot renew --dry-run
```

## 🔧 Device Configuration for Cloud

### Configure F22 Devices to Push to Your Domain

**On each F22 device:**

```
MENU → Communication → Cloud Server Setting

Server Mode: Enable
Domain Name Enable: Yes
Server Address: yourdomain.com
Server Port: 443 (for HTTPS)
Push URL: /api/punch
Push Method: POST
Push Format: JSON
Push Interval: 30 seconds
```

## 📊 Monitoring and Management

### Application Management Commands

```bash
# Start application
sudo systemctl start hrm-biometric-backend

# Stop application
sudo systemctl stop hrm-biometric-backend

# Restart application
sudo systemctl restart hrm-biometric-backend

# Check status
sudo systemctl status hrm-biometric-backend

# View logs (real-time)
sudo journalctl -u hrm-biometric-backend -f

# View logs (last 100 lines)
sudo journalctl -u hrm-biometric-backend -n 100
```

### Quick Scripts (Already created by deployment script)

```bash
# View logs
/opt/hrm-biometric-backend/logs.sh

# Restart application
/opt/hrm-biometric-backend/restart.sh

# Stop application
/opt/hrm-biometric-backend/stop.sh
```

## 🧪 Testing Your Deployment

### Test 1: Health Check

```bash
curl https://yourdomain.com/api/multi-device/health
```

### Test 2: Cloud Punch API

```bash
curl -X POST https://yourdomain.com/api/punch \
  -H "Content-Type: application/json" \
  -d '{
    "emp_code": "52",
    "device_serial": "TEST_DEVICE",
    "punch_time": "2025-07-12 10:30:00",
    "punch_state": "0",
    "verify_type": "1"
  }'
```

### Test 3: Device Status

```bash
curl https://yourdomain.com/api/multi-device/devices
```

## 🔧 Troubleshooting

### Common Issues

1. **Application won't start**
   ```bash
   # Check logs
   sudo journalctl -u hrm-biometric-backend -n 50
   
   # Check Java version
   java -version
   
   # Check port availability
   sudo netstat -tlnp | grep 8080
   ```

2. **Database connection issues**
   ```bash
   # Test MySQL connection
   mysql -u hrm_user -p -h localhost new_hrm
   
   # Check MySQL status
   sudo systemctl status mysql
   ```

3. **SSL certificate issues**
   ```bash
   # Check certificate status
   sudo certbot certificates
   
   # Renew certificate
   sudo certbot renew
   ```

## 🚀 Performance Optimization

### JVM Tuning

Edit the systemd service file:

```bash
sudo nano /etc/systemd/system/hrm-biometric-backend.service
```

Update the Environment line:

```
Environment=JAVA_OPTS="-Xms1g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Database Optimization

```sql
-- Optimize MySQL for production
SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB
SET GLOBAL max_connections = 200;
SET GLOBAL query_cache_size = 67108864; -- 64MB
```

## 🎯 Final Checklist

- [ ] VPS setup with Java 17
- [ ] MySQL databases created
- [ ] Application deployed and running
- [ ] Environment variables configured
- [ ] Nginx reverse proxy configured
- [ ] SSL certificate installed
- [ ] Firewall configured (ports 80, 443, 22)
- [ ] F22 devices configured to push to domain
- [ ] Health checks passing
- [ ] Monitoring setup

## 🌐 Your URLs After Deployment

- **Main Application**: https://yourdomain.com
- **Cloud Punch API**: https://yourdomain.com/api/punch
- **Device Management**: https://yourdomain.com/api/multi-device/devices
- **Health Check**: https://yourdomain.com/api/multi-device/health

**Your HRM Biometric Backend is now live on Hostinger VPS!** 🎉
