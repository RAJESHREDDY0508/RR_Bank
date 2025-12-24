import React, { Suspense } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate, Link } from "react-router-dom";
import { useAuth } from "./hooks/useAuth";

// ⭐ Lazy-loaded admin pages
const AdminDashboard = React.lazy(() => import("./pages/Dashboard"));
const Users = React.lazy(() => import("./pages/Users"));
const Accounts = React.lazy(() => import("./pages/Accounts"));
const Transactions = React.lazy(() => import("./pages/Transactions"));
const AuditLogs = React.lazy(() => import("./pages/AuditLogs"));
const Login = React.lazy(() => import("./pages/Login"));

// ⭐ Prefetch optimization (loads chunks on hover)
const prefetch = (importFn: () => Promise<any>) => {
  importFn();
};

// ⭐ Loading skeleton
const PageLoader = () => (
  <div style={{ padding: "40px", textAlign: "center", fontSize: "20px" }}>
    Loading Admin Panel...
  </div>
);

const PrivateRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <PageLoader />;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return children;
};

export default function AdminApp() {
  return (
    <Router>
      <div className="admin-wrapper">

        {/* ⭐ Navigation with Prefetch */}
        <nav className="sidebar">
          <Link
            to="/dashboard"
            onMouseEnter={() => prefetch(() => import("./pages/Dashboard"))}
          >
            Dashboard
          </Link>

          <Link
            to="/users"
            onMouseEnter={() => prefetch(() => import("./pages/Users"))}
          >
            Users
          </Link>

          <Link
            to="/accounts"
            onMouseEnter={() => prefetch(() => import("./pages/Accounts"))}
          >
            Accounts
          </Link>

          <Link
            to="/transactions"
            onMouseEnter={() => prefetch(() => import("./pages/Transactions"))}
          >
            Transactions
          </Link>

          <Link
            to="/audit"
            onMouseEnter={() => prefetch(() => import("./pages/AuditLogs"))}
          >
            Audit Logs
          </Link>
        </nav>

        {/* ⭐ Suspense wrapper for all lazy pages */}
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route
              path="/dashboard"
              element={
                <PrivateRoute>
                  <AdminDashboard />
                </PrivateRoute>
              }
            />

            <Route
              path="/users"
              element={
                <PrivateRoute>
                  <Users />
                </PrivateRoute>
              }
            />

            <Route
              path="/accounts"
              element={
                <PrivateRoute>
                  <Accounts />
                </PrivateRoute>
              }
            />

            <Route
              path="/transactions"
              element={
                <PrivateRoute>
                  <Transactions />
                </PrivateRoute>
              }
            />

            <Route
              path="/audit"
              element={
                <PrivateRoute>
                  <AuditLogs />
                </PrivateRoute>
              }
            />

            <Route path="/login" element={<Login />} />

            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Suspense>
      </div>
    </Router>
  );
}
