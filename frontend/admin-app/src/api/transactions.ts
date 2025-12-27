import apiClient from './client';
import type { Transaction, PaginatedResponse } from '../types';

export interface TransactionSearchParams {
  searchTerm?: string;
  transactionType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
  page?: number;
  size?: number;
}

export const transactionsApi = {
  // ✅ FIX: Use /admin/transactions path for admin operations
  getTransactions: async (params: TransactionSearchParams = {}): Promise<PaginatedResponse<Transaction>> => {
    const response = await apiClient.get('/admin/transactions', { params });
    return response.data;
  },

  getTransaction: async (transactionId: string): Promise<Transaction> => {
    const response = await apiClient.get(`/admin/transactions/${transactionId}`);
    return response.data;
  },

  cancelTransaction: async (transactionId: string, reason: string): Promise<void> => {
    await apiClient.post(`/admin/transactions/${transactionId}/cancel`, { reason });
  },

  reverseTransaction: async (transactionId: string, reason: string): Promise<void> => {
    await apiClient.post(`/admin/transactions/${transactionId}/reverse`, { reason });
  },

  investigateTransaction: async (transactionId: string, notes: string): Promise<void> => {
    await apiClient.post(`/admin/transactions/${transactionId}/investigate`, { notes });
  },

  // Approve pending transaction
  approveTransaction: async (transactionId: string, notes?: string): Promise<void> => {
    await apiClient.post(`/admin/transactions/${transactionId}/approve`, { notes });
  },

  // Reject pending transaction
  rejectTransaction: async (transactionId: string, reason: string): Promise<void> => {
    await apiClient.post(`/admin/transactions/${transactionId}/reject`, { reason });
  },
};
