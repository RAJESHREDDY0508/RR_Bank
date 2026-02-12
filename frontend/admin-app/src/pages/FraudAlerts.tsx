/**
 * Fraud Alerts Page with Responsive Design and Permission-based Actions
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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  LinearProgress,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Visibility,
  CheckCircle,
  Cancel,
  Flag,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { fraudAlertsApi } from '../api/fraudAlerts';
import { formatDateTime, formatCurrency } from '../utils/format';
import ErrorBanner, { parseApiError } from '../components/common/ErrorBanner';
import FilterPanel, { FilterField, FilterValues } from '../components/common/FilterPanel';
import ResponsiveTable, { Column } from '../components/common/ResponsiveTable';
import { useRBAC } from '../hooks/useRBAC';
import { ShowIfPermitted } from '../components/common/PrivateRoute';
import { FraudAlert, FraudAlertStatus } from '../types';
import { toast } from 'react-toastify';

const FraudAlerts: React.FC = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { hasPermission } = useRBAC();

  const [loading, setLoading] = useState(true);
  const [alerts, setAlerts] = useState<FraudAlert[]>([]);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);

  // Resolution dialog state
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [selectedAlert, setSelectedAlert] = useState<FraudAlert | null>(null);
  const [resolution, setResolution] = useState<FraudAlertStatus>('FALSE_POSITIVE');
  const [resolutionNotes, setResolutionNotes] = useState('');

  // Filter state
  const [filters, setFilters] = useState<FilterValues>({
    search: '',
    status: '',
    eventType: '',
  });

  // Filter configuration
  const filterFields: FilterField[] = [
    {
      id: 'status',
      label: 'Status',
      type: 'select',
      options: [
        { value: 'PENDING', label: 'Pending' },
        { value: 'CONFIRMED_FRAUD', label: 'Confirmed Fraud' },
        { value: 'FALSE_POSITIVE', label: 'False Positive' },
        { value: 'NEEDS_REVIEW', label: 'Needs Review' },
      ],
      gridSize: 2,
    },
    {
      id: 'eventType',
      label: 'Event Type',
      type: 'select',
      options: [
        { value: 'SUSPICIOUS_LOGIN', label: 'Suspicious Login' },
        { value: 'LARGE_TRANSACTION', label: 'Large Transaction' },
        { value: 'UNUSUAL_PATTERN', label: 'Unusual Pattern' },
        { value: 'MULTIPLE_FAILED_ATTEMPTS', label: 'Multiple Failed Attempts' },
      ],
      gridSize: 2,
    },
  ];

  // Table columns
  const columns: Column<FraudAlert>[] = [
    {
      id: 'accountNumber',
      label: 'Account',
      accessor: 'accountNumber',
      priority: 1,
    },
    {
      id: 'customerName',
      label: 'Customer',
      accessor: 'customerName',
      priority: 2,
    },
    {
      id: 'eventType',
      label: 'Event',
      accessor: 'eventType',
      format: (value: string) => (
        <Chip
          label={value?.replace(/_/g, ' ') || 'UNKNOWN'}
          size="small"
          color="warning"
          variant="outlined"
        />
      ),
      priority: 3,
    },
    {
      id: 'riskScore',
      label: 'Risk',
      accessor: 'riskScore',
      format: (value: number) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 100 }}>
          <LinearProgress
            variant="determinate"
            value={value}
            color={value >= 80 ? 'error' : value >= 50 ? 'warning' : 'success'}
            sx={{ flexGrow: 1, height: 8, borderRadius: 4 }}
          />
          <Typography variant="caption" fontWeight="bold">
            {value}%
          </Typography>
        </Box>
      ),
      priority: 4,
    },
    {
      id: 'status',
      label: 'Status',
      accessor: 'status',
      format: (value: FraudAlertStatus) => (
        <Chip
          label={value?.replace(/_/g, ' ') || 'PENDING'}
          size="small"
          color={getStatusColor(value)}
        />
      ),
      priority: 5,
    },
    {
      id: 'createdAt',
      label: 'Date',
      accessor: (row) => formatDateTime(row.createdAt),
      priority: 6,
      hideOnMobile: true,
    },
  ];

  const getStatusColor = (status: FraudAlertStatus): 'warning' | 'error' | 'success' | 'info' | 'default' => {
    switch (status) {
      case 'PENDING': return 'warning';
      case 'CONFIRMED_FRAUD': return 'error';
      case 'FALSE_POSITIVE': return 'success';
      case 'NEEDS_REVIEW': return 'info';
      default: return 'default';
    }
  };

  const loadAlerts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fraudAlertsApi.getAlerts({
        page,
        size: rowsPerPage,
        search: filters.search,
        status: filters.status,
        eventType: filters.eventType,
      });

      setAlerts(response.content || []);
      setTotalCount(response.totalElements || 0);
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(errorInfo.message);
      setRequestId(errorInfo.requestId || null);
      setAlerts([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, filters]);

  useEffect(() => {
    loadAlerts();
  }, [loadAlerts]);

  const handleSearch = () => {
    setPage(0);
    loadAlerts();
  };

  const handleClearFilters = () => {
    setFilters({ search: '', status: '', eventType: '' });
    setPage(0);
  };

  const handleView = (alert: FraudAlert) => {
    navigate(`/fraud-alerts/${alert.id}`);
  };

  const handleOpenResolve = (alert: FraudAlert) => {
    setSelectedAlert(alert);
    setResolution('FALSE_POSITIVE');
    setResolutionNotes('');
    setResolveDialogOpen(true);
  };

  const handleResolve = async () => {
    if (!selectedAlert || !resolutionNotes.trim()) return;

    try {
      await fraudAlertsApi.resolveAlert(selectedAlert.id, {
        status: resolution,
        notes: resolutionNotes,
      });
      toast.success('Alert resolved successfully');
      setResolveDialogOpen(false);
      loadAlerts();
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      toast.error(errorInfo.message);
    }
  };

  // Render actions for each row
  const renderActions = (alert: FraudAlert) => (
    <Box sx={{ display: 'flex', gap: 0.5 }}>
      <Tooltip title="View Details">
        <IconButton size="small" onClick={() => handleView(alert)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>

      <ShowIfPermitted permission="FRAUD_ALERT_MANAGE">
        {alert.status === 'PENDING' && (
          <Tooltip title="Resolve Alert">
            <IconButton
              size="small"
              color="primary"
              onClick={(e) => {
                e.stopPropagation();
                handleOpenResolve(alert);
              }}
            >
              <Flag fontSize="small" />
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
          Fraud Alerts
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadAlerts}
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
          onRetry={loadAlerts}
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
        searchPlaceholder="Search by account, customer..."
      />

      {/* Data Table/Cards */}
      <ResponsiveTable
        columns={columns}
        data={alerts}
        loading={loading}
        error={null}
        keyExtractor={(row) => row.id}
        onRowClick={handleView}
        actions={renderActions}
        cardTitle={(row) => row.accountNumber}
        cardSubtitle={(row) => row.customerName}
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
        emptyMessage="No fraud alerts found."
      />

      {/* Resolution Dialog */}
      <Dialog open={resolveDialogOpen} onClose={() => setResolveDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Resolve Fraud Alert</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Account: {selectedAlert?.accountNumber}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Event: {selectedAlert?.eventType?.replace(/_/g, ' ')}
            </Typography>

            <FormControl fullWidth sx={{ mb: 2 }}>
              <InputLabel>Resolution</InputLabel>
              <Select
                value={resolution}
                onChange={(e) => setResolution(e.target.value as FraudAlertStatus)}
                label="Resolution"
              >
                <MenuItem value="CONFIRMED_FRAUD">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Cancel color="error" fontSize="small" />
                    Confirmed Fraud
                  </Box>
                </MenuItem>
                <MenuItem value="FALSE_POSITIVE">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <CheckCircle color="success" fontSize="small" />
                    False Positive
                  </Box>
                </MenuItem>
                <MenuItem value="NEEDS_REVIEW">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Flag color="info" fontSize="small" />
                    Needs Further Review
                  </Box>
                </MenuItem>
              </Select>
            </FormControl>

            <TextField
              fullWidth
              multiline
              rows={3}
              label="Resolution Notes"
              value={resolutionNotes}
              onChange={(e) => setResolutionNotes(e.target.value)}
              placeholder="Provide details about this resolution..."
              required
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResolveDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleResolve}
            disabled={!resolutionNotes.trim()}
            color={resolution === 'CONFIRMED_FRAUD' ? 'error' : 'primary'}
          >
            Resolve
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default FraudAlerts;
