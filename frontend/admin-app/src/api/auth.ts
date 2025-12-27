import apiClient from './client';
import type { User } from '../types';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

// Backend response type
interface BackendAuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
  userId: string;
  username: string;
  email: string;
  role: string;
  mfaRequired?: boolean;
  success?: boolean;
  message?: string;
}

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    // ✅ FIX: Backend expects 'usernameOrEmail', not 'username'
    const response = await apiClient.post<BackendAuthResponse>('/auth/login', {
      usernameOrEmail: credentials.username,
      password: credentials.password
    });
    const data = response.data;

    // Store tokens
    if (data.accessToken) {
      localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
    }
    if (data.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
    }

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
    try {
      const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
      if (refreshToken) {
        await apiClient.post('/auth/logout', null, {
          headers: {
            Authorization: `Bearer ${refreshToken}`,
          },
        });
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
    }
  },

  refreshToken: async (refreshToken: string): Promise<{ accessToken: string }> => {
    const response = await apiClient.post<BackendAuthResponse>(
      '/auth/refresh',
      null,
      {
        headers: {
          Authorization: `Bearer ${refreshToken}`,
        },
      }
    );
    
    // Store the new access token
    if (response.data.accessToken) {
      localStorage.setItem(ACCESS_TOKEN_KEY, response.data.accessToken);
    }
    
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
