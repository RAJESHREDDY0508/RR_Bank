import apiClient from './client';
import { PageResponse, ApiResponse } from '../types';

export interface AuditLog {
  id: string;
  adminUserId?: string;
  adminUsername: string;
  action: string;
  actionType?: string;
  entityType?: string;
  entityId?: string;
  description?: string;
  oldValue?: string;
  newValue?: string;
  ipAddress?: string;
  userAgent?: string;
  status: string;
  errorMessage?: string;
  createdAt: string;
}

export interface AuditLogFilters {
  search?: string;
  adminUserId?: string;
  action?: string;
  actionType?: string;
  entityType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export const auditLogsApi = {
  getLogs: async (filters: AuditLogFilters = {}): Promise<PageResponse<AuditLog>> => {
    const params = new URLSearchParams();
    if (filters.search) params.append('search', filters.search);
    if (filters.adminUserId) params.append('adminUserId', filters.adminUserId);
    if (filters.action) params.append('action', filters.action);
    if (filters.actionType) params.append('actionType', filters.actionType);
    if (filters.entityType) params.append('entityType', filters.entityType);
    if (filters.status) params.append('status', filters.status);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());

    const response = await apiClient.get<ApiResponse<PageResponse<AuditLog>>>(
      `/admin/audit-logs?${params.toString()}`
    );
    return response.data.data;
  },

  getSecurityEvents: async (
    startDate?: string,
    endDate?: string,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<AuditLog>> => {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    params.append('page', page.toString());
    params.append('size', size.toString());

    const response = await apiClient.get<ApiResponse<PageResponse<AuditLog>>>(
      `/admin/audit-logs/security-events?${params.toString()}`
    );
    return response.data.data;
  },

  getAdminActions: async (
    adminUserId?: string,
    entityType?: string,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<AuditLog>> => {
    const params = new URLSearchParams();
    if (adminUserId) params.append('adminUserId', adminUserId);
    if (entityType) params.append('entityType', entityType);
    params.append('page', page.toString());
    params.append('size', size.toString());

    const response = await apiClient.get<ApiResponse<PageResponse<AuditLog>>>(
      `/admin/audit-logs/admin-actions?${params.toString()}`
    );
    return response.data.data;
  },

  getLog: async (id: string): Promise<AuditLog> => {
    const response = await apiClient.get<ApiResponse<AuditLog>>(`/admin/audit-logs/${id}`);
    return response.data.data;
  },
};

export default auditLogsApi;
