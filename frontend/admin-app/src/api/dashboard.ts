import apiClient from './client';

export interface DashboardStats {
  totalCustomers: number;
  activeCustomers: number;
  newCustomersToday: number;
  customerGrowthPercent: number;
  totalAccounts: number;
  activeAccounts: number;
  frozenAccounts: number;
  totalBalance: number;
  todayDeposits: number;
  todayWithdrawals: number;
  todayTransfers: number;
  totalTransactions: number;
  todayTransactions: number;
  transactionGrowthPercent: number;
  pendingFraudAlerts: number;
  pendingAccountRequests: number;
  pendingKycReviews: number;
  activeAdmins: number;
  adminActionsToday: number;
}

export interface TransactionChartData {
  dates: string[];
  deposits: number[];
  withdrawals: number[];
  transfers: number[];
  amounts: number[];
}

export interface CustomerGrowthData {
  dates: string[];
  newCustomers: number[];
  cumulativeCustomers: number[];
}

export interface RecentActivity {
  id: string;
  type: string;
  description: string;
  entityType: string;
  entityId: string;
  status: string;
  performedBy: string;
  timestamp: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

/**
 * Type guard to validate DashboardStats response
 */
function isValidDashboardStats(data: unknown): data is DashboardStats {
  if (!data || typeof data !== 'object') return false;
  const stats = data as Record<string, unknown>;
  return (
    typeof stats.totalCustomers === 'number' &&
    typeof stats.totalAccounts === 'number' &&
    typeof stats.totalBalance === 'number'
  );
}

export const dashboardApi = {
  getStats: async (): Promise<{ data: DashboardStats }> => {
    const requestId = `stats-${Date.now()}`;
    console.log(`[${requestId}] Calling GET /admin/dashboard/stats`);
    
    try {
      const response = await apiClient.get<ApiResponse<DashboardStats>>('/admin/dashboard/stats');
      console.log(`[${requestId}] Response status:`, response.status);
      console.log(`[${requestId}] Response data:`, response.data);
      
      // Validate response structure
      if (!response.data) {
        throw new Error('Empty response from server');
      }
      
      // Handle wrapped response (ApiResponse format)
      const statsData = response.data.data || response.data;
      
      if (!isValidDashboardStats(statsData)) {
        console.error(`[${requestId}] Invalid response format:`, statsData);
        throw new Error('Invalid dashboard stats format - missing required fields');
      }
      
      return { data: statsData as DashboardStats };
    } catch (error: any) {
      console.error(`[${requestId}] Error:`, error);
      console.error(`[${requestId}] Status:`, error.response?.status);
      console.error(`[${requestId}] Response:`, error.response?.data);
      throw error;
    }
  },

  getTransactionChartData: async (days: number = 30): Promise<{ data: TransactionChartData }> => {
    const response = await apiClient.get<ApiResponse<TransactionChartData>>(
      `/admin/dashboard/charts/transactions?days=${days}`
    );
    return { data: response.data.data };
  },

  getCustomerGrowthData: async (days: number = 30): Promise<{ data: CustomerGrowthData }> => {
    const response = await apiClient.get<ApiResponse<CustomerGrowthData>>(
      `/admin/dashboard/charts/customers?days=${days}`
    );
    return { data: response.data.data };
  },

  getRecentActivity: async (): Promise<{ data: RecentActivity[] }> => {
    const response = await apiClient.get<ApiResponse<RecentActivity[]>>('/admin/dashboard/recent-activity');
    return { data: response.data.data || [] };
  },
};

export default dashboardApi;
