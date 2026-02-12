import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authService, customerService } from '../services/bankService';

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
  const [kycStatus, setKycStatus] = useState(null); // 'PENDING', 'APPROVED', 'REJECTED'
  const [kycRejectionReason, setKycRejectionReason] = useState(null);
  const [loading, setLoading] = useState(true);

  // Fetch KYC status for a user
  const fetchKycStatus = useCallback(async (userId) => {
    if (!userId) return;
    try {
      const statusData = await customerService.getKycStatus(userId);
      setKycStatus(statusData.kycStatus || 'PENDING');
      setKycRejectionReason(statusData.rejectionReason || null);
    } catch (error) {
      console.error('Failed to fetch KYC status:', error);
      // Default to PENDING if we can't fetch status
      setKycStatus('PENDING');
    }
  }, []);

  useEffect(() => {
    // Check if user is logged in on mount
    const initAuth = async () => {
      const currentUser = authService.getCurrentUser();
      if (currentUser) {
        setUser(currentUser);
        // Fetch KYC status
        await fetchKycStatus(currentUser.id);
      }
      setLoading(false);
    };
    initAuth();
  }, [fetchKycStatus]);

  const login = async (credentials) => {
    const data = await authService.login(credentials);
    // Extract user info from response
    const userInfo = data.user || {
      id: data.id,
      username: data.username,
      email: data.email,
      firstName: data.firstName,
      lastName: data.lastName,
      role: data.role
    };
    setUser(userInfo);
    // Fetch KYC status after login
    await fetchKycStatus(userInfo.id);
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
    // New users start with PENDING KYC
    setKycStatus('PENDING');
    setKycRejectionReason(null);
    return data;
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
      setKycStatus(null);
      setKycRejectionReason(null);
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

  // Refresh KYC status - can be called after admin approves
  const refreshKycStatus = async () => {
    if (user?.id) {
      await fetchKycStatus(user.id);
    }
  };

  // Check if user can perform transactions
  const canTransact = kycStatus === 'APPROVED';

  const value = {
    user,
    login,
    register,
    logout,
    updateUser,
    isAuthenticated: !!user,
    loading,
    // KYC related
    kycStatus,
    kycRejectionReason,
    refreshKycStatus,
    canTransact,
    isKycPending: kycStatus === 'PENDING',
    isKycApproved: kycStatus === 'APPROVED',
    isKycRejected: kycStatus === 'REJECTED',
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
