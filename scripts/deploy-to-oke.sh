#!/bin/bash

# ============================================================
# RR-Bank - Deploy to Oracle Kubernetes Engine (OKE)
# ============================================================

set -e

# Configuration
NAMESPACE=${KUBE_NAMESPACE:-rrbank}
OCI_REGION=${OCI_REGION:-iad}
OCI_NAMESPACE=${OCI_NAMESPACE:-your-namespace}

echo "============================================================"
echo "Deploying RR-Bank to OKE"
echo "Kubernetes Namespace: $NAMESPACE"
echo "OCI Region: $OCI_REGION"
echo "============================================================"

# Check kubectl connection
echo ""
echo "Checking Kubernetes connection..."
kubectl cluster-info

# Create namespace if not exists
echo ""
echo "Creating namespace $NAMESPACE..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Replace placeholders in manifests
echo ""
echo "Preparing Kubernetes manifests..."

# Create temp directory for processed manifests
TEMP_DIR=$(mktemp -d)
cp kubernetes/*.yaml $TEMP_DIR/

# Replace variables in manifests
for FILE in $TEMP_DIR/*.yaml; do
    sed -i "s/\${OCI_REGION}/$OCI_REGION/g" $FILE
    sed -i "s/\${OCI_NAMESPACE}/$OCI_NAMESPACE/g" $FILE
done

# Apply manifests in order
echo ""
echo "Applying Kubernetes manifests..."
echo "----------------------------------------"

# 1. Secrets and ConfigMaps
echo "1. Creating secrets and configmaps..."
kubectl apply -f $TEMP_DIR/00-namespace-secrets.yaml

# 2. Core services (no dependencies)
echo "2. Deploying core services..."
kubectl apply -f $TEMP_DIR/01-auth-service.yaml
kubectl apply -f $TEMP_DIR/02-ledger-service.yaml

# Wait for core services
echo "   Waiting for core services to be ready..."
kubectl wait --for=condition=available --timeout=120s deployment/auth-service -n $NAMESPACE || true
kubectl wait --for=condition=available --timeout=120s deployment/ledger-service -n $NAMESPACE || true

# 3. Other backend services
echo "3. Deploying other backend services..."
for FILE in $TEMP_DIR/0[3-8]-*.yaml; do
    if [ -f "$FILE" ]; then
        kubectl apply -f $FILE
    fi
done

# 4. Admin Service
echo "4. Deploying Admin Service..."
kubectl apply -f $TEMP_DIR/10-admin-service.yaml

# 5. API Gateway
echo "5. Deploying API Gateway..."
kubectl apply -f $TEMP_DIR/09-api-gateway.yaml

# 6. Frontends
echo "6. Deploying Frontends..."
kubectl apply -f $TEMP_DIR/11-customer-frontend.yaml
kubectl apply -f $TEMP_DIR/12-admin-frontend.yaml

# Cleanup temp directory
rm -rf $TEMP_DIR

# Wait for all deployments
echo ""
echo "Waiting for all deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment --all -n $NAMESPACE || true

# Get deployment status
echo ""
echo "============================================================"
echo "Deployment Status"
echo "============================================================"
kubectl get deployments -n $NAMESPACE
echo ""
kubectl get pods -n $NAMESPACE
echo ""
kubectl get services -n $NAMESPACE

# Get LoadBalancer IP
echo ""
echo "============================================================"
echo "External Access Points"
echo "============================================================"
echo "API Gateway:"
kubectl get service api-gateway -n $NAMESPACE -o jsonpath='  IP: {.status.loadBalancer.ingress[0].ip}'
echo ""

echo ""
echo "Ingress URLs:"
kubectl get ingress -n $NAMESPACE
echo ""

echo ""
echo "============================================================"
echo "Deployment Complete!"
echo "============================================================"
echo ""
echo "Services deployed:"
echo "  - 9 Backend Microservices"
echo "  - 1 API Gateway"
echo "  - 2 Frontend Applications (Customer + Admin)"
echo ""
echo "Next steps:"
echo "1. Update DNS records:"
echo "   - api.rrbank.example.com → API Gateway IP"
echo "   - rrbank.example.com → Customer Frontend Ingress"
echo "   - admin.rrbank.example.com → Admin Frontend Ingress"
echo ""
echo "2. Update CORS_ALLOWED_ORIGINS in ConfigMap with your domains"
echo ""
echo "3. Configure SSL/TLS certificates (Let's Encrypt via cert-manager)"
