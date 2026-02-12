/**
 * Transactions Page with Responsive Design and Permission-based Actions
 */

import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Button,
  Chip,
  IconButton,
  useTheme,
  useMediaQuery,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  FileDownload,
  Visibility,
  Undo,
} from '@mui/icons-material';
import { transactionsApi } from '../api/transactions';
import { formatCurrency, formatDateTime } from '../utils/format';
import ErrorBanner, { parseApiError } from '../components/common/ErrorBanner';
import FilterPanel, { FilterField, FilterValues } from '../components/common/FilterPanel';
import ResponsiveTable, { Column } from '../components/common/ResponsiveTable';
import { useRBAC } from '../hooks/useRBAC';
import { ShowIfPermitted } from '../components/common/PrivateRoute';
import { Transaction, TransactionStatus } from '../types';
import { toast } from 'react-toastify';

const Transactions: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { hasPermission } = useRBAC();

  const [loading, setLoading] = useState(true);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  // Reversal dialog state
  const [reversalDialogOpen, setReversalDialogOpen] = useState(false);
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [reversalReason, setReversalReason] = useState('');

  // Filter state
  const [filters, setFilters] = useState<FilterValues>({
    search: '',
    status: '',
    type: '',
    startDate: null,
    endDate: null,
  });

  // Filter configuration
  const filterFields: FilterField[] = [
    {
      id: 'status',
      label: 'Status',
      type: 'select',
      options: [
        { value: 'PENDING', label: 'Pending' },
        { value: 'COMPLETED', label: 'Completed' },
        { value: 'FAILED', label: 'Failed' },
        { value: 'CANCELLED', label: 'Cancelled' },
      ],
      gridSize: 2,
    },
    {
      id: 'type',
      label: 'Type',
      type: 'select',
      options: [
        { value: 'DEPOSIT', label: 'Deposit' },
        { value: 'WITHDRAWAL', label: 'Withdrawal' },
        { value: 'TRANSFER', label: 'Transfer' },
        { value: 'PAYMENT', label: 'Payment' },
      ],
      gridSize: 2,
    },
  ];

  // Table columns
  const columns: Column<Transaction>[] = [
    {
      id: 'reference',
      label: 'Reference',
      accessor: 'transactionReference',
      priority: 1,
    },
    {
      id: 'type',
      label: 'Type',
      accessor: 'transactionType',
      format: (value: string) => (
        <Chip label={value} size="small" color={getTypeColor(value)} variant="outlined" />
      ),
      priority: 2,
    },
    {
      id: 'amount',
      label: 'Amount',
      accessor: (row) => formatCurrency(row.amount, row.currency),
      priority: 3,
    },
    {
      id: 'from',
      label: 'From',
      accessor: 'fromAccountNumber',
      format: (value: string) => value || '-',
      priority: 5,
      hideOnMobile: true,
    },
    {
      id: 'to',
      label: 'To',
      accessor: 'toAccountNumber',
      format: (value: string) => value || '-',
      priority: 6,
      hideOnMobile: true,
    },
    {
      id: 'status',
      label: 'Status',
      accessor: 'status',
      format: (value: TransactionStatus) => (
        <Chip label={value} size="small" color={getStatusColor(value)} />
      ),
      priority: 4,
    },
    {
      id: 'date',
      label: 'Date',
      accessor: (row) => formatDateTime(row.createdAt),
      priority: 7,
      hideOnMobile: true,
    },
  ];

  const getTypeColor = (type: string): 'primary' | 'secondary' | 'success' | 'info' | 'default' => {
    switch (type) {
      case 'DEPOSIT': return 'success';
      case 'WITHDRAWAL': return 'secondary';
      case 'TRANSFER': return 'primary';
      case 'PAYMENT': return 'info';
      default: return 'default';
    }
  };

  const getStatusColor = (status: TransactionStatus): 'success' | 'warning' | 'error' | 'default' => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'PENDING': return 'warning';
      case 'FAILED': return 'error';
      case 'CANCELLED': return 'default';
      default: return 'default';
    }
  };

  const loadTransactions = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await transactionsApi.getTransactions({
        page,
        size: rowsPerPage,
        search: filters.search,
        status: filters.status,
        type: filters.type,
      });

      setTransactions(response.content || []);
      setTotalCount(response.totalElements || 0);
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(errorInfo.message);
      setRequestId(errorInfo.requestId || null);
      setTransactions([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, filters]);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  const handleSearch = () => {
    setPage(0);
    loadTransactions();
  };

  const handleClearFilters = () => {
    setFilters({ search: '', status: '', type: '', startDate: null, endDate: null });
    setPage(0);
  };

  const handleExport = async () => {
    if (!hasPermission('TXN_EXPORT')) {
      toast.error('You do not have permission to export transactions');
      return;
    }

    try {
      setExporting(true);
      const blob = await transactionsApi.exportTransactions({
        status: filters.status,
        type: filters.type,
      });

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `transactions-${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      toast.success('Transactions exported successfully');
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      toast.error(errorInfo.message);
    } finally {
      setExporting(false);
    }
  };

  const handleReversalRequest = (transaction: Transaction) => {
    setSelectedTransaction(transaction);
    setReversalReason('');
    setReversalDialogOpen(true);
  };

  const submitReversalRequest = async () => {
    if (!selectedTransaction || !reversalReason.trim()) return;

    try {
      await transactionsApi.requestReversal(selectedTransaction.id, reversalReason);
      toast.success('Reversal request submitted');
      setReversalDialogOpen(false);
      loadTransactions();
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      toast.error(errorInfo.message);
    }
  };

  // Render actions for each row
  const renderActions = (transaction: Transaction) => (
    <Box sx={{ display: 'flex', gap: 0.5 }}>
      <Tooltip title="View Details">
        <IconButton size="small">
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>

      <ShowIfPermitted permission="TXN_REVERSAL_REQUEST">
        {transaction.status === 'COMPLETED' && (
          <Tooltip title="Request Reversal">
            <IconButton
              size="small"
              color="warning"
              onClick={(e) => {
                e.stopPropagation();
                handleReversalRequest(transaction);
              }}
            >
              <Undo fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
      </ShowIfPermitted>
    </Box>
  );

  return (
    <Box>
      {/* Header */}
      <Box
        sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          justifyContent: 'space-between',
          alignItems: { xs: 'stretch', sm: 'center' },
          mb: 3,
          gap: 2,
        }}
      >
        <Typography variant={isMobile ? 'h5' : 'h4'} fontWeight="bold">
          Transaction Management
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadTransactions}
            disabled={loading}
            size={isMobile ? 'small' : 'medium'}
          >
            Refresh
          </Button>
          <ShowIfPermitted permission="TXN_EXPORT">
            <Button
              variant="contained"
              startIcon={<FileDownload />}
              onClick={handleExport}
              disabled={exporting || loading}
              size={isMobile ? 'small' : 'medium'}
            >
              {exporting ? 'Exporting...' : 'Export CSV'}
            </Button>
          </ShowIfPermitted>
        </Box>
      </Box>

      {/* Error Banner */}
      {error && (
        <ErrorBanner
          error={error}
          requestId={requestId}
          onRetry={loadTransactions}
          onClose={() => setError(null)}
        />
      )}

      {/* Filters */}
      <FilterPanel
        fields={filterFields}
        values={filters}
        onChange={setFilters}
        onSearch={handleSearch}
        onClear={handleClearFilters}
        searchPlaceholder="Search by reference, account..."
      />

      {/* Data Table/Cards */}
      <ResponsiveTable
        columns={columns}
        data={transactions}
        loading={loading}
        error={null}
        keyExtractor={(row) => row.id}
        actions={renderActions}
        cardTitle={(row) => row.transactionReference}
        cardSubtitle={(row) => formatCurrency(row.amount, row.currency)}
        pagination={{
          page,
          rowsPerPage,
          totalCount,
          onPageChange: setPage,
          onRowsPerPageChange: (size) => {
            setRowsPerPage(size);
            setPage(0);
          },
        }}
        emptyMessage="No transactions found."
      />

      {/* Reversal Request Dialog */}
      <Dialog open={reversalDialogOpen} onClose={() => setReversalDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Request Transaction Reversal</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Transaction: {selectedTransaction?.transactionReference}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Amount: {selectedTransaction && formatCurrency(selectedTransaction.amount, selectedTransaction.currency)}
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Reason for Reversal"
            value={reversalReason}
            onChange={(e) => setReversalReason(e.target.value)}
            placeholder="Please provide a reason for this reversal request..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReversalDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="warning"
            onClick={submitReversalRequest}
            disabled={!reversalReason.trim()}
          >
            Submit Request
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Transactions;
