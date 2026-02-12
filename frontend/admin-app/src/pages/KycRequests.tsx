/**
 * KYC Requests Page - Admin can view and approve/reject KYC verification requests
 */

import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Paper,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Button,
  Chip,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  TextField,
  Card,
  CardContent,
  Grid,
  Tabs,
  Tab,
  Alert,
  Snackbar,
  CircularProgress,
  Skeleton,
} from '@mui/material';
import {
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  Refresh as RefreshIcon,
  Person as PersonIcon,
  Pending as PendingIcon,
  Verified as VerifiedIcon,
  Block as BlockIcon,
} from '@mui/icons-material';
import { kycApi, KycCustomer, KycStats } from '../api/kyc';
import { PageResponse } from '../types';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`kyc-tabpanel-${index}`}
      aria-labelledby={`kyc-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

const KycRequests = () => {
  const [tabValue, setTabValue] = useState(0);
  const [customers, setCustomers] = useState<KycCustomer[]>([]);
  const [stats, setStats] = useState<KycStats>({ pending: 0, approved: 0, rejected: 0 });
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [statsLoading, setStatsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState<KycCustomer | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error',
  });

  const getStatusForTab = (tab: number): string => {
    switch (tab) {
      case 0: return 'PENDING';
      case 1: return 'APPROVED';
      case 2: return 'REJECTED';
      default: return 'PENDING';
    }
  };

  const fetchStats = useCallback(async () => {
    setStatsLoading(true);
    try {
      const statsData = await kycApi.getKycStats();
      setStats(statsData);
    } catch (err) {
      console.error('Failed to fetch KYC stats:', err);
      // Keep default stats
    } finally {
      setStatsLoading(false);
    }
  }, []);

  const fetchCustomers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const status = getStatusForTab(tabValue);
      let response: PageResponse<KycCustomer>;
      
      if (status === 'PENDING') {
        response = await kycApi.getPendingKycRequests(page, rowsPerPage);
      } else {
        response = await kycApi.getCustomersByKycStatus(status, page, rowsPerPage);
      }

      setCustomers(response?.content || []);
      setTotalElements(response?.totalElements || 0);
    } catch (err: any) {
      console.error('Failed to fetch customers:', err);
      setError('Failed to load KYC requests. Please try again.');
      setCustomers([]);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  }, [tabValue, page, rowsPerPage]);

  useEffect(() => {
    fetchStats();
    fetchCustomers();
  }, [fetchStats, fetchCustomers]);

  const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
    setPage(0);
  };

  const handleChangePage = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleApproveClick = (customer: KycCustomer) => {
    setSelectedCustomer(customer);
    setApproveDialogOpen(true);
  };

  const handleRejectClick = (customer: KycCustomer) => {
    setSelectedCustomer(customer);
    setRejectReason('');
    setRejectDialogOpen(true);
  };

  const handleApproveConfirm = async () => {
    if (!selectedCustomer) return;

    setActionLoading(true);
    try {
      await kycApi.approveKyc(selectedCustomer.id);
      setSnackbar({
        open: true,
        message: `KYC approved for ${selectedCustomer.fullName || selectedCustomer.email}`,
        severity: 'success',
      });
      setApproveDialogOpen(false);
      fetchStats();
      fetchCustomers();
    } catch (err: any) {
      setSnackbar({
        open: true,
        message: err.response?.data?.message || 'Failed to approve KYC',
        severity: 'error',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectConfirm = async () => {
    if (!selectedCustomer) return;

    setActionLoading(true);
    try {
      await kycApi.rejectKyc(selectedCustomer.id, rejectReason || undefined);
      setSnackbar({
        open: true,
        message: `KYC rejected for ${selectedCustomer.fullName || selectedCustomer.email}`,
        severity: 'success',
      });
      setRejectDialogOpen(false);
      fetchStats();
      fetchCustomers();
    } catch (err: any) {
      setSnackbar({
        open: true,
        message: err.response?.data?.message || 'Failed to reject KYC',
        severity: 'error',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const handleRefresh = () => {
    fetchStats();
    fetchCustomers();
  };

  const getStatusChip = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Chip icon={<PendingIcon />} label="Pending" color="warning" size="small" />;
      case 'APPROVED':
        return <Chip icon={<VerifiedIcon />} label="Approved" color="success" size="small" />;
      case 'REJECTED':
        return <Chip icon={<BlockIcon />} label="Rejected" color="error" size="small" />;
      default:
        return <Chip label={status} size="small" />;
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateString;
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1">
          KYC Verification Requests
        </Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={handleRefresh}
          disabled={loading}
        >
          Refresh
        </Button>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <PendingIcon sx={{ fontSize: 40, color: 'warning.main' }} />
              <Box>
                {statsLoading ? (
                  <Skeleton width={60} height={40} />
                ) : (
                  <Typography variant="h4">{stats.pending}</Typography>
                )}
                <Typography variant="body2" color="text.secondary">
                  Pending Requests
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <VerifiedIcon sx={{ fontSize: 40, color: 'success.main' }} />
              <Box>
                {statsLoading ? (
                  <Skeleton width={60} height={40} />
                ) : (
                  <Typography variant="h4">{stats.approved}</Typography>
                )}
                <Typography variant="body2" color="text.secondary">
                  Approved
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <BlockIcon sx={{ fontSize: 40, color: 'error.main' }} />
              <Box>
                {statsLoading ? (
                  <Skeleton width={60} height={40} />
                ) : (
                  <Typography variant="h4">{stats.rejected}</Typography>
                )}
                <Typography variant="body2" color="text.secondary">
                  Rejected
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs */}
      <Paper sx={{ mb: 2 }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="KYC status tabs">
          <Tab
            icon={<PendingIcon />}
            label={`Pending (${stats.pending})`}
            iconPosition="start"
          />
          <Tab
            icon={<VerifiedIcon />}
            label={`Approved (${stats.approved})`}
            iconPosition="start"
          />
          <Tab
            icon={<BlockIcon />}
            label={`Rejected (${stats.rejected})`}
            iconPosition="start"
          />
        </Tabs>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Table */}
      <TabPanel value={tabValue} index={tabValue}>
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Customer</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Registered</TableCell>
                {tabValue === 2 && <TableCell>Rejection Reason</TableCell>}
                {tabValue === 0 && <TableCell align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton /></TableCell>
                    <TableCell><Skeleton /></TableCell>
                    <TableCell><Skeleton /></TableCell>
                    <TableCell><Skeleton width={80} /></TableCell>
                    <TableCell><Skeleton /></TableCell>
                    {tabValue === 2 && <TableCell><Skeleton /></TableCell>}
                    {tabValue === 0 && <TableCell><Skeleton width={80} /></TableCell>}
                  </TableRow>
                ))
              ) : customers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={tabValue === 0 ? 6 : tabValue === 2 ? 6 : 5} align="center">
                    <Box sx={{ py: 5 }}>
                      <PersonIcon sx={{ fontSize: 60, color: 'text.disabled', mb: 2 }} />
                      <Typography variant="h6" color="text.secondary">
                        No {getStatusForTab(tabValue).toLowerCase()} KYC requests
                      </Typography>
                    </Box>
                  </TableCell>
                </TableRow>
              ) : (
                customers.map((customer) => (
                  <TableRow key={customer.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <PersonIcon color="action" />
                        <Box>
                          <Typography variant="body2" fontWeight="medium">
                            {customer.fullName || `${customer.firstName || ''} ${customer.lastName || ''}`.trim() || 'N/A'}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            ID: {customer.id?.substring(0, 8) || 'N/A'}...
                          </Typography>
                        </Box>
                      </Box>
                    </TableCell>
                    <TableCell>{customer.email || '-'}</TableCell>
                    <TableCell>{customer.phoneNumber || '-'}</TableCell>
                    <TableCell>{getStatusChip(customer.kycStatus || 'PENDING')}</TableCell>
                    <TableCell>{formatDate(customer.createdAt)}</TableCell>
                    {tabValue === 2 && (
                      <TableCell>
                        <Typography variant="body2" color="error">
                          {customer.kycRejectionReason || '-'}
                        </Typography>
                      </TableCell>
                    )}
                    {tabValue === 0 && (
                      <TableCell align="right">
                        <Tooltip title="Approve KYC">
                          <span>
                            <IconButton
                              color="success"
                              onClick={() => handleApproveClick(customer)}
                              size="small"
                            >
                              <ApproveIcon />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip title="Reject KYC">
                          <span>
                            <IconButton
                              color="error"
                              onClick={() => handleRejectClick(customer)}
                              size="small"
                            >
                              <RejectIcon />
                            </IconButton>
                          </span>
                        </Tooltip>
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            onPageChange={handleChangePage}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={handleChangeRowsPerPage}
            rowsPerPageOptions={[5, 10, 25, 50]}
          />
        </TableContainer>
      </TabPanel>

      {/* Approve Dialog */}
      <Dialog open={approveDialogOpen} onClose={() => setApproveDialogOpen(false)}>
        <DialogTitle>Approve KYC Verification</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to approve KYC verification for{' '}
            <strong>
              {selectedCustomer?.fullName || selectedCustomer?.email}
            </strong>
            ?
            <br /><br />
            This will enable the customer to perform transactions (deposit, withdraw, transfer).
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveDialogOpen(false)} disabled={actionLoading}>
            Cancel
          </Button>
          <Button
            onClick={handleApproveConfirm}
            color="success"
            variant="contained"
            disabled={actionLoading}
            startIcon={actionLoading ? <CircularProgress size={20} /> : <ApproveIcon />}
          >
            Approve
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)}>
        <DialogTitle>Reject KYC Verification</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Are you sure you want to reject KYC verification for{' '}
            <strong>
              {selectedCustomer?.fullName || selectedCustomer?.email}
            </strong>
            ?
            <br />
            The customer will not be able to perform transactions.
          </DialogContentText>
          <TextField
            autoFocus
            label="Rejection Reason (optional)"
            fullWidth
            multiline
            rows={3}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="Enter reason for rejection..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogOpen(false)} disabled={actionLoading}>
            Cancel
          </Button>
          <Button
            onClick={handleRejectConfirm}
            color="error"
            variant="contained"
            disabled={actionLoading}
            startIcon={actionLoading ? <CircularProgress size={20} /> : <RejectIcon />}
          >
            Reject
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default KycRequests;
