import apiClient from './client';

export interface TransactionLimit {
  id: string;
  userId: string;
  limitType: 'TRANSFER' | 'WITHDRAWAL' | 'DEPOSIT' | 'PAYMENT' | 'ALL';
  dailyLimit: number;
  perTransactionLimit: number;
  monthlyLimit: number;
  remainingDaily: number;
  remainingMonthly: number;
  enabled: boolean;
}

export interface UserLimitsResponse {
  limits: TransactionLimit[];
}

export const userLimitsApi = {
  // Get user's transaction limits
  getUserLimits: async (userId: string): Promise<TransactionLimit[]> => {
    const response = await apiClient.get(`/users/${userId}/limits`);
    return response.data;
  },

  // Update user's transaction limits
  updateUserLimits: async (
    userId: string,
    limitType: string,
    dailyLimit?: number,
    perTransactionLimit?: number,
    monthlyLimit?: number
  ): Promise<TransactionLimit> => {
    const response = await apiClient.put(`/users/${userId}/limits`, null, {
      params: {
        limitType,
        dailyLimit,
        perTransactionLimit,
        monthlyLimit
      }
    });
    return response.data;
  }
};

export default userLimitsApi;
