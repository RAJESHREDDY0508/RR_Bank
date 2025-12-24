import apiClient from './client';
import { LoginRequest, LoginResponse, RegisterRequest, User } from '../types';

// Backend response type (different from frontend interface)
interface BackendAuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  username: string;
  email: string;
  role: string;
  expiresIn: number;
}

// ✅ FIX: Standardized to use 'accessToken' everywhere
const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export const authApi = {
  // Login
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<BackendAuthResponse>('/auth/login', credentials);
    const data = response.data;

    // ✅ FIX: Use accessToken directly from backend
    return {
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      user: {
        userId: data.userId,
        username: data.username,
        email: data.email,
        role: data.role as User['role'],
        isActive: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      expiresIn: data.expiresIn,
    };
  },

  // Register
  register: async (userData: RegisterRequest): Promise<User> => {
    const response = await apiClient.post('/auth/register', userData);
    return response.data;
  },

  // Logout
  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem('user');
  },

  // Refresh token - handled in interceptor, but keeping for compatibility
  refreshToken: async (refreshToken: string): Promise<LoginResponse> => {
    const response = await apiClient.post<BackendAuthResponse>(
      '/auth/refresh',
      null,
      {
        headers: {
          Authorization: `Bearer ${refreshToken}`,
        },
      }
    );
    const data = response.data;

    return {
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      user: {
        userId: data.userId,
        username: data.username,
        email: data.email,
        role: data.role as User['role'],
        isActive: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      expiresIn: data.expiresIn,
    };
  },

  // Get current user
  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get('/auth/me');
    return response.data;
  },

  // Change password
  changePassword: async (oldPassword: string, newPassword: string): Promise<void> => {
    await apiClient.post('/auth/change-password', {
      oldPassword,
      newPassword,
    });
  },

  // Forgot password
  forgotPassword: async (email: string): Promise<void> => {
    await apiClient.post('/auth/forgot-password', { email });
  },

  // Reset password
  resetPassword: async (token: string, newPassword: string): Promise<void> => {
    await apiClient.post('/auth/reset-password', {
      token,
      newPassword,
    });
  },

  // Verify email
  verifyEmail: async (token: string): Promise<void> => {
    await apiClient.post('/auth/verify-email', { token });
  },
};

export default authApi;
