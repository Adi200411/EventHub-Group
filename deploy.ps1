# EventHub Simple Deployment Script - PowerShell Version
# Usage: ./deploy.ps1

Write-Host "EventHub Simple Deployment Script" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green

function Test-CommandSuccess {
    param($Description)
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: $Description failed" -ForegroundColor Red
        exit 1
    } else {
        Write-Host "$Description completed successfully" -ForegroundColor Green
    }
}

# Step 1: Check Docker
Write-Host ""
Write-Host "Step 1: Checking Docker..." -ForegroundColor Yellow
docker --version
Test-CommandSuccess "Docker version check"

docker info | Out-Null
Test-CommandSuccess "Docker daemon connection"

# Step 2: Stop existing containers
Write-Host ""
Write-Host "Step 2: Cleaning up existing containers..." -ForegroundColor Yellow
try {
    docker compose down -v 2>$null
} catch {
    # Ignore errors - container may not exist
}
Write-Host "Cleanup completed" -ForegroundColor Green

# Step 3: Build Docker image
Write-Host ""
Write-Host "Step 3: Building Docker image..." -ForegroundColor Yellow
Write-Host "This may take a few minutes..."
docker build -t eventhub:latest .
Test-CommandSuccess "Docker image build"

# Step 4: Start services
Write-Host ""
Write-Host "Step 4: Starting services..." -ForegroundColor Yellow
docker compose up -d
Test-CommandSuccess "Starting services with docker compose"

# Step 5: Wait for services
Write-Host ""
Write-Host "Step 5: Waiting for services to start..." -ForegroundColor Yellow
Write-Host "Checking service status..."
Start-Sleep -Seconds 15

docker compose ps
Write-Host ""

# Step 6: Check if web service is running
Write-Host "📋 Step 6: Testing web service..." -ForegroundColor Yellow
$webStatus = docker compose ps | Select-String "web.*running"
if ($webStatus) {
    Write-Host "Web service is running" -ForegroundColor Green
} else {
    Write-Host "Web service may be starting up. Checking logs..." -ForegroundColor Yellow
    docker compose logs web --tail=10
}

# Step 7: Check if database is running
Write-Host ""
Write-Host "📋 Step 7: Testing database..." -ForegroundColor Yellow
$dbStatus = docker compose ps | Select-String "mysql.*running"
if ($dbStatus) {
    Write-Host "Database is running" -ForegroundColor Green
} else {
    Write-Host "Database may be starting up. Checking logs..." -ForegroundColor Yellow
    docker compose logs mysql --tail=10
}

Write-Host ""
Write-Host "Deployment script completed!" -ForegroundColor Green
Write-Host "You can access the application at: http://localhost:8080" -ForegroundColor Cyan