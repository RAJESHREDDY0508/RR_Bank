/**
 * API Client with improved error handling and RBAC support
 */

import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { toast } from 'react-toastify';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

// Paths that should NOT include Authorization header
const PUBLIC_PATHS = [
  '/admin/auth/login',
  '/admin/auth/refresh',
  '/auth/login',
  '/auth/register',
  '/auth/refresh',
  '/auth/forgot-password',
  '/auth/reset-password',
];

// Error response interface
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error?: string;
  errorCode?: string;
  code?: string;
  message: string;
  path: string;
  requestId?: string;
  details?: Array<{ field: string; message: string }>;
}

export const apiClient = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
});

// Track if we're currently refreshing the token
let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

const onRefreshed = (token: string) => {
  refreshSubscribers.forEach(callback => callback(token));
  refreshSubscribers = [];
};

const addRefreshSubscriber = (callback: (token: string) => void) => {
  refreshSubscribers.push(callback);
};

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const isPublicPath = PUBLIC_PATHS.some(path => config.url?.includes(path));

    if (!isPublicPath) {
      const token = localStorage.getItem(ACCESS_TOKEN_KEY);
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }

    // Add request ID for tracking
    const requestId = `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    if (config.headers) {
      config.headers['X-Request-ID'] = requestId;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle errors and token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const requestUrl = originalRequest?.url || '';

    // Check if this is a login/auth request
    const isAuthRequest = PUBLIC_PATHS.some(path => requestUrl.includes(path));

    // Handle 401 Unauthorized - attempt token refresh
    if (error.response?.status === 401 && !originalRequest._retry && !isAuthRequest) {
      if (isRefreshing) {
        // Wait for token refresh
        return new Promise(resolve => {
          addRefreshSubscriber((token: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            resolve(apiClient(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
        if (refreshToken) {
          const response = await axios.post(
            `${API_BASE_URL}/api/admin/auth/refresh`,
            null,
            {
              headers: {
                Authorization: `Bearer ${refreshToken}`,
              },
            }
          );

          const { accessToken, refreshToken: newRefreshToken } = response.data.data;
          localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);

          if (newRefreshToken) {
            localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken);
          }

          isRefreshing = false;
          onRefreshed(accessToken);

          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          }
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        isRefreshing = false;
        refreshSubscribers = [];
        
        // Refresh failed, logout user
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        localStorage.removeItem('adminUser');
        
        // Only redirect if not already on login page
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login';
          toast.error('Session expired. Please login again.');
        }
        return Promise.reject(refreshError);
      }
    }

    // Handle specific error codes with appropriate messages
    if (!isAuthRequest && error.response) {
      const status = error.response.status;
      const errorData = error.response.data;
      const requestId = errorData?.requestId || error.response.headers?.['x-request-id'];

      switch (status) {
        case 400:
          // Validation errors - show field-specific messages if available
          if (errorData?.details && errorData.details.length > 0) {
            const fieldErrors = errorData.details.map(d => `${d.field}: ${d.message}`).join(', ');
            toast.error(`Validation error: ${fieldErrors}`);
          } else {
            toast.error(errorData?.message || 'Invalid request. Please check your input.');
          }
          break;

        case 403:
          toast.error(errorData?.message || 'Access denied. You do not have permission for this action.');
          break;

        case 404:
          toast.error(errorData?.message || 'Resource not found.');
          break;

        case 409:
          toast.error(errorData?.message || 'Conflict. The resource may already exist.');
          break;

        case 422:
          toast.error(errorData?.message || 'Unable to process request.');
          break;

        case 429:
          toast.error('Too many requests. Please try again later.');
          break;

        case 500:
        case 502:
        case 503:
        case 504:
          const errorMsg = requestId
            ? `Server error. Request ID: ${requestId}`
            : 'Server error. Please try again later.';
          toast.error(errorMsg);
          break;

        default:
          if (!isAuthRequest) {
            toast.error(errorData?.message || 'An unexpected error occurred.');
          }
      }
    } else if (error.code === 'ECONNABORTED') {
      toast.error('Request timed out. Please try again.');
    } else if (!error.response) {
      toast.error('Network error. Please check your connection.');
    }

    return Promise.reject(error);
  }
);

// Helper function to extract error message
export const getErrorMessage = (error: any): string => {
  if (error.response?.data?.message) {
    return error.response.data.message;
  }
  if (error.message) {
    return error.message;
  }
  return 'An unexpected error occurred';
};

// Helper function to get request ID from error
export const getRequestId = (error: any): string | null => {
  return error.response?.data?.requestId || 
         error.response?.headers?.['x-request-id'] || 
         null;
};

export default apiClient;
