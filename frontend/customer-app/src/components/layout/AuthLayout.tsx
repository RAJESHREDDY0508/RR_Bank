import React from 'react';
import { Outlet, Navigate } from 'react-router-dom';
import { useAppSelector } from '../../hooks/useRedux';

const AuthLayout: React.FC = () => {
  const { isAuthenticated } = useAppSelector((state) => state.auth);
  const { isDark } = useAppSelector((state) => state.theme);

  // Redirect to dashboard if already authenticated
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className={`min-h-screen ${isDark ? 'dark bg-gray-900' : 'bg-gradient-to-br from-blue-500 to-purple-600'}`}>
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="w-full max-w-md">
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-white dark:bg-gray-800 rounded-full shadow-lg mb-4">
              <span className="text-3xl font-bold text-blue-600">RR</span>
            </div>
            <h1 className="text-3xl font-bold text-white mb-2">RR-Bank</h1>
            <p className="text-blue-100">Your trusted banking partner</p>
          </div>
          
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
