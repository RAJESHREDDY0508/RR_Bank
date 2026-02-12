import apiClient from './client';
import { PageResponse, ApiResponse, Account, AccountStatus } from '../types';

export interface AccountTransaction {
  id: string;
  transactionReference: string;
  transactionType: string;
  status: string;
  amount: number;
  currency: string;
  fromAccountId?: string;
  toAccountId?: string;
  description?: string;
  createdAt: string;
  completedAt?: string;
}

export interface AccountFilters {
  type?: string;
  accountType?: string;
  status?: string;
  search?: string;
  page?: number;
  size?: number;
}

export const accountsApi = {
  getAccounts: async (filters: AccountFilters = {}): Promise<PageResponse<Account>> => {
    const params = new URLSearchParams();
    if (filters.type) params.append('type', filters.type);
    if (filters.accountType) params.append('accountType', filters.accountType);
    if (filters.status) params.append('status', filters.status);
    if (filters.search) params.append('search', filters.search);
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());

    const response = await apiClient.get<ApiResponse<PageResponse<Account>>>(
      `/admin/accounts?${params.toString()}`
    );
    return response.data.data;
  },

  getAccount: async (id: string): Promise<Account> => {
    const response = await apiClient.get<ApiResponse<Account>>(`/admin/accounts/${id}`);
    return response.data.data;
  },

  updateAccountStatus: async (id: string, action: string, reason?: string): Promise<Account> => {
    const response = await apiClient.put<ApiResponse<Account>>(
      `/admin/accounts/${id}/status`,
      { action, reason }
    );
    return response.data.data;
  },

  getAccountTransactions: async (
    id: string,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<AccountTransaction>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<AccountTransaction>>>(
      `/admin/accounts/${id}/transactions?page=${page}&size=${size}`
    );
    return response.data.data;
  },

  freezeAccount: async (id: string, reason: string): Promise<Account> => {
    return accountsApi.updateAccountStatus(id, 'FREEZE', reason);
  },

  unfreezeAccount: async (id: string, reason?: string): Promise<Account> => {
    return accountsApi.updateAccountStatus(id, 'UNFREEZE', reason || 'Account unfrozen by admin');
  },

  closeAccount: async (id: string, reason: string): Promise<Account> => {
    return accountsApi.updateAccountStatus(id, 'CLOSE', reason);
  },
};

export default accountsApi;
