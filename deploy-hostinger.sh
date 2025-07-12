#!/bin/bash

# Hostinger VPS Deployment Script for HRM Biometric Backend
# This script deploys your Spring Boot application to Hostinger VPS

echo "🚀 Starting Hostinger VPS Deployment..."

# Configuration
APP_NAME="hrm-biometric-backend"
JAR_FILE="demo-0.0.1-SNAPSHOT.jar"
APP_PORT="8080"
PROFILE="production"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️ $1${NC}"
}

# Check if JAR file exists
if [ ! -f "target/$JAR_FILE" ]; then
    print_error "JAR file not found! Please run: ./mvnw clean package -DskipTests"
    exit 1
fi

print_status "JAR file found: target/$JAR_FILE"

# Create application directory
APP_DIR="/opt/$APP_NAME"
print_info "Creating application directory: $APP_DIR"

sudo mkdir -p $APP_DIR
sudo mkdir -p $APP_DIR/logs
sudo mkdir -p $APP_DIR/uploads

# Copy JAR file
print_info "Copying JAR file to $APP_DIR"
sudo cp target/$JAR_FILE $APP_DIR/

# Create environment file template
print_info "Creating environment configuration..."
sudo tee $APP_DIR/.env > /dev/null <<EOF
# Database Configuration
DATABASE_URL=jdbc:mysql://localhost:3306/new_hrm?allowMultiQueries=true&useSSL=true&serverTimezone=UTC
DATABASE_USERNAME=hrm_user
DATABASE_PASSWORD=your_db_password

# Firebase Configuration
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY_ID=your-private-key-id
FIREBASE_PRIVATE_KEY=your-private-key
FIREBASE_CLIENT_EMAIL=your-client-email
FIREBASE_CLIENT_ID=your-client-id
FIREBASE_AUTH_URI=https://accounts.google.com/o/oauth2/auth
FIREBASE_TOKEN_URI=https://oauth2.googleapis.com/token
FIREBASE_AUTH_PROVIDER_X509_CERT_URL=https://www.googleapis.com/oauth2/v1/certs
FIREBASE_CLIENT_X509_CERT_URL=your-client-cert-url
FIREBASE_UNIVERSE_DOMAIN=googleapis.com

# Mail Configuration
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Google Maps API
GOOGLE_MAPS_API_KEY=your-maps-api-key

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# SSL Configuration (optional)
SSL_ENABLED=false
SSL_KEYSTORE_PATH=
SSL_KEYSTORE_PASSWORD=
EOF

# Create systemd service file
print_info "Creating systemd service..."
sudo tee /etc/systemd/system/$APP_NAME.service > /dev/null <<EOF
[Unit]
Description=HRM Biometric Backend Spring Boot Application
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=$PROFILE -Dserver.port=$APP_PORT $APP_DIR/$JAR_FILE
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=$APP_NAME

# Environment variables
Environment=SPRING_PROFILES_ACTIVE=$PROFILE
Environment=SERVER_PORT=$APP_PORT
EnvironmentFile=$APP_DIR/.env

# JVM Options
Environment=JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"

[Install]
WantedBy=multi-user.target
EOF

# Create startup script
print_info "Creating startup script..."
sudo tee $APP_DIR/start.sh > /dev/null <<EOF
#!/bin/bash
cd $APP_DIR
source .env
java -jar -Dspring.profiles.active=$PROFILE -Dserver.port=$APP_PORT $JAR_FILE
EOF

sudo chmod +x $APP_DIR/start.sh

# Create stop script
sudo tee $APP_DIR/stop.sh > /dev/null <<EOF
#!/bin/bash
sudo systemctl stop $APP_NAME
EOF

sudo chmod +x $APP_DIR/stop.sh

# Create restart script
sudo tee $APP_DIR/restart.sh > /dev/null <<EOF
#!/bin/bash
sudo systemctl restart $APP_NAME
sudo systemctl status $APP_NAME
EOF

sudo chmod +x $APP_DIR/restart.sh

# Create log viewing script
sudo tee $APP_DIR/logs.sh > /dev/null <<EOF
#!/bin/bash
echo "📋 Viewing application logs (Press Ctrl+C to exit)..."
sudo journalctl -u $APP_NAME -f
EOF

sudo chmod +x $APP_DIR/logs.sh

# Set permissions
sudo chown -R root:root $APP_DIR
sudo chmod 755 $APP_DIR
sudo chmod 644 $APP_DIR/$JAR_FILE

# Reload systemd and enable service
print_info "Configuring systemd service..."
sudo systemctl daemon-reload
sudo systemctl enable $APP_NAME

print_status "Deployment completed successfully!"

echo ""
echo "📋 Next Steps:"
echo "1. Edit environment variables: sudo nano $APP_DIR/.env"
echo "2. Start the application: sudo systemctl start $APP_NAME"
echo "3. Check status: sudo systemctl status $APP_NAME"
echo "4. View logs: sudo journalctl -u $APP_NAME -f"
echo ""
echo "🌐 Application will be available at:"
echo "   http://your-server-ip:$APP_PORT"
echo "   https://yourdomain.com (if you configure reverse proxy)"
echo ""
echo "📁 Application files location: $APP_DIR"
echo "🔧 Service management:"
echo "   Start:   sudo systemctl start $APP_NAME"
echo "   Stop:    sudo systemctl stop $APP_NAME"
echo "   Restart: sudo systemctl restart $APP_NAME"
echo "   Status:  sudo systemctl status $APP_NAME"
echo ""
print_warning "Don't forget to configure your database and update the .env file!"
EOF
