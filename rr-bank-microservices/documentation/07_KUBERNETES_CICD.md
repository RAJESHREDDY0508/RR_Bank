# KUBERNETES DEPLOYMENT + CI/CD PIPELINES

## STEP 8: KUBERNETES MANIFESTS

### namespace.yml
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: rr-bank
  labels:
    name: rr-bank
```

### configmap.yml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rr-bank-config
  namespace: rr-bank
data:
  EUREKA_SERVER: "http://discovery-server:8761/eureka/"
  CONFIG_SERVER_URI: "http://config-server:8888"
  KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"
  REDIS_HOST: "redis"
  REDIS_PORT: "6379"
```

### secrets.yml
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: rr-bank-secrets
  namespace: rr-bank
type: Opaque
stringData:
  JWT_SECRET: "your-256-bit-secret-key-here"
  DB_PASSWORD: "your-db-password"
  CONFIG_SERVER_USERNAME: "configuser"
  CONFIG_SERVER_PASSWORD: "configpass"
```

### postgres-statefulset.yml
```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: rr-bank
spec:
  ports:
  - port: 5432
    name: postgres
  clusterIP: None
  selector:
    app: postgres
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: rr-bank
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
          name: postgres
        env:
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rr-bank-secrets
              key: DB_PASSWORD
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 10Gi
```

### kafka-deployment.yml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka
  namespace: rr-bank
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
      - name: kafka
        image: confluentinc/cp-kafka:7.5.0
        ports:
        - containerPort: 9092
        env:
        - name: KAFKA_ZOOKEEPER_CONNECT
          value: "zookeeper:2181"
        - name: KAFKA_ADVERTISED_LISTENERS
          value: "PLAINTEXT://kafka:9092"
        - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
          value: "1"
---
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: rr-bank
spec:
  ports:
  - port: 9092
    targetPort: 9092
  selector:
    app: kafka
```

### redis-deployment.yml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: rr-bank
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: rr-bank
spec:
  ports:
  - port: 6379
    targetPort: 6379
  selector:
    app: redis
```

### ingress.yml
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: rr-bank-ingress
  namespace: rr-bank
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.rrbank.com
    secretName: rrbank-tls
  rules:
  - host: api.rrbank.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-gateway
            port:
              number: 8080
```

### hpa-auth-service.yml
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service-hpa
  namespace: rr-bank
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Complete Deployment Script
```bash
#!/bin/bash

# deploy-all.sh
echo "Deploying RR-Bank Microservices to Kubernetes..."

# Create namespace
kubectl apply -f namespace.yml

# Create ConfigMaps and Secrets
kubectl apply -f configmap.yml
kubectl apply -f secrets.yml

# Deploy Infrastructure
echo "Deploying infrastructure..."
kubectl apply -f postgres-statefulset.yml
kubectl apply -f kafka-deployment.yml
kubectl apply -f redis-deployment.yml

# Wait for infrastructure
echo "Waiting for infrastructure to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n rr-bank --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n rr-bank --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n rr-bank --timeout=300s

# Deploy Config and Discovery
echo "Deploying config and discovery services..."
kubectl apply -f config-server-deployment.yml
kubectl apply -f discovery-server-deployment.yml

sleep 30

# Deploy Gateway
echo "Deploying API Gateway..."
kubectl apply -f api-gateway-deployment.yml

sleep 20

# Deploy Business Services
echo "Deploying business services..."
kubectl apply -f auth-service-deployment.yml
kubectl apply -f user-service-deployment.yml
kubectl apply -f account-service-deployment.yml
kubectl apply -f transaction-service-deployment.yml
kubectl apply -f payment-service-deployment.yml
kubectl apply -f notification-service-deployment.yml

# Apply HPA
echo "Applying Horizontal Pod Autoscalers..."
kubectl apply -f hpa/

# Apply Ingress
echo "Applying Ingress rules..."
kubectl apply -f ingress.yml

echo "Deployment complete!"
echo "Check status: kubectl get pods -n rr-bank"
```

---

## STEP 9: CI/CD PIPELINES

### GitHub Actions - .github/workflows/build-and-deploy.yml
```yaml
name: Build and Deploy RR-Bank Microservices

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_PREFIX: rrbank

jobs:
  build-shared-library:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      
      - name: Build Shared Library
        run: |
          cd shared-library
          mvn clean install -DskipTests
      
      - name: Upload Shared Library
        uses: actions/upload-artifact@v3
        with:
          name: shared-library
          path: shared-library/target/*.jar

  build-services:
    needs: build-shared-library
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [
          config-server,
          discovery-server,
          api-gateway,
          auth-service,
          user-service,
          account-service,
          transaction-service,
          payment-service,
          notification-service,
          audit-service,
          fraud-service,
          admin-service
        ]
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      
      - name: Download Shared Library
        uses: actions/download-artifact@v3
        with:
          name: shared-library
          path: ~/.m2/repository/com/rrbank/shared-library/1.0.0/
      
      - name: Build Service
        run: |
          cd ${{ matrix.service }}
          mvn clean package -DskipTests
      
      - name: Run Tests
        run: |
          cd ${{ matrix.service }}
          mvn test
      
      - name: Build Docker Image
        run: |
          cd ${{ matrix.service }}
          docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}:${{ github.sha }} .
          docker tag ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}:${{ github.sha }} \
                     ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}:latest
      
      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v2
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Push Docker Image
        run: |
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}:${{ github.sha }}
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}:latest

  deploy-dev:
    needs: build-services
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: development
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure kubectl
        uses: azure/k8s-set-context@v3
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBE_CONFIG_DEV }}
      
      - name: Deploy to Development
        run: |
          cd kubernetes
          ./deploy-all.sh
      
      - name: Verify Deployment
        run: |
          kubectl get pods -n rr-bank
          kubectl get services -n rr-bank

  deploy-staging:
    needs: build-services
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: staging
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure kubectl
        uses: azure/k8s-set-context@v3
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBE_CONFIG_STAGING }}
      
      - name: Deploy to Staging
        run: |
          cd kubernetes
          ./deploy-all.sh
      
      - name: Run Smoke Tests
        run: |
          chmod +x ./scripts/smoke-tests.sh
          ./scripts/smoke-tests.sh

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure kubectl
        uses: azure/k8s-set-context@v3
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBE_CONFIG_PROD }}
      
      - name: Deploy to Production
        run: |
          cd kubernetes
          ./deploy-all.sh
      
      - name: Run Health Checks
        run: |
          chmod +x ./scripts/health-check.sh
          ./scripts/health-check.sh
      
      - name: Notify Slack
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'RR-Bank Deployment to Production ${{ job.status }}'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Jenkinsfile
```groovy
pipeline {
    agent any
    
    environment {
        REGISTRY = 'ghcr.io'
        IMAGE_PREFIX = 'rrbank'
        MAVEN_HOME = tool 'Maven-3.9'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Shared Library') {
            steps {
                dir('shared-library') {
                    sh '${MAVEN_HOME}/bin/mvn clean install -DskipTests'
                }
            }
        }
        
        stage('Build Services') {
            parallel {
                stage('Auth Service') {
                    steps {
                        buildService('auth-service')
                    }
                }
                stage('User Service') {
                    steps {
                        buildService('user-service')
                    }
                }
                stage('Account Service') {
                    steps {
                        buildService('account-service')
                    }
                }
                stage('Transaction Service') {
                    steps {
                        buildService('transaction-service')
                    }
                }
                // Add other services...
            }
        }
        
        stage('Run Tests') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh '${MAVEN_HOME}/bin/mvn test'
                    }
                }
                stage('Integration Tests') {
                    steps {
                        sh '${MAVEN_HOME}/bin/mvn verify -P integration-tests'
                    }
                }
            }
        }
        
        stage('Build Docker Images') {
            steps {
                script {
                    def services = [
                        'auth-service', 'user-service', 'account-service',
                        'transaction-service', 'payment-service', 'notification-service'
                    ]
                    
                    services.each { service ->
                        dir(service) {
                            sh """
                                docker build -t ${REGISTRY}/${IMAGE_PREFIX}/${service}:${BUILD_NUMBER} .
                                docker tag ${REGISTRY}/${IMAGE_PREFIX}/${service}:${BUILD_NUMBER} \
                                           ${REGISTRY}/${IMAGE_PREFIX}/${service}:latest
                            """
                        }
                    }
                }
            }
        }
        
        stage('Push Images') {
            steps {
                script {
                    docker.withRegistry("https://${REGISTRY}", 'github-credentials') {
                        def services = [
                            'auth-service', 'user-service', 'account-service',
                            'transaction-service', 'payment-service', 'notification-service'
                        ]
                        
                        services.each { service ->
                            sh """
                                docker push ${REGISTRY}/${IMAGE_PREFIX}/${service}:${BUILD_NUMBER}
                                docker push ${REGISTRY}/${IMAGE_PREFIX}/${service}:latest
                            """
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Dev') {
            when {
                branch 'develop'
            }
            steps {
                sh '''
                    kubectl config use-context dev-cluster
                    cd kubernetes
                    ./deploy-all.sh
                '''
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to Production?', ok: 'Deploy'
                sh '''
                    kubectl config use-context prod-cluster
                    cd kubernetes
                    ./deploy-all.sh
                '''
            }
        }
    }
    
    post {
        success {
            slackSend(color: 'good', message: "Build Successful: ${env.JOB_NAME} ${env.BUILD_NUMBER}")
        }
        failure {
            slackSend(color: 'danger', message: "Build Failed: ${env.JOB_NAME} ${env.BUILD_NUMBER}")
        }
    }
}

def buildService(String service) {
    dir(service) {
        sh '${MAVEN_HOME}/bin/mvn clean package -DskipTests'
    }
}
```

### Rollback Script
```bash
#!/bin/bash

# rollback.sh - Rollback to previous version

SERVICE_NAME=$1
PREVIOUS_VERSION=$2

if [ -z "$SERVICE_NAME" ] || [ -z "$PREVIOUS_VERSION" ]; then
    echo "Usage: ./rollback.sh <service-name> <previous-version>"
    exit 1
fi

echo "Rolling back $SERVICE_NAME to version $PREVIOUS_VERSION..."

kubectl set image deployment/$SERVICE_NAME \
    $SERVICE_NAME=ghcr.io/rrbank/$SERVICE_NAME:$PREVIOUS_VERSION \
    -n rr-bank

kubectl rollout status deployment/$SERVICE_NAME -n rr-bank

echo "Rollback complete!"
```
