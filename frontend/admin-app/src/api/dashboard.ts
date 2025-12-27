import apiClient from './client';
import type { DashboardMetrics } from '../types';

export const dashboardApi = {
  getMetrics: async (): Promise<DashboardMetrics> => {
    // ✅ FIX: Use /admin/dashboard path
    const response = await apiClient.get('/admin/dashboard/metrics');
    return response.data;
  },

  getTransactionStats: async (period: 'today' | 'week' | 'month'): Promise<any> => {
    const response = await apiClient.get(`/admin/dashboard/transaction-stats`, {
      params: { period },
    });
    return response.data;
  },

  getCustomerGrowth: async (period: 'week' | 'month' | 'year'): Promise<any> => {
    const response = await apiClient.get(`/admin/dashboard/customer-growth`, {
      params: { period },
    });
    return response.data;
  },

  getRecentActivity: async (limit = 10): Promise<any[]> => {
    const response = await apiClient.get('/admin/dashboard/recent-activity', {
      params: { limit },
    });
    return response.data;
  },

  getSystemHealth: async (): Promise<any> => {
    const response = await apiClient.get('/admin/dashboard/system-health');
    return response.data;
  },
};
