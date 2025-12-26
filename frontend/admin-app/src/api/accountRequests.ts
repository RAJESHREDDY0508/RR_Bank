import apiClient from './client';

export interface AccountRequest {
  id: string;
  userId: string;
  accountType: string;
  initialDeposit: number;
  currency: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  requestNotes: string | null;
  adminNotes: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  accountId: string | null;
  createdAt: string;
}

export interface AccountRequestsResponse {
  content: AccountRequest[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export interface PendingCountResponse {
  pendingCount: number;
}

export const accountRequestsApi = {
  // Get pending account requests
  getPendingRequests: async (page = 0, size = 20): Promise<AccountRequestsResponse> => {
    const response = await apiClient.get('/account-requests', {
      params: { page, size }
    });
    return response.data;
  },

  // Get pending requests count
  getPendingCount: async (): Promise<PendingCountResponse> => {
    const response = await apiClient.get('/account-requests/count');
    return response.data;
  },

  // Approve account request
  approveRequest: async (requestId: string, notes?: string): Promise<AccountRequest> => {
    const response = await apiClient.post(`/account-requests/${requestId}/approve`, { notes });
    return response.data;
  },

  // Reject account request
  rejectRequest: async (requestId: string, notes: string): Promise<AccountRequest> => {
    const response = await apiClient.post(`/account-requests/${requestId}/reject`, { notes });
    return response.data;
  }
};

export default accountRequestsApi;
