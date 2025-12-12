#!/bin/bash

echo "EventHub Simple Deployment Script"
echo "====================================="

# Function to check last command status
check_status() {
    if [ $? -ne 0 ]; then
        echo "Error: $1 failed"
        exit 1
    else
        echo "$1 completed successfully"
    fi
}

# Step 1: Check Docker
echo ""
echo "Step 1: Checking Docker..."
docker --version
check_status "Docker version check"

docker info > /dev/null 2>&1
check_status "Docker daemon connection"

# Step 2: Stop existing containers
echo ""
echo "Step 2: Cleaning up existing containers..."
docker compose down -v 2>/dev/null || true
echo "Cleanup completed"

# Step 3: Build Docker image
echo ""
echo "Step 3: Building Docker image..."
echo "This may take a few minutes..."
docker build -t eventhub:latest .
check_status "Docker image build"

# Step 4: Start services
echo ""
echo "Step 4: Starting services..."
docker compose up -d
check_status "Starting services with docker compose"

# Step 5: Wait for services
echo ""
echo "Step 5: Waiting for services to start..."
echo "Checking service status..."
sleep 15  # Give services time to start

docker compose ps
echo ""

# Step 6: Check if web service is running
echo "📋 Step 6: Testing web service..."
if docker compose ps | grep -q "web.*running"; then
    echo "Web service is running"
else
    echo "Web service may be starting up. Checking logs..."
    docker compose logs web --tail=10
fi

# Step 7: Check if database is running
echo ""
echo "📋 Step 7: Testing database..."
if docker compose ps | grep -q "mysql.*running"; then
    echo "Database is running"
else
    echo "Database may be starting up. Checking logs..."
    docker compose logs mysql --tail=10
fi



