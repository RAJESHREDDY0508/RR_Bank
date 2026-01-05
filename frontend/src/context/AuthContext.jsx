import { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/bankService';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user is logged in on mount
    const currentUser = authService.getCurrentUser();
    if (currentUser) {
      setUser(currentUser);
    }
    setLoading(false);
  }, []);

  const login = async (credentials) => {
    const data = await authService.login(credentials);
    // Extract user info from response
    // Backend returns { accessToken, refreshToken, user: { id, username, email, ... } }
    // or { accessToken, refreshToken, id, username, email, ... }
    const userInfo = data.user || {
      id: data.id,
      username: data.username,
      email: data.email,
      firstName: data.firstName,
      lastName: data.lastName,
      role: data.role
    };
    setUser(userInfo);
    return data;
  };

  const register = async (userData) => {
    const data = await authService.register(userData);
    // After registration, user info is stored in localStorage by authService
    const userInfo = data.user || {
      id: data.id,
      username: data.username,
      email: data.email,
      firstName: data.firstName,
      lastName: data.lastName,
      role: data.role
    };
    setUser(userInfo);
    return data;
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
    }
  };

  const updateUser = (userData) => {
    setUser(prev => ({ ...prev, ...userData }));
    // Update localStorage
    const stored = authService.getCurrentUser();
    if (stored) {
      localStorage.setItem('user', JSON.stringify({ ...stored, ...userData }));
    }
  };

  const value = {
    user,
    login,
    register,
    logout,
    updateUser,
    isAuthenticated: !!user,
    loading,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
