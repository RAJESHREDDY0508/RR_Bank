// User Types
export interface User {
  userId: string;
  username: string;
  email: string;
  role: string;
  firstName?: string;
  lastName?: string;
  createdAt?: string;
  lastLogin?: string;
  status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
}

// Customer Types
export interface Customer {
  id?: string;
  userId: string;
  username?: string;
  email: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
  dateOfBirth?: string;
  kycStatus: 'PENDING' | 'IN_PROGRESS' | 'VERIFIED' | 'REJECTED' | 'EXPIRED';
  customerSegment?: 'REGULAR' | 'PREMIUM' | 'VIP' | 'CORPORATE';
  accountStatus?: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  createdAt: string;
  totalAccounts?: number;
  totalBalance?: number;
}

// Account Types
export interface Account {
  id?: string;
  accountNumber: string;
  customerId?: string;
  userId?: string;
  accountType: 'SAVINGS' | 'CHECKING' | 'CREDIT' | 'BUSINESS';
  balance: number;
  availableBalance?: number;
  currency: string;
  status: 'PENDING' | 'ACTIVE' | 'FROZEN' | 'CLOSED' | 'SUSPENDED';
  createdAt: string;
  openedAt?: string;
  lastTransactionDate?: string;
}

// Transaction Types
export interface Transaction {
  id?: string;
  transactionId?: string;
  transactionReference?: string;
  accountNumber?: string;
  fromAccountId?: string;
  toAccountId?: string;
  transactionType: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'PAYMENT' | 'FEE' | 'INTEREST' | 'REFUND' | 'ADJUSTMENT';
  amount: number;
  currency: string;
  description?: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REVERSED';
  timestamp?: string;
  createdAt?: string;
  completedAt?: string;
  fromAccount?: string;
  toAccount?: string;
  metadata?: Record<string, any>;
}

// Fraud Alert Types
export interface FraudAlert {
  id?: string;
  alertId?: string;
  transactionId?: string;
  accountId?: string;
  accountNumber?: string;
  customerId?: string;
  alertType?: 'SUSPICIOUS_ACTIVITY' | 'LARGE_TRANSACTION' | 'UNUSUAL_PATTERN' | 'MULTIPLE_FAILED_ATTEMPTS';
  eventType?: string;
  riskScore?: number;
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  severity?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status?: 'NEW' | 'INVESTIGATING' | 'RESOLVED' | 'FALSE_POSITIVE';
  resolved?: boolean;
  description?: string;
  flaggedReason?: string;
  detectedAt?: string;
  createdAt?: string;
  resolvedAt?: string;
  resolvedBy?: string;
  notes?: string;
  resolutionNotes?: string;
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
  id?: string;
  logId?: string;
  userId: string;
  username?: string;
  action: string;
  resource?: string;
  entityType?: string;
  resourceId?: string;
  entityId?: string;
  details?: string;
  oldValue?: any;
  newValue?: any;
  ipAddress?: string;
  timestamp?: string;
  createdAt?: string;
  status: 'SUCCESS' | 'FAILURE';
}

// Dashboard Metrics Types
export interface DashboardMetrics {
  totalCustomers: number;
  activeCustomers?: number;
  totalAccounts: number;
  activeAccounts?: number;
  totalTransactions?: number;
  totalBalance: number;
  todayTransactions: number;
  todayTransactionVolume?: number;
  pendingFraudAlerts?: number;
  pendingAccountRequests?: number;
  systemHealth?: 'HEALTHY' | 'WARNING' | 'CRITICAL';
}

// Report Types
export interface Report {
  id?: string;
  reportId?: string;
  reportType: 'TRANSACTIONS' | 'CUSTOMERS' | 'FRAUD' | 'FINANCIAL';
  generatedBy?: string;
  generatedAt?: string;
  createdAt?: string;
  parameters?: Record<string, any>;
  format: 'PDF' | 'EXCEL' | 'CSV';
  status: 'GENERATING' | 'COMPLETED' | 'FAILED';
  downloadUrl?: string;
}

// Account Request Types
export interface AccountRequest {
  id: string;
  userId: string;
  customerName?: string;
  customerEmail?: string;
  accountType: string;
  initialDeposit: number;
  currency: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  requestNotes?: string;
  adminNotes?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  accountId?: string;
  createdAt: string;
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
  number?: number;
  currentPage?: number;
  size?: number;
  pageSize?: number;
}
