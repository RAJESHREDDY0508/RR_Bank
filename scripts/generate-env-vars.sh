# RR-Bank Environment Variables Generator
# Fill in your values and use this to copy-paste into Railway/Vercel

# ==========================================
# FILL IN THESE VALUES FIRST
# ==========================================

# Supabase
SUPABASE_HOST="aws-0-us-east-1.pooler.supabase.com"  # Change region as needed
SUPABASE_PROJECT_REF="your-project-ref"               # From Supabase dashboard
SUPABASE_PASSWORD="your-database-password"            # Your DB password

# Upstash Redis
UPSTASH_HOST="your-endpoint.upstash.io"
UPSTASH_PASSWORD="your-redis-password"

# JWT (generate a secure random string, minimum 32 chars)
JWT_SECRET="change-this-to-a-secure-random-string-at-least-32-characters"

# Your Vercel URL (update after Vercel deployment)
FRONTEND_URL="https://your-app.vercel.app"

# ==========================================
# GENERATED ENVIRONMENT VARIABLES
# ==========================================

echo "============================================"
echo "API GATEWAY Environment Variables"
echo "============================================"
cat << EOF
PORT=8080
SERVER_PORT=8080
JWT_SECRET=${JWT_SECRET}
CORS_ALLOWED_ORIGINS=${FRONTEND_URL},http://localhost:3000,http://localhost:5173
AUTH_SERVICE_URL=http://auth-service.railway.internal:8081
CUSTOMER_SERVICE_URL=http://customer-service.railway.internal:8082
ACCOUNT_SERVICE_URL=http://account-service.railway.internal:8083
TRANSACTION_SERVICE_URL=http://transaction-service.railway.internal:8084
LEDGER_SERVICE_URL=http://ledger-service.railway.internal:8085
NOTIFICATION_SERVICE_URL=http://notification-service.railway.internal:8086
FRAUD_SERVICE_URL=http://fraud-service.railway.internal:8087
AUDIT_SERVICE_URL=http://audit-service.railway.internal:8088
LOG_LEVEL=INFO
EOF

echo ""
echo "============================================"
echo "AUTH SERVICE Environment Variables"
echo "============================================"
cat << EOF
PORT=8081
SERVER_PORT=8081
SPRING_DATASOURCE_URL=jdbc:postgresql://${SUPABASE_HOST}:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.${SUPABASE_PROJECT_REF}
SPRING_DATASOURCE_PASSWORD=${SUPABASE_PASSWORD}
SPRING_REDIS_HOST=${UPSTASH_HOST}
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=${UPSTASH_PASSWORD}
SPRING_DATA_REDIS_SSL_ENABLED=true
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
LOG_LEVEL=INFO
SPRING_PROFILES_ACTIVE=production
EOF

echo ""
echo "============================================"
echo "CUSTOMER SERVICE Environment Variables"
echo "============================================"
cat << EOF
PORT=8082
SERVER_PORT=8082
SPRING_DATASOURCE_URL=jdbc:postgresql://${SUPABASE_HOST}:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.${SUPABASE_PROJECT_REF}
SPRING_DATASOURCE_PASSWORD=${SUPABASE_PASSWORD}
LOG_LEVEL=INFO
SPRING_PROFILES_ACTIVE=production
EOF

echo ""
echo "============================================"
echo "ACCOUNT SERVICE Environment Variables"
echo "============================================"
cat << EOF
PORT=8083
SERVER_PORT=8083
SPRING_DATASOURCE_URL=jdbc:postgresql://${SUPABASE_HOST}:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.${SUPABASE_PROJECT_REF}
SPRING_DATASOURCE_PASSWORD=${SUPABASE_PASSWORD}
SPRING_REDIS_HOST=${UPSTASH_HOST}
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=${UPSTASH_PASSWORD}
SPRING_DATA_REDIS_SSL_ENABLED=true
SERVICES_LEDGER_URL=http://ledger-service.railway.internal:8085
JWT_SECRET=${JWT_SECRET}
LOG_LEVEL=INFO
SPRING_PROFILES_ACTIVE=production
SPRING_FLYWAY_ENABLED=false
EOF

echo ""
echo "============================================"
echo "TRANSACTION SERVICE Environment Variables"
echo "============================================"
cat << EOF
PORT=8084
SERVER_PORT=8084
SPRING_DATASOURCE_URL=jdbc:postgresql://${SUPABASE_HOST}:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.${SUPABASE_PROJECT_REF}
SPRING_DATASOURCE_PASSWORD=${SUPABASE_PASSWORD}
SPRING_REDIS_HOST=${UPSTASH_HOST}
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=${UPSTASH_PASSWORD}
SPRING_DATA_REDIS_SSL_ENABLED=true
SERVICES_LEDGER_URL=http://ledger-service.railway.internal:8085
SERVICES_FRAUD_URL=http://fraud-service.railway.internal:8087
SERVICES_ACCOUNT_URL=http://account-service.railway.internal:8083
JWT_SECRET=${JWT_SECRET}
LOG_LEVEL=INFO
SPRING_PROFILES_ACTIVE=production
EOF

echo ""
echo "============================================"
echo "LEDGER SERVICE Environment Variables"
echo "============================================"
cat << EOF
PORT=8085
SERVER_PORT=8085
SPRING_DATASOURCE_URL=jdbc:postgresql://${SUPABASE_HOST}:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.${SUPABASE_PROJECT_REF}
SPRING_DATASOURCE_PASSWORD=${SUPABASE_PASSWORD}
SPRING_REDIS_HOST=${UPSTASH_HOST}
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=${UPSTASH_PASSWORD}
SPRING_DATA_REDIS_SSL_ENABLED=true
LOG_LEVEL=INFO
SPRING_PROFILES_ACTIVE=production
EOF

echo ""
echo "============================================"
echo "NOTIFICATION SERVICE Environment Variables"
echo "============================================"
cat << EOF
PORT=8086
SERVER_PORT=8086
SPRING_DATASOURCE_URL=jdbc:postgresql://${SUPABASE_HOST}:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.${SUPABASE_PROJECT_REF}
SPRING_DATASOURCE_PASSWORD=${SUPABASE_PASSWORD}
LOG_LEVEL=INFO
SPRING_PROFILES_ACTIVE=production
EOF

echo ""
echo "============================================"
echo "FRAUD SERVICE Environment Variables"
echo "============================================"
cat << EOF
PORT=8087
SERVER_PORT=8087
SPRING_DATASOURCE_URL=jdbc:postgresql://${SUPABASE_HOST}:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.${SUPABASE_PROJECT_REF}
SPRING_DATASOURCE_PASSWORD=${SUPABASE_PASSWORD}
SPRING_REDIS_HOST=${UPSTASH_HOST}
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=${UPSTASH_PASSWORD}
SPRING_DATA_REDIS_SSL_ENABLED=true
LOG_LEVEL=INFO
SPRING_PROFILES_ACTIVE=production
EOF

echo ""
echo "============================================"
echo "AUDIT SERVICE Environment Variables"
echo "============================================"
cat << EOF
PORT=8088
SERVER_PORT=8088
SPRING_DATASOURCE_URL=jdbc:postgresql://${SUPABASE_HOST}:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.${SUPABASE_PROJECT_REF}
SPRING_DATASOURCE_PASSWORD=${SUPABASE_PASSWORD}
LOG_LEVEL=INFO
SPRING_PROFILES_ACTIVE=production
EOF

echo ""
echo "============================================"
echo "VERCEL (Frontend) Environment Variables"
echo "============================================"
echo "VITE_API_URL=https://YOUR-RAILWAY-API-GATEWAY-URL/api"
