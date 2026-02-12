/**
 * Accounts Page with Responsive Design and Permission-based Actions
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
  Visibility,
  Lock,
  LockOpen,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { accountsApi } from '../api/accounts';
import { formatCurrency, formatDateTime } from '../utils/format';
import ErrorBanner, { parseApiError } from '../components/common/ErrorBanner';
import FilterPanel, { FilterField, FilterValues } from '../components/common/FilterPanel';
import ResponsiveTable, { Column } from '../components/common/ResponsiveTable';
import { useRBAC } from '../hooks/useRBAC';
import { ShowIfPermitted } from '../components/common/PrivateRoute';
import { Account, AccountStatus } from '../types';
import { toast } from 'react-toastify';

const Accounts: React.FC = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { hasPermission } = useRBAC();

  const [loading, setLoading] = useState(true);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);

  // Freeze dialog state
  const [freezeDialogOpen, setFreezeDialogOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const [freezeReason, setFreezeReason] = useState('');

  // Filter state
  const [filters, setFilters] = useState<FilterValues>({
    search: '',
    status: '',
    type: '',
  });

  // Filter configuration
  const filterFields: FilterField[] = [
    {
      id: 'status',
      label: 'Status',
      type: 'select',
      options: [
        { value: 'ACTIVE', label: 'Active' },
        { value: 'FROZEN', label: 'Frozen' },
        { value: 'CLOSED', label: 'Closed' },
        { value: 'PENDING', label: 'Pending' },
      ],
      gridSize: 2,
    },
    {
      id: 'type',
      label: 'Type',
      type: 'select',
      options: [
        { value: 'SAVINGS', label: 'Savings' },
        { value: 'CHECKING', label: 'Checking' },
        { value: 'CREDIT', label: 'Credit' },
        { value: 'BUSINESS', label: 'Business' },
      ],
      gridSize: 2,
    },
  ];

  // Table columns
  const columns: Column<Account>[] = [
    {
      id: 'accountNumber',
      label: 'Account #',
      accessor: 'accountNumber',
      priority: 1,
    },
    {
      id: 'type',
      label: 'Type',
      accessor: 'accountType',
      format: (value: string) => (
        <Chip label={value} size="small" variant="outlined" />
      ),
      priority: 2,
    },
    {
      id: 'customerName',
      label: 'Customer',
      accessor: 'customerName',
      priority: 3,
    },
    {
      id: 'balance',
      label: 'Balance',
      accessor: (row) => formatCurrency(row.balance, row.currency),
      priority: 4,
    },
    {
      id: 'status',
      label: 'Status',
      accessor: 'status',
      format: (value: AccountStatus) => (
        <Chip
          label={value}
          size="small"
          color={getStatusColor(value)}
        />
      ),
      priority: 5,
    },
    {
      id: 'createdAt',
      label: 'Created',
      accessor: (row) => formatDateTime(row.createdAt),
      priority: 6,
      hideOnMobile: true,
    },
  ];

  const getStatusColor = (status: AccountStatus): 'success' | 'error' | 'warning' | 'default' => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'FROZEN': return 'error';
      case 'PENDING': return 'warning';
      case 'CLOSED': return 'default';
      default: return 'default';
    }
  };

  const loadAccounts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await accountsApi.getAccounts({
        page,
        size: rowsPerPage,
        search: filters.search,
        status: filters.status,
        type: filters.type,
      });

      setAccounts(response.content || []);
      setTotalCount(response.totalElements || 0);
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(errorInfo.message);
      setRequestId(errorInfo.requestId || null);
      setAccounts([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, filters]);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  const handleSearch = () => {
    setPage(0);
    loadAccounts();
  };

  const handleClearFilters = () => {
    setFilters({ search: '', status: '', type: '' });
    setPage(0);
  };

  const handleView = (account: Account) => {
    navigate(`/accounts/${account.id}`);
  };

  const handleOpenFreeze = (account: Account) => {
    setSelectedAccount(account);
    setFreezeReason('');
    setFreezeDialogOpen(true);
  };

  const handleFreezeUnfreeze = async () => {
    if (!selectedAccount) return;

    try {
      const isFrozen = selectedAccount.status === 'FROZEN';
      
      if (isFrozen) {
        await accountsApi.unfreezeAccount(selectedAccount.id);
        toast.success('Account unfrozen successfully');
      } else {
        if (!freezeReason.trim()) {
          toast.error('Please provide a reason for freezing');
          return;
        }
        await accountsApi.freezeAccount(selectedAccount.id, freezeReason);
        toast.success('Account frozen successfully');
      }
      
      setFreezeDialogOpen(false);
      loadAccounts();
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      toast.error(errorInfo.message);
    }
  };

  // Render actions for each row
  const renderActions = (account: Account) => (
    <Box sx={{ display: 'flex', gap: 0.5 }}>
      <Tooltip title="View Details">
        <IconButton size="small" onClick={() => handleView(account)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>

      <ShowIfPermitted permission="ACCOUNT_UPDATE_STATUS">
        {account.status === 'FROZEN' ? (
          <Tooltip title="Unfreeze Account">
            <IconButton
              size="small"
              color="success"
              onClick={(e) => {
                e.stopPropagation();
                handleOpenFreeze(account);
              }}
            >
              <LockOpen fontSize="small" />
            </IconButton>
          </Tooltip>
        ) : account.status === 'ACTIVE' ? (
          <Tooltip title="Freeze Account">
            <IconButton
              size="small"
              color="error"
              onClick={(e) => {
                e.stopPropagation();
                handleOpenFreeze(account);
              }}
            >
              <Lock fontSize="small" />
            </IconButton>
          </Tooltip>
        ) : null}
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
          Account Management
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadAccounts}
            disabled={loading}
            size={isMobile ? 'small' : 'medium'}
          >
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Error Banner */}
      {error && (
        <ErrorBanner
          error={error}
          requestId={requestId}
          onRetry={loadAccounts}
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
        searchPlaceholder="Search by account number, customer..."
      />

      {/* Data Table/Cards */}
      <ResponsiveTable
        columns={columns}
        data={accounts}
        loading={loading}
        error={null}
        keyExtractor={(row) => row.id}
        onRowClick={handleView}
        actions={renderActions}
        cardTitle={(row) => row.accountNumber}
        cardSubtitle={(row) => formatCurrency(row.balance, row.currency)}
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
        emptyMessage="No accounts found."
      />

      {/* Freeze/Unfreeze Dialog */}
      <Dialog open={freezeDialogOpen} onClose={() => setFreezeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedAccount?.status === 'FROZEN' ? 'Unfreeze Account' : 'Freeze Account'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Account: {selectedAccount?.accountNumber}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Customer: {selectedAccount?.customerName}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Balance: {selectedAccount && formatCurrency(selectedAccount.balance, selectedAccount.currency)}
          </Typography>

          {selectedAccount?.status !== 'FROZEN' && (
            <TextField
              fullWidth
              multiline
              rows={3}
              label="Reason for Freezing"
              value={freezeReason}
              onChange={(e) => setFreezeReason(e.target.value)}
              placeholder="Please provide a reason for freezing this account..."
              required
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFreezeDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color={selectedAccount?.status === 'FROZEN' ? 'success' : 'error'}
            onClick={handleFreezeUnfreeze}
            disabled={selectedAccount?.status !== 'FROZEN' && !freezeReason.trim()}
          >
            {selectedAccount?.status === 'FROZEN' ? 'Unfreeze' : 'Freeze'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Accounts;
