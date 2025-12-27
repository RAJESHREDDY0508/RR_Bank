import apiClient from './client';
import type { AuditLog, PaginatedResponse } from '../types';

export interface AuditLogSearchParams {
  userId?: string;
  action?: string;
  resource?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
  page?: number;
  size?: number;
}

export const auditLogsApi = {
  // ✅ FIX: Use /admin/audit-logs path for admin operations
  getAuditLogs: async (params: AuditLogSearchParams = {}): Promise<PaginatedResponse<AuditLog>> => {
    const response = await apiClient.get('/admin/audit-logs', { params });
    return response.data;
  },

  getAuditLog: async (logId: string): Promise<AuditLog> => {
    const response = await apiClient.get(`/admin/audit-logs/${logId}`);
    return response.data;
  },

  getUserAuditLogs: async (userId: string, page = 0, size = 20): Promise<PaginatedResponse<AuditLog>> => {
    const response = await apiClient.get(`/admin/audit-logs/user/${userId}`, {
      params: { page, size },
    });
    return response.data;
  },

  exportAuditLogs: async (params: AuditLogSearchParams): Promise<Blob> => {
    const response = await apiClient.get('/admin/audit-logs/export', {
      params,
      responseType: 'blob',
    });
    return response.data;
  },
};
