import apiClient from './client';
import type { Customer, PaginatedResponse } from '../types';

export interface CustomerSearchParams {
  searchTerm?: string;
  status?: string;
  kycStatus?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export const customersApi = {
  getCustomers: async (params: CustomerSearchParams = {}): Promise<PaginatedResponse<Customer>> => {
    const response = await apiClient.get('/customers', { params });
    return response.data;
  },

  getCustomer: async (userId: string): Promise<Customer> => {
    const response = await apiClient.get(`/customers/${userId}`);
    return response.data;
  },

  updateCustomerStatus: async (userId: string, status: string): Promise<void> => {
    await apiClient.patch(`/customers/${userId}/status`, { status });
  },

  updateKycStatus: async (userId: string, kycStatus: string): Promise<void> => {
    await apiClient.patch(`/customers/${userId}/kyc-status`, { kycStatus });
  },

  getCustomerAccounts: async (userId: string): Promise<any[]> => {
    const response = await apiClient.get(`/customers/${userId}/accounts`);
    return response.data;
  },

  getCustomerTransactions: async (userId: string, page = 0, size = 20): Promise<PaginatedResponse<any>> => {
    const response = await apiClient.get(`/customers/${userId}/transactions`, {
      params: { page, size },
    });
    return response.data;
  },

  suspendCustomer: async (userId: string, reason: string): Promise<void> => {
    await apiClient.post(`/customers/${userId}/suspend`, { reason });
  },

  reactivateCustomer: async (userId: string): Promise<void> => {
    await apiClient.post(`/customers/${userId}/reactivate`);
  },
};
