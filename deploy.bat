@echo off
setlocal

echo EventHub Simple Deployment Script
echo =====================================

:: Step 1: Check Docker
echo.
echo Step 1: Checking Docker...
docker --version
if errorlevel 1 (
    echo Error: Docker version check failed
    exit /b 1
)
echo Docker version check completed successfully

docker info >nul 2>&1
if errorlevel 1 (
    echo Error: Docker daemon connection failed
    exit /b 1
)
echo Docker daemon connection completed successfully

:: Step 2: Stop existing containers
echo.
echo Step 2: Cleaning up existing containers...
docker compose down -v >nul 2>&1
echo Cleanup completed

:: Step 3: Build Docker image
echo.
echo Step 3: Building Docker image...
echo This may take a few minutes...
docker build -t eventhub:latest .
if errorlevel 1 (
    echo Error: Docker image build failed
    exit /b 1
)
echo Docker image build completed successfully

:: Step 4: Start services
echo.
echo Step 4: Starting services...
docker compose up -d
if errorlevel 1 (
    echo Error: Starting services with docker compose failed
    exit /b 1
)
echo Starting services with docker compose completed successfully

:: Step 5: Wait for services
echo.
echo Step 5: Waiting for services to start...
echo Checking service status...
timeout /t 15 /nobreak >nul

docker compose ps
echo.

:: Step 6: Check if web service is running
echo Step 6: Testing web service...
docker compose ps | findstr "web" | findstr "running" >nul
if errorlevel 1 (
    echo Web service may be starting up. Checking logs...
    docker compose logs web --tail=10
) else (
    echo Web service is running
)

:: Step 7: Check if database is running
echo.
echo Step 7: Testing database...
docker compose ps | findstr "mysql" | findstr "running" >nul
if errorlevel 1 (
    echo Database may be starting up. Checking logs...
    docker compose logs mysql --tail=10
) else (
    echo Database is running
)

echo.
echo Deployment script completed!
echo You can access the application at: http://localhost:8080