/**
 * Audit Logs Page with Responsive Design
 */

import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Button,
  Chip,
  useTheme,
  useMediaQuery,
  Tooltip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Divider,
  Grid,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Visibility,
} from '@mui/icons-material';
import { auditLogsApi } from '../api/auditLogs';
import { formatDateTime } from '../utils/format';
import ErrorBanner, { parseApiError } from '../components/common/ErrorBanner';
import FilterPanel, { FilterField, FilterValues } from '../components/common/FilterPanel';
import ResponsiveTable, { Column } from '../components/common/ResponsiveTable';
import { AuditLog } from '../types';

const AuditLogs: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const [loading, setLoading] = useState(true);
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);

  // Detail dialog state
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  // Filter state
  const [filters, setFilters] = useState<FilterValues>({
    search: '',
    action: '',
    entityType: '',
    status: '',
  });

  // Filter configuration
  const filterFields: FilterField[] = [
    {
      id: 'action',
      label: 'Action',
      type: 'select',
      options: [
        { value: 'CREATE', label: 'Create' },
        { value: 'UPDATE', label: 'Update' },
        { value: 'DELETE', label: 'Delete' },
        { value: 'LOGIN', label: 'Login' },
        { value: 'LOGOUT', label: 'Logout' },
        { value: 'VIEW', label: 'View' },
      ],
      gridSize: 2,
    },
    {
      id: 'entityType',
      label: 'Entity Type',
      type: 'select',
      options: [
        { value: 'CUSTOMER', label: 'Customer' },
        { value: 'ACCOUNT', label: 'Account' },
        { value: 'TRANSACTION', label: 'Transaction' },
        { value: 'ADMIN_USER', label: 'Admin User' },
        { value: 'FRAUD_ALERT', label: 'Fraud Alert' },
        { value: 'SETTINGS', label: 'Settings' },
      ],
      gridSize: 2,
    },
    {
      id: 'status',
      label: 'Status',
      type: 'select',
      options: [
        { value: 'SUCCESS', label: 'Success' },
        { value: 'FAILED', label: 'Failed' },
      ],
      gridSize: 2,
    },
  ];

  // Table columns
  const columns: Column<AuditLog>[] = [
    {
      id: 'adminUsername',
      label: 'Admin',
      accessor: 'adminUsername',
      priority: 1,
    },
    {
      id: 'action',
      label: 'Action',
      accessor: 'action',
      format: (value: string) => (
        <Chip
          label={value}
          size="small"
          color={getActionColor(value)}
          variant="outlined"
        />
      ),
      priority: 2,
    },
    {
      id: 'entityType',
      label: 'Entity',
      accessor: 'entityType',
      format: (value: string) => value?.replace(/_/g, ' ') || '-',
      priority: 3,
    },
    {
      id: 'description',
      label: 'Description',
      accessor: (row) => {
        const desc = row.description || '';
        return desc.length > 50 ? desc.substring(0, 50) + '...' : desc;
      },
      priority: 4,
      hideOnMobile: true,
    },
    {
      id: 'status',
      label: 'Status',
      accessor: 'status',
      format: (value: string) => (
        <Chip
          label={value}
          size="small"
          color={value === 'SUCCESS' ? 'success' : 'error'}
        />
      ),
      priority: 5,
    },
    {
      id: 'createdAt',
      label: 'Time',
      accessor: (row) => formatDateTime(row.createdAt),
      priority: 6,
    },
  ];

  const getActionColor = (action: string): 'success' | 'primary' | 'error' | 'warning' | 'info' | 'default' => {
    switch (action) {
      case 'CREATE': return 'success';
      case 'UPDATE': return 'primary';
      case 'DELETE': return 'error';
      case 'LOGIN': return 'info';
      case 'LOGOUT': return 'warning';
      default: return 'default';
    }
  };

  const loadLogs = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await auditLogsApi.getLogs({
        page,
        size: rowsPerPage,
        search: filters.search,
        action: filters.action,
        entityType: filters.entityType,
        status: filters.status,
      });

      setLogs(response.content || []);
      setTotalCount(response.totalElements || 0);
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(errorInfo.message);
      setRequestId(errorInfo.requestId || null);
      setLogs([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, filters]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  const handleSearch = () => {
    setPage(0);
    loadLogs();
  };

  const handleClearFilters = () => {
    setFilters({ search: '', action: '', entityType: '', status: '' });
    setPage(0);
  };

  const handleViewDetails = (log: AuditLog) => {
    setSelectedLog(log);
    setDetailDialogOpen(true);
  };

  // Render actions for each row
  const renderActions = (log: AuditLog) => (
    <Tooltip title="View Details">
      <IconButton size="small" onClick={() => handleViewDetails(log)}>
        <Visibility fontSize="small" />
      </IconButton>
    </Tooltip>
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
          Audit Logs
        </Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={loadLogs}
          disabled={loading}
          size={isMobile ? 'small' : 'medium'}
        >
          Refresh
        </Button>
      </Box>

      {/* Error Banner */}
      {error && (
        <ErrorBanner
          error={error}
          requestId={requestId}
          onRetry={loadLogs}
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
        searchPlaceholder="Search by admin, description..."
      />

      {/* Data Table/Cards */}
      <ResponsiveTable
        columns={columns}
        data={logs}
        loading={loading}
        error={null}
        keyExtractor={(row) => row.id}
        actions={renderActions}
        cardTitle={(row) => row.adminUsername}
        cardSubtitle={(row) => formatDateTime(row.createdAt)}
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
        emptyMessage="No audit logs found."
      />

      {/* Detail Dialog */}
      <Dialog
        open={detailDialogOpen}
        onClose={() => setDetailDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Audit Log Details</DialogTitle>
        <DialogContent>
          {selectedLog && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={6}>
                <Typography variant="caption" color="text.secondary">Admin</Typography>
                <Typography variant="body1">{selectedLog.adminUsername}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="caption" color="text.secondary">Action</Typography>
                <Typography variant="body1">
                  <Chip label={selectedLog.action} size="small" color={getActionColor(selectedLog.action)} />
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="caption" color="text.secondary">Entity Type</Typography>
                <Typography variant="body1">{selectedLog.entityType?.replace(/_/g, ' ') || '-'}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="caption" color="text.secondary">Entity ID</Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {selectedLog.entityId || '-'}
                </Typography>
              </Grid>
              <Grid item xs={12}>
                <Typography variant="caption" color="text.secondary">Description</Typography>
                <Typography variant="body1">{selectedLog.description || '-'}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="caption" color="text.secondary">Status</Typography>
                <Typography variant="body1">
                  <Chip
                    label={selectedLog.status}
                    size="small"
                    color={selectedLog.status === 'SUCCESS' ? 'success' : 'error'}
                  />
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="caption" color="text.secondary">IP Address</Typography>
                <Typography variant="body1">{selectedLog.ipAddress || '-'}</Typography>
              </Grid>
              <Grid item xs={12}>
                <Typography variant="caption" color="text.secondary">User Agent</Typography>
                <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
                  {selectedLog.userAgent || '-'}
                </Typography>
              </Grid>
              <Grid item xs={12}>
                <Typography variant="caption" color="text.secondary">Timestamp</Typography>
                <Typography variant="body1">{formatDateTime(selectedLog.createdAt)}</Typography>
              </Grid>
              {selectedLog.oldValue && (
                <Grid item xs={12}>
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="caption" color="text.secondary">Old Value</Typography>
                  <Box
                    sx={{
                      bgcolor: 'grey.100',
                      p: 1,
                      borderRadius: 1,
                      fontFamily: 'monospace',
                      fontSize: '0.75rem',
                      overflow: 'auto',
                      maxHeight: 150,
                    }}
                  >
                    <pre style={{ margin: 0 }}>{selectedLog.oldValue}</pre>
                  </Box>
                </Grid>
              )}
              {selectedLog.newValue && (
                <Grid item xs={12}>
                  <Typography variant="caption" color="text.secondary">New Value</Typography>
                  <Box
                    sx={{
                      bgcolor: 'grey.100',
                      p: 1,
                      borderRadius: 1,
                      fontFamily: 'monospace',
                      fontSize: '0.75rem',
                      overflow: 'auto',
                      maxHeight: 150,
                    }}
                  >
                    <pre style={{ margin: 0 }}>{selectedLog.newValue}</pre>
                  </Box>
                </Grid>
              )}
              {selectedLog.errorMessage && (
                <Grid item xs={12}>
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="caption" color="error">Error Message</Typography>
                  <Typography variant="body2" color="error">
                    {selectedLog.errorMessage}
                  </Typography>
                </Grid>
              )}
            </Grid>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AuditLogs;
