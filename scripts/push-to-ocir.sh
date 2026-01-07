#!/bin/bash

# ============================================================
# RR-Bank - Push Images to Oracle Container Registry (OCIR)
# ============================================================

set -e

# Configuration
REGION=${OCI_REGION:-iad}
NAMESPACE=${OCI_NAMESPACE:-your-namespace}
REGISTRY=${REGION}.ocir.io/${NAMESPACE}
TAG=${TAG:-latest}

echo "============================================================"
echo "Pushing RR-Bank Images to OCIR"
echo "Registry: $REGISTRY"
echo "Tag: $TAG"
echo "============================================================"

# Login to OCIR
echo ""
echo "Logging in to OCIR..."
echo "Make sure you have set up:"
echo "  - OCI_USERNAME: <tenancy-namespace>/<username>"
echo "  - OCI_AUTH_TOKEN: Your auth token"
echo ""

if [ -z "$OCI_USERNAME" ] || [ -z "$OCI_AUTH_TOKEN" ]; then
    echo "Please set OCI_USERNAME and OCI_AUTH_TOKEN environment variables"
    echo "Example:"
    echo "  export OCI_USERNAME='mytenancy/oracleidentitycloudservice/user@email.com'"
    echo "  export OCI_AUTH_TOKEN='your-auth-token'"
    exit 1
fi

echo $OCI_AUTH_TOKEN | docker login ${REGION}.ocir.io -u "$OCI_USERNAME" --password-stdin

# Images to push
IMAGES=(
    "api-gateway"
    "auth-service"
    "customer-service"
    "account-service"
    "transaction-service"
    "ledger-service"
    "fraud-service"
    "notification-service"
    "audit-service"
    "frontend"
)

# Push each image
for IMAGE in "${IMAGES[@]}"; do
    echo ""
    echo "Pushing $IMAGE..."
    echo "----------------------------------------"
    
    docker push $REGISTRY/rrbank/$IMAGE:$TAG
    echo "✓ Pushed $IMAGE successfully"
done

echo ""
echo "============================================================"
echo "All images pushed to OCIR successfully!"
echo "============================================================"
echo ""
echo "Images are available at:"
for IMAGE in "${IMAGES[@]}"; do
    echo "  $REGISTRY/rrbank/$IMAGE:$TAG"
done
