import api from './api';
import { v4 as uuidv4 } from 'uuid';

// ============================================================
// AUTH SERVICE - Enhanced with password reset, email verification
// ============================================================
export const authService = {
  register: async (userData) => {
    // Clean phone number - remove formatting characters
    const cleanedData = {
      ...userData,
      phoneNumber: userData.phoneNumber ? userData.phoneNumber.replace(/[\s\-\(\)]/g, '') : undefined
    };
    
    const response = await api.post('/auth/register', cleanedData);
    const { accessToken, refreshToken, user, ...rest } = response.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    // Store user info - handle both nested and flat structures
    const userInfo = user || rest;
    localStorage.setItem('user', JSON.stringify(userInfo));
    return response.data;
  },

  login: async (credentials) => {
    // Map frontend 'email' field to backend 'usernameOrEmail' field
    const loginPayload = {
      usernameOrEmail: credentials.email || credentials.usernameOrEmail,
      password: credentials.password
    };
    
    const response = await api.post('/auth/login', loginPayload);
    const { accessToken, refreshToken, user, ...rest } = response.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    // Store user info - handle both nested and flat structures
    const userInfo = user || rest;
    localStorage.setItem('user', JSON.stringify(userInfo));
    return response.data;
  },

  logout: async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        await api.post('/auth/logout', null, {
          headers: { Authorization: `Bearer ${refreshToken}` }
        });
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },

  logoutEverywhere: async () => {
    try {
      await api.post('/auth/logout-all');
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },

  forgotPassword: async (email) => {
    const response = await api.post('/auth/forgot-password', { email });
    return response.data;
  },

  resetPassword: async (token, newPassword, confirmPassword) => {
    const response = await api.post('/auth/reset-password', {
      token,
      newPassword,
      confirmPassword
    });
    return response.data;
  },

  verifyEmail: async (token) => {
    const response = await api.post(`/auth/verify-email?token=${token}`);
    return response.data;
  },

  resendVerification: async (email) => {
    const response = await api.post(`/auth/resend-verification?email=${email}`);
    return response.data;
  },

  getCurrentUser: () => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  isAuthenticated: () => {
    return !!localStorage.getItem('accessToken');
  },

  isAdmin: () => {
    const user = authService.getCurrentUser();
    return user?.role === 'ADMIN' || user?.role === 'MANAGER';
  },

  getLoginHistory: async (limit = 10) => {
    const response = await api.get(`/auth/login-history?limit=${limit}`);
    return response.data;
  },

  getActiveSessions: async () => {
    const response = await api.get('/auth/sessions');
    return response.data;
  }
};

// ============================================================
// ACCOUNT SERVICE - Fixed to match backend endpoints
// ============================================================
export const accountService = {
  // Get all accounts for current user
  getAccounts: async () => {
    const response = await api.get('/accounts/me');
    return response.data;
  },

  // Get account by ID
  getAccountById: async (accountId) => {
    const response = await api.get(`/accounts/${accountId}`);
    return response.data;
  },

  // Get account by account number
  getAccountByNumber: async (accountNumber) => {
    // Remove dashes if present for lookup
    const cleanNumber = accountNumber.replace(/-/g, '');
    const response = await api.get(`/accounts/number/${accountNumber}`);
    return response.data;
  },

  // Get account balance
  getBalance: async (accountId) => {
    const response = await api.get(`/accounts/${accountId}/balance`);
    return response.data;
  },

  // Create new account (direct creation - for logged in users)
  createAccount: async (accountType, currency = 'USD') => {
    const user = authService.getCurrentUser();
    if (!user?.id) {
      throw new Error('User not authenticated');
    }
    const response = await api.post('/accounts', {
      userId: user.id,
      accountType,
      currency
    });
    return response.data;
  },

  // Request account opening (alias for createAccount with better UX)
  requestAccount: async (accountType, initialDeposit = 0, currency = 'USD', notes = '') => {
    // First create the account
    const account = await accountService.createAccount(accountType, currency);
    
    // If there's an initial deposit, make it
    if (initialDeposit > 0 && account?.id) {
      try {
        await transactionService.deposit(account.id, initialDeposit, 'Initial deposit');
      } catch (err) {
        console.error('Initial deposit failed:', err);
        // Account was created, just the deposit failed
      }
    }
    
    return account;
  },

  // Get user's account requests (returns accounts for now)
  getMyRequests: async () => {
    // Since we don't have a separate request system, return empty array
    return [];
  },

  // Cancel pending account request (no-op for now)
  cancelRequest: async (requestId) => {
    return { success: true };
  },

  // Close account (update status)
  closeAccount: async (accountId) => {
    const response = await api.patch(`/accounts/${accountId}/status`, null, {
      params: { status: 'CLOSED' }
    });
    return response.data;
  },

  // Get accounts by user ID
  getAccountsByUser: async (userId) => {
    const response = await api.get(`/accounts/user/${userId}`);
    return response.data;
  }
};

// ============================================================
// TRANSACTION SERVICE - Fixed to match backend endpoints
// ============================================================
export const transactionService = {
  // Deposit money - use /transactions/deposit endpoint
  deposit: async (accountId, amount, description = '') => {
    const idempotencyKey = uuidv4();
    const response = await api.post('/transactions/deposit',
      { 
        accountId, 
        amount: parseFloat(amount),
        description 
      },
      { headers: { 'Idempotency-Key': idempotencyKey } }
    );
    return response.data;
  },

  // Withdraw money - use /transactions/withdraw endpoint
  withdraw: async (accountId, amount, description = '') => {
    const idempotencyKey = uuidv4();
    const response = await api.post('/transactions/withdraw',
      { 
        accountId, 
        amount: parseFloat(amount),
        description 
      },
      { headers: { 'Idempotency-Key': idempotencyKey } }
    );
    return response.data;
  },

  // Transfer money between accounts
  transfer: async (fromAccountId, toAccountId, amount, description = '') => {
    const idempotencyKey = uuidv4();
    const response = await api.post('/transactions/transfer',
      { 
        fromAccountId, 
        toAccountId, 
        amount: parseFloat(amount),
        description 
      },
      { headers: { 'Idempotency-Key': idempotencyKey } }
    );
    return response.data;
  },

  // Transfer money by account number
  transferByAccountNumber: async (fromAccountId, toAccountNumber, amount, description = '') => {
    // First lookup the account by number
    const targetAccount = await accountService.getAccountByNumber(toAccountNumber);
    if (!targetAccount?.id) {
      throw new Error('Destination account not found');
    }
    // Then do the transfer
    return transactionService.transfer(fromAccountId, targetAccount.id, amount, description);
  },

  // Get transactions for account (paginated) with optional date filters
  getTransactions: async (accountId, page = 0, size = 20, filters = {}) => {
    const params = { page, size };
    
    if (filters.startDate) {
      params.startDate = filters.startDate;
    }
    if (filters.endDate) {
      params.endDate = filters.endDate;
    }
    if (filters.type) {
      params.type = filters.type;
    }
    
    const response = await api.get(`/transactions/account/${accountId}`, { params });
    return response.data;
  },

  // Get transaction by ID
  getTransactionById: async (transactionId) => {
    const response = await api.get(`/transactions/${transactionId}`);
    return response.data;
  },

  // Get transaction by reference
  getTransactionByReference: async (reference) => {
    const response = await api.get(`/transactions/reference/${reference}`);
    return response.data;
  },

  // Get recent transactions (convenience method)
  getRecentTransactions: async (accountId, limit = 10) => {
    const response = await api.get(`/transactions/account/${accountId}`, {
      params: { page: 0, size: limit }
    });
    return response.data?.content || response.data || [];
  },

  // Export transactions as CSV
  exportTransactions: async (accountId, startDate, endDate) => {
    const params = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    
    const response = await api.get(`/transactions/account/${accountId}/export`, {
      params,
      responseType: 'blob'
    });
    return response.data;
  },

  // Get transaction statistics (mock for now)
  getStats: async (accountId) => {
    // This would need a backend endpoint
    return {
      totalDeposits: 0,
      totalWithdrawals: 0,
      totalTransfers: 0
    };
  },

  // Get user's transaction limits (mock for now)
  getLimits: async () => {
    // Return default limits
    return [{
      limitType: 'TRANSFER',
      perTransactionLimit: 10000,
      dailyLimit: 50000,
      monthlyLimit: 200000,
      remainingDaily: 50000,
      remainingMonthly: 200000
    }];
  },

  // Search transactions
  searchTransactions: async (filters = {}, page = 0, size = 20) => {
    const response = await api.get('/transactions/search', {
      params: { ...filters, page, size }
    });
    return response.data;
  }
};

// ============================================================
// ADMIN SERVICE - Account requests, fraud queue, user management
// ============================================================
export const adminService = {
  // Dashboard stats
  getDashboard: async () => {
    const response = await api.get('/admin/dashboard');
    return response.data;
  },

  // ---- Account Requests ----
  getPendingRequests: async (page = 0, size = 20) => {
    const response = await api.get('/admin/account-requests', {
      params: { page, size }
    });
    return response.data;
  },

  getPendingRequestsCount: async () => {
    const response = await api.get('/admin/account-requests/count');
    return response.data;
  },

  approveAccountRequest: async (requestId, notes = '') => {
    const response = await api.post(`/admin/account-requests/${requestId}/approve`, { notes });
    return response.data;
  },

  rejectAccountRequest: async (requestId, notes) => {
    const response = await api.post(`/admin/account-requests/${requestId}/reject`, { notes });
    return response.data;
  },

  // ---- Fraud Management ----
  getFraudQueue: async (page = 0, size = 20) => {
    const response = await api.get('/admin/fraud/queue', {
      params: { page, size }
    });
    return response.data;
  },

  approveFraudEvent: async (eventId, notes = '') => {
    const response = await api.post(`/admin/fraud/${eventId}/approve`, { notes });
    return response.data;
  },

  rejectFraudEvent: async (eventId, notes) => {
    const response = await api.post(`/admin/fraud/${eventId}/reject`, { notes });
    return response.data;
  },

  getFraudStats: async () => {
    const response = await api.get('/admin/fraud/stats');
    return response.data;
  },

  // ---- User Limits ----
  getUserLimits: async (userId) => {
    const response = await api.get(`/admin/users/${userId}/limits`);
    return response.data;
  },

  updateUserLimits: async (userId, limitType, dailyLimit, perTransactionLimit, monthlyLimit) => {
    const response = await api.put(`/admin/users/${userId}/limits`, null, {
      params: { limitType, dailyLimit, perTransactionLimit, monthlyLimit }
    });
    return response.data;
  }
};

// ============================================================
// CUSTOMER SERVICE
// ============================================================
export const customerService = {
  getProfile: async () => {
    const response = await api.get('/customers/profile');
    return response.data;
  },

  updateProfile: async (profileData) => {
    const response = await api.put('/customers/profile', profileData);
    return response.data;
  },

  getCustomerById: async (customerId) => {
    const response = await api.get(`/customers/${customerId}`);
    return response.data;
  },

  createCustomer: async (customerData) => {
    const response = await api.post('/customers', customerData);
    return response.data;
  },

  getCustomerByUserId: async (userId) => {
    const response = await api.get(`/customers/user/${userId}`);
    return response.data;
  },

  // KYC Status methods
  getKycStatus: async (userId) => {
    const response = await api.get(`/customers/user/${userId}/kyc-status`);
    return response.data;
  },

  getMyCustomerProfile: async () => {
    const response = await api.get('/customers/me');
    return response.data;
  }
};

// ============================================================
// NOTIFICATION SERVICE
// ============================================================
export const notificationService = {
  getNotifications: async (page = 0, size = 20) => {
    const response = await api.get('/notifications', {
      params: { page, size }
    });
    return response.data;
  },

  getUnreadCount: async () => {
    const response = await api.get('/notifications/unread/count');
    return response.data;
  },

  markAsRead: async (notificationId) => {
    const response = await api.put(`/notifications/${notificationId}/read`);
    return response.data;
  },

  markAllAsRead: async () => {
    const response = await api.put('/notifications/read-all');
    return response.data;
  }
};

// ============================================================
// STATEMENT SERVICE
// ============================================================
export const statementService = {
  getStatements: async (accountId, page = 0, size = 12) => {
    const response = await api.get(`/statements/account/${accountId}`, {
      params: { page, size }
    });
    return response.data;
  },

  downloadStatement: async (statementId) => {
    const response = await api.get(`/statements/${statementId}/download`, {
      responseType: 'blob'
    });
    return response.data;
  },

  generateStatement: async (accountId, startDate, endDate) => {
    const response = await api.post('/statements/generate', {
      accountId,
      startDate,
      endDate
    });
    return response.data;
  }
};

export default {
  authService,
  accountService,
  transactionService,
  adminService,
  customerService,
  notificationService,
  statementService
};
