/**
 * Auth API - Updated for RBAC with roles and permissions
 */

import apiClient from './client';
import { Permission, RoleName, AdminUser } from '../types/rbac';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AdminUserResponse {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string; // Legacy single role
  roles: RoleName[]; // New RBAC roles
  permissions: Permission[]; // Permissions from all roles
  department?: string;
  status: string;
  mustChangePassword: boolean;
  lastLogin?: string;
  createdAt?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AdminUserResponse;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const ADMIN_USER_KEY = 'adminUser';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    // Clear any existing tokens before login
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(ADMIN_USER_KEY);

    const response = await apiClient.post<ApiResponse<LoginResponse>>('/admin/auth/login', credentials);
    const data = response.data.data;

    // Store tokens
    if (data.accessToken) {
      localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
    }
    if (data.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
    }
    
    // Store user with roles and permissions
    if (data.user) {
      // Ensure roles array exists (fallback to single role if needed)
      const user: AdminUser = {
        id: data.user.userId,
        userId: data.user.userId,
        username: data.user.username,
        email: data.user.email,
        firstName: data.user.firstName,
        lastName: data.user.lastName,
        roles: data.user.roles || [data.user.role as RoleName],
        permissions: data.user.permissions || [],
        status: data.user.status as any,
        department: data.user.department,
        mustChangePassword: data.user.mustChangePassword,
        lastLogin: data.user.lastLogin,
        createdAt: data.user.createdAt,
      };
      localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(user));
    }

    return data;
  },

  logout: async (): Promise<void> => {
    try {
      await apiClient.post('/admin/auth/logout');
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(ADMIN_USER_KEY);
    }
  },

  refreshToken: async (): Promise<LoginResponse> => {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await apiClient.post<ApiResponse<LoginResponse>>(
      '/admin/auth/refresh',
      null,
      {
        headers: {
          Authorization: `Bearer ${refreshToken}`,
        },
      }
    );

    const data = response.data.data;

    if (data.accessToken) {
      localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
    }
    if (data.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
    }

    return data;
  },

  getCurrentUser: async (): Promise<AdminUserResponse> => {
    const response = await apiClient.get<ApiResponse<AdminUserResponse>>('/admin/auth/me');
    const userData = response.data.data;

    // Update stored user data
    const user: AdminUser = {
      id: userData.userId,
      userId: userData.userId,
      username: userData.username,
      email: userData.email,
      firstName: userData.firstName,
      lastName: userData.lastName,
      roles: userData.roles || [userData.role as RoleName],
      permissions: userData.permissions || [],
      status: userData.status as any,
      department: userData.department,
      mustChangePassword: userData.mustChangePassword,
      lastLogin: userData.lastLogin,
      createdAt: userData.createdAt,
    };
    localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(user));

    return userData;
  },

  changePassword: async (currentPassword: string, newPassword: string, confirmPassword: string): Promise<void> => {
    await apiClient.post('/admin/auth/change-password', {
      currentPassword,
      newPassword,
      confirmPassword,
    });
  },

  getStoredUser: (): AdminUser | null => {
    const userJson = localStorage.getItem(ADMIN_USER_KEY);
    if (userJson) {
      try {
        return JSON.parse(userJson);
      } catch {
        return null;
      }
    }
    return null;
  },

  isAuthenticated: (): boolean => {
    return !!localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  getAccessToken: (): string | null => {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  // Get user permissions (from stored user or token)
  getUserPermissions: (): Permission[] => {
    const user = authApi.getStoredUser();
    return user?.permissions || [];
  },

  // Get user roles
  getUserRoles: (): RoleName[] => {
    const user = authApi.getStoredUser();
    return user?.roles || [];
  },

  // Check if user has specific permission
  hasPermission: (permission: Permission): boolean => {
    const user = authApi.getStoredUser();
    if (!user) return false;
    if (user.roles?.includes('SUPER_ADMIN')) return true;
    return user.permissions?.includes(permission) || false;
  },

  // Check if user has any of the specified permissions
  hasAnyPermission: (...permissions: Permission[]): boolean => {
    const user = authApi.getStoredUser();
    if (!user) return false;
    if (user.roles?.includes('SUPER_ADMIN')) return true;
    return permissions.some(p => user.permissions?.includes(p));
  },
};

export default authApi;
