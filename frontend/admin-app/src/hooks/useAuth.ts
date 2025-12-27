import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAppDispatch, useAppSelector } from './useRedux';
import { loginStart, loginSuccess, loginFailure, logout as logoutAction } from '../store/authSlice';
import { authApi } from '../api/auth';

export const useAuth = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { user, token, isAuthenticated, loading, error } = useAppSelector((state) => state.auth);

  const login = useCallback(async (username: string, password: string) => {
    try {
      dispatch(loginStart());
      
      // ✅ FIX: Pass credentials as object, not separate arguments
      const response = await authApi.login({ username, password });
      
      // ✅ FIX: authApi.login returns { accessToken, refreshToken, user } directly
      if (response.accessToken && response.user) {
        dispatch(loginSuccess({
          user: response.user,
          token: response.accessToken,
        }));
        toast.success('Login successful!');
        navigate('/dashboard');
      } else {
        throw new Error('Invalid response from server');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Login failed. Please try again.';
      dispatch(loginFailure(errorMessage));
      toast.error(errorMessage);
    }
  }, [dispatch, navigate]);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('Logout API error:', error);
    } finally {
      dispatch(logoutAction());
      toast.info('Logged out successfully');
      navigate('/login');
    }
  }, [dispatch, navigate]);

  return {
    user,
    token,
    isAuthenticated,
    loading,
    error,
    login,
    logout,
  };
};
