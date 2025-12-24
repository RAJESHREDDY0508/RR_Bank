#!/bin/bash

# Actuator Security Verification Script
# Tests that actuator endpoints are properly secured

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "🔒 Actuator Security Test Suite"
echo "========================================="
echo ""

# Test 1: Public Health Check (Should Pass)
echo "Test 1: Public health check..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/health)
if [ "$RESPONSE" == "200" ]; then
    echo -e "${GREEN}✅ PASS${NC} - Public health check accessible"
else
    echo -e "${RED}❌ FAIL${NC} - Expected 200, got $RESPONSE"
fi
echo ""

# Test 2: Public Info (Should Pass)
echo "Test 2: Public info endpoint..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/info)
if [ "$RESPONSE" == "200" ]; then
    echo -e "${GREEN}✅ PASS${NC} - Public info accessible"
else
    echo -e "${RED}❌ FAIL${NC} - Expected 200, got $RESPONSE"
fi
echo ""

# Test 3: Environment Variables (Should Be Blocked)
echo "Test 3: Environment variables (should be blocked)..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/env)
if [ "$RESPONSE" == "403" ] || [ "$RESPONSE" == "404" ]; then
    echo -e "${GREEN}✅ PASS${NC} - Environment variables blocked (got $RESPONSE)"
else
    echo -e "${RED}❌ FAIL${NC} - SECURITY RISK! Environment exposed (got $RESPONSE)"
fi
echo ""

# Test 4: Beans Configuration (Should Be Blocked)
echo "Test 4: Beans configuration (should be blocked)..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/beans)
if [ "$RESPONSE" == "403" ] || [ "$RESPONSE" == "404" ]; then
    echo -e "${GREEN}✅ PASS${NC} - Beans configuration blocked (got $RESPONSE)"
else
    echo -e "${RED}❌ FAIL${NC} - SECURITY RISK! Beans exposed (got $RESPONSE)"
fi
echo ""

# Test 5: Config Properties (Should Be Blocked)
echo "Test 5: Config properties (should be blocked)..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/configprops)
if [ "$RESPONSE" == "403" ] || [ "$RESPONSE" == "404" ]; then
    echo -e "${GREEN}✅ PASS${NC} - Config properties blocked (got $RESPONSE)"
else
    echo -e "${RED}❌ FAIL${NC} - SECURITY RISK! Config properties exposed (got $RESPONSE)"
fi
echo ""

# Test 6: Mappings (Should Be Blocked)
echo "Test 6: Route mappings (should be blocked)..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/mappings)
if [ "$RESPONSE" == "403" ] || [ "$RESPONSE" == "404" ]; then
    echo -e "${GREEN}✅ PASS${NC} - Route mappings blocked (got $RESPONSE)"
else
    echo -e "${RED}❌ FAIL${NC} - SECURITY RISK! Route mappings exposed (got $RESPONSE)"
fi
echo ""

# Test 7: Detailed Health Without Auth (Should Be Blocked)
echo "Test 7: Detailed health without auth (should show minimal info)..."
RESPONSE=$(curl -s $BASE_URL/actuator/health)
if echo "$RESPONSE" | grep -q '"components"'; then
    echo -e "${RED}❌ FAIL${NC} - SECURITY RISK! Detailed health info exposed"
    echo "Response: $RESPONSE"
else
    echo -e "${GREEN}✅ PASS${NC} - Only basic health status shown"
fi
echo ""

# Test 8: Admin Access (Optional - requires credentials)
echo "Test 8: Admin access to detailed health..."
echo -e "${YELLOW}ℹ️  Skipping - requires admin credentials${NC}"
echo "   To test manually:"
echo "   1. Login: curl -X POST $BASE_URL/api/auth/login -H 'Content-Type: application/json' -d '{\"username\":\"admin\",\"password\":\"admin123\"}'"
echo "   2. Access: curl -H 'Authorization: Bearer <token>' $BASE_URL/actuator/health"
echo ""

echo "========================================="
echo "📊 Test Summary"
echo "========================================="
echo ""
echo "If all tests passed (✅), your actuator endpoints are secure!"
echo "If any test failed (❌), there's a security vulnerability!"
echo ""
echo "Next steps:"
echo "1. Restart your application if you just made changes"
echo "2. Check application.properties for actuator settings"
echo "3. Check SecurityConfig.java for endpoint security"
echo ""
