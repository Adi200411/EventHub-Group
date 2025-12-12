#!/bin/bash

echo "Running tests locally using Docker..."

# Build and run tests, capture exit code
docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
COMPOSE_EXIT_CODE=$?

# Clean up
echo "Cleaning up containers..."
docker-compose -f docker-compose.test.yml down -v

# Exit with the same code as docker-compose
if [ "$COMPOSE_EXIT_CODE" = "0" ]; then
    echo "✅ Tests passed!"
    exit 0
else
    echo "❌ Tests failed!"
    exit 1
fi