# Device Configuration Guide for Cloud HRM Backend

## 🎯 Overview
This guide explains how to configure biometric devices to automatically send data to your cloud HRM backend without manual IP configuration.

## 🔧 Cloud Backend URLs

### Production URLs (Replace with your domain):
```
Main Punch API: https://yourdomain.com/api/punch
Bulk Punch API: https://yourdomain.com/api/punch/bulk
Device Heartbeat: https://yourdomain.com/api/device/heartbeat
```

### Development URLs (Local testing):
```
Main Punch API: http://localhost:8181/api/punch
Bulk Punch API: http://localhost:8181/api/punch/bulk
Device Heartbeat: http://localhost:8181/api/device/heartbeat
```

## 🔧 ZKTeco F22 Configuration

### Step 1: Access Device Menu
1. Press **MENU** on F22 device
2. Navigate to **Communication** → **Cloud Server Setting**

### Step 2: Configure Cloud Push
```
Server Mode: Enable
Domain Name Enable: Yes
Server Address: yourdomain.com
Server Port: 443 (for HTTPS) or 80 (for HTTP)
Enable Proxy Server: No
```

### Step 3: Set Push URL
```
Push URL: /api/punch
Push Method: POST
Push Format: JSON
Push Interval: 30 seconds
```

### Step 4: Data Format Configuration
The device should send data in this JSON format:
```json
{
  "emp_code": "52",
  "device_serial": "BOCK194960340",
  "punch_time": "2025-07-11 14:30:00",
  "punch_state": "0",
  "verify_type": "1"
}
```

## 🔧 Alternative Configuration Methods

### Method 1: HTTP Push (Recommended)
```
URL: https://yourdomain.com/api/punch
Method: POST
Content-Type: application/json
Authentication: None (or API key if implemented)
```

### Method 2: Bulk Push (For multiple records)
```
URL: https://yourdomain.com/api/punch/bulk
Method: POST
Content-Type: application/json

Body Format:
{
  "device_serial": "BOCK194960340",
  "punches": [
    {
      "emp_code": "52",
      "punch_time": "2025-07-11 14:30:00",
      "punch_state": "0",
      "verify_type": "1"
    },
    {
      "emp_code": "552",
      "punch_time": "2025-07-11 14:31:00",
      "punch_state": "0",
      "verify_type": "1"
    }
  ]
}
```

### Method 3: Heartbeat (Device Status)
```
URL: https://yourdomain.com/api/device/heartbeat
Method: POST
Interval: Every 5 minutes

Body Format:
{
  "device_serial": "BOCK194960340",
  "status": "online",
  "device_ip": "192.168.1.101",
  "timestamp": "2025-07-11 14:30:00"
}
```

## 🌐 Cloud Deployment Options

### Option 1: Hostinger VPS
1. Deploy your Spring Boot application
2. Configure domain name (e.g., hrm.yourdomain.com)
3. Set up SSL certificate
4. Configure devices to push to: https://hrm.yourdomain.com/api/punch

### Option 2: AWS/Google Cloud
1. Deploy using Docker container
2. Set up load balancer
3. Configure auto-scaling
4. Use managed database service

### Option 3: Heroku (Simple deployment)
1. Push code to Heroku
2. Configure environment variables
3. Use Heroku Postgres addon
4. Devices push to: https://your-app.herokuapp.com/api/punch

## 🔧 Environment Variables for Production

Create a `.env` file or set these environment variables:

```bash
# Database Configuration
DATABASE_URL=jdbc:mysql://your-db-host:3306/new_hrm
DATABASE_USERNAME=your_db_user
DATABASE_PASSWORD=your_db_password

# Firebase Configuration
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY=your-private-key
FIREBASE_CLIENT_EMAIL=your-client-email

# Mail Configuration
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Google Maps API
GOOGLE_MAPS_API_KEY=your-maps-api-key

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

## 🚀 Testing Device Configuration

### Test 1: Manual API Test
```bash
curl -X POST https://yourdomain.com/api/punch \
  -H "Content-Type: application/json" \
  -d '{
    "emp_code": "52",
    "device_serial": "TEST_DEVICE",
    "punch_time": "2025-07-11 14:30:00",
    "punch_state": "0",
    "verify_type": "1"
  }'
```

### Test 2: Device Heartbeat Test
```bash
curl -X POST https://yourdomain.com/api/device/heartbeat \
  -H "Content-Type: application/json" \
  -d '{
    "device_serial": "BOCK194960340",
    "status": "online",
    "device_ip": "192.168.1.101"
  }'
```

### Test 3: Check Device Status
```bash
curl https://yourdomain.com/api/multi-device/devices
```

## 🔧 Troubleshooting

### Common Issues:

1. **Device can't reach server**
   - Check internet connectivity
   - Verify domain name resolution
   - Test with IP address first

2. **SSL Certificate issues**
   - Use HTTP for testing first
   - Ensure valid SSL certificate
   - Check certificate chain

3. **Authentication errors**
   - Verify API endpoint is accessible
   - Check CORS configuration
   - Test with curl/Postman first

4. **Data format issues**
   - Check JSON format
   - Verify field names match
   - Test with sample data

### Debug Steps:
1. Test local deployment first
2. Use ngrok for temporary public URL
3. Check server logs for errors
4. Verify database connectivity
5. Test with multiple devices

## 📊 Monitoring

### Check System Status:
```bash
# Health check
curl https://yourdomain.com/api/multi-device/health

# Device status
curl https://yourdomain.com/api/multi-device/devices

# Today's statistics
curl https://yourdomain.com/api/multi-device/stats/today
```

## 🎯 Benefits of This Setup

✅ **No manual IP configuration** - Devices connect via domain name
✅ **Automatic device registration** - New devices appear automatically
✅ **Centralized management** - All devices send to one backend
✅ **Real-time processing** - Immediate attendance processing
✅ **Multi-location support** - Works across different networks
✅ **Scalable architecture** - Easy to add new devices/locations
