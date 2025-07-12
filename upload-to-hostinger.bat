@echo off
echo 🚀 Hostinger VPS Upload Script
echo ================================

REM Configuration - UPDATE THESE VALUES
set VPS_IP=your-vps-ip
set VPS_USER=root
set JAR_FILE=target\demo-0.0.1-SNAPSHOT.jar

echo.
echo 📋 Upload Configuration:
echo VPS IP: %VPS_IP%
echo User: %VPS_USER%
echo JAR File: %JAR_FILE%
echo.

REM Check if JAR file exists
if not exist "%JAR_FILE%" (
    echo ❌ JAR file not found: %JAR_FILE%
    echo Please run: mvnw clean package -DskipTests
    pause
    exit /b 1
)

echo ✅ JAR file found: %JAR_FILE%
echo.

REM Create temporary directory for upload
if not exist "temp-upload" mkdir temp-upload

REM Copy files to upload directory
echo 📦 Preparing files for upload...
copy "%JAR_FILE%" temp-upload\
copy "deploy-hostinger.sh" temp-upload\
copy "HOSTINGER_DEPLOYMENT_GUIDE.md" temp-upload\

echo.
echo 📤 Files ready for upload:
dir temp-upload

echo.
echo 🔧 Manual Upload Instructions:
echo ================================
echo.
echo 1. Use WinSCP, FileZilla, or SCP to upload files:
echo    - Connect to: %VPS_IP%
echo    - Username: %VPS_USER%
echo    - Upload temp-upload folder contents to: /opt/
echo.
echo 2. Or use SCP command (if you have it installed):
echo    scp -r temp-upload/* %VPS_USER%@%VPS_IP%:/opt/
echo.
echo 3. Then SSH into your VPS and run:
echo    ssh %VPS_USER%@%VPS_IP%
echo    cd /opt
echo    chmod +x deploy-hostinger.sh
echo    ./deploy-hostinger.sh
echo.

REM Option to open WinSCP if available
echo 🔧 Would you like to open WinSCP for upload? (y/n)
set /p choice=
if /i "%choice%"=="y" (
    echo Opening WinSCP...
    start "" "C:\Program Files (x86)\WinSCP\WinSCP.exe"
)

echo.
echo 📋 Next Steps After Upload:
echo 1. SSH into your VPS
echo 2. Run the deployment script
echo 3. Configure environment variables
echo 4. Start the application
echo.
echo 📖 See HOSTINGER_DEPLOYMENT_GUIDE.md for detailed instructions
echo.

pause
