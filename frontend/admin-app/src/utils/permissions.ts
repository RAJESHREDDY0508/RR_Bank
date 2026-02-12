/**
 * Permission utilities for RBAC implementation
 * Provides helper functions for permission checking throughout the admin app
 */

import { Permission, AdminUser, RoleName, DEFAULT_ROLE_PERMISSIONS } from '../types/rbac';

// Storage keys
const ADMIN_USER_KEY = 'adminUser';
const PERMISSIONS_KEY = 'userPermissions';

/**
 * Get current user from storage
 */
export const getCurrentUser = (): AdminUser | null => {
  try {
    const userJson = localStorage.getItem(ADMIN_USER_KEY);
    if (userJson) {
      return JSON.parse(userJson);
    }
    return null;
  } catch {
    return null;
  }
};

/**
 * Get current user's permissions
 */
export const getUserPermissions = (): Permission[] => {
  const user = getCurrentUser();
  if (!user) return [];
  
  // First try to get permissions directly from user object
  if (user.permissions && user.permissions.length > 0) {
    return user.permissions;
  }
  
  // Fall back to stored permissions
  try {
    const permsJson = localStorage.getItem(PERMISSIONS_KEY);
    if (permsJson) {
      return JSON.parse(permsJson);
    }
  } catch {
    // Ignore
  }
  
  // Fall back to role-based permissions
  return getPermissionsFromRoles(user.roles || []);
};

/**
 * Get permissions from role names (fallback for legacy support)
 */
export const getPermissionsFromRoles = (roles: RoleName[]): Permission[] => {
  const permissionSet = new Set<Permission>();
  
  roles.forEach(role => {
    const rolePermissions = DEFAULT_ROLE_PERMISSIONS[role];
    if (rolePermissions) {
      rolePermissions.forEach(p => permissionSet.add(p));
    }
  });
  
  return Array.from(permissionSet);
};

/**
 * Check if user has a specific permission
 */
export const hasPermission = (permission: Permission): boolean => {
  const user = getCurrentUser();
  if (!user) return false;
  
  // Super admin has all permissions
  if (user.roles?.includes('SUPER_ADMIN')) {
    return true;
  }
  
  const permissions = getUserPermissions();
  return permissions.includes(permission);
};

/**
 * Check if user has any of the specified permissions
 */
export const hasAnyPermission = (...permissions: Permission[]): boolean => {
  const user = getCurrentUser();
  if (!user) return false;
  
  // Super admin has all permissions
  if (user.roles?.includes('SUPER_ADMIN')) {
    return true;
  }
  
  const userPermissions = getUserPermissions();
  return permissions.some(p => userPermissions.includes(p));
};

/**
 * Check if user has all of the specified permissions
 */
export const hasAllPermissions = (...permissions: Permission[]): boolean => {
  const user = getCurrentUser();
  if (!user) return false;
  
  // Super admin has all permissions
  if (user.roles?.includes('SUPER_ADMIN')) {
    return true;
  }
  
  const userPermissions = getUserPermissions();
  return permissions.every(p => userPermissions.includes(p));
};

/**
 * Check if user has a specific role
 */
export const hasRole = (role: RoleName): boolean => {
  const user = getCurrentUser();
  if (!user) return false;
  return user.roles?.includes(role) ?? false;
};

/**
 * Check if user has any of the specified roles
 */
export const hasAnyRole = (...roles: RoleName[]): boolean => {
  const user = getCurrentUser();
  if (!user) return false;
  return roles.some(r => user.roles?.includes(r));
};

/**
 * Check if user is super admin
 */
export const isSuperAdmin = (): boolean => {
  return hasRole('SUPER_ADMIN');
};

/**
 * Check route access based on required permissions
 */
export const canAccessRoute = (
  requiredPermissions: Permission[],
  requireAll: boolean = false
): boolean => {
  if (requiredPermissions.length === 0) return true;
  
  const user = getCurrentUser();
  if (!user) return false;
  
  // Super admin has all permissions
  if (user.roles?.includes('SUPER_ADMIN')) {
    return true;
  }
  
  const userPermissions = getUserPermissions();
  
  if (requireAll) {
    return requiredPermissions.every(p => userPermissions.includes(p));
  } else {
    return requiredPermissions.some(p => userPermissions.includes(p));
  }
};

/**
 * Filter navigation items based on user permissions
 */
export const filterNavItemsByPermission = <T extends { permissions?: Permission[]; requireAll?: boolean; children?: T[] }>(
  items: T[]
): T[] => {
  const user = getCurrentUser();
  if (!user) return [];
  
  const isSuperAdminUser = user.roles?.includes('SUPER_ADMIN');
  
  return items.filter(item => {
    if (isSuperAdminUser) return true;
    if (!item.permissions || item.permissions.length === 0) return true;
    
    return canAccessRoute(item.permissions, item.requireAll);
  }).map(item => {
    if (item.children && item.children.length > 0) {
      return {
        ...item,
        children: filterNavItemsByPermission(item.children),
      };
    }
    return item;
  });
};

/**
 * Get user's primary role (for display purposes)
 */
export const getPrimaryRole = (): RoleName | null => {
  const user = getCurrentUser();
  if (!user || !user.roles || user.roles.length === 0) return null;
  
  // Priority order for display
  const rolePriority: RoleName[] = [
    'SUPER_ADMIN',
    'SECURITY_ADMIN',
    'OPERATIONS_MANAGER',
    'KYC_COMPLIANCE',
    'FRAUD_ANALYST',
    'AUDITOR',
    'CUSTOMER_SUPPORT',
    'ADMIN',
    'SUPPORT',
  ];
  
  for (const role of rolePriority) {
    if (user.roles.includes(role)) {
      return role;
    }
  }
  
  return user.roles[0];
};

/**
 * Format role name for display
 */
export const formatRoleName = (role: RoleName): string => {
  return role.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
};

/**
 * Format permission name for display
 */
export const formatPermissionName = (permission: Permission): string => {
  return permission.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
};

/**
 * Store user data and permissions
 */
export const storeUserData = (user: AdminUser): void => {
  localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(user));
  if (user.permissions) {
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(user.permissions));
  }
};

/**
 * Clear user data
 */
export const clearUserData = (): void => {
  localStorage.removeItem(ADMIN_USER_KEY);
  localStorage.removeItem(PERMISSIONS_KEY);
};
