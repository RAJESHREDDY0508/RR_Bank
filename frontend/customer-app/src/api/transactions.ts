import apiClient from './client';
import {
  Transaction,
  TransferRequest,
  TransactionFilter,
  PaginatedResponse,
} from '../types';

export const transactionsApi = {
  // Get transactions for an account
  getAccountTransactions: async (
    accountNumber: string,
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<Transaction>> => {
    const response = await apiClient.get(
      `/transactions/account/${accountNumber}`,
      {
        params: { page, size },
      }
    );
    return response.data;
  },

  // Get transaction by ID
  getTransactionById: async (transactionId: string): Promise<Transaction> => {
    const response = await apiClient.get(`/transactions/${transactionId}`);
    return response.data;
  },

  // Transfer funds between accounts
  transfer: async (transferData: TransferRequest): Promise<Transaction> => {
    const response = await apiClient.post('/transactions/transfer', transferData);
    return response.data;
  },

  // Search transactions with filters
  searchTransactions: async (
    filters: TransactionFilter
  ): Promise<PaginatedResponse<Transaction>> => {
    const response = await apiClient.get('/transactions/search', {
      params: filters,
    });
    return response.data;
  },

  // Get recent transactions
  getRecentTransactions: async (limit: number = 10): Promise<Transaction[]> => {
    const response = await apiClient.get('/transactions/recent', {
      params: { limit },
    });
    return response.data;
  },

  // Get transaction history for date range
  getTransactionHistory: async (
    accountNumber: string,
    startDate: string,
    endDate: string
  ): Promise<Transaction[]> => {
    const response = await apiClient.get(
      `/transactions/account/${accountNumber}/history`,
      {
        params: { startDate, endDate },
      }
    );
    return response.data;
  },

  // Get transaction statistics
  getTransactionStats: async (accountNumber: string, period: 'week' | 'month' | 'year'): Promise<{
    totalTransactions: number;
    totalDeposits: number;
    totalWithdrawals: number;
    totalTransfers: number;
    averageTransaction: number;
    chartData: any;
  }> => {
    const response = await apiClient.get(
      `/transactions/account/${accountNumber}/stats`,
      {
        params: { period },
      }
    );
    return response.data;
  },

  // Download transaction receipt
  downloadReceipt: async (transactionId: string): Promise<Blob> => {
    const response = await apiClient.get(
      `/transactions/${transactionId}/receipt`,
      {
        responseType: 'blob',
      }
    );
    return response.data;
  },
};

export default transactionsApi;
