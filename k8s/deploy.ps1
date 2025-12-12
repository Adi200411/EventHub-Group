# EventHub Kubernetes Deployment Script for Windows (PowerShell)
# Author: EventHub Team
# Description: Deploys the EventHub application to Kubernetes cluster

param(
    [Parameter(Position=0)]
    [ValidateSet('deploy', 'status', 'logs', 'cleanup', 'help')]
    [string]$Command = 'deploy'
)

# Function to write colored output
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = 'White'
    )
    Write-Host $Message -ForegroundColor $Color
}

function Write-Status {
    param([string]$Message)
    Write-ColorOutput "[INFO] $Message" -Color Cyan
}

function Write-Success {
    param([string]$Message)
    Write-ColorOutput "[SUCCESS] $Message" -Color Green
}

function Write-Warning {
    param([string]$Message)
    Write-ColorOutput "[WARNING] $Message" -Color Yellow
}

function Write-ErrorMsg {
    param([string]$Message)
    Write-ColorOutput "[ERROR] $Message" -Color Red
}

# Function to check if command exists
function Test-Command {
    param([string]$CommandName)
    return Get-Command $CommandName -ErrorAction SilentlyContinue
}

# Function to check prerequisites
function Test-Prerequisites {
    Write-Status "Checking prerequisites..."
    
    if (-not (Test-Command kubectl)) {
        Write-ErrorMsg "kubectl is not installed. Please install kubectl first."
        Write-Host "Visit: https://kubernetes.io/docs/tasks/tools/"
        exit 1
    }
    
    if (-not (Test-Command docker)) {
        Write-ErrorMsg "Docker is not installed. Please install Docker first."
        Write-Host "Visit: https://docs.docker.com/get-docker/"
        exit 1
    }
    
    # Check if kubectl can connect to cluster
    try {
        kubectl cluster-info 2>$null | Out-Null
    }
    catch {
        Write-ErrorMsg "Cannot connect to Kubernetes cluster. Please ensure:"
        Write-Host "  - Minikube is running (minikube start)"
        Write-Host "  - Or Docker Desktop Kubernetes is enabled"
        Write-Host "  - Or you're connected to a cloud cluster"
        exit 1
    }
    
    Write-Success "Prerequisites check passed"
}

# Function to detect Kubernetes environment
function Get-K8sEnvironment {
    Write-Status "Detecting Kubernetes environment..."
    
    $context = kubectl config current-context 2>$null
    if ($context -like "*minikube*") {
        $global:K8sEnv = "minikube"
        Write-Status "Detected Minikube environment"
    }
    elseif ($context -like "*docker-desktop*") {
        $global:K8sEnv = "docker-desktop"
        Write-Status "Detected Docker Desktop environment"
    }
    else {
        $global:K8sEnv = "other"
        Write-Status "Detected other Kubernetes environment: $context"
    }
}

# Function to build Docker image
function Build-Image {
    Write-Status "Building EventHub Docker image..."
    
    # Build the application first
    Write-Status "Building application with Maven..."
    if (Test-Path ".\mvnw.cmd") {
        .\mvnw.cmd clean package -DskipTests
    }
    elseif (Test-Path ".\mvnw") {
        # Set executable permission if on WSL or Git Bash
        if (Get-Command bash -ErrorAction SilentlyContinue) {
            bash -c "chmod +x ./mvnw && ./mvnw clean package -DskipTests"
        }
        else {
            Write-ErrorMsg "Maven wrapper found but cannot execute. Please install Maven or use WSL."
            exit 1
        }
    }
    else {
        mvn clean package -DskipTests
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "Failed to build application"
        exit 1
    }
    
    # Build Docker image
    Write-Status "Building Docker image..."
    docker build -t eventhub-webapp:latest .
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "Failed to build Docker image"
        exit 1
    }
    
    # Load image into minikube if needed
    if ($global:K8sEnv -eq "minikube") {
        Write-Status "Loading image into Minikube..."
        minikube image load eventhub-webapp:latest
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Failed to load image into Minikube. Continuing anyway..."
        }
    }
    
    Write-Success "Docker image built successfully"
}

# Function to deploy to Kubernetes
function Deploy-ToK8s {
    Write-Status "Deploying to Kubernetes..."
    
    # Apply manifests in order
    kubectl apply -f k8s/namespace.yaml
    kubectl apply -f k8s/secrets.yaml
    kubectl apply -f k8s/configmap.yaml
    kubectl apply -f k8s/mysql-deployment.yaml
    
    # Wait for MySQL to be ready
    Write-Status "Waiting for MySQL to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/mysql -n eventhub
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "MySQL deployment failed or timed out"
        Write-Status "Checking MySQL logs..."
        kubectl logs deployment/mysql -n eventhub --tail=20
        exit 1
    }
    
    # Deploy web application
    kubectl apply -f k8s/webapp-deployment.yaml
    
    # Wait for web application to be ready
    Write-Status "Waiting for web application to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/webapp -n eventhub
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "Web application deployment failed or timed out"
        Write-Status "Checking webapp logs..."
        kubectl logs deployment/webapp -n eventhub --tail=20
        exit 1
    }
    
    Write-Success "Deployment completed successfully"
}

# Function to show deployment status
function Show-Status {
    Write-Status "Deployment Status:"
    Write-Host
    
    Write-Status "Pods:"
    kubectl get pods -n eventhub
    Write-Host
    
    Write-Status "Services:"
    kubectl get services -n eventhub
    Write-Host
    
    Write-Status "Deployments:"
    kubectl get deployments -n eventhub
    Write-Host
}

# Function to get access information
function Get-AccessInfo {
    Write-Status "Getting access information..."
    
    if ($global:K8sEnv -eq "minikube") {
        Write-Success "Access your application at:"
        minikube service webapp-service -n eventhub --url
    }
    elseif ($global:K8sEnv -eq "docker-desktop") {
        Write-Success "Setting up port forwarding..."
        Write-Status "Run the following command in a separate terminal:"
        Write-Host "kubectl port-forward service/webapp-service 8080:80 -n eventhub"
        Write-Host "Then access: http://localhost:8080"
    }
    else {
        Write-Success "Get the external IP with:"
        Write-Host "kubectl get service webapp-service -n eventhub"
    }
}

# Function to cleanup deployment
function Remove-Deployment {
    Write-Warning "Cleaning up EventHub deployment..."
    kubectl delete namespace eventhub --ignore-not-found=true
    Write-Success "Cleanup completed"
}

# Function to show logs
function Show-Logs {
    Write-Host "Choose which logs to view:"
    Write-Host "1. Web Application logs"
    Write-Host "2. MySQL logs"
    Write-Host "3. All logs"
    $choice = Read-Host "Enter choice (1-3)"
    
    switch ($choice) {
        "1" {
            Write-Status "Showing web application logs..."
            kubectl logs -f deployment/webapp -n eventhub
        }
        "2" {
            Write-Status "Showing MySQL logs..."
            kubectl logs -f deployment/mysql -n eventhub
        }
        "3" {
            Write-Status "Showing all logs..."
            Start-Job -ScriptBlock { kubectl logs -f deployment/webapp -n eventhub }
            Start-Job -ScriptBlock { kubectl logs -f deployment/mysql -n eventhub }
            Write-Host "Press Ctrl+C to stop viewing logs"
            Get-Job | Wait-Job
        }
        default {
            Write-ErrorMsg "Invalid choice"
        }
    }
}

# Function to show help
function Show-Help {
    Write-Host "EventHub Kubernetes Deployment Script"
    Write-Host ""
    Write-Host "Usage: .\deploy.ps1 [COMMAND]"
    Write-Host ""
    Write-Host "Commands:"
    Write-Host "  deploy    Deploy the application (default)"
    Write-Host "  status    Show deployment status"
    Write-Host "  logs      Show application logs"
    Write-Host "  cleanup   Remove the deployment"
    Write-Host "  help      Show this help message"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\deploy.ps1                # Deploy the application"
    Write-Host "  .\deploy.ps1 deploy        # Deploy the application"
    Write-Host "  .\deploy.ps1 status        # Show deployment status"
    Write-Host "  .\deploy.ps1 logs          # Show logs"
    Write-Host "  .\deploy.ps1 cleanup       # Remove deployment"
}

# Main execution
switch ($Command) {
    "deploy" {
        Test-Prerequisites
        Get-K8sEnvironment
        Build-Image
        Deploy-ToK8s
        Show-Status
        Get-AccessInfo
    }
    "status" {
        Show-Status
    }
    "logs" {
        Show-Logs
    }
    "cleanup" {
        Remove-Deployment
    }
    "help" {
        Show-Help
    }
    default {
        Write-ErrorMsg "Unknown command: $Command"
        Show-Help
        exit 1
    }
}