// Re-export RBAC types
export * from './rbac';

// Common types
export interface PageResponse<T> {
  content: T[];
  page?: number; // Optional for backward compatibility
  number: number; // Spring Data Page uses 'number' for page number
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean; // Spring Data Page includes 'empty' field
  hasNext?: boolean;
  hasPrevious?: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  code?: string;
  errorCode?: string;
  message: string;
  path: string;
  requestId?: string;
  details?: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

// Chart types
export interface ChartData {
  labels: string[];
  datasets: ChartDataset[];
}

export interface ChartDataset {
  label: string;
  data: number[];
  borderColor?: string;
  backgroundColor?: string;
  fill?: boolean;
}

// Status types
export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'LOCKED';
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED' | 'PENDING';
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
export type FraudAlertStatus = 'PENDING' | 'CONFIRMED_FRAUD' | 'FALSE_POSITIVE' | 'NEEDS_REVIEW';
export type KycStatus = 'PENDING' | 'VERIFIED' | 'REJECTED' | 'EXPIRED' | 'IN_PROGRESS';

// Utility types
export type SortDirection = 'asc' | 'desc';

export interface PaginationParams {
  page: number;
  size: number;
  sortBy?: string;
  sortDir?: SortDirection;
}

export interface DateRange {
  startDate: string;
  endDate: string;
}

// Customer type
export interface Customer {
  id: string;
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName?: string;
  phoneNumber?: string;
  phone?: string;
  status: CustomerStatus;
  kycStatus: KycStatus;
  customerSegment?: string;
  dateOfBirth?: string;
  address?: {
    line1: string;
    line2?: string;
    city: string;
    state: string;
    postalCode: string;
    country: string;
  };
  accountCount?: number;
  totalBalance?: number;
  createdAt: string;
  lastLogin?: string;
}

// Account type
export interface Account {
  id: string;
  accountNumber: string;
  accountType: string;
  status: AccountStatus;
  currency: string;
  balance: number;
  availableBalance: number;
  userId: string;
  customerName?: string;
  customerEmail?: string;
  createdAt: string;
  updatedAt?: string;
  frozenAt?: string;
  frozenReason?: string;
}

// Account Request type
export interface AccountRequest {
  id: string;
  userId: string;
  customerName: string;
  customerEmail?: string;
  accountType: string;
  currency: string;
  initialDeposit?: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reason?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  createdAt: string;
}

// Transaction type
export interface Transaction {
  id: string;
  transactionReference: string;
  transactionType: string;
  status: TransactionStatus;
  amount: number;
  currency: string;
  fromAccountId?: string;
  fromAccountNumber?: string;
  toAccountId?: string;
  toAccountNumber?: string;
  description?: string;
  failureReason?: string;
  initiatedBy?: string;
  initiatedByName?: string;
  createdAt: string;
  completedAt?: string;
}

// FraudAlert type
export interface FraudAlert {
  id: string;
  accountId: string;
  accountNumber: string;
  userId: string;
  customerName: string;
  transactionId?: string;
  transactionRef?: string;
  eventType: string;
  decision: string;
  reason: string;
  riskScore: number;
  status: FraudAlertStatus;
  resolvedBy?: string;
  resolvedAt?: string;
  resolutionNotes?: string;
  createdAt: string;
}

// AuditLog type
export interface AuditLog {
  id: string;
  adminUserId?: string;
  adminUsername: string;
  action: string;
  actionType?: string;
  entityType?: string;
  entityId?: string;
  description?: string;
  oldValue?: string;
  newValue?: string;
  ipAddress?: string;
  userAgent?: string;
  status: string;
  errorMessage?: string;
  createdAt: string;
}

// Statement type
export interface Statement {
  id: string;
  accountId: string;
  accountNumber: string;
  userId: string;
  customerName?: string;
  statementType: 'MONTHLY' | 'QUARTERLY' | 'ANNUAL' | 'CUSTOM';
  startDate: string;
  endDate: string;
  generatedAt: string;
  generatedBy?: string;
  status: 'PENDING' | 'GENERATED' | 'FAILED';
  downloadUrl?: string;
}

// Payment type
export interface Payment {
  id: string;
  paymentReference: string;
  paymentType: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  amount: number;
  currency: string;
  fromAccountId?: string;
  fromAccountNumber?: string;
  beneficiaryName?: string;
  beneficiaryAccount?: string;
  beneficiaryBank?: string;
  description?: string;
  scheduledDate?: string;
  processedAt?: string;
  failureReason?: string;
  createdAt: string;
}

// Report type
export interface Report {
  id: string;
  name: string;
  type: string;
  description?: string;
  generatedAt: string;
  generatedBy: string;
  status: string;
  downloadUrl?: string;
  parameters?: Record<string, unknown>;
}

// Settings type
export interface SystemSettings {
  id: string;
  category: string;
  key: string;
  value: string;
  description?: string;
  dataType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON';
  isEditable: boolean;
  lastModifiedBy?: string;
  lastModifiedAt?: string;
}
