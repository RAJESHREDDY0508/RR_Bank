import apiClient from './client';
import type { FraudAlert, PaginatedResponse } from '../types';

export interface FraudAlertSearchParams {
  status?: string;
  severity?: string;
  alertType?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export const fraudAlertsApi = {
  // ✅ FIX: Use /admin/fraud path for admin operations
  getFraudAlerts: async (params: FraudAlertSearchParams = {}): Promise<PaginatedResponse<FraudAlert>> => {
    const response = await apiClient.get('/admin/fraud/queue', { params });
    return response.data;
  },

  getFraudAlert: async (alertId: string): Promise<FraudAlert> => {
    const response = await apiClient.get(`/admin/fraud/${alertId}`);
    return response.data;
  },

  updateAlertStatus: async (alertId: string, status: string, notes?: string): Promise<void> => {
    await apiClient.patch(`/admin/fraud/${alertId}/status`, { status, notes });
  },

  assignAlert: async (alertId: string, assignedTo: string): Promise<void> => {
    await apiClient.post(`/admin/fraud/${alertId}/assign`, { assignedTo });
  },

  resolveAlert: async (alertId: string, resolution: string, notes: string): Promise<void> => {
    await apiClient.post(`/admin/fraud/${alertId}/resolve`, {
      resolution,
      notes,
    });
  },

  escalateAlert: async (alertId: string, notes: string): Promise<void> => {
    await apiClient.post(`/admin/fraud/${alertId}/escalate`, { notes });
  },

  // Approve fraud event (mark as false positive)
  approveEvent: async (eventId: string, notes?: string): Promise<void> => {
    await apiClient.post(`/admin/fraud/${eventId}/approve`, { notes });
  },

  // Reject fraud event (confirm as fraud)
  rejectEvent: async (eventId: string, notes: string): Promise<void> => {
    await apiClient.post(`/admin/fraud/${eventId}/reject`, { notes });
  },

  // Get fraud statistics
  getStats: async (): Promise<any> => {
    const response = await apiClient.get('/admin/fraud/stats');
    return response.data;
  },
};
