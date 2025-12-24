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
  getTransactions: async (params: TransactionSearchParams = {}): Promise<PaginatedResponse<Transaction>> => {
    const response = await apiClient.get('/transactions', { params });
    return response.data;
  },

  getTransaction: async (transactionId: string): Promise<Transaction> => {
    const response = await apiClient.get(`/transactions/${transactionId}`);
    return response.data;
  },

  cancelTransaction: async (transactionId: string, reason: string): Promise<void> => {
    await apiClient.post(`/transactions/${transactionId}/cancel`, { reason });
  },

  reverseTransaction: async (transactionId: string, reason: string): Promise<void> => {
    await apiClient.post(`/transactions/${transactionId}/reverse`, { reason });
  },

  investigateTransaction: async (transactionId: string, notes: string): Promise<void> => {
    await apiClient.post(`/transactions/${transactionId}/investigate`, { notes });
  },
};
