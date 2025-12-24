import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from './useRedux';
import { login, register, logout, clearError } from '../store/authSlice';
import { LoginCredentials, RegisterData } from '../types';
import { useNavigate } from 'react-router-dom';

export const useAuth = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { user, isAuthenticated, loading, error } = useAppSelector((state) => state.auth);

  const handleLogin = async (credentials: LoginCredentials) => {
    try {
      await dispatch(login(credentials)).unwrap();
      navigate('/dashboard');
    } catch (err) {
      // Error is handled in the slice
    }
  };

  const handleRegister = async (data: RegisterData) => {
    try {
      await dispatch(register(data)).unwrap();
      navigate('/dashboard');
    } catch (err) {
      // Error is handled in the slice
    }
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const clearAuthError = () => {
    dispatch(clearError());
  };

  return {
    user,
    isAuthenticated,
    loading,
    error,
    login: handleLogin,
    register: handleRegister,
    logout: handleLogout,
    clearError: clearAuthError,
  };
};
