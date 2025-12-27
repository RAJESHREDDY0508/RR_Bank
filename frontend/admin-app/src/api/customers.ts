import apiClient from './client';
import type { Customer, PaginatedResponse } from '../types';

export interface CustomerSearchParams {
  searchTerm?: string;
  search?: string;
  status?: string;
  kycStatus?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export const customersApi = {
  // Get all customers with pagination and search
  getCustomers: async (params: CustomerSearchParams = {}): Promise<PaginatedResponse<Customer>> => {
    const response = await apiClient.get('/admin/customers', { params });
    return response.data;
  },

  // Alias for getCustomers - used by Customers.tsx
  getAll: async (params: CustomerSearchParams = {}) => {
    const response = await apiClient.get('/admin/customers', { 
      params: {
        page: params.page || 0,
        size: params.size || 20,
        searchTerm: params.search || params.searchTerm,
        status: params.status,
        kycStatus: params.kycStatus
      }
    });
    return response;
  },

  getCustomer: async (customerId: string): Promise<Customer> => {
    const response = await apiClient.get(`/admin/customers/${customerId}`);
    return response.data;
  },

  updateCustomerStatus: async (customerId: string, status: string): Promise<void> => {
    await apiClient.patch(`/admin/customers/${customerId}/status`, { status });
  },

  updateKycStatus: async (customerId: string, kycStatus: string): Promise<void> => {
    await apiClient.patch(`/admin/customers/${customerId}/kyc-status`, { kycStatus });
  },

  getCustomerAccounts: async (customerId: string): Promise<any[]> => {
    const response = await apiClient.get(`/admin/customers/${customerId}/accounts`);
    return response.data;
  },

  getCustomerTransactions: async (customerId: string, page = 0, size = 20): Promise<PaginatedResponse<any>> => {
    const response = await apiClient.get(`/admin/customers/${customerId}/transactions`, {
      params: { page, size },
    });
    return response.data;
  },

  suspendCustomer: async (customerId: string, reason: string): Promise<void> => {
    await apiClient.post(`/admin/customers/${customerId}/suspend`, { reason });
  },

  reactivateCustomer: async (customerId: string): Promise<void> => {
    await apiClient.post(`/admin/customers/${customerId}/reactivate`);
  },

  // Approve KYC verification
  approveKyc: async (customerId: string, notes?: string): Promise<void> => {
    await apiClient.post(`/admin/customers/${customerId}/kyc/approve`, { notes });
  },

  // Reject KYC verification
  rejectKyc: async (customerId: string, reason: string): Promise<void> => {
    await apiClient.post(`/admin/customers/${customerId}/kyc/reject`, { reason });
  },
};

export default customersApi;
