import { Routes, Route, Navigate } from 'react-router-dom';

// Pages
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Customers from './pages/Customers';
import CustomerDetails from './pages/CustomerDetails';
import Accounts from './pages/Accounts';
import AccountDetails from './pages/AccountDetails';
import Transactions from './pages/Transactions';
import Payments from './pages/Payments';
import FraudAlerts from './pages/FraudAlerts';
import FraudAlertDetails from './pages/FraudAlertDetails';
import Statements from './pages/Statements';
import AuditLogs from './pages/AuditLogs';
import AdminUsers from './pages/AdminUsers';
import Settings from './pages/Settings';

// Layout
import MainLayout from './components/layout/MainLayout';
import AuthLayout from './components/layout/AuthLayout';

// Guards
import PrivateRoute from './components/common/PrivateRoute';

function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
      </Route>

      {/* Protected routes */}
      <Route element={<PrivateRoute><MainLayout /></PrivateRoute>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/customers" element={<Customers />} />
        <Route path="/customers/:id" element={<CustomerDetails />} />
        <Route path="/accounts" element={<Accounts />} />
        <Route path="/accounts/:id" element={<AccountDetails />} />
        <Route path="/transactions" element={<Transactions />} />
        <Route path="/payments" element={<Payments />} />
        <Route path="/fraud-alerts" element={<FraudAlerts />} />
        <Route path="/fraud-alerts/:id" element={<FraudAlertDetails />} />
        <Route path="/statements" element={<Statements />} />
        <Route path="/audit-logs" element={<AuditLogs />} />
        <Route path="/users" element={<AdminUsers />} />
        <Route path="/settings" element={<Settings />} />
      </Route>

      {/* Redirect root to dashboard */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      
      {/* 404 */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
