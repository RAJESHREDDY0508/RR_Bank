import apiClient from './client';
import type { Report, PageResponse, ApiResponse } from '../types';

export interface ReportFilters {
  type?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export const reportsApi = {
  getReports: async (filters: ReportFilters = {}): Promise<PageResponse<Report>> => {
    const params = new URLSearchParams();
    if (filters.type) params.append('type', filters.type);
    if (filters.status) params.append('status', filters.status);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());

    const response = await apiClient.get<ApiResponse<PageResponse<Report>>>(
      `/admin/reports?${params.toString()}`
    );
    return response.data.data;
  },

  getReport: async (id: string): Promise<Report> => {
    const response = await apiClient.get<ApiResponse<Report>>(`/admin/reports/${id}`);
    return response.data.data;
  },

  generateReport: async (type: string, parameters?: Record<string, unknown>): Promise<Report> => {
    const response = await apiClient.post<ApiResponse<Report>>('/admin/reports/generate', {
      type,
      parameters,
    });
    return response.data.data;
  },

  downloadReport: async (id: string): Promise<Blob> => {
    const response = await apiClient.get(`/admin/reports/${id}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  deleteReport: async (id: string): Promise<void> => {
    await apiClient.delete(`/admin/reports/${id}`);
  },
};

export default reportsApi;
