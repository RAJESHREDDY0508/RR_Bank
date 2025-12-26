import React, { Suspense } from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
} from "react-router-dom";

import Loader from "./components/Loader";
import { AuthProvider } from "./context/AuthContext";
import PrivateRoute from "./components/PrivateRoute";
import ErrorBoundary from "./components/ErrorBoundary";

import Landing from "./pages/Landing";
import Login from "./pages/Login";
import Register from "./pages/Register";

// Lazy-loaded pages
const Dashboard = React.lazy(() => import("./pages/Dashboard"));
const Accounts = React.lazy(() => import("./pages/Accounts"));
const Transfer = React.lazy(() => import("./pages/Transfer"));
const Transactions = React.lazy(() => import("./pages/Transactions"));
const ForgotPassword = React.lazy(() => import("./pages/ForgotPassword"));
const ResetPassword = React.lazy(() => import("./pages/ResetPassword"));

function App() {
  return (
    <AuthProvider>
      <Router>
        <ErrorBoundary>
          <Suspense fallback={<Loader />}>
            <Routes>
              <Route path="/" element={<Landing />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              <Route path="/reset-password" element={<ResetPassword />} />

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
                path="/accounts/:accountId"
                element={
                  <PrivateRoute>
                    <Accounts />
                  </PrivateRoute>
                }
              />

              <Route
                path="/transfer"
                element={
                  <PrivateRoute>
                    <Transfer />
                  </PrivateRoute>
                }
              />

              <Route
                path="/deposit"
                element={
                  <PrivateRoute>
                    <Transfer />
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
                path="/transactions/:accountId"
                element={
                  <PrivateRoute>
                    <Transactions />
                  </PrivateRoute>
                }
              />
            </Routes>
          </Suspense>
        </ErrorBoundary>
      </Router>
    </AuthProvider>
  );
}

export default App;
