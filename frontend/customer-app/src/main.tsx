import React, { Suspense } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate, Link } from "react-router-dom";
import { useAuth } from "./hooks/useAuth";

// ⭐ Lazy-loaded pages
const Dashboard = React.lazy(() => import("./pages/Dashboard"));
const Accounts = React.lazy(() => import("./pages/Accounts"));
const Transactions = React.lazy(() => import("./pages/Transactions"));
const Profile = React.lazy(() => import("./pages/Profile"));
const Login = React.lazy(() => import("./pages/Login"));

// ⭐ Prefetch function (improves load speed on hover)
const prefetch = (importFn: () => Promise<any>) => {
  importFn();
};

// ⭐ Loading skeleton
const PageLoader = () => (
  <div style={{ padding: "40px", fontSize: "18px", textAlign: "center" }}>
    Loading...
  </div>
);

// ⭐ Protected Route
const PrivateRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated, loading } = useAuth();
  
  if (loading) return <PageLoader />;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return children;
};

export default function App() {
  return (
    <Router>
      <div className="app-container">

        {/* ⭐ Navigation with Prefetch Support */}
        <nav className="nav-bar">
          <Link
            to="/dashboard"
            onMouseEnter={() => prefetch(() => import("./pages/Dashboard"))}
          >
            Dashboard
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
            to="/profile"
            onMouseEnter={() => prefetch(() => import("./pages/Profile"))}
          >
            Profile
          </Link>
        </nav>

        {/* ⭐ All lazy-loaded routes use Suspense */}
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route
              path="/dashboard"
              element={
                <PrivateRoute>
                  <Dashboard />
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
              path="/profile"
              element={
                <PrivateRoute>
                  <Profile />
                </PrivateRoute>
              }
            />

            {/* Public */}
            <Route path="/login" element={<Login />} />

            {/* Default */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Suspense>
      </div>
    </Router>
  );
}
