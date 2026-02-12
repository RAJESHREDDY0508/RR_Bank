/**
 * Main App Component with Permission-based Routing
 */

import { Routes, Route, Navigate } from 'react-router-dom';
import { RBACProvider } from './hooks/useRBAC';

// Pages
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Customers from './pages/Customers';
import CustomerDetails from './pages/CustomerDetails';
import Accounts from './pages/Accounts';
import AccountDetails from './pages/AccountDetails';
import AccountRequests from './pages/AccountRequests';
import Transactions from './pages/Transactions';
import Payments from './pages/Payments';
import FraudAlerts from './pages/FraudAlerts';
import FraudAlertDetails from './pages/FraudAlertDetails';
import Statements from './pages/Statements';
import AuditLogs from './pages/AuditLogs';
import AdminUsers from './pages/AdminUsers';
import Settings from './pages/Settings';
import KycRequests from './pages/KycRequests';

// Layout
import MainLayout from './components/layout/MainLayout';
import AuthLayout from './components/layout/AuthLayout';

// Guards
import PrivateRoute, { PermissionRoute } from './components/common/PrivateRoute';

// Types
import { Permission } from './types/rbac';

// Route permission configuration
interface RouteConfig {
  path: string;
  element: React.ReactNode;
  permissions?: Permission[];
  requireAll?: boolean;
}

// Define protected routes with their required permissions
const protectedRoutes: RouteConfig[] = [
  {
    path: '/dashboard',
    element: <Dashboard />,
    permissions: ['DASHBOARD_READ'],
  },
  {
    path: '/customers',
    element: <Customers />,
    permissions: ['CUSTOMER_READ'],
  },
  {
    path: '/customers/:id',
    element: <CustomerDetails />,
    permissions: ['CUSTOMER_READ'],
  },
  {
    path: '/accounts',
    element: <Accounts />,
    permissions: ['ACCOUNT_READ'],
  },
  {
    path: '/accounts/:id',
    element: <AccountDetails />,
    permissions: ['ACCOUNT_READ'],
  },
  {
    path: '/account-requests',
    element: <AccountRequests />,
    permissions: ['ACCOUNT_APPROVE_REQUESTS'],
  },
  {
    path: '/kyc-requests',
    element: <KycRequests />,
    permissions: ['CUSTOMER_READ'],
  },
  {
    path: '/transactions',
    element: <Transactions />,
    permissions: ['TXN_READ'],
  },
  {
    path: '/payments',
    element: <Payments />,
    permissions: ['PAYMENT_READ'],
  },
  {
    path: '/fraud-alerts',
    element: <FraudAlerts />,
    permissions: ['FRAUD_ALERT_READ'],
  },
  {
    path: '/fraud-alerts/:id',
    element: <FraudAlertDetails />,
    permissions: ['FRAUD_ALERT_READ'],
  },
  {
    path: '/statements',
    element: <Statements />,
    permissions: ['STATEMENT_READ'],
  },
  {
    path: '/audit-logs',
    element: <AuditLogs />,
    permissions: ['AUDIT_READ'],
  },
  {
    path: '/users',
    element: <AdminUsers />,
    permissions: ['ADMIN_USER_READ'],
  },
  {
    path: '/settings',
    element: <Settings />,
    permissions: ['SETTINGS_READ'],
  },
];

function AppRoutes() {
  return (
    <Routes>
      {/* Public routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
      </Route>

      {/* Protected routes with permission checks */}
      <Route element={<PrivateRoute><MainLayout /></PrivateRoute>}>
        {protectedRoutes.map(route => (
          <Route
            key={route.path}
            path={route.path}
            element={
              route.permissions && route.permissions.length > 0 ? (
                <PermissionRoute
                  requiredPermissions={route.permissions}
                  requireAll={route.requireAll}
                >
                  {route.element}
                </PermissionRoute>
              ) : (
                route.element
              )
            }
          />
        ))}
      </Route>

      {/* Redirect root to dashboard */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />

      {/* 404 - redirect to dashboard */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

function App() {
  return (
    <RBACProvider>
      <AppRoutes />
    </RBACProvider>
  );
}

export default App;
