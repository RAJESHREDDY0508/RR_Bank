import apiClient from './client';
import { PageResponse, ApiResponse, Customer, CustomerStatus, KycStatus } from '../types';

export interface CustomerAccount {
  id: string;
  accountNumber: string;
  accountType: string;
  status: string;
  currency: string;
  balance: number;
  availableBalance: number;
  userId: string;
  customerName: string;
  createdAt: string;
}

export interface CustomerTransaction {
  id: string;
  transactionReference: string;
  transactionType: string;
  status: string;
  amount: number;
  currency: string;
  description?: string;
  createdAt: string;
  completedAt?: string;
}

export interface CustomerFilters {
  search?: string;
  status?: string;
  kycStatus?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export const customersApi = {
  getCustomers: async (filters: CustomerFilters = {}): Promise<PageResponse<Customer>> => {
    const params = new URLSearchParams();
    if (filters.search) params.append('search', filters.search);
    if (filters.status) params.append('status', filters.status);
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());
    if (filters.sortBy) params.append('sortBy', filters.sortBy);
    if (filters.sortDir) params.append('sortDir', filters.sortDir);

    const response = await apiClient.get<ApiResponse<PageResponse<Customer>>>(
      `/admin/customers?${params.toString()}`
    );
    return response.data.data;
  },

  getCustomer: async (id: string): Promise<Customer> => {
    const response = await apiClient.get<ApiResponse<Customer>>(`/admin/customers/${id}`);
    return response.data.data;
  },

  updateCustomerStatus: async (id: string, status: string, reason?: string): Promise<Customer> => {
    const response = await apiClient.put<ApiResponse<Customer>>(
      `/admin/customers/${id}/status`,
      { status, reason }
    );
    return response.data.data;
  },

  getCustomerAccounts: async (id: string): Promise<CustomerAccount[]> => {
    const response = await apiClient.get<ApiResponse<CustomerAccount[]>>(
      `/admin/customers/${id}/accounts`
    );
    return response.data.data;
  },

  getCustomerTransactions: async (
    id: string,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<CustomerTransaction>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<CustomerTransaction>>>(
      `/admin/customers/${id}/transactions?page=${page}&size=${size}`
    );
    return response.data.data;
  },
};

export default customersApi;
