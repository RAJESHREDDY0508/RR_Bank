import apiClient from './client';
import { PageResponse, ApiResponse, Transaction, TransactionStatus } from '../types';

export interface TransactionFilters {
  search?: string;
  type?: string;
  transactionType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
  accountNumber?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface TransactionStats {
  totalTransactions: number;
  todayTransactions: number;
  todayDeposits: number;
  todayWithdrawals: number;
  todayTransfers: number;
  pendingTransactions: number;
  failedToday: number;
  growthPercent: number;
}

export const transactionsApi = {
  getTransactions: async (filters: TransactionFilters = {}): Promise<PageResponse<Transaction>> => {
    const params = new URLSearchParams();
    if (filters.search) params.append('search', filters.search);
    if (filters.type) params.append('type', filters.type);
    if (filters.transactionType) params.append('transactionType', filters.transactionType);
    if (filters.status) params.append('status', filters.status);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.minAmount !== undefined) params.append('minAmount', filters.minAmount.toString());
    if (filters.maxAmount !== undefined) params.append('maxAmount', filters.maxAmount.toString());
    if (filters.accountNumber) params.append('accountNumber', filters.accountNumber);
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());
    if (filters.sortBy) params.append('sortBy', filters.sortBy);
    if (filters.sortDir) params.append('sortDir', filters.sortDir);

    const response = await apiClient.get<ApiResponse<PageResponse<Transaction>>>(
      `/admin/transactions?${params.toString()}`
    );
    return response.data.data;
  },

  getTransaction: async (id: string): Promise<Transaction> => {
    const response = await apiClient.get<ApiResponse<Transaction>>(`/admin/transactions/${id}`);
    return response.data.data;
  },

  getTransactionByReference: async (reference: string): Promise<Transaction> => {
    const response = await apiClient.get<ApiResponse<Transaction>>(
      `/admin/transactions/reference/${reference}`
    );
    return response.data.data;
  },

  getStats: async (): Promise<TransactionStats> => {
    const response = await apiClient.get<ApiResponse<TransactionStats>>('/admin/transactions/stats');
    return response.data.data;
  },

  exportTransactions: async (filters: TransactionFilters = {}): Promise<Blob> => {
    const params = new URLSearchParams();
    if (filters.type) params.append('type', filters.type);
    if (filters.transactionType) params.append('transactionType', filters.transactionType);
    if (filters.status) params.append('status', filters.status);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);

    const response = await apiClient.get(`/admin/transactions/export?${params.toString()}`, {
      responseType: 'blob',
    });
    return response.data;
  },

  requestReversal: async (transactionId: string, reason: string): Promise<void> => {
    await apiClient.post(`/admin/transactions/${transactionId}/reversal-request`, {
      reason,
    });
  },
};

export default transactionsApi;
