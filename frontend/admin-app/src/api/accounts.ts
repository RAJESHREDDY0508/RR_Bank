import apiClient from './client';
import type { Account, PaginatedResponse } from '../types';

export interface AccountSearchParams {
  searchTerm?: string;
  accountType?: string;
  status?: string;
  page?: number;
  size?: number;
}

export const accountsApi = {
  getAccounts: async (params: AccountSearchParams = {}): Promise<PaginatedResponse<Account>> => {
    const response = await apiClient.get('/accounts', { params });
    return response.data;
  },

  getAccount: async (accountNumber: string): Promise<Account> => {
    const response = await apiClient.get(`/accounts/${accountNumber}`);
    return response.data;
  },

  freezeAccount: async (accountNumber: string, reason: string): Promise<void> => {
    await apiClient.post(`/accounts/${accountNumber}/freeze`, { reason });
  },

  unfreezeAccount: async (accountNumber: string): Promise<void> => {
    await apiClient.post(`/accounts/${accountNumber}/unfreeze`);
  },

  closeAccount: async (accountNumber: string, reason: string): Promise<void> => {
    await apiClient.post(`/accounts/${accountNumber}/close`, { reason });
  },

  getAccountTransactions: async (
    accountNumber: string,
    page = 0,
    size = 20
  ): Promise<PaginatedResponse<any>> => {
    const response = await apiClient.get(`/accounts/${accountNumber}/transactions`, {
      params: { page, size },
    });
    return response.data;
  },

  adjustBalance: async (
    accountNumber: string,
    amount: number,
    reason: string
  ): Promise<void> => {
    await apiClient.post(`/accounts/${accountNumber}/adjust-balance`, {
      amount,
      reason,
    });
  },
};
