#!/bin/bash

# EventHub Kubernetes Deployment Script for Unix/Linux/macOS
# Author: EventHub Team
# Description: Deploys the EventHub application to Kubernetes cluster

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    if ! command_exists kubectl; then
        print_error "kubectl is not installed. Please install kubectl first."
        echo "Visit: https://kubernetes.io/docs/tasks/tools/"
        exit 1
    fi
    
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker first."
        echo "Visit: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    # Check if kubectl can connect to cluster
    if ! kubectl cluster-info >/dev/null 2>&1; then
        print_error "Cannot connect to Kubernetes cluster. Please ensure:"
        echo "  - Minikube is running (minikube start)"
        echo "  - Or Docker Desktop Kubernetes is enabled"
        echo "  - Or you're connected to a cloud cluster"
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

# Function to detect Kubernetes environment
detect_k8s_env() {
    print_status "Detecting Kubernetes environment..."
    
    CONTEXT=$(kubectl config current-context)
    if [[ "$CONTEXT" == *"minikube"* ]]; then
        K8S_ENV="minikube"
        print_status "Detected Minikube environment"
    elif [[ "$CONTEXT" == *"docker-desktop"* ]]; then
        K8S_ENV="docker-desktop"
        print_status "Detected Docker Desktop environment"
    else
        K8S_ENV="other"
        print_status "Detected other Kubernetes environment: $CONTEXT"
    fi
}

# Function to build Docker image
build_image() {
    print_status "Building EventHub Docker image..."
    
    # Check if Maven wrapper exists and fix permissions if needed
    if [ -f "./mvnw" ]; then
        chmod +x ./mvnw
    fi
    
    # Build the application first
    print_status "Building application with Maven..."
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi
    
    # Build Docker image
    docker build -t eventhub-webapp:latest . || {
        print_error "Failed to build Docker image"
        exit 1
    }
    
    # Load image into minikube if needed
    if [ "$K8S_ENV" = "minikube" ]; then
        print_status "Loading image into Minikube..."
        minikube image load eventhub-webapp:latest || {
            print_warning "Failed to load image into Minikube. Continuing anyway..."
        }
    fi
    
    print_success "Docker image built successfully"
}

# Function to deploy to Kubernetes
deploy_to_k8s() {
    print_status "Deploying to Kubernetes..."
    
    # Apply manifests in order
    kubectl apply -f k8s/namespace.yaml
    kubectl apply -f k8s/secrets.yaml
    kubectl apply -f k8s/configmap.yaml
    kubectl apply -f k8s/mysql-deployment.yaml
    
    # Wait for MySQL to be ready
    print_status "Waiting for MySQL to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/mysql -n eventhub || {
        print_error "MySQL deployment failed or timed out"
        print_status "Checking MySQL logs..."
        kubectl logs deployment/mysql -n eventhub --tail=20
        exit 1
    }
    
    # Deploy web application
    kubectl apply -f k8s/webapp-deployment.yaml
    
    # Wait for web application to be ready
    print_status "Waiting for web application to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/webapp -n eventhub || {
        print_error "Web application deployment failed or timed out"
        print_status "Checking webapp logs..."
        kubectl logs deployment/webapp -n eventhub --tail=20
        exit 1
    }
    
    print_success "Deployment completed successfully"
}

# Function to show deployment status
show_status() {
    print_status "Deployment Status:"
    echo
    
    print_status "Pods:"
    kubectl get pods -n eventhub
    echo
    
    print_status "Services:"
    kubectl get services -n eventhub
    echo
    
    print_status "Deployments:"
    kubectl get deployments -n eventhub
    echo
}

# Function to get access information
get_access_info() {
    print_status "Getting access information..."
    
    if [ "$K8S_ENV" = "minikube" ]; then
        print_success "Access your application at:"
        minikube service webapp-service -n eventhub --url
    elif [ "$K8S_ENV" = "docker-desktop" ]; then
        print_success "Setting up port forwarding..."
        print_status "Run the following command in a separate terminal:"
        echo "kubectl port-forward service/webapp-service 8080:80 -n eventhub"
        echo "Then access: http://localhost:8080"
    else
        print_success "Get the external IP with:"
        echo "kubectl get service webapp-service -n eventhub"
    fi
}

# Function to cleanup deployment
cleanup() {
    print_warning "Cleaning up EventHub deployment..."
    kubectl delete namespace eventhub --ignore-not-found=true
    print_success "Cleanup completed"
}

# Function to show logs
show_logs() {
    echo "Choose which logs to view:"
    echo "1. Web Application logs"
    echo "2. MySQL logs"
    echo "3. All logs"
    read -p "Enter choice (1-3): " choice
    
    case $choice in
        1)
            print_status "Showing web application logs..."
            kubectl logs -f deployment/webapp -n eventhub
            ;;
        2)
            print_status "Showing MySQL logs..."
            kubectl logs -f deployment/mysql -n eventhub
            ;;
        3)
            print_status "Showing all logs..."
            kubectl logs -f deployment/webapp -n eventhub &
            kubectl logs -f deployment/mysql -n eventhub &
            wait
            ;;
        *)
            print_error "Invalid choice"
            ;;
    esac
}

# Function to show help
show_help() {
    echo "EventHub Kubernetes Deployment Script"
    echo
    echo "Usage: $0 [COMMAND]"
    echo
    echo "Commands:"
    echo "  deploy    Deploy the application (default)"
    echo "  status    Show deployment status"
    echo "  logs      Show application logs"
    echo "  cleanup   Remove the deployment"
    echo "  help      Show this help message"
    echo
    echo "Examples:"
    echo "  $0                 # Deploy the application"
    echo "  $0 deploy         # Deploy the application"
    echo "  $0 status         # Show deployment status"
    echo "  $0 logs           # Show logs"
    echo "  $0 cleanup        # Remove deployment"
}

# Main function
main() {
    case "${1:-deploy}" in
        "deploy")
            check_prerequisites
            detect_k8s_env
            build_image
            deploy_to_k8s
            show_status
            get_access_info
            ;;
        "status")
            show_status
            ;;
        "logs")
            show_logs
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"