import api from './api';

export const authService = {
  // Register new user
  register: async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
  },

  // Login user
  login: async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    const { accessToken, refreshToken, ...user } = response.data;
    
    // Store tokens and user info
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
    
    return response.data;
  },

  // Logout user
  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  },

  // Get current user
  getCurrentUser: () => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  // Check if user is authenticated
  isAuthenticated: () => {
    return !!localStorage.getItem('accessToken');
  },
};

export const accountService = {
  // Get all accounts for current user
  getAccounts: async () => {
    const response = await api.get('/accounts');
    return response.data;
  },

  // Get account by account number
  getAccountByNumber: async (accountNumber) => {
    const response = await api.get(`/accounts/${accountNumber}`);
    return response.data;
  },

  // Get account balance
  getBalance: async (accountNumber) => {
    const response = await api.get(`/accounts/${accountNumber}/balance`);
    return response.data;
  },

  // Deposit money
  deposit: async (accountNumber, amount) => {
    const response = await api.post(`/accounts/${accountNumber}/deposit`, { amount });
    return response.data;
  },

  // Withdraw money
  withdraw: async (accountNumber, amount) => {
    const response = await api.post(`/accounts/${accountNumber}/withdraw`, { amount });
    return response.data;
  },
};

export const transactionService = {
  // Get transactions for an account
  getTransactions: async (accountNumber, page = 0, size = 10) => {
    const response = await api.get(`/transactions/account/${accountNumber}`, {
      params: { page, size },
    });
    return response.data;
  },

  // Transfer money
  transfer: async (transferData) => {
    const response = await api.post('/transactions/transfer', transferData);
    return response.data;
  },

  // Get transaction by ID
  getTransactionById: async (transactionId) => {
    const response = await api.get(`/transactions/${transactionId}`);
    return response.data;
  },
};
