import React, { useState, useEffect } from 'react';
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
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Tooltip,
  CircularProgress,
  Alert,
  Card,
  CardContent,
  Grid
} from '@mui/material';
import {
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  Visibility as ViewIcon,
  Refresh as RefreshIcon,
  PendingActions as PendingIcon,
  AccountBalance as AccountIcon
} from '@mui/icons-material';
import { accountRequestsApi, AccountRequest } from '../api/accountRequests';
import { format } from 'date-fns';

const AccountRequests: React.FC = () => {
  const [requests, setRequests] = useState<AccountRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [pendingCount, setPendingCount] = useState(0);
  
  // Modal states
  const [selectedRequest, setSelectedRequest] = useState<AccountRequest | null>(null);
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [notes, setNotes] = useState('');
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    fetchRequests();
    fetchPendingCount();
  }, [page, rowsPerPage]);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      const data = await accountRequestsApi.getPendingRequests(page, rowsPerPage);
      setRequests(data.content);
      setTotalElements(data.totalElements);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load account requests');
    } finally {
      setLoading(false);
    }
  };

  const fetchPendingCount = async () => {
    try {
      const data = await accountRequestsApi.getPendingCount();
      setPendingCount(data.pendingCount);
    } catch (err) {
      console.error('Failed to fetch pending count:', err);
    }
  };

  const handleApprove = async () => {
    if (!selectedRequest) return;
    
    try {
      setProcessing(true);
      await accountRequestsApi.approveRequest(selectedRequest.id, notes);
      setApproveDialogOpen(false);
      setNotes('');
      setSelectedRequest(null);
      fetchRequests();
      fetchPendingCount();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to approve request');
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRequest || !notes.trim()) return;
    
    try {
      setProcessing(true);
      await accountRequestsApi.rejectRequest(selectedRequest.id, notes);
      setRejectDialogOpen(false);
      setNotes('');
      setSelectedRequest(null);
      fetchRequests();
      fetchPendingCount();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reject request');
    } finally {
      setProcessing(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'warning';
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'error';
      case 'CANCELLED': return 'default';
      default: return 'default';
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount || 0);
  };

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom>
            Account Requests
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Review and approve new account opening requests
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={() => { fetchRequests(); fetchPendingCount(); }}
        >
          Refresh
        </Button>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <PendingIcon sx={{ fontSize: 40, color: 'warning.main', mr: 2 }} />
                <Box>
                  <Typography variant="h4">{pendingCount}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Pending Requests
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Requests Table */}
      <Paper sx={{ width: '100%', overflow: 'hidden' }}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Date</TableCell>
                <TableCell>User ID</TableCell>
                <TableCell>Account Type</TableCell>
                <TableCell>Initial Deposit</TableCell>
                <TableCell>Currency</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 5 }}>
                    <CircularProgress />
                  </TableCell>
                </TableRow>
              ) : requests.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 5 }}>
                    <Typography color="text.secondary">No pending requests</Typography>
                  </TableCell>
                </TableRow>
              ) : (
                requests.map((request) => (
                  <TableRow key={request.id} hover>
                    <TableCell>
                      {format(new Date(request.createdAt), 'MMM dd, yyyy HH:mm')}
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {request.userId.substring(0, 8)}...
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip 
                        icon={<AccountIcon />} 
                        label={request.accountType} 
                        size="small" 
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>{formatCurrency(request.initialDeposit)}</TableCell>
                    <TableCell>{request.currency}</TableCell>
                    <TableCell>
                      <Chip 
                        label={request.status} 
                        color={getStatusColor(request.status) as any}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="right">
                      {request.status === 'PENDING' && (
                        <>
                          <Tooltip title="Approve">
                            <IconButton
                              color="success"
                              onClick={() => {
                                setSelectedRequest(request);
                                setApproveDialogOpen(true);
                              }}
                            >
                              <ApproveIcon />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Reject">
                            <IconButton
                              color="error"
                              onClick={() => {
                                setSelectedRequest(request);
                                setRejectDialogOpen(true);
                              }}
                            >
                              <RejectIcon />
                            </IconButton>
                          </Tooltip>
                        </>
                      )}
                      <Tooltip title="View Details">
                        <IconButton
                          onClick={() => {
                            setSelectedRequest(request);
                            setViewDialogOpen(true);
                          }}
                        >
                          <ViewIcon />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          rowsPerPageOptions={[5, 10, 25]}
          component="div"
          count={totalElements}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
        />
      </Paper>

      {/* Approve Dialog */}
      <Dialog open={approveDialogOpen} onClose={() => setApproveDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Approve Account Request</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Are you sure you want to approve this {selectedRequest?.accountType} account request?
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Initial deposit: {formatCurrency(selectedRequest?.initialDeposit || 0)}
          </Typography>
          <TextField
            fullWidth
            label="Notes (Optional)"
            multiline
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Add any notes for the approval..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveDialogOpen(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            color="success" 
            onClick={handleApprove}
            disabled={processing}
            startIcon={processing ? <CircularProgress size={20} /> : <ApproveIcon />}
          >
            Approve
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Account Request</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Please provide a reason for rejecting this account request.
          </Typography>
          <TextField
            fullWidth
            label="Rejection Reason"
            multiline
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            required
            error={!notes.trim()}
            helperText={!notes.trim() ? 'Reason is required' : ''}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogOpen(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            color="error" 
            onClick={handleReject}
            disabled={processing || !notes.trim()}
            startIcon={processing ? <CircularProgress size={20} /> : <RejectIcon />}
          >
            Reject
          </Button>
        </DialogActions>
      </Dialog>

      {/* View Details Dialog */}
      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Request Details</DialogTitle>
        <DialogContent>
          {selectedRequest && (
            <Box sx={{ pt: 1 }}>
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Request ID</Typography>
                  <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                    {selectedRequest.id}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">User ID</Typography>
                  <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                    {selectedRequest.userId}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Account Type</Typography>
                  <Typography variant="body2">{selectedRequest.accountType}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Initial Deposit</Typography>
                  <Typography variant="body2">{formatCurrency(selectedRequest.initialDeposit)}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Currency</Typography>
                  <Typography variant="body2">{selectedRequest.currency}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Status</Typography>
                  <Chip label={selectedRequest.status} color={getStatusColor(selectedRequest.status) as any} size="small" />
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="caption" color="text.secondary">Created At</Typography>
                  <Typography variant="body2">
                    {format(new Date(selectedRequest.createdAt), 'MMMM dd, yyyy HH:mm:ss')}
                  </Typography>
                </Grid>
                {selectedRequest.requestNotes && (
                  <Grid item xs={12}>
                    <Typography variant="caption" color="text.secondary">User Notes</Typography>
                    <Typography variant="body2">{selectedRequest.requestNotes}</Typography>
                  </Grid>
                )}
                {selectedRequest.adminNotes && (
                  <Grid item xs={12}>
                    <Typography variant="caption" color="text.secondary">Admin Notes</Typography>
                    <Typography variant="body2">{selectedRequest.adminNotes}</Typography>
                  </Grid>
                )}
                {selectedRequest.reviewedBy && (
                  <Grid item xs={12}>
                    <Typography variant="caption" color="text.secondary">Reviewed By</Typography>
                    <Typography variant="body2">
                      {selectedRequest.reviewedBy} on {format(new Date(selectedRequest.reviewedAt!), 'MMM dd, yyyy HH:mm')}
                    </Typography>
                  </Grid>
                )}
              </Grid>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AccountRequests;
