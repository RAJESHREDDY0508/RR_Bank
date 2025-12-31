@echo off
echo Creating microservices directory structure...

REM Auth Service
mkdir "services\auth-service\src\main\java\com\rrbank\auth\config" 2>nul
mkdir "services\auth-service\src\main\java\com\rrbank\auth\controller" 2>nul
mkdir "services\auth-service\src\main\java\com\rrbank\auth\dto" 2>nul
mkdir "services\auth-service\src\main\java\com\rrbank\auth\entity" 2>nul
mkdir "services\auth-service\src\main\java\com\rrbank\auth\repository" 2>nul
mkdir "services\auth-service\src\main\java\com\rrbank\auth\service" 2>nul
mkdir "services\auth-service\src\main\java\com\rrbank\auth\security" 2>nul
mkdir "services\auth-service\src\main\java\com\rrbank\auth\event" 2>nul
mkdir "services\auth-service\src\main\resources" 2>nul

REM Customer Service
mkdir "services\customer-service\src\main\java\com\rrbank\customer\controller" 2>nul
mkdir "services\customer-service\src\main\java\com\rrbank\customer\entity" 2>nul
mkdir "services\customer-service\src\main\java\com\rrbank\customer\repository" 2>nul
mkdir "services\customer-service\src\main\java\com\rrbank\customer\service" 2>nul
mkdir "services\customer-service\src\main\resources" 2>nul

REM Account Service
mkdir "services\account-service\src\main\java\com\rrbank\account\controller" 2>nul
mkdir "services\account-service\src\main\java\com\rrbank\account\dto" 2>nul
mkdir "services\account-service\src\main\java\com\rrbank\account\entity" 2>nul
mkdir "services\account-service\src\main\java\com\rrbank\account\repository" 2>nul
mkdir "services\account-service\src\main\java\com\rrbank\account\service" 2>nul
mkdir "services\account-service\src\main\resources" 2>nul

REM Transaction Service
mkdir "services\transaction-service\src\main\java\com\rrbank\transaction\config" 2>nul
mkdir "services\transaction-service\src\main\java\com\rrbank\transaction\controller" 2>nul
mkdir "services\transaction-service\src\main\java\com\rrbank\transaction\dto" 2>nul
mkdir "services\transaction-service\src\main\java\com\rrbank\transaction\entity" 2>nul
mkdir "services\transaction-service\src\main\java\com\rrbank\transaction\repository" 2>nul
mkdir "services\transaction-service\src\main\java\com\rrbank\transaction\service" 2>nul
mkdir "services\transaction-service\src\main\java\com\rrbank\transaction\saga" 2>nul
mkdir "services\transaction-service\src\main\java\com\rrbank\transaction\event" 2>nul
mkdir "services\transaction-service\src\main\resources" 2>nul

REM Ledger Service
mkdir "services\ledger-service\src\main\java\com\rrbank\ledger\config" 2>nul
mkdir "services\ledger-service\src\main\java\com\rrbank\ledger\controller" 2>nul
mkdir "services\ledger-service\src\main\java\com\rrbank\ledger\dto" 2>nul
mkdir "services\ledger-service\src\main\java\com\rrbank\ledger\entity" 2>nul
mkdir "services\ledger-service\src\main\java\com\rrbank\ledger\repository" 2>nul
mkdir "services\ledger-service\src\main\java\com\rrbank\ledger\service" 2>nul
mkdir "services\ledger-service\src\main\java\com\rrbank\ledger\event" 2>nul
mkdir "services\ledger-service\src\main\resources" 2>nul

REM Notification Service
mkdir "services\notification-service\src\main\java\com\rrbank\notification\controller" 2>nul
mkdir "services\notification-service\src\main\java\com\rrbank\notification\entity" 2>nul
mkdir "services\notification-service\src\main\java\com\rrbank\notification\repository" 2>nul
mkdir "services\notification-service\src\main\java\com\rrbank\notification\service" 2>nul
mkdir "services\notification-service\src\main\java\com\rrbank\notification\event" 2>nul
mkdir "services\notification-service\src\main\resources" 2>nul

REM Fraud Service
mkdir "services\fraud-service\src\main\java\com\rrbank\fraud\controller" 2>nul
mkdir "services\fraud-service\src\main\java\com\rrbank\fraud\dto" 2>nul
mkdir "services\fraud-service\src\main\java\com\rrbank\fraud\service" 2>nul
mkdir "services\fraud-service\src\main\resources" 2>nul

REM Audit Service
mkdir "services\audit-service\src\main\java\com\rrbank\audit\controller" 2>nul
mkdir "services\audit-service\src\main\java\com\rrbank\audit\entity" 2>nul
mkdir "services\audit-service\src\main\java\com\rrbank\audit\repository" 2>nul
mkdir "services\audit-service\src\main\java\com\rrbank\audit\service" 2>nul
mkdir "services\audit-service\src\main\java\com\rrbank\audit\event" 2>nul
mkdir "services\audit-service\src\main\resources" 2>nul

REM API Gateway
mkdir "api-gateway\src\main\java\com\rrbank\gateway\config" 2>nul
mkdir "api-gateway\src\main\java\com\rrbank\gateway\filter" 2>nul
mkdir "api-gateway\src\main\resources" 2>nul

REM Docker and Scripts
mkdir "docker" 2>nul
mkdir "scripts" 2>nul

echo Directory structure created successfully!
echo.
echo Now you need to copy the source files...
pause
