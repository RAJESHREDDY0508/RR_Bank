/**
 * Permission-based Route Guard Component
 * Replaces role-based guards with granular permission checks
 */

import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Box, Typography, Button, Paper, Alert } from '@mui/material';
import { Block, ArrowBack, Security } from '@mui/icons-material';
import { useRBAC } from '../../hooks/useRBAC';
import { Permission } from '../../types/rbac';
import Loading from './Loading';

interface PermissionRouteProps {
  children: React.ReactNode;
  requiredPermissions?: Permission[];
  requireAll?: boolean;
  fallbackPath?: string;
  showUnauthorizedPage?: boolean;
}

/**
 * Route guard that checks permissions before rendering children
 */
const PermissionRoute: React.FC<PermissionRouteProps> = ({
  children,
  requiredPermissions = [],
  requireAll = false,
  fallbackPath = '/dashboard',
  showUnauthorizedPage = true,
}) => {
  const location = useLocation();
  const { isAuthenticated, isLoading, hasAnyPermission, hasAllPermissions, isSuperAdmin } = useRBAC();

  // Show loading while checking auth
  if (isLoading) {
    return <Loading text="Checking permissions..." />;
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // No permissions required, allow access
  if (requiredPermissions.length === 0) {
    return <>{children}</>;
  }

  // Super admin bypasses all permission checks
  if (isSuperAdmin()) {
    return <>{children}</>;
  }

  // Check permissions
  const hasAccess = requireAll
    ? hasAllPermissions(...requiredPermissions)
    : hasAnyPermission(...requiredPermissions);

  if (!hasAccess) {
    if (showUnauthorizedPage) {
      return <UnauthorizedView fallbackPath={fallbackPath} requiredPermissions={requiredPermissions} />;
    }
    return <Navigate to={fallbackPath} replace />;
  }

  return <>{children}</>;
};

/**
 * Unauthorized/Forbidden view component
 */
interface UnauthorizedViewProps {
  fallbackPath: string;
  requiredPermissions?: Permission[];
}

const UnauthorizedView: React.FC<UnauthorizedViewProps> = ({ fallbackPath, requiredPermissions }) => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '60vh',
        p: 3,
      }}
    >
      <Paper
        elevation={3}
        sx={{
          p: 4,
          maxWidth: 500,
          width: '100%',
          textAlign: 'center',
        }}
      >
        <Box
          sx={{
            bgcolor: 'error.light',
            borderRadius: '50%',
            width: 80,
            height: 80,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            mx: 'auto',
            mb: 3,
          }}
        >
          <Block sx={{ fontSize: 40, color: 'error.main' }} />
        </Box>

        <Typography variant="h4" gutterBottom fontWeight="bold" color="error.main">
          Access Denied
        </Typography>

        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          You don't have permission to access this page. Please contact your administrator if you believe this is an error.
        </Typography>

        {requiredPermissions && requiredPermissions.length > 0 && (
          <Alert severity="info" sx={{ mb: 3, textAlign: 'left' }}>
            <Typography variant="body2" fontWeight="bold" gutterBottom>
              Required permissions:
            </Typography>
            <Box component="ul" sx={{ m: 0, pl: 2 }}>
              {requiredPermissions.map(permission => (
                <li key={permission}>
                  <Typography variant="caption">{permission.replace(/_/g, ' ')}</Typography>
                </li>
              ))}
            </Box>
          </Alert>
        )}

        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Button
            variant="contained"
            startIcon={<ArrowBack />}
            href={fallbackPath}
            sx={{ minWidth: 140 }}
          >
            Go Back
          </Button>
          <Button
            variant="outlined"
            startIcon={<Security />}
            href="/dashboard"
            sx={{ minWidth: 140 }}
          >
            Dashboard
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

/**
 * Legacy PrivateRoute component - now uses PermissionRoute internally
 */
interface PrivateRouteProps {
  children: React.ReactNode;
  requiredRoles?: string[];
  requiredPermissions?: Permission[];
}

const PrivateRoute: React.FC<PrivateRouteProps> = ({ 
  children, 
  requiredRoles,
  requiredPermissions,
}) => {
  const location = useLocation();
  const { isAuthenticated, isLoading, roles } = useRBAC();

  if (isLoading) {
    return <Loading text="Loading..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Legacy role check
  if (requiredRoles && requiredRoles.length > 0) {
    const hasRequiredRole = requiredRoles.some(role => roles.includes(role as any));
    if (!hasRequiredRole) {
      return <UnauthorizedView fallbackPath="/dashboard" />;
    }
  }

  // Permission check
  if (requiredPermissions && requiredPermissions.length > 0) {
    return (
      <PermissionRoute requiredPermissions={requiredPermissions}>
        {children}
      </PermissionRoute>
    );
  }

  return <>{children}</>;
};

/**
 * HOC to wrap components with permission check
 */
export function withPermission<P extends object>(
  WrappedComponent: React.ComponentType<P>,
  requiredPermissions: Permission[],
  requireAll: boolean = false
): React.FC<P> {
  const WithPermissionComponent: React.FC<P> = (props) => {
    return (
      <PermissionRoute requiredPermissions={requiredPermissions} requireAll={requireAll}>
        <WrappedComponent {...props} />
      </PermissionRoute>
    );
  };

  WithPermissionComponent.displayName = `WithPermission(${WrappedComponent.displayName || WrappedComponent.name || 'Component'})`;

  return WithPermissionComponent;
}

/**
 * Component that only renders children if user has required permissions
 */
interface RequirePermissionProps {
  permissions: Permission[];
  requireAll?: boolean;
  fallback?: React.ReactNode;
  children: React.ReactNode;
}

export const RequirePermission: React.FC<RequirePermissionProps> = ({
  permissions,
  requireAll = false,
  fallback = null,
  children,
}) => {
  const { hasAnyPermission, hasAllPermissions, isSuperAdmin } = useRBAC();

  if (isSuperAdmin()) {
    return <>{children}</>;
  }

  const hasAccess = requireAll
    ? hasAllPermissions(...permissions)
    : hasAnyPermission(...permissions);

  return hasAccess ? <>{children}</> : <>{fallback}</>;
};

/**
 * Component that hides children if user doesn't have permission
 */
interface ShowIfPermittedProps {
  permission: Permission;
  children: React.ReactNode;
}

export const ShowIfPermitted: React.FC<ShowIfPermittedProps> = ({ permission, children }) => {
  const { hasPermission, isSuperAdmin } = useRBAC();

  if (isSuperAdmin() || hasPermission(permission)) {
    return <>{children}</>;
  }

  return null;
};

export { PermissionRoute, UnauthorizedView };
export default PrivateRoute;
