// User Types
export interface User {
  userId: string;
  username: string;
  email: string;
  role: 'ADMIN' | 'SUPER_ADMIN' | 'SUPPORT';
  createdAt: string;
  lastLogin?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
}

// Customer Types
export interface Customer {
  userId: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  kycStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  accountStatus: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  createdAt: string;
  totalAccounts: number;
  totalBalance: number;
}

// Account Types
export interface Account {
  accountNumber: string;
  userId: string;
  accountType: 'SAVINGS' | 'CHECKING' | 'CREDIT';
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  createdAt: string;
  lastTransactionDate?: string;
}

// Transaction Types
export interface Transaction {
  transactionId: string;
  accountNumber: string;
  transactionType: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'PAYMENT';
  amount: number;
  currency: string;
  description: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  timestamp: string;
  fromAccount?: string;
  toAccount?: string;
  metadata?: Record<string, any>;
}

// Fraud Alert Types
export interface FraudAlert {
  alertId: string;
  transactionId: string;
  accountNumber: string;
  customerId: string;
  alertType: 'SUSPICIOUS_ACTIVITY' | 'LARGE_TRANSACTION' | 'UNUSUAL_PATTERN' | 'MULTIPLE_FAILED_ATTEMPTS';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'NEW' | 'INVESTIGATING' | 'RESOLVED' | 'FALSE_POSITIVE';
  description: string;
  detectedAt: string;
  resolvedAt?: string;
  resolvedBy?: string;
  notes?: string;
}

// Payment Types
export interface Payment {
  paymentId: string;
  accountNumber: string;
  paymentType: 'BILL' | 'LOAN' | 'CREDIT_CARD' | 'MERCHANT';
  amount: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  scheduledDate: string;
  processedDate?: string;
  recipient: string;
  description: string;
}

// Audit Log Types
export interface AuditLog {
  logId: string;
  userId: string;
  username: string;
  action: string;
  resource: string;
  resourceId: string;
  details: string;
  ipAddress: string;
  timestamp: string;
  status: 'SUCCESS' | 'FAILURE';
}

// Dashboard Metrics Types
export interface DashboardMetrics {
  totalCustomers: number;
  activeCustomers: number;
  totalAccounts: number;
  totalBalance: number;
  todayTransactions: number;
  todayTransactionVolume: number;
  pendingFraudAlerts: number;
  systemHealth: 'HEALTHY' | 'WARNING' | 'CRITICAL';
}

// Report Types
export interface Report {
  reportId: string;
  reportType: 'TRANSACTIONS' | 'CUSTOMERS' | 'FRAUD' | 'FINANCIAL';
  generatedBy: string;
  generatedAt: string;
  parameters: Record<string, any>;
  format: 'PDF' | 'EXCEL' | 'CSV';
  status: 'GENERATING' | 'COMPLETED' | 'FAILED';
  downloadUrl?: string;
}

// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}
