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
  // ✅ FIX: Use /admin/accounts path for admin operations
  getAccounts: async (params: AccountSearchParams = {}): Promise<PaginatedResponse<Account>> => {
    const response = await apiClient.get('/admin/accounts', { params });
    return response.data;
  },

  getAccount: async (accountId: string): Promise<Account> => {
    const response = await apiClient.get(`/admin/accounts/${accountId}`);
    return response.data;
  },

  freezeAccount: async (accountId: string, reason: string): Promise<void> => {
    await apiClient.post(`/admin/accounts/${accountId}/freeze`, { reason });
  },

  unfreezeAccount: async (accountId: string): Promise<void> => {
    await apiClient.post(`/admin/accounts/${accountId}/unfreeze`);
  },

  closeAccount: async (accountId: string, reason: string): Promise<void> => {
    await apiClient.post(`/admin/accounts/${accountId}/close`, { reason });
  },

  getAccountTransactions: async (
    accountId: string,
    page = 0,
    size = 20
  ): Promise<PaginatedResponse<any>> => {
    const response = await apiClient.get(`/admin/accounts/${accountId}/transactions`, {
      params: { page, size },
    });
    return response.data;
  },

  adjustBalance: async (
    accountId: string,
    amount: number,
    reason: string
  ): Promise<void> => {
    await apiClient.post(`/admin/accounts/${accountId}/adjust-balance`, {
      amount,
      reason,
    });
  },

  // Update account status
  updateStatus: async (accountId: string, status: string, reason?: string): Promise<void> => {
    await apiClient.patch(`/admin/accounts/${accountId}/status`, { status, reason });
  },
};
