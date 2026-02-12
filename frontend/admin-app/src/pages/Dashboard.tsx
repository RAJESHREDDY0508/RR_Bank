/**
 * Dashboard Page with Responsive Design and Permission Checks
 */

import React, { useEffect, useState, useCallback } from 'react';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Skeleton,
  useTheme,
  useMediaQuery,
  Button,
  Stack,
  Tooltip,
} from '@mui/material';
import {
  People,
  AccountBalance,
  TrendingUp,
  Warning,
  Refresh,
  ArrowUpward,
  ArrowDownward,
  Error as ErrorIcon,
  PendingActions,
  Receipt,
  Security,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { dashboardApi, DashboardStats } from '../api/dashboard';
import { formatCurrency, formatDateTime } from '../utils/format';
import ErrorBanner, { parseApiError } from '../components/common/ErrorBanner';
import { useRBAC } from '../hooks/useRBAC';
import { RequirePermission, ShowIfPermitted } from '../components/common/PrivateRoute';

// Default stats
const DEFAULT_STATS: DashboardStats = {
  totalCustomers: 0,
  activeCustomers: 0,
  newCustomersToday: 0,
  customerGrowthPercent: 0,
  totalAccounts: 0,
  activeAccounts: 0,
  frozenAccounts: 0,
  totalBalance: 0,
  todayDeposits: 0,
  todayWithdrawals: 0,
  todayTransfers: 0,
  totalTransactions: 0,
  todayTransactions: 0,
  transactionGrowthPercent: 0,
  pendingFraudAlerts: 0,
  pendingAccountRequests: 0,
  pendingKycReviews: 0,
  activeAdmins: 0,
  adminActionsToday: 0,
};

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
  growth?: number;
  loading: boolean;
  error: boolean;
  onClick?: () => void;
}

const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  icon,
  color,
  growth,
  loading,
  error,
  onClick,
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  return (
    <Card
      elevation={2}
      sx={{
        cursor: onClick ? 'pointer' : 'default',
        transition: 'transform 0.2s, box-shadow 0.2s',
        '&:hover': onClick ? {
          transform: 'translateY(-4px)',
          boxShadow: 4,
        } : {},
      }}
      onClick={onClick}
    >
      <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
        <Box display="flex" justifyContent="space-between" alignItems="flex-start">
          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Typography
              color="textSecondary"
              gutterBottom
              variant="body2"
              sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}
            >
              {title}
            </Typography>
            {loading ? (
              <Skeleton variant="text" width={80} height={40} />
            ) : error ? (
              <Typography variant="h5" fontWeight="bold" color="error">
                --
              </Typography>
            ) : (
              <Typography
                variant={isMobile ? 'h5' : 'h4'}
                fontWeight="bold"
                sx={{ wordBreak: 'break-word' }}
              >
                {value}
              </Typography>
            )}
            {!loading && !error && growth !== undefined && (
              <Box display="flex" alignItems="center" mt={0.5}>
                {growth >= 0 ? (
                  <ArrowUpward fontSize="small" color="success" />
                ) : (
                  <ArrowDownward fontSize="small" color="error" />
                )}
                <Typography
                  variant="caption"
                  color={growth >= 0 ? 'success.main' : 'error.main'}
                  fontWeight="medium"
                >
                  {Math.abs(growth)}%
                </Typography>
              </Box>
            )}
          </Box>
          <Box
            sx={{
              bgcolor: error ? 'error.light' : `${color}.light`,
              borderRadius: 2,
              p: { xs: 0.75, sm: 1 },
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              ml: 1,
              flexShrink: 0,
            }}
          >
            {error ? (
              <ErrorIcon sx={{ fontSize: { xs: 28, sm: 36 }, color: 'error.main' }} />
            ) : (
              React.cloneElement(icon as React.ReactElement, {
                sx: { fontSize: { xs: 28, sm: 36 }, color: `${color}.main` },
              })
            )}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const isTablet = useMediaQuery(theme.breakpoints.down('md'));

  const { hasPermission, isSuperAdmin, user } = useRBAC();

  const [loading, setLoading] = useState({ stats: true, activity: true });
  const [error, setError] = useState<{ stats: string | null; activity: string | null }>({
    stats: null,
    activity: null,
  });
  const [stats, setStats] = useState<DashboardStats>(DEFAULT_STATS);
  const [recentActivity, setRecentActivity] = useState<any[]>([]);
  const [requestId, setRequestId] = useState<string | null>(null);

  const generateRequestId = () => `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

  const loadDashboardData = useCallback(async () => {
    const reqId = generateRequestId();
    setRequestId(reqId);
    setLoading({ stats: true, activity: true });
    setError({ stats: null, activity: null });

    // Load stats
    try {
      const response = await dashboardApi.getStats();
      if (response.data && typeof response.data.totalCustomers === 'number') {
        setStats(response.data);
      } else {
        throw new Error('Invalid response format');
      }
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(prev => ({ ...prev, stats: errorInfo.message }));
    } finally {
      setLoading(prev => ({ ...prev, stats: false }));
    }

    // Load recent activity
    try {
      const activityResponse = await dashboardApi.getRecentActivity();
      setRecentActivity(activityResponse.data || []);
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(prev => ({ ...prev, activity: errorInfo.message }));
      setRecentActivity([]);
    } finally {
      setLoading(prev => ({ ...prev, activity: false }));
    }
  }, []);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  // Quick action cards based on permissions
  const quickActions = [
    {
      label: 'View Customers',
      description: 'Manage all customers',
      path: '/customers',
      permission: 'CUSTOMER_READ' as const,
      icon: <People />,
    },
    {
      label: 'Fraud Alerts',
      description: `${stats.pendingFraudAlerts} pending alerts`,
      path: '/fraud-alerts',
      permission: 'FRAUD_ALERT_READ' as const,
      icon: <Warning />,
      highlight: stats.pendingFraudAlerts > 0,
    },
    {
      label: 'Transactions',
      description: 'View all transactions',
      path: '/transactions',
      permission: 'TXN_READ' as const,
      icon: <Receipt />,
    },
    {
      label: 'Account Requests',
      description: `${stats.pendingAccountRequests} pending`,
      path: '/account-requests',
      permission: 'ACCOUNT_APPROVE_REQUESTS' as const,
      icon: <PendingActions />,
      highlight: stats.pendingAccountRequests > 0,
    },
  ];

  return (
    <Box>
      {/* Header */}
      <Box
        sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          justifyContent: 'space-between',
          alignItems: { xs: 'flex-start', sm: 'center' },
          mb: 3,
          gap: 2,
        }}
      >
        <Box>
          <Typography variant={isMobile ? 'h5' : 'h4'} fontWeight="bold">
            Dashboard Overview
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Welcome back, {user?.firstName || user?.username}
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {!isMobile && requestId && (
            <Typography variant="caption" color="text.secondary">
              ID: {requestId.substring(0, 12)}...
            </Typography>
          )}
          <Tooltip title="Refresh data">
            <span>
              <IconButton
                onClick={loadDashboardData}
                color="primary"
                disabled={loading.stats}
              >
                <Refresh />
              </IconButton>
            </span>
          </Tooltip>
        </Box>
      </Box>

      {/* Error Alert */}
      {error.stats && (
        <ErrorBanner
          error={error.stats}
          requestId={requestId}
          onRetry={loadDashboardData}
          onClose={() => setError(prev => ({ ...prev, stats: null }))}
        />
      )}

      {/* Stats Cards */}
      <Grid container spacing={{ xs: 2, sm: 3 }} mb={4}>
        <ShowIfPermitted permission="CUSTOMER_READ">
          <Grid item xs={6} sm={6} md={3}>
            <StatCard
              title="Total Customers"
              value={stats.totalCustomers.toLocaleString()}
              icon={<People />}
              color="primary"
              growth={stats.customerGrowthPercent}
              loading={loading.stats}
              error={!!error.stats}
              onClick={() => hasPermission('CUSTOMER_READ') && navigate('/customers')}
            />
          </Grid>
        </ShowIfPermitted>

        <ShowIfPermitted permission="ACCOUNT_READ">
          <Grid item xs={6} sm={6} md={3}>
            <StatCard
              title="Total Accounts"
              value={stats.totalAccounts.toLocaleString()}
              icon={<AccountBalance />}
              color="success"
              loading={loading.stats}
              error={!!error.stats}
              onClick={() => hasPermission('ACCOUNT_READ') && navigate('/accounts')}
            />
          </Grid>
        </ShowIfPermitted>

        <ShowIfPermitted permission="TXN_READ">
          <Grid item xs={6} sm={6} md={3}>
            <StatCard
              title="Total Balance"
              value={formatCurrency(stats.totalBalance)}
              icon={<TrendingUp />}
              color="info"
              loading={loading.stats}
              error={!!error.stats}
            />
          </Grid>
        </ShowIfPermitted>

        <ShowIfPermitted permission="FRAUD_ALERT_READ">
          <Grid item xs={6} sm={6} md={3}>
            <StatCard
              title="Fraud Alerts"
              value={stats.pendingFraudAlerts}
              icon={<Warning />}
              color="error"
              loading={loading.stats}
              error={!!error.stats}
              onClick={() => hasPermission('FRAUD_ALERT_READ') && navigate('/fraud-alerts')}
            />
          </Grid>
        </ShowIfPermitted>
      </Grid>

      {/* Today's Activity - Responsive Grid */}
      <Grid container spacing={{ xs: 2, sm: 3 }} mb={4}>
        <ShowIfPermitted permission="TXN_READ">
          <Grid item xs={12} md={6}>
            <Paper elevation={2} sx={{ p: { xs: 2, sm: 3 } }}>
              <Typography variant="h6" gutterBottom fontWeight="bold">
                Today's Transactions
              </Typography>
              {loading.stats ? (
                <Skeleton variant="text" width={100} height={60} />
              ) : (
                <>
                  <Typography variant="h3" color="primary" fontWeight="bold">
                    {stats.todayTransactions}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" mt={1}>
                    {stats.transactionGrowthPercent >= 0 ? '+' : ''}
                    {stats.transactionGrowthPercent}% from yesterday
                  </Typography>
                </>
              )}
            </Paper>
          </Grid>
        </ShowIfPermitted>

        <ShowIfPermitted permission="TXN_READ">
          <Grid item xs={12} md={6}>
            <Paper elevation={2} sx={{ p: { xs: 2, sm: 3 } }}>
              <Typography variant="h6" gutterBottom fontWeight="bold">
                Today's Deposits
              </Typography>
              {loading.stats ? (
                <Skeleton variant="text" width={150} height={60} />
              ) : (
                <>
                  <Typography variant="h3" color="success.main" fontWeight="bold">
                    {formatCurrency(stats.todayDeposits)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" mt={1}>
                    From customer deposits
                  </Typography>
                </>
              )}
            </Paper>
          </Grid>
        </ShowIfPermitted>
      </Grid>

      {/* Recent Activity - Responsive */}
      <ShowIfPermitted permission="AUDIT_READ">
        <Paper elevation={2} sx={{ p: { xs: 2, sm: 3 }, mb: 4 }}>
          <Typography variant="h6" gutterBottom fontWeight="bold">
            Recent Activity
          </Typography>
          {error.activity && (
            <ErrorBanner
              error={error.activity}
              severity="warning"
              onRetry={loadDashboardData}
            />
          )}

          {/* Mobile: Card view */}
          {isMobile ? (
            <Stack spacing={2}>
              {loading.activity ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <Paper key={i} variant="outlined" sx={{ p: 2 }}>
                    <Skeleton variant="text" width="60%" />
                    <Skeleton variant="text" width="80%" />
                    <Skeleton variant="text" width="40%" />
                  </Paper>
                ))
              ) : recentActivity.length === 0 ? (
                <Typography color="text.secondary" textAlign="center" py={3}>
                  No recent activity
                </Typography>
              ) : (
                recentActivity.slice(0, 5).map((activity) => (
                  <Paper key={activity.id} variant="outlined" sx={{ p: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Chip
                        label={(activity.type || activity.entityType || 'ACTION').replace('_', ' ')}
                        size="small"
                        color={activity.type === 'FRAUD_ALERT' ? 'error' : 'primary'}
                      />
                      <Chip
                        label={activity.status}
                        size="small"
                        color={
                          activity.status === 'SUCCESS' || activity.status === 'completed'
                            ? 'success'
                            : activity.status === 'PENDING'
                            ? 'warning'
                            : 'default'
                        }
                      />
                    </Box>
                    <Typography variant="body2">{activity.description}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {formatDateTime(activity.timestamp || activity.createdAt)}
                    </Typography>
                  </Paper>
                ))
              )}
            </Stack>
          ) : (
            /* Desktop: Table view */
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Type</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Time</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {loading.activity ? (
                    Array.from({ length: 3 }).map((_, i) => (
                      <TableRow key={i}>
                        <TableCell><Skeleton /></TableCell>
                        <TableCell><Skeleton /></TableCell>
                        <TableCell><Skeleton /></TableCell>
                        <TableCell><Skeleton /></TableCell>
                      </TableRow>
                    ))
                  ) : recentActivity.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        <Typography color="text.secondary">No recent activity</Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    recentActivity.map((activity) => (
                      <TableRow key={activity.id} hover>
                        <TableCell>
                          <Chip
                            label={(activity.type || activity.entityType || 'ACTION').replace('_', ' ')}
                            size="small"
                            color={activity.type === 'FRAUD_ALERT' ? 'error' : 'primary'}
                          />
                        </TableCell>
                        <TableCell>{activity.description}</TableCell>
                        <TableCell>
                          <Chip
                            label={activity.status}
                            size="small"
                            color={
                              activity.status === 'SUCCESS' || activity.status === 'completed'
                                ? 'success'
                                : activity.status === 'PENDING'
                                ? 'warning'
                                : 'default'
                            }
                          />
                        </TableCell>
                        <TableCell>
                          {formatDateTime(activity.timestamp || activity.createdAt)}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Paper>
      </ShowIfPermitted>

      {/* Quick Actions - Responsive Grid */}
      <Typography variant="h6" fontWeight="bold" mb={2}>
        Quick Actions
      </Typography>
      <Grid container spacing={2}>
        {quickActions.map((action) => (
          <RequirePermission key={action.path} permissions={[action.permission]}>
            <Grid item xs={6} sm={6} md={3}>
              <Paper
                elevation={action.highlight ? 3 : 1}
                sx={{
                  p: 2,
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  border: action.highlight ? 2 : 0,
                  borderColor: action.highlight ? 'warning.main' : 'transparent',
                  '&:hover': {
                    bgcolor: 'action.hover',
                    transform: 'translateY(-2px)',
                  },
                }}
                onClick={() => navigate(action.path)}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  {action.icon}
                  <Typography variant="subtitle2" fontWeight="bold">
                    {action.label}
                  </Typography>
                </Box>
                <Typography variant="caption" color="text.secondary">
                  {action.description}
                </Typography>
              </Paper>
            </Grid>
          </RequirePermission>
        ))}
      </Grid>
    </Box>
  );
};

export default Dashboard;
