import apiClient from './client';
import { ApiResponse, PageResponse } from '../types';

export interface AccountRequest {
  id: string;
  requestNumber?: string;
  userId?: string;
  customerName?: string;
  customerEmail?: string;
  customerPhone?: string;
  accountType: string;
  initialDeposit?: number;
  currency?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  idType?: string;
  idNumber?: string;
  address?: string;
  requestNotes?: string | null;
  adminNotes?: string | null;
  rejectionReason?: string | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  accountId?: string | null;
  createdAt: string;
}

export interface PendingCountResponse {
  pendingCount?: number;
  count?: number;
}

export const accountRequestsApi = {
  // Get pending account requests
  getPendingRequests: async (page = 0, size = 20): Promise<PageResponse<AccountRequest>> => {
    try {
      const response = await apiClient.get<ApiResponse<PageResponse<AccountRequest>>>('/admin/account-requests', {
        params: { page, size }
      });
      // Handle ApiResponse wrapper
      const data = response.data?.data || response.data;
      return {
        content: data?.content || [],
        totalElements: data?.totalElements || 0,
        totalPages: data?.totalPages || 0,
        size: data?.size || size,
        number: data?.number || page,
        first: data?.first ?? true,
        last: data?.last ?? true,
        empty: data?.empty ?? true
      };
    } catch (error) {
      console.error('Error fetching account requests:', error);
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size,
        number: page,
        first: true,
        last: true,
        empty: true
      };
    }
  },

  // Get pending requests count
  getPendingCount: async (): Promise<PendingCountResponse> => {
    try {
      const response = await apiClient.get<ApiResponse<{ count: number }>>('/admin/account-requests/count');
      // Handle ApiResponse wrapper
      const data = response.data?.data || response.data;
      return { 
        pendingCount: data?.count ?? 0,
        count: data?.count ?? 0
      };
    } catch (error) {
      console.error('Error fetching pending count:', error);
      return { pendingCount: 0, count: 0 };
    }
  },

  // Approve account request
  approveRequest: async (requestId: string, notes?: string): Promise<AccountRequest> => {
    const response = await apiClient.post<ApiResponse<AccountRequest>>(`/admin/account-requests/${requestId}/approve`, { notes });
    return response.data?.data || response.data;
  },

  // Reject account request
  rejectRequest: async (requestId: string, notes: string): Promise<AccountRequest> => {
    const response = await apiClient.post<ApiResponse<AccountRequest>>(`/admin/account-requests/${requestId}/reject`, { reason: notes });
    return response.data?.data || response.data;
  }
};

export default accountRequestsApi;
