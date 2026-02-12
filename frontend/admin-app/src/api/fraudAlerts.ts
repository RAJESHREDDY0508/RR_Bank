import apiClient from './client';
import { PageResponse, ApiResponse, FraudAlertStatus, FraudAlert } from '../types';

export interface TransactionLimit {
  id: string;
  userId: string;
  customerName: string;
  limitType: string;
  dailyLimit: number;
  perTransactionLimit: number;
  monthlyLimit: number;
  usedToday: number;
  usedThisMonth: number;
  remainingDaily: number;
  remainingMonthly: number;
  updatedAt: string;
}

export interface FraudStats {
  totalAlerts: number;
  pendingAlerts: number;
  confirmedFraud: number;
  falsePositives: number;
  alertsThisWeek: number;
  averageResolutionTime: string;
}

export interface FraudAlertFilters {
  search?: string;
  status?: string;
  eventType?: string;
  page?: number;
  size?: number;
}

export interface ResolveAlertRequest {
  status: FraudAlertStatus;
  notes: string;
}

export const fraudAlertsApi = {
  getAlerts: async (filters: FraudAlertFilters = {}): Promise<PageResponse<FraudAlert>> => {
    const params = new URLSearchParams();
    if (filters.search) params.append('search', filters.search);
    if (filters.status) params.append('status', filters.status);
    if (filters.eventType) params.append('eventType', filters.eventType);
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());

    const response = await apiClient.get<ApiResponse<PageResponse<FraudAlert>>>(
      `/admin/fraud/alerts?${params.toString()}`
    );
    return response.data.data;
  },

  getAlert: async (id: string): Promise<FraudAlert> => {
    const response = await apiClient.get<ApiResponse<FraudAlert>>(`/admin/fraud/alerts/${id}`);
    return response.data.data;
  },

  resolveAlert: async (id: string, data: ResolveAlertRequest): Promise<FraudAlert> => {
    const response = await apiClient.put<ApiResponse<FraudAlert>>(
      `/admin/fraud/alerts/${id}/resolve`,
      { resolution: data.status, notes: data.notes }
    );
    return response.data.data;
  },

  getStats: async (): Promise<FraudStats> => {
    const response = await apiClient.get<ApiResponse<FraudStats>>('/admin/fraud/stats');
    return response.data.data;
  },

  getLimits: async (page: number = 0, size: number = 20): Promise<PageResponse<TransactionLimit>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<TransactionLimit>>>(
      `/admin/fraud/limits?page=${page}&size=${size}`
    );
    return response.data.data;
  },

  getUserLimits: async (userId: string): Promise<TransactionLimit[]> => {
    const response = await apiClient.get<ApiResponse<TransactionLimit[]>>(
      `/admin/fraud/limits/${userId}`
    );
    return response.data.data;
  },

  updateUserLimits: async (
    userId: string,
    dailyLimit: number,
    perTransactionLimit: number,
    monthlyLimit: number,
    reason?: string
  ): Promise<TransactionLimit> => {
    const response = await apiClient.put<ApiResponse<TransactionLimit>>(
      `/admin/fraud/limits/${userId}`,
      { dailyLimit, perTransactionLimit, monthlyLimit, reason }
    );
    return response.data.data;
  },
};

export default fraudAlertsApi;
