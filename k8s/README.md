# Kubernetes Deployment Guide

This directory contains Kubernetes manifests and deployment scripts for the EventHub application. The deployment includes a MySQL database and the Spring Boot web application with proper health checks, resource limits, and cross-platform automation.

## Quick Start

### Prerequisites
- Kubernetes cluster Docker Desktop
- Docker installed and running
- kubectl configured to connect to your cluster

### Deploy with One Command

**Unix/Linux/macOS:**
```bash
./k8s/deploy.sh
```

**Windows PowerShell:**
```powershell
.\k8s\deploy.ps1
```

## Architecture Overview

The deployment consists of:

- **Namespace**: `eventhub` - Isolated environment for all resources
- **MySQL Database**: Persistent storage with health checks and resource limits
- **Web Application**: Spring Boot app with init containers and rolling updates
- **Services**: Internal MySQL service and external webapp service
- **Secrets**: Secure storage for database credentials
- **ConfigMap**: Application configuration and environment variables

## Deployment Scripts

### Cross-Platform Support

All deployment scripts provide the same functionality across different platforms:

1. **deploy.sh** (Unix/Linux/macOS)
   - Bash script with full error handling
   - Colored output for better readability
   - Automatic environment detection

2. **deploy.ps1** (Windows PowerShell)
   - PowerShell script with parameter validation
   - Cross-platform Maven wrapper support
   - Rich console output


### Available Commands

All scripts support these commands:

```bash
# Deploy the application (default)
./deploy.sh
./deploy.sh deploy

# Show deployment status
./deploy.sh status

# View application logs
./deploy.sh logs

# Clean up deployment
./deploy.sh cleanup

# Show help
./deploy.sh help
```

## Manual Deployment

If you prefer to deploy manually or need to customize the deployment:

### 1. Apply Kubernetes Manifests

```bash
# Create namespace and basic resources
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml

# Deploy MySQL database
kubectl apply -f k8s/mysql-deployment.yaml

# Wait for MySQL to be ready
kubectl wait --for=condition=available --timeout=300s deployment/mysql -n eventhub

# Deploy web application
kubectl apply -f k8s/webapp-deployment.yaml

# Wait for webapp to be ready
kubectl wait --for=condition=available --timeout=300s deployment/webapp -n eventhub
```

### 2. Access the Application

**For Docker Desktop:**
```bash
kubectl port-forward service/webapp-service 8080:80 -n eventhub
# Then access http://localhost:8080
```


## Configuration Details

### Database Configuration
- **Image**: mysql:8.0
- **Storage**: 1Gi persistent volume
- **Resources**: 256Mi memory, 250m CPU
- **Health Checks**: Startup, readiness, and liveness probes
- **Port**: 3306 (internal only)

### Web Application Configuration
- **Image**: eventhub-webapp:latest (built from Dockerfile)
- **Resources**: 512Mi memory, 500m CPU
- **Health Checks**: HTTP health endpoints
- **Port**: 8080 (exposed via service)
- **Init Container**: Waits for MySQL availability

### Security
- Database credentials stored in Kubernetes secrets (base64 encoded)
- No root privileges required for containers
- Resource limits prevent resource exhaustion
- Network policies can be added for additional isolation

## Troubleshooting

### Common Issues

**1. Image Pull Errors**
```bash
# For Minikube, ensure image is loaded
minikube image load eventhub-webapp:latest

# For other environments, push to registry
docker tag eventhub-webapp:latest your-registry/eventhub-webapp:latest
docker push your-registry/eventhub-webapp:latest
```

**2. MySQL Connection Issues**
```bash
# Check MySQL logs
kubectl logs deployment/mysql -n eventhub

# Verify MySQL is ready
kubectl get pods -n eventhub
kubectl describe pod <mysql-pod-name> -n eventhub
```

**3. Application Startup Issues**
```bash
# Check webapp logs
kubectl logs deployment/webapp -n eventhub

# Check events
kubectl get events -n eventhub --sort-by=.metadata.creationTimestamp
```

### Debugging Commands

```bash
# Get all resources
kubectl get all -n eventhub

# Describe deployments
kubectl describe deployment mysql -n eventhub
kubectl describe deployment webapp -n eventhub

# Check persistent volumes
kubectl get pv,pvc -n eventhub

# View detailed pod information
kubectl get pods -n eventhub -o wide
```

### Cleanup

To remove the entire deployment:

```bash
# Using script
./deploy.sh cleanup

# Manual cleanup
kubectl delete namespace eventhub
```

## Customization

### Resource Limits
Edit the deployment YAML files to adjust resource requests and limits:

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

### Environment Variables
Modify `k8s/configmap.yaml` to add or change application configuration:

```yaml
data:
  SPRING_PROFILES_ACTIVE: "prod"
  LOGGING_LEVEL_ROOT: "INFO"
  # Add your custom variables here
```

### Database Configuration
To use an external database, update the configmap with your database connection details and remove the MySQL deployment.

## Production Considerations

### High Availability
- Increase replica count for the web application
- Use ReadWriteMany storage for shared data
- Configure pod anti-affinity rules
- Set up horizontal pod autoscaling

### Security
- Enable network policies
- Use service meshes (Istio, Linkerd)
- Implement proper RBAC
- Regular security scanning of images

### Monitoring
- Deploy Prometheus and Grafana
- Configure application metrics endpoints
- Set up log aggregation (ELK stack)
- Create alerting rules

### Backup
- Regular database backups using CronJobs
- Persistent volume snapshots
- Configuration backup strategies

## Support

For issues related to:
- **Kubernetes**: Check kubectl and cluster connectivity
- **Docker**: Verify Docker daemon and image building
- **Application**: Review Spring Boot logs and configuration
- **Database**: Check MySQL logs and connection parameters

The deployment scripts include comprehensive error handling and logging to help diagnose issues quickly.