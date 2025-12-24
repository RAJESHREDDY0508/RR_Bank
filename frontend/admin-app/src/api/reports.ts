import apiClient from './client';
import type { Report } from '../types';

export interface ReportGenerateParams {
  reportType: 'TRANSACTIONS' | 'CUSTOMERS' | 'FRAUD' | 'FINANCIAL';
  startDate?: string;
  endDate?: string;
  filters?: Record<string, any>;
  format: 'PDF' | 'EXCEL' | 'CSV';
}

export const reportsApi = {
  generateReport: async (params: ReportGenerateParams): Promise<Report> => {
    const response = await apiClient.post('/reports/generate', params);
    return response.data;
  },

  getReports: async (page = 0, size = 20): Promise<any> => {
    const response = await apiClient.get('/reports', {
      params: { page, size },
    });
    return response.data;
  },

  getReport: async (reportId: string): Promise<Report> => {
    const response = await apiClient.get(`/reports/${reportId}`);
    return response.data;
  },

  downloadReport: async (reportId: string): Promise<Blob> => {
    const response = await apiClient.get(`/reports/${reportId}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  deleteReport: async (reportId: string): Promise<void> => {
    await apiClient.delete(`/reports/${reportId}`);
  },
};
