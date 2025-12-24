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
      const response = await authApi.login(username, password);
      
      if (response.data.token && response.data.user) {
        dispatch(loginSuccess({
          user: response.data.user,
          token: response.data.token,
        }));
        toast.success('Login successful!');
        navigate('/dashboard');
      } else {
        throw new Error('Invalid response from server');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Login failed. Please try again.';
      dispatch(loginFailure(errorMessage));
      toast.error(errorMessage);
    }
  }, [dispatch, navigate]);

  const logout = useCallback(() => {
    dispatch(logoutAction());
    toast.info('Logged out successfully');
    navigate('/login');
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
