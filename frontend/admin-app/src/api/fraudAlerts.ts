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
  getFraudAlerts: async (params: FraudAlertSearchParams = {}): Promise<PaginatedResponse<FraudAlert>> => {
    const response = await apiClient.get('/fraud-alerts', { params });
    return response.data;
  },

  getFraudAlert: async (alertId: string): Promise<FraudAlert> => {
    const response = await apiClient.get(`/fraud-alerts/${alertId}`);
    return response.data;
  },

  updateAlertStatus: async (alertId: string, status: string, notes?: string): Promise<void> => {
    await apiClient.patch(`/fraud-alerts/${alertId}/status`, { status, notes });
  },

  assignAlert: async (alertId: string, assignedTo: string): Promise<void> => {
    await apiClient.post(`/fraud-alerts/${alertId}/assign`, { assignedTo });
  },

  resolveAlert: async (alertId: string, resolution: string, notes: string): Promise<void> => {
    await apiClient.post(`/fraud-alerts/${alertId}/resolve`, {
      resolution,
      notes,
    });
  },

  escalateAlert: async (alertId: string, notes: string): Promise<void> => {
    await apiClient.post(`/fraud-alerts/${alertId}/escalate`, { notes });
  },
};
