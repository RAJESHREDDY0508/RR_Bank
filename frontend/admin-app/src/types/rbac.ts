/**
 * Banking-grade RBAC Types for RR-Bank Admin Console
 * Implements permission-based access control with full TypeScript support
 */

// All available permissions in the system (must match backend Permission enum)
export type Permission =
  | 'DASHBOARD_READ'
  | 'CUSTOMER_READ'
  | 'CUSTOMER_UPDATE_STATUS'
  | 'CUSTOMER_KYC_UPDATE'
  | 'CUSTOMER_NOTES_WRITE'
  | 'ACCOUNT_READ'
  | 'ACCOUNT_UPDATE_STATUS'
  | 'ACCOUNT_APPROVE_REQUESTS'
  | 'TXN_READ'
  | 'TXN_EXPORT'
  | 'TXN_REVERSAL_REQUEST'
  | 'PAYMENT_READ'
  | 'PAYMENT_MANAGE'
  | 'FRAUD_ALERT_READ'
  | 'FRAUD_ALERT_MANAGE'
  | 'FRAUD_RULES_MANAGE'
  | 'STATEMENT_READ'
  | 'STATEMENT_GENERATE'
  | 'AUDIT_READ'
  | 'ADMIN_USER_READ'
  | 'ADMIN_USER_MANAGE'
  | 'SETTINGS_READ'
  | 'SETTINGS_MANAGE'
  | 'RBAC_MANAGE';

// All system roles
export type RoleName =
  | 'SUPER_ADMIN'
  | 'SECURITY_ADMIN'
  | 'AUDITOR'
  | 'OPERATIONS_MANAGER'
  | 'KYC_COMPLIANCE'
  | 'FRAUD_ANALYST'
  | 'CUSTOMER_SUPPORT'
  | 'ADMIN'  // Legacy
  | 'SUPPORT'; // Legacy

// Role with its permissions
export interface Role {
  id: string;
  name: RoleName;
  description?: string;
  isSystemRole: boolean;
  permissions: Permission[];
}

// Admin user with RBAC information
export interface AdminUser {
  id: string;
  userId: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: RoleName[];
  permissions: Permission[];
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'LOCKED';
  department?: string;
  mustChangePassword: boolean;
  lastLogin?: string;
  createdAt?: string;
}

// Permission check options
export interface PermissionCheckOptions {
  requireAll?: boolean; // If true, user must have ALL permissions; if false, ANY permission
}

// Route permission configuration
export interface RoutePermission {
  path: string;
  permissions: Permission[];
  requireAll?: boolean;
  redirectTo?: string;
}

// Navigation item with permission requirements
export interface NavItem {
  id: string;
  label: string;
  path: string;
  icon: string;
  permissions: Permission[]; // Permissions required to see this item
  requireAll?: boolean;
  children?: NavItem[];
  badge?: number;
  badgeColor?: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
}

// Permission groups for UI organization
export const PERMISSION_GROUPS: Record<string, { label: string; permissions: Permission[] }> = {
  dashboard: {
    label: 'Dashboard',
    permissions: ['DASHBOARD_READ'],
  },
  customers: {
    label: 'Customer Management',
    permissions: ['CUSTOMER_READ', 'CUSTOMER_UPDATE_STATUS', 'CUSTOMER_KYC_UPDATE', 'CUSTOMER_NOTES_WRITE'],
  },
  accounts: {
    label: 'Account Management',
    permissions: ['ACCOUNT_READ', 'ACCOUNT_UPDATE_STATUS', 'ACCOUNT_APPROVE_REQUESTS'],
  },
  transactions: {
    label: 'Transaction Management',
    permissions: ['TXN_READ', 'TXN_EXPORT', 'TXN_REVERSAL_REQUEST'],
  },
  payments: {
    label: 'Payment Management',
    permissions: ['PAYMENT_READ', 'PAYMENT_MANAGE'],
  },
  fraud: {
    label: 'Fraud Management',
    permissions: ['FRAUD_ALERT_READ', 'FRAUD_ALERT_MANAGE', 'FRAUD_RULES_MANAGE'],
  },
  statements: {
    label: 'Statement Management',
    permissions: ['STATEMENT_READ', 'STATEMENT_GENERATE'],
  },
  audit: {
    label: 'Audit',
    permissions: ['AUDIT_READ'],
  },
  adminUsers: {
    label: 'Admin User Management',
    permissions: ['ADMIN_USER_READ', 'ADMIN_USER_MANAGE'],
  },
  settings: {
    label: 'Settings',
    permissions: ['SETTINGS_READ', 'SETTINGS_MANAGE'],
  },
  rbac: {
    label: 'RBAC Management',
    permissions: ['RBAC_MANAGE'],
  },
};

// Role descriptions for UI
export const ROLE_DESCRIPTIONS: Record<RoleName, string> = {
  SUPER_ADMIN: 'Full system access with all permissions',
  SECURITY_ADMIN: 'Security, fraud management, and admin user management',
  AUDITOR: 'Read-only access to all data for audit purposes',
  OPERATIONS_MANAGER: 'Operations, account management, and transaction oversight',
  KYC_COMPLIANCE: 'KYC verification and customer compliance',
  FRAUD_ANALYST: 'Fraud detection, analysis, and alert management',
  CUSTOMER_SUPPORT: 'Customer service and basic account viewing',
  ADMIN: 'Legacy admin role',
  SUPPORT: 'Legacy support role',
};

// Role display names for UI
export const ROLE_DISPLAY_NAMES: Record<RoleName, string> = {
  SUPER_ADMIN: 'Super Admin',
  SECURITY_ADMIN: 'Security Admin',
  AUDITOR: 'Auditor',
  OPERATIONS_MANAGER: 'Operations Manager',
  KYC_COMPLIANCE: 'KYC Compliance',
  FRAUD_ANALYST: 'Fraud Analyst',
  CUSTOMER_SUPPORT: 'Customer Support',
  ADMIN: 'Admin',
  SUPPORT: 'Support',
};

/**
 * Format role name for display
 * Converts SNAKE_CASE to Title Case
 */
export const formatRoleName = (role: RoleName | string): string => {
  // Check if we have a predefined display name
  if (ROLE_DISPLAY_NAMES[role as RoleName]) {
    return ROLE_DISPLAY_NAMES[role as RoleName];
  }
  
  // Fallback: Convert SNAKE_CASE to Title Case
  return role
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
};

/**
 * Format permission name for display
 * Converts SNAKE_CASE to Title Case
 */
export const formatPermissionName = (permission: Permission | string): string => {
  return permission
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
};

// Default role permissions mapping (for reference - actual permissions come from backend)
export const DEFAULT_ROLE_PERMISSIONS: Record<RoleName, Permission[]> = {
  SUPER_ADMIN: [
    'DASHBOARD_READ', 'CUSTOMER_READ', 'CUSTOMER_UPDATE_STATUS', 'CUSTOMER_KYC_UPDATE', 'CUSTOMER_NOTES_WRITE',
    'ACCOUNT_READ', 'ACCOUNT_UPDATE_STATUS', 'ACCOUNT_APPROVE_REQUESTS',
    'TXN_READ', 'TXN_EXPORT', 'TXN_REVERSAL_REQUEST',
    'PAYMENT_READ', 'PAYMENT_MANAGE',
    'FRAUD_ALERT_READ', 'FRAUD_ALERT_MANAGE', 'FRAUD_RULES_MANAGE',
    'STATEMENT_READ', 'STATEMENT_GENERATE',
    'AUDIT_READ',
    'ADMIN_USER_READ', 'ADMIN_USER_MANAGE',
    'SETTINGS_READ', 'SETTINGS_MANAGE',
    'RBAC_MANAGE',
  ],
  SECURITY_ADMIN: [
    'DASHBOARD_READ', 'CUSTOMER_READ', 'ACCOUNT_READ', 'TXN_READ',
    'FRAUD_ALERT_READ', 'FRAUD_ALERT_MANAGE', 'FRAUD_RULES_MANAGE',
    'ADMIN_USER_READ', 'ADMIN_USER_MANAGE',
    'SETTINGS_READ', 'SETTINGS_MANAGE',
    'AUDIT_READ',
  ],
  AUDITOR: [
    'DASHBOARD_READ', 'CUSTOMER_READ', 'ACCOUNT_READ', 'TXN_READ',
    'PAYMENT_READ', 'STATEMENT_READ', 'AUDIT_READ',
    'ADMIN_USER_READ', 'SETTINGS_READ', 'FRAUD_ALERT_READ',
  ],
  OPERATIONS_MANAGER: [
    'DASHBOARD_READ', 'CUSTOMER_READ', 'CUSTOMER_UPDATE_STATUS',
    'ACCOUNT_READ', 'ACCOUNT_UPDATE_STATUS', 'ACCOUNT_APPROVE_REQUESTS',
    'TXN_READ', 'TXN_EXPORT',
    'STATEMENT_READ', 'STATEMENT_GENERATE',
    'FRAUD_ALERT_READ', 'AUDIT_READ',
  ],
  KYC_COMPLIANCE: [
    'DASHBOARD_READ', 'CUSTOMER_READ', 'CUSTOMER_KYC_UPDATE', 'CUSTOMER_UPDATE_STATUS',
    'ACCOUNT_READ', 'TXN_READ',
    'STATEMENT_READ', 'AUDIT_READ', 'FRAUD_ALERT_READ',
  ],
  FRAUD_ANALYST: [
    'DASHBOARD_READ', 'CUSTOMER_READ', 'ACCOUNT_READ', 'TXN_READ',
    'FRAUD_ALERT_READ', 'FRAUD_ALERT_MANAGE',
    'TXN_REVERSAL_REQUEST', 'AUDIT_READ',
  ],
  CUSTOMER_SUPPORT: [
    'DASHBOARD_READ', 'CUSTOMER_READ', 'ACCOUNT_READ', 'TXN_READ',
    'PAYMENT_READ', 'STATEMENT_READ',
    'CUSTOMER_NOTES_WRITE', 'AUDIT_READ',
  ],
  // Legacy roles map to similar permissions
  ADMIN: [
    'DASHBOARD_READ', 'CUSTOMER_READ', 'CUSTOMER_UPDATE_STATUS',
    'ACCOUNT_READ', 'ACCOUNT_UPDATE_STATUS',
    'TXN_READ', 'FRAUD_ALERT_READ', 'FRAUD_ALERT_MANAGE',
    'AUDIT_READ',
  ],
  SUPPORT: [
    'DASHBOARD_READ', 'CUSTOMER_READ', 'ACCOUNT_READ', 'TXN_READ',
    'STATEMENT_READ',
  ],
};
