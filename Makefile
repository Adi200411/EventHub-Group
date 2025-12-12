# Cross-platform Makefile for Windows and Unix systems
.PHONY: start db app stop clean test build up help

# Detect OS and set appropriate commands
ifeq ($(OS),Windows_NT)
    # Windows commands
    MVNW = mvnw.cmd
    SLEEP = timeout /t 10 /nobreak >nul
    SHELL = cmd
    RM_DIR = rmdir /s /q
    # For Windows, we use 'type' to pipe the schema file
    APPLY_SCHEMA = if exist schema.sql (type schema.sql | docker exec -i eventhub-mysql mysql -uroot -proot123 eventhub) else (echo "schema.sql not found, skipping.")
    CLEAN_MYSQL = if exist mysql_data ($(RM_DIR) mysql_data)
    CLEAN_TARGET = if exist target ($(RM_DIR) target)
else
    # Unix/Linux/macOS commands
    MVNW = ./mvnw
    SLEEP = sleep 10
    RM_DIR = rm -rf
    # For Unix, we use 'cat' to pipe the schema file
    APPLY_SCHEMA = if [ -f schema.sql ]; then cat schema.sql | docker exec -i eventhub-mysql mysql -uroot -proot123 eventhub; else echo "schema.sql not found, skipping."; fi
    CLEAN_MYSQL = if [ -d mysql_data ]; then $(RM_DIR) mysql_data; fi
    CLEAN_TARGET = if [ -d target ]; then $(RM_DIR) target; fi
endif

# Default target
all: help

help:
	@echo "Available targets:"
	@echo "  start  - Build and start the complete application (database + app)"
	@echo "  build  - Build the Maven project and start MySQL"
	@echo "  db     - Start MySQL database and apply schema"
	@echo "  app    - Start the Spring Boot application"
	@echo "  stop   - Stop all containers"
	@echo "  up     - Start containers without building"
	@echo "  clean  - Remove database volume (WARNING: deletes all data)"
	@echo "  test   - Run Maven tests"

start: build app

build:
	@echo "Building Maven project..."
	$(MVNW) clean package -DskipTests
	@echo "Start MySQL with Docker..."
	docker-compose up -d
	@echo "Waiting for MySQL to initialize..."
	@$(SLEEP)

db:
	@echo "Starting MySQL with Docker..."
	docker-compose up -d

	@echo "Waiting for MySQL to initialize..."
	@$(SLEEP)

	@echo "Applying schema.sql if it exists..."
	@$(APPLY_SCHEMA)

app:
	@echo "Starting Spring Boot..."
	$(MVNW) spring-boot:run

stop:
	@echo "Stopping containers..."
	docker-compose down

up:
	@echo "Starting containers..."
	docker-compose up -d

clean:
	@echo "Removing DB volume..."
	docker-compose down -v
	@$(CLEAN_MYSQL)
	@$(CLEAN_TARGET)


test:
	@echo "Running tests..."
	$(MVNW) test