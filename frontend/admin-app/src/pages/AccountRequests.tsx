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
  Grid,
  Skeleton
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
      setError(null);
      const data = await accountRequestsApi.getPendingRequests(page, rowsPerPage);
      setRequests(data?.content || []);
      setTotalElements(data?.totalElements || 0);
    } catch (err: any) {
      console.error('Error fetching requests:', err);
      setError('Failed to load account requests');
      setRequests([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchPendingCount = async () => {
    try {
      const data = await accountRequestsApi.getPendingCount();
      setPendingCount(data?.pendingCount || data?.count || 0);
    } catch (err) {
      console.error('Failed to fetch pending count:', err);
      setPendingCount(0);
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

  const getStatusColor = (status: string): 'warning' | 'success' | 'error' | 'default' => {
    switch (status) {
      case 'PENDING': return 'warning';
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'error';
      case 'CANCELLED': return 'default';
      default: return 'default';
    }
  };

  const formatCurrency = (amount: number | undefined) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount || 0);
  };

  const formatDate = (dateString: string | undefined) => {
    if (!dateString) return '-';
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
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
          disabled={loading}
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
                <TableCell>Request #</TableCell>
                <TableCell>Customer</TableCell>
                <TableCell>Account Type</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
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
                    <TableCell><Skeleton width={80} /></TableCell>
                    <TableCell><Skeleton width={100} /></TableCell>
                  </TableRow>
                ))
              ) : requests.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 5 }}>
                    <PendingIcon sx={{ fontSize: 60, color: 'text.disabled', mb: 2 }} />
                    <Typography color="text.secondary">No account requests found</Typography>
                  </TableCell>
                </TableRow>
              ) : (
                requests.map((request) => (
                  <TableRow key={request.id} hover>
                    <TableCell>{formatDate(request.createdAt)}</TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {request.requestNumber || request.id?.substring(0, 8) || 'N/A'}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{request.customerName || 'N/A'}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {request.customerEmail || ''}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip 
                        icon={<AccountIcon />} 
                        label={request.accountType || 'N/A'} 
                        size="small" 
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={request.status || 'UNKNOWN'} 
                        color={getStatusColor(request.status)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="right">
                      {request.status === 'PENDING' && (
                        <>
                          <Tooltip title="Approve">
                            <span>
                              <IconButton
                                color="success"
                                onClick={() => {
                                  setSelectedRequest(request);
                                  setApproveDialogOpen(true);
                                }}
                              >
                                <ApproveIcon />
                              </IconButton>
                            </span>
                          </Tooltip>
                          <Tooltip title="Reject">
                            <span>
                              <IconButton
                                color="error"
                                onClick={() => {
                                  setSelectedRequest(request);
                                  setRejectDialogOpen(true);
                                }}
                              >
                                <RejectIcon />
                              </IconButton>
                            </span>
                          </Tooltip>
                        </>
                      )}
                      <Tooltip title="View Details">
                        <span>
                          <IconButton
                            onClick={() => {
                              setSelectedRequest(request);
                              setViewDialogOpen(true);
                            }}
                          >
                            <ViewIcon />
                          </IconButton>
                        </span>
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
            Are you sure you want to approve this {selectedRequest?.accountType} account request for {selectedRequest?.customerName || 'this customer'}?
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
                  <Typography variant="caption" color="text.secondary">Request #</Typography>
                  <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                    {selectedRequest.requestNumber || selectedRequest.id}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Status</Typography>
                  <Box>
                    <Chip label={selectedRequest.status} color={getStatusColor(selectedRequest.status)} size="small" />
                  </Box>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="caption" color="text.secondary">Customer</Typography>
                  <Typography variant="body2">{selectedRequest.customerName || 'N/A'}</Typography>
                  <Typography variant="caption" color="text.secondary">{selectedRequest.customerEmail}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Account Type</Typography>
                  <Typography variant="body2">{selectedRequest.accountType}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">ID Type</Typography>
                  <Typography variant="body2">{selectedRequest.idType || 'N/A'}</Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="caption" color="text.secondary">Address</Typography>
                  <Typography variant="body2">{selectedRequest.address || 'N/A'}</Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="caption" color="text.secondary">Created At</Typography>
                  <Typography variant="body2">{formatDate(selectedRequest.createdAt)}</Typography>
                </Grid>
                {selectedRequest.reviewedBy && (
                  <Grid item xs={12}>
                    <Typography variant="caption" color="text.secondary">Reviewed By</Typography>
                    <Typography variant="body2">
                      {selectedRequest.reviewedBy} on {formatDate(selectedRequest.reviewedAt || undefined)}
                    </Typography>
                  </Grid>
                )}
                {selectedRequest.rejectionReason && (
                  <Grid item xs={12}>
                    <Typography variant="caption" color="text.secondary">Rejection Reason</Typography>
                    <Typography variant="body2" color="error">{selectedRequest.rejectionReason}</Typography>
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
