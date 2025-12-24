// User types
export interface User {
  userId: string;
  username: string;
  email: string;
  fullName?: string;
  phoneNumber?: string;
  role: 'CUSTOMER' | 'ADMIN' | 'MANAGER';
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

// ✅ FIX: Standardized to use 'accessToken' (matches backend)
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
  expiresIn: number;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  phoneNumber?: string;
}

// Account types
export interface Account {
  accountNumber: string;
  accountType: 'SAVINGS' | 'CHECKING' | 'CREDIT' | 'LOAN';
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'INACTIVE' | 'FROZEN' | 'CLOSED';
  userId: string;
  createdAt: string;
  updatedAt: string;
  interestRate?: number;
  overdraftLimit?: number;
}

export interface AccountBalance {
  accountNumber: string;
  balance: number;
  availableBalance: number;
  currency: string;
  lastUpdated: string;
}

export interface DepositRequest {
  accountNumber: string;
  amount: number;
  description?: string;
}

export interface WithdrawRequest {
  accountNumber: string;
  amount: number;
  description?: string;
}

// Transaction types
export interface Transaction {
  transactionId: string;
  accountNumber: string;
  transactionType: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'PAYMENT' | 'FEE' | 'INTEREST';
  amount: number;
  balance: number;
  description: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  timestamp: string;
  referenceNumber?: string;
  toAccountNumber?: string;
  fromAccountNumber?: string;
}

export interface TransferRequest {
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: number;
  description?: string;
}

export interface TransactionFilter {
  accountNumber?: string;
  transactionType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
  page?: number;
  size?: number;
}

// Payment types
export interface Payment {
  paymentId: string;
  accountNumber: string;
  payeeType: 'BILLER' | 'MERCHANT' | 'INDIVIDUAL';
  payeeName: string;
  payeeAccountNumber?: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'SCHEDULED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  scheduledDate?: string;
  completedDate?: string;
  description?: string;
  referenceNumber?: string;
  recurring: boolean;
  recurringFrequency?: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentRequest {
  accountNumber: string;
  payeeType: 'BILLER' | 'MERCHANT' | 'INDIVIDUAL';
  payeeName: string;
  payeeAccountNumber?: string;
  amount: number;
  scheduledDate?: string;
  description?: string;
  recurring?: boolean;
  recurringFrequency?: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
}

// Statement types
export interface Statement {
  statementId: string;
  accountNumber: string;
  statementPeriod: string;
  startDate: string;
  endDate: string;
  openingBalance: number;
  closingBalance: number;
  totalDeposits: number;
  totalWithdrawals: number;
  transactionCount: number;
  generatedDate: string;
  downloadUrl?: string;
}

// Notification types
export interface Notification {
  notificationId: string;
  userId: string;
  type: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR' | 'TRANSACTION' | 'SECURITY';
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  actionUrl?: string;
}

// Settings types
export interface UserSettings {
  userId: string;
  theme: 'light' | 'dark' | 'auto';
  notifications: {
    email: boolean;
    sms: boolean;
    push: boolean;
    transactionAlerts: boolean;
    securityAlerts: boolean;
    marketingEmails: boolean;
  };
  security: {
    twoFactorEnabled: boolean;
    loginAlerts: boolean;
    biometricEnabled: boolean;
  };
  preferences: {
    language: string;
    currency: string;
    dateFormat: string;
    timeZone: string;
  };
}

// API Response types
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  error?: string;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Dashboard types
export interface DashboardData {
  totalBalance: number;
  accounts: Account[];
  recentTransactions: Transaction[];
  upcomingPayments: Payment[];
  notifications: Notification[];
  monthlySpending: { month: string; amount: number }[];
  categorySpending: { category: string; amount: number; percentage: number }[];
}

// Chart data types
export interface ChartData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    backgroundColor?: string | string[];
    borderColor?: string | string[];
    borderWidth?: number;
  }[];
}

// Form types
export interface FormError {
  field: string;
  message: string;
}
