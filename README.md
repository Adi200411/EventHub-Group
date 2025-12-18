
# EventHub

# Group Information

## Members
* Aditya Gandi 
* Mohammad Azhar Khalid 
* Patrick Hannan 
* Diego Espinel Hernandez 
* Ishandeep Singh 
* Ben Hodder

# How to run the application

## Pre-requisites
* Java 17 or higher
* Maven
* Docker (for MySQL database)
* Node.js and npm (for JavaScript testing)

## Steps to run the application

### Login Credentials for Testing
* **Admin User**:
    * Username: `admin@eventhub.com`
    * Password: `admin`

* **Organiser User**:
    * Username: `organiser@rmit.edu.au`
    * Password: `password`

* **Student User**:
    * Username: `jane.smith@student.rmit.edu.au`
    * Password: `password`

    * Username: `john.doe@student.rmit.edu.au`
    * Password: `password`

### Step-by-Step Instructions

**Note** Windows users please use PowerShell for the commands below, especially for fixing line ending issues.

1. Clone the repository
    ```bash
    git clone https://github.com/cosc2299-2025/team-project-group-p05-01.git
    cd team-project-group-p05-01
    ```

2. **Fix line endings (if on Windows or encountering ^M errors):** 
    **Windows PowerShell:**
    ```powershell
    (Get-Content mvnw -Raw) -replace "`r`n", "`n" | Set-Content mvnw -NoNewline
    ```

3. Run the tests to ensure the setup is correct
    ```bash
    docker compose -f docker-compose.test.yml up --build
    ```
    **Note:** The first time you run this, it may take a while to download the necessary Docker images. Use control + C once the tests complete to stop the containers.

4. Stop the test database after tests complete
    ```bash
    docker compose -f docker-compose.test.yml down
    ```
5. Set up and run the MySQL database using Docker
    ```bash
    docker compose up mysql -d
    ```
6. Build the application
    ```bash
    ./mvnw clean install -DskipTests
    ```
7. Run the application
    ```bash
    ./mvnw spring-boot:run
    ```
8. Access the application at `http://localhost:8080`

## Testing for JavaScript

1. Install Jest
    ``` bash
    npm install --save-dev jest-environment-jsdom
    ```

2. Run Jest
    ```bash
    npx jest
    ```

## Deployment Options

### Using Automated Scripts

The project includes deployment scripts for different platforms:

- **`deploy.sh`** - Unix/Linux/macOS deployment script
- **`deploy.bat`** - Windows Command Prompt script  
- **`deploy.ps1`** - Windows PowerShell script (recommended for Windows)

These scripts will:
1. Start the MySQL database in Docker
2. Wait for the database to be ready
3. Build the application with Maven
4. Run the application

### Deployment Scenarios

**Full deployment (database + application):**
```bash
# Unix/Linux/macOS
./deploy.sh

# Windows PowerShell
.\deploy.ps1

# Windows Command Prompt
deploy.bat
```

**Database only (for development):**
```bash
docker compose up -d mysql
docker compose stop web
```

**Application only (when database is already running):**
```bash
./mvnw spring-boot:run
```

### Stopping the Application

**Stop everything:**
```bash
# Stop all containers
docker compose down

# Stop application if running locally
# Use Ctrl+C in the terminal where it's running
```

**Keep database running, stop web application only:**
```bash
docker compose stop web
```

## Kubernetes Deployment

The project includes comprehensive Kubernetes deployment scripts for production-ready container orchestration. This deployment provides high availability, scalability, and proper resource management.

### Prerequisites for Kubernetes Deployment

- **Kubernetes cluster**  Docker Desktop with Kubernetes enabled, **It will not work without kubernetes enabled on docker**.
- **kubectl** installed and configured to connect to your cluster
- **Docker** installed and running

### Kubernetes Deployment Scripts

The project provides cross-platform Kubernetes deployment scripts:

- **`k8s/deploy.sh`** - Unix/Linux/macOS Kubernetes deployment script
- **`k8s/deploy.ps1`** - Windows PowerShell script

### Quick Kubernetes Deployment

**Unix/Linux/macOS:**
```bash
./k8s/deploy.sh
```

**Windows PowerShell:**
```powershell
.\k8s\deploy.ps1
```

**NOTE** At the end of the deployment, the script will provide instructions on how to access the application.

### Kubernetes Deployment Features

The Kubernetes deployment includes:

- **Isolated namespace** (`eventhub`) for all resources
- **MySQL database** with persistent storage and health checks
- **Spring Boot application** with multiple replicas for high availability
- **Init containers** ensuring proper startup order
- **Resource limits** preventing resource exhaustion
- **Health checks** for automatic restart of failed containers
- **Secrets management** for secure credential storage

### Available Kubernetes Commands

All Kubernetes deployment scripts support these commands:

```bash
# Deploy the application (default)
./k8s/deploy.sh
./k8s/deploy.sh deploy

# Show deployment status and pod information
./k8s/deploy.sh status

# View application logs (interactive selection)
./k8s/deploy.sh logs

# Clean up entire deployment
./k8s/deploy.sh cleanup

# Show help and usage information
./k8s/deploy.sh help
```

### Accessing Your Kubernetes Application

After successful deployment, the script will provide access information:

**For Docker Desktop Kubernetes:**
```bash
# Run this in a separate terminal
kubectl port-forward service/webapp-service 8080:80 -n eventhub
# Then access: http://localhost:8080
```


### Kubernetes Deployment Architecture

The deployment creates:

1. **Namespace**: `eventhub` - Isolated environment for all resources
2. **MySQL StatefulSet**: Database with 1Gi persistent volume and health probes
3. **Web Application Deployment**: Spring Boot app with 2 replicas and resource limits
4. **Services**: Internal MySQL service and external webapp LoadBalancer service
5. **Secrets**: Base64 encoded database credentials
6. **ConfigMap**: Application configuration and environment variables

### Monitoring Your Kubernetes Deployment

Check deployment status:
```bash
# View all resources in the eventhub namespace
kubectl get all -n eventhub

# Check pod logs
kubectl logs deployment/webapp -n eventhub
kubectl logs deployment/mysql -n eventhub

# Describe deployments for detailed information
kubectl describe deployment webapp -n eventhub
```

### Scaling Your Kubernetes Application

Scale the web application:
```bash
# Scale to 3 replicas
kubectl scale deployment webapp --replicas=3 -n eventhub

# Check scaling status
kubectl get deployment webapp -n eventhub
```

### Troubleshooting Kubernetes Deployment

**Common Issues:**

1. **Pod Startup Issues**:
   ```bash
   kubectl logs deployment/webapp -n eventhub --tail=50
   kubectl describe pod <pod-name> -n eventhub
   ```

2. **Database Connection Issues**:
   ```bash
   kubectl logs deployment/mysql -n eventhub
   kubectl get events -n eventhub --sort-by=.metadata.creationTimestamp
   ```

3. **Port Conflicts** (if 8080 is in use):
   ```bash
   kubectl port-forward service/webapp-service 8081:80 -n eventhub
   ```

For detailed Kubernetes deployment information, see the [Kubernetes README](k8s/README.md).

See [Instructions](INSTRUCTIONS.md)

