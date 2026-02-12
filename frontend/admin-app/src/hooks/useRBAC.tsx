/**
 * RBAC Context and Provider
 * Provides permission-based access control throughout the application
 */

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { Permission, AdminUser, RoleName } from '../types/rbac';
import { authApi } from '../api/auth';
import { 
  hasPermission, 
  hasAnyPermission, 
  hasAllPermissions, 
  hasRole, 
  isSuperAdmin,
  getUserPermissions,
  getCurrentUser,
  storeUserData,
  clearUserData,
} from '../utils/permissions';

interface RBACContextType {
  user: AdminUser | null;
  permissions: Permission[];
  roles: RoleName[];
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  
  // Permission checks
  hasPermission: (permission: Permission) => boolean;
  hasAnyPermission: (...permissions: Permission[]) => boolean;
  hasAllPermissions: (...permissions: Permission[]) => boolean;
  hasRole: (role: RoleName) => boolean;
  isSuperAdmin: () => boolean;
  
  // Actions
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const RBACContext = createContext<RBACContextType | undefined>(undefined);

interface RBACProviderProps {
  children: ReactNode;
}

export const RBACProvider: React.FC<RBACProviderProps> = ({ children }) => {
  const [user, setUser] = useState<AdminUser | null>(null);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [roles, setRoles] = useState<RoleName[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Initialize from storage on mount
  useEffect(() => {
    const initializeAuth = async () => {
      setIsLoading(true);
      try {
        const storedUser = getCurrentUser();
        if (storedUser && authApi.isAuthenticated()) {
          setUser(storedUser);
          setPermissions(storedUser.permissions || getUserPermissions());
          setRoles(storedUser.roles || []);
          
          // Verify token is still valid by fetching current user
          try {
            const currentUser = await authApi.getCurrentUser();
            const adminUser: AdminUser = {
              id: currentUser.userId,
              userId: currentUser.userId,
              username: currentUser.username,
              email: currentUser.email,
              firstName: currentUser.firstName,
              lastName: currentUser.lastName,
              roles: (currentUser as any).roles || [currentUser.role as RoleName],
              permissions: (currentUser as any).permissions || [],
              status: 'ACTIVE',
              mustChangePassword: currentUser.mustChangePassword,
            };
            setUser(adminUser);
            setPermissions(adminUser.permissions.length > 0 ? adminUser.permissions : getUserPermissions());
            setRoles(adminUser.roles);
            storeUserData(adminUser);
          } catch (err) {
            // Token invalid, clear auth
            console.warn('Token validation failed, clearing auth');
            clearUserData();
            setUser(null);
            setPermissions([]);
            setRoles([]);
          }
        }
      } catch (err) {
        console.error('Auth initialization error:', err);
        setError('Failed to initialize authentication');
      } finally {
        setIsLoading(false);
      }
    };

    initializeAuth();
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await authApi.login({ username, password });
      
      // Map response to AdminUser
      const adminUser: AdminUser = {
        id: response.user.userId,
        userId: response.user.userId,
        username: response.user.username,
        email: response.user.email,
        firstName: response.user.firstName,
        lastName: response.user.lastName,
        roles: (response.user as any).roles || [response.user.role as RoleName],
        permissions: (response.user as any).permissions || [],
        status: 'ACTIVE',
        mustChangePassword: response.user.mustChangePassword,
      };
      
      setUser(adminUser);
      setPermissions(adminUser.permissions.length > 0 ? adminUser.permissions : getUserPermissions());
      setRoles(adminUser.roles);
      storeUserData(adminUser);
    } catch (err: any) {
      const message = err.response?.data?.message || err.message || 'Login failed';
      setError(message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    setIsLoading(true);
    try {
      await authApi.logout();
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      clearUserData();
      setUser(null);
      setPermissions([]);
      setRoles([]);
      setIsLoading(false);
    }
  }, []);

  const refreshUser = useCallback(async () => {
    if (!authApi.isAuthenticated()) return;
    
    try {
      const currentUser = await authApi.getCurrentUser();
      const adminUser: AdminUser = {
        id: currentUser.userId,
        userId: currentUser.userId,
        username: currentUser.username,
        email: currentUser.email,
        firstName: currentUser.firstName,
        lastName: currentUser.lastName,
        roles: (currentUser as any).roles || [currentUser.role as RoleName],
        permissions: (currentUser as any).permissions || [],
        status: 'ACTIVE',
        mustChangePassword: currentUser.mustChangePassword,
      };
      setUser(adminUser);
      setPermissions(adminUser.permissions.length > 0 ? adminUser.permissions : getUserPermissions());
      setRoles(adminUser.roles);
      storeUserData(adminUser);
    } catch (err) {
      console.error('Failed to refresh user:', err);
    }
  }, []);

  // Permission check functions bound to current user
  const checkPermission = useCallback((permission: Permission): boolean => {
    if (!user) return false;
    if (roles.includes('SUPER_ADMIN')) return true;
    return permissions.includes(permission);
  }, [user, roles, permissions]);

  const checkAnyPermission = useCallback((...perms: Permission[]): boolean => {
    if (!user) return false;
    if (roles.includes('SUPER_ADMIN')) return true;
    return perms.some(p => permissions.includes(p));
  }, [user, roles, permissions]);

  const checkAllPermissions = useCallback((...perms: Permission[]): boolean => {
    if (!user) return false;
    if (roles.includes('SUPER_ADMIN')) return true;
    return perms.every(p => permissions.includes(p));
  }, [user, roles, permissions]);

  const checkRole = useCallback((role: RoleName): boolean => {
    return roles.includes(role);
  }, [roles]);

  const checkSuperAdmin = useCallback((): boolean => {
    return roles.includes('SUPER_ADMIN');
  }, [roles]);

  const value: RBACContextType = {
    user,
    permissions,
    roles,
    isAuthenticated: !!user && authApi.isAuthenticated(),
    isLoading,
    error,
    hasPermission: checkPermission,
    hasAnyPermission: checkAnyPermission,
    hasAllPermissions: checkAllPermissions,
    hasRole: checkRole,
    isSuperAdmin: checkSuperAdmin,
    login,
    logout,
    refreshUser,
  };

  return (
    <RBACContext.Provider value={value}>
      {children}
    </RBACContext.Provider>
  );
};

/**
 * Hook to access RBAC context
 */
export const useRBAC = (): RBACContextType => {
  const context = useContext(RBACContext);
  if (context === undefined) {
    throw new Error('useRBAC must be used within a RBACProvider');
  }
  return context;
};

/**
 * Hook to check specific permission
 */
export const usePermission = (permission: Permission): boolean => {
  const { hasPermission } = useRBAC();
  return hasPermission(permission);
};

/**
 * Hook to check multiple permissions (any)
 */
export const useAnyPermission = (...permissions: Permission[]): boolean => {
  const { hasAnyPermission } = useRBAC();
  return hasAnyPermission(...permissions);
};

/**
 * Hook to check multiple permissions (all)
 */
export const useAllPermissions = (...permissions: Permission[]): boolean => {
  const { hasAllPermissions } = useRBAC();
  return hasAllPermissions(...permissions);
};

export default RBACContext;
