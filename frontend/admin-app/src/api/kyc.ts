import apiClient from './client';
import { PageResponse, ApiResponse } from '../types';

export interface KycCustomer {
  id: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phoneNumber?: string;
  kycStatus: string;
  kycRejectionReason?: string;
  kycVerifiedAt?: string;
  createdAt: string;
}

export interface KycStats {
  pending: number;
  approved: number;
  rejected: number;
}

export const kycApi = {
  // Get pending KYC requests
  getPendingKycRequests: async (page: number = 0, size: number = 20): Promise<PageResponse<KycCustomer>> => {
    try {
      const response = await apiClient.get<ApiResponse<PageResponse<KycCustomer>>>(
        `/admin/kyc/pending?page=${page}&size=${size}`
      );
      return response.data.data;
    } catch (error) {
      console.error('Error fetching pending KYC requests:', error);
      return { content: [], totalElements: 0, totalPages: 0, size, number: page, first: true, last: true, empty: true };
    }
  },

  // Get KYC stats
  getKycStats: async (): Promise<KycStats> => {
    try {
      const response = await apiClient.get<ApiResponse<KycStats>>('/admin/kyc/stats');
      return response.data.data || { pending: 0, approved: 0, rejected: 0 };
    } catch (error) {
      console.error('Error fetching KYC stats:', error);
      return { pending: 0, approved: 0, rejected: 0 };
    }
  },

  // Get customers by KYC status
  getCustomersByKycStatus: async (
    status: string,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<KycCustomer>> => {
    try {
      const response = await apiClient.get<ApiResponse<PageResponse<KycCustomer>>>(
        `/admin/kyc/status/${status}?page=${page}&size=${size}`
      );
      return response.data.data;
    } catch (error) {
      console.error('Error fetching customers by KYC status:', error);
      return { content: [], totalElements: 0, totalPages: 0, size, number: page, first: true, last: true, empty: true };
    }
  },

  // Get customer KYC details
  getCustomerKycDetails: async (customerId: string): Promise<KycCustomer | null> => {
    try {
      const response = await apiClient.get<ApiResponse<KycCustomer>>(`/admin/kyc/${customerId}`);
      return response.data.data;
    } catch (error) {
      console.error('Error fetching customer KYC details:', error);
      return null;
    }
  },

  // Approve KYC by customer ID
  approveKyc: async (customerId: string): Promise<KycCustomer> => {
    const response = await apiClient.post<ApiResponse<KycCustomer>>(
      `/admin/kyc/${customerId}/approve`
    );
    return response.data.data;
  },

  // Approve KYC by user ID
  approveKycByUserId: async (userId: string): Promise<KycCustomer> => {
    const response = await apiClient.post<ApiResponse<KycCustomer>>(
      `/admin/kyc/user/${userId}/approve`
    );
    return response.data.data;
  },

  // Reject KYC by customer ID
  rejectKyc: async (customerId: string, reason?: string): Promise<KycCustomer> => {
    const response = await apiClient.post<ApiResponse<KycCustomer>>(
      `/admin/kyc/${customerId}/reject`,
      { reason }
    );
    return response.data.data;
  },

  // Reject KYC by user ID
  rejectKycByUserId: async (userId: string, reason?: string): Promise<KycCustomer> => {
    const response = await apiClient.post<ApiResponse<KycCustomer>>(
      `/admin/kyc/user/${userId}/reject`,
      { reason }
    );
    return response.data.data;
  },
};

export default kycApi;
