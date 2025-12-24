import apiClient from './client';
import type { User } from '../types';

export interface LoginRequest {
  username: string;
  password: string;
}

// ✅ FIX: Standardized to use 'accessToken' everywhere
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

// Backend response type (different from frontend interface)
interface BackendAuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  username: string;
  email: string;
  role: string;
  expiresIn?: number;
}

// ✅ FIX: Standardized to use 'accessToken' everywhere
const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export const authApi = {
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
        role: data.role,
        firstName: data.username,
        lastName: '',
      },
    };
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },

  refreshToken: async (refreshToken: string): Promise<{ accessToken: string }> => {
    // ✅ FIX: Send refresh token in Authorization header
    const response = await apiClient.post<BackendAuthResponse>(
      '/auth/refresh',
      null,
      {
        headers: {
          Authorization: `Bearer ${refreshToken}`,
        },
      }
    );
    
    // ✅ FIX: Backend returns accessToken
    return {
      accessToken: response.data.accessToken,
    };
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get('/auth/me');
    return response.data;
  },

  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await apiClient.post('/auth/change-password', {
      currentPassword,
      newPassword,
    });
  },
};
