import React, { useEffect, useState } from 'react';
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
} from '@mui/material';
import {
  People,
  AccountBalance,
  TrendingUp,
  Warning,
  Refresh,
  ArrowUpward,
  ArrowDownward,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { dashboardApi } from '../api/dashboard';
import { formatCurrency, formatDateTime } from '../utils/format';
import Loading from '../components/common/Loading';

interface DashboardStats {
  totalCustomers: number;
  totalAccounts: number;
  totalBalance: number;
  pendingFraudAlerts: number;
  todayTransactions: number;
  todayRevenue: number;
  customerGrowth: number;
  transactionGrowth: number;
}

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DashboardStats>({
    totalCustomers: 0,
    totalAccounts: 0,
    totalBalance: 0,
    pendingFraudAlerts: 0,
    todayTransactions: 0,
    todayRevenue: 0,
    customerGrowth: 0,
    transactionGrowth: 0,
  });
  const [recentActivity, setRecentActivity] = useState<any[]>([]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const response = await dashboardApi.getStats();
      setStats(response.data);
      
      // Mock recent activity for now
      setRecentActivity([
        { id: 1, type: 'NEW_CUSTOMER', description: 'New customer registration', timestamp: new Date().toISOString(), status: 'completed' },
        { id: 2, type: 'TRANSACTION', description: 'High-value transaction detected', timestamp: new Date().toISOString(), status: 'pending' },
        { id: 3, type: 'FRAUD_ALERT', description: 'Suspicious activity flagged', timestamp: new Date().toISOString(), status: 'investigating' },
      ]);
    } catch (error) {
      console.error('Error loading dashboard data:', error);
      // Use mock data if API fails
      setStats({
        totalCustomers: 1245,
        totalAccounts: 2890,
        totalBalance: 12458900.50,
        pendingFraudAlerts: 3,
        todayTransactions: 156,
        todayRevenue: 45678.90,
        customerGrowth: 12.5,
        transactionGrowth: 8.3,
      });
    } finally {
      setLoading(false);
    }
  };

  const StatCard = ({ title, value, icon, color, growth }: any) => (
    <Card elevation={3}>
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography color="textSecondary" gutterBottom variant="body2">
              {title}
            </Typography>
            <Typography variant="h4" component="div" fontWeight="bold">
              {value}
            </Typography>
            {growth !== undefined && (
              <Box display="flex" alignItems="center" mt={1}>
                {growth >= 0 ? (
                  <ArrowUpward fontSize="small" color="success" />
                ) : (
                  <ArrowDownward fontSize="small" color="error" />
                )}
                <Typography
                  variant="body2"
                  color={growth >= 0 ? 'success.main' : 'error.main'}
                  fontWeight="medium"
                >
                  {Math.abs(growth)}% from last month
                </Typography>
              </Box>
            )}
          </Box>
          <Box
            sx={{
              bgcolor: `${color}.100`,
              borderRadius: 2,
              p: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );

  if (loading) {
    return <Loading text="Loading dashboard..." />;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight="bold">
          Dashboard Overview
        </Typography>
        <IconButton onClick={loadDashboardData} color="primary">
          <Refresh />
        </IconButton>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} mb={4}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Customers"
            value={stats.totalCustomers.toLocaleString()}
            icon={<People sx={{ fontSize: 40, color: 'primary.main' }} />}
            color="primary"
            growth={stats.customerGrowth}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Accounts"
            value={stats.totalAccounts.toLocaleString()}
            icon={<AccountBalance sx={{ fontSize: 40, color: 'success.main' }} />}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Balance"
            value={formatCurrency(stats.totalBalance)}
            icon={<TrendingUp sx={{ fontSize: 40, color: 'info.main' }} />}
            color="info"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Fraud Alerts"
            value={stats.pendingFraudAlerts}
            icon={<Warning sx={{ fontSize: 40, color: 'error.main' }} />}
            color="error"
          />
        </Grid>
      </Grid>

      {/* Today's Activity */}
      <Grid container spacing={3} mb={4}>
        <Grid item xs={12} md={6}>
          <Paper elevation={3} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom fontWeight="bold">
              Today's Transactions
            </Typography>
            <Typography variant="h3" color="primary" fontWeight="bold">
              {stats.todayTransactions}
            </Typography>
            <Typography variant="body2" color="textSecondary" mt={1}>
              {stats.transactionGrowth >= 0 ? '+' : ''}{stats.transactionGrowth}% from yesterday
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} md={6}>
          <Paper elevation={3} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom fontWeight="bold">
              Today's Revenue
            </Typography>
            <Typography variant="h3" color="success.main" fontWeight="bold">
              {formatCurrency(stats.todayRevenue)}
            </Typography>
            <Typography variant="body2" color="textSecondary" mt={1}>
              From transaction fees
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      {/* Recent Activity */}
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom fontWeight="bold">
          Recent Activity
        </Typography>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Type</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Time</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {recentActivity.map((activity) => (
                <TableRow key={activity.id} hover>
                  <TableCell>
                    <Chip
                      label={activity.type.replace('_', ' ')}
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
                        activity.status === 'completed' ? 'success' :
                        activity.status === 'pending' ? 'warning' : 'default'
                      }
                    />
                  </TableCell>
                  <TableCell>{formatDateTime(activity.timestamp)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Quick Actions */}
      <Grid container spacing={2} mt={4}>
        <Grid item xs={12} sm={6} md={3}>
          <Paper
            elevation={2}
            sx={{ p: 2, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}
            onClick={() => navigate('/customers')}
          >
            <Typography variant="subtitle1" fontWeight="bold">
              View Customers
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Manage all customers
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Paper
            elevation={2}
            sx={{ p: 2, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}
            onClick={() => navigate('/fraud-alerts')}
          >
            <Typography variant="subtitle1" fontWeight="bold">
              Fraud Alerts
            </Typography>
            <Typography variant="body2" color="textSecondary">
              {stats.pendingFraudAlerts} pending alerts
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Paper
            elevation={2}
            sx={{ p: 2, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}
            onClick={() => navigate('/transactions')}
          >
            <Typography variant="subtitle1" fontWeight="bold">
              Transactions
            </Typography>
            <Typography variant="body2" color="textSecondary">
              View all transactions
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Paper
            elevation={2}
            sx={{ p: 2, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}
            onClick={() => navigate('/audit-logs')}
          >
            <Typography variant="subtitle1" fontWeight="bold">
              Audit Logs
            </Typography>
            <Typography variant="body2" color="textSecondary">
              System activity logs
            </Typography>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
