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
    const { accessToken, refreshToken, ...user } = response.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
    return response.data;
  },

  login: async (credentials) => {
    // ✅ FIX: Map frontend 'email' field to backend 'usernameOrEmail' field
    const loginPayload = {
      usernameOrEmail: credentials.email || credentials.usernameOrEmail,
      password: credentials.password
    };
    
    const response = await api.post('/auth/login', loginPayload);
    const { accessToken, refreshToken, ...user } = response.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
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
// ACCOUNT SERVICE - Enhanced with account requests
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

  // Get account balance
  getBalance: async (accountId) => {
    const response = await api.get(`/accounts/${accountId}/balance`);
    return response.data;
  },

  // Create new account (requires customerId)
  createAccount: async (customerId, accountType, initialBalance = 0, currency = 'USD') => {
    const response = await api.post('/accounts', {
      customerId,
      accountType,
      initialBalance,
      currency
    });
    return response.data;
  },

  // Submit account opening request (goes through approval workflow)
  requestAccount: async (accountType, initialDeposit = 0, currency = 'USD', notes = '') => {
    const response = await api.post('/accounts/requests', {
      accountType,
      initialDeposit,
      currency,
      notes
    });
    return response.data;
  },

  // Get user's account requests
  getMyRequests: async () => {
    const response = await api.get('/accounts/requests');
    return response.data;
  },

  // Cancel pending account request
  cancelRequest: async (requestId) => {
    const response = await api.delete(`/accounts/requests/${requestId}`);
    return response.data;
  },

  // Close account (balance must be zero)
  closeAccount: async (accountId) => {
    const response = await api.patch(`/accounts/${accountId}/close`);
    return response.data;
  },

  // Get accounts by customer ID
  getAccountsByCustomer: async (customerId) => {
    const response = await api.get(`/accounts/customer/${customerId}`);
    return response.data;
  }
};

// ============================================================
// TRANSACTION SERVICE - Enhanced with deposit/withdraw
// ============================================================
export const transactionService = {
  // Deposit money
  deposit: async (accountId, amount, description = '') => {
    const response = await api.post(`/accounts/${accountId}/deposit`, {
      amount,
      description
    });
    return response.data;
  },

  // Withdraw money
  withdraw: async (accountId, amount, description = '') => {
    const response = await api.post(`/accounts/${accountId}/withdraw`, {
      amount,
      description
    });
    return response.data;
  },

  // Transfer money between accounts
  transfer: async (fromAccountId, toAccountId, amount, description = '') => {
    const idempotencyKey = uuidv4();
    const response = await api.post('/transactions/transfer',
      { fromAccountId, toAccountId, amount, description },
      { headers: { 'Idempotency-Key': idempotencyKey } }
    );
    return response.data;
  },

  // Get transactions for account (paginated)
  getTransactions: async (accountId, page = 0, size = 20) => {
    const response = await api.get(`/transactions/account/${accountId}`, {
      params: { page, size }
    });
    return response.data;
  },

  // Get transaction by ID
  getTransactionById: async (transactionId) => {
    const response = await api.get(`/transactions/${transactionId}`);
    return response.data;
  },

  // Get recent transactions
  getRecentTransactions: async (accountId, limit = 10) => {
    const response = await api.get(`/transactions/account/${accountId}/recent`, {
      params: { limit }
    });
    return response.data;
  },

  // Get transaction statistics
  getStats: async (accountId) => {
    const response = await api.get(`/transactions/account/${accountId}/stats`);
    return response.data;
  },

  // Get user's transaction limits
  getLimits: async () => {
    const response = await api.get('/transactions/limits');
    return response.data;
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
