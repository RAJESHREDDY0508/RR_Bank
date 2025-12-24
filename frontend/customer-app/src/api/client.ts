import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import toast from 'react-hot-toast';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
});

// ✅ FIX: Standardized to use 'accessToken' everywhere
const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

// Request interceptor - Add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor - Handle errors and token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Handle 401 Unauthorized - Token expired
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
        if (refreshToken) {
          // ✅ FIX: Send refresh token in Authorization header
          const response = await axios.post(
            `${API_BASE_URL}/auth/refresh`,
            null,
            {
              headers: {
                Authorization: `Bearer ${refreshToken}`,
              },
            }
          );

          // ✅ FIX: Backend returns accessToken
          const { accessToken, refreshToken: newRefreshToken } = response.data;
          localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
          
          // Update refresh token if backend sends a new one
          if (newRefreshToken) {
            localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken);
          }

          // Retry the original request with new token
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          }
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed - logout user
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        localStorage.removeItem('user');
        window.location.href = '/login';
        toast.error('Session expired. Please login again.');
        return Promise.reject(refreshError);
      }
    }

    // Handle other errors
    const errorMessage = error.response?.data?.message || 
                         error.response?.data?.error || 
                         error.message ||
                         'An error occurred';

    switch (error.response?.status) {
      case 400:
        toast.error(`Bad Request: ${errorMessage}`);
        break;
      case 401:
        toast.error('Unauthorized. Please login again.');
        break;
      case 403:
        toast.error('Access denied. You do not have permission.');
        break;
      case 404:
        toast.error('Resource not found.');
        break;
      case 409:
        toast.error(`Conflict: ${errorMessage}`);
        break;
      case 422:
        toast.error(`Validation Error: ${errorMessage}`);
        break;
      case 429:
        toast.error('Too many requests. Please try again later.');
        break;
      case 500:
        toast.error('Server error. Please try again later.');
        break;
      case 503:
        toast.error('Service unavailable. Please try again later.');
        break;
      default:
        if (error.code === 'ECONNABORTED') {
          toast.error('Request timeout. Please check your connection.');
        } else if (error.code === 'ERR_NETWORK') {
          toast.error('Network error. Please check your connection.');
        } else {
          toast.error(errorMessage);
        }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
