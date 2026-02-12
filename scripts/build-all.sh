#!/bin/bash

# ============================================================
# RR-Bank - Build All Docker Images Script
# ============================================================

set -e

# Configuration
REGISTRY=${OCI_REGION:-iad}.ocir.io/${OCI_NAMESPACE:-your-namespace}
TAG=${TAG:-latest}

echo "============================================================"
echo "Building RR-Bank Docker Images"
echo "Registry: $REGISTRY"
echo "Tag: $TAG"
echo "============================================================"

# Services to build (path:name format)
SERVICES=(
    "api-gateway"
    "services/auth-service:auth-service"
    "services/customer-service:customer-service"
    "services/account-service:account-service"
    "services/transaction-service:transaction-service"
    "services/ledger-service:ledger-service"
    "services/fraud-service:fraud-service"
    "services/notification-service:notification-service"
    "services/audit-service:audit-service"
    "services/admin-service:admin-service"
    "frontend"
    "frontend/admin-app:admin-frontend"
)

# Build each service
for SERVICE in "${SERVICES[@]}"; do
    IFS=':' read -r PATH NAME <<< "$SERVICE"
    NAME=${NAME:-$(basename $PATH)}
    
    echo ""
    echo "Building $NAME..."
    echo "----------------------------------------"
    
    if [ -f "$PATH/Dockerfile" ]; then
        docker build -t $REGISTRY/rrbank/$NAME:$TAG $PATH
        echo "✓ Built $NAME successfully"
    else
        echo "⚠ Dockerfile not found for $NAME at $PATH/Dockerfile"
    fi
done

echo ""
echo "============================================================"
echo "All images built successfully!"
echo "============================================================"
echo ""
echo "Images created:"
for SERVICE in "${SERVICES[@]}"; do
    IFS=':' read -r PATH NAME <<< "$SERVICE"
    NAME=${NAME:-$(basename $PATH)}
    echo "  - $REGISTRY/rrbank/$NAME:$TAG"
done
echo ""
echo "To push images to OCIR, run:"
echo "  ./scripts/push-to-ocir.sh"
