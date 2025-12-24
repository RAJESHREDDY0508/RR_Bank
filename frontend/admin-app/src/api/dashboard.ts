import apiClient from './client';
import type { DashboardMetrics } from '../types';

export const dashboardApi = {
  getMetrics: async (): Promise<DashboardMetrics> => {
    const response = await apiClient.get('/dashboard/metrics');
    return response.data;
  },

  getTransactionStats: async (period: 'today' | 'week' | 'month'): Promise<any> => {
    const response = await apiClient.get(`/dashboard/transaction-stats`, {
      params: { period },
    });
    return response.data;
  },

  getCustomerGrowth: async (period: 'week' | 'month' | 'year'): Promise<any> => {
    const response = await apiClient.get(`/dashboard/customer-growth`, {
      params: { period },
    });
    return response.data;
  },

  getRecentActivity: async (limit = 10): Promise<any[]> => {
    const response = await apiClient.get('/dashboard/recent-activity', {
      params: { limit },
    });
    return response.data;
  },

  getSystemHealth: async (): Promise<any> => {
    const response = await apiClient.get('/dashboard/system-health');
    return response.data;
  },
};
