/**
 * Customers Page with Responsive Design and Permission-based Actions
 */

import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Chip,
  IconButton,
  useTheme,
  useMediaQuery,
  Tooltip,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  Visibility,
  Refresh as RefreshIcon,
  PersonAdd as PersonAddIcon,
  MoreVert,
  Edit,
  Block,
  CheckCircle,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { customersApi } from '../api/customers';
import { formatDate, formatPhoneNumber } from '../utils/format';
import Loading from '../components/common/Loading';
import ErrorBanner, { parseApiError } from '../components/common/ErrorBanner';
import FilterPanel, { FilterField, FilterValues } from '../components/common/FilterPanel';
import ResponsiveTable, { Column } from '../components/common/ResponsiveTable';
import { useRBAC } from '../hooks/useRBAC';
import { ShowIfPermitted } from '../components/common/PrivateRoute';
import { Customer, KycStatus, CustomerStatus } from '../types';

const Customers: React.FC = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const { hasPermission } = useRBAC();

  const [loading, setLoading] = useState(true);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);

  // Filter state
  const [filters, setFilters] = useState<FilterValues>({
    search: '',
    status: '',
    kycStatus: '',
  });

  // Action menu state
  const [actionMenuAnchor, setActionMenuAnchor] = useState<null | HTMLElement>(null);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);

  // Filter configuration
  const filterFields: FilterField[] = [
    {
      id: 'status',
      label: 'Status',
      type: 'select',
      options: [
        { value: 'ACTIVE', label: 'Active' },
        { value: 'INACTIVE', label: 'Inactive' },
        { value: 'SUSPENDED', label: 'Suspended' },
        { value: 'LOCKED', label: 'Locked' },
      ],
      gridSize: 2,
    },
    {
      id: 'kycStatus',
      label: 'KYC Status',
      type: 'select',
      options: [
        { value: 'PENDING', label: 'Pending' },
        { value: 'IN_PROGRESS', label: 'In Progress' },
        { value: 'VERIFIED', label: 'Verified' },
        { value: 'REJECTED', label: 'Rejected' },
        { value: 'EXPIRED', label: 'Expired' },
      ],
      gridSize: 2,
    },
  ];

  // Table columns
  const columns: Column<Customer>[] = [
    {
      id: 'id',
      label: 'ID',
      accessor: (row) => (row.id || row.userId || '').toString().substring(0, 8) + '...',
      priority: 5,
      hideOnMobile: true,
    },
    {
      id: 'name',
      label: 'Name',
      accessor: (row) => `${row.firstName} ${row.lastName}`,
      priority: 1,
    },
    {
      id: 'email',
      label: 'Email',
      accessor: 'email',
      priority: 2,
    },
    {
      id: 'phone',
      label: 'Phone',
      accessor: (row) => formatPhoneNumber(row.phone || row.phoneNumber || 'N/A'),
      priority: 4,
      hideOnMobile: true,
    },
    {
      id: 'kycStatus',
      label: 'KYC',
      accessor: 'kycStatus',
      format: (value: KycStatus) => (
        <Chip
          label={value || 'PENDING'}
          size="small"
          color={getKycStatusColor(value)}
        />
      ),
      priority: 3,
    },
    {
      id: 'status',
      label: 'Status',
      accessor: 'status',
      format: (value: CustomerStatus) => (
        <Chip
          label={value || 'ACTIVE'}
          size="small"
          variant="outlined"
          color={getStatusColor(value)}
        />
      ),
      priority: 6,
      hideOnMobile: true,
    },
    {
      id: 'createdAt',
      label: 'Joined',
      accessor: (row) => formatDate(row.createdAt),
      priority: 7,
      hideOnMobile: true,
    },
  ];

  const getKycStatusColor = (status: KycStatus): 'success' | 'warning' | 'info' | 'error' | 'default' => {
    switch (status) {
      case 'VERIFIED': return 'success';
      case 'PENDING': return 'warning';
      case 'IN_PROGRESS': return 'info';
      case 'REJECTED': return 'error';
      case 'EXPIRED': return 'default';
      default: return 'default';
    }
  };

  const getStatusColor = (status: CustomerStatus): 'success' | 'warning' | 'error' | 'default' => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'INACTIVE': return 'default';
      case 'SUSPENDED': return 'warning';
      case 'LOCKED': return 'error';
      default: return 'default';
    }
  };

  const loadCustomers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await customersApi.getCustomers({
        page,
        size: rowsPerPage,
        search: filters.search,
        status: filters.status,
        kycStatus: filters.kycStatus,
      });

      setCustomers(response.content || []);
      setTotalCount(response.totalElements || 0);
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(errorInfo.message);
      setRequestId(errorInfo.requestId || null);
      setCustomers([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, filters]);

  useEffect(() => {
    loadCustomers();
  }, [loadCustomers]);

  const handleSearch = () => {
    setPage(0);
    loadCustomers();
  };

  const handleClearFilters = () => {
    setFilters({ search: '', status: '', kycStatus: '' });
    setPage(0);
  };

  const handleView = (customer: Customer) => {
    navigate(`/customers/${customer.id || customer.userId}`);
  };

  const handleActionMenuOpen = (event: React.MouseEvent<HTMLElement>, customer: Customer) => {
    event.stopPropagation();
    setActionMenuAnchor(event.currentTarget);
    setSelectedCustomer(customer);
  };

  const handleActionMenuClose = () => {
    setActionMenuAnchor(null);
    setSelectedCustomer(null);
  };

  const handleStatusChange = async (status: CustomerStatus) => {
    if (!selectedCustomer) return;
    
    try {
      await customersApi.updateCustomerStatus(selectedCustomer.id || selectedCustomer.userId, status);
      loadCustomers();
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(errorInfo.message);
    }
    handleActionMenuClose();
  };

  // Render actions for each row
  const renderActions = (customer: Customer) => (
    <Box sx={{ display: 'flex', gap: 0.5 }}>
      <Tooltip title="View Details">
        <IconButton size="small" onClick={() => handleView(customer)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>
      
      <ShowIfPermitted permission="CUSTOMER_UPDATE_STATUS">
        <Tooltip title="More Actions">
          <IconButton
            size="small"
            onClick={(e) => handleActionMenuOpen(e, customer)}
          >
            <MoreVert fontSize="small" />
          </IconButton>
        </Tooltip>
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
          Customer Management
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadCustomers}
            disabled={loading}
            size={isMobile ? 'small' : 'medium'}
          >
            Refresh
          </Button>
          <ShowIfPermitted permission="CUSTOMER_UPDATE_STATUS">
            <Button
              variant="contained"
              startIcon={<PersonAddIcon />}
              size={isMobile ? 'small' : 'medium'}
            >
              Add Customer
            </Button>
          </ShowIfPermitted>
        </Box>
      </Box>

      {/* Error Banner */}
      {error && (
        <ErrorBanner
          error={error}
          requestId={requestId}
          onRetry={loadCustomers}
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
        searchPlaceholder="Search by name, email, or phone..."
      />

      {/* Data Table/Cards */}
      <ResponsiveTable
        columns={columns}
        data={customers}
        loading={loading}
        error={null}
        keyExtractor={(row) => row.id || row.userId}
        onRowClick={handleView}
        actions={renderActions}
        cardTitle={(row) => `${row.firstName} ${row.lastName}`}
        cardSubtitle={(row) => row.email}
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
        emptyMessage={
          filters.search || filters.status || filters.kycStatus
            ? 'No customers found matching your filters.'
            : 'No customers registered yet.'
        }
      />

      {/* Action Menu */}
      <Menu
        anchorEl={actionMenuAnchor}
        open={Boolean(actionMenuAnchor)}
        onClose={handleActionMenuClose}
      >
        <MenuItem onClick={() => handleView(selectedCustomer!)}>
          <ListItemIcon>
            <Visibility fontSize="small" />
          </ListItemIcon>
          <ListItemText>View Details</ListItemText>
        </MenuItem>
        
        {hasPermission('CUSTOMER_UPDATE_STATUS') && (
          <>
            <MenuItem
              onClick={() => handleStatusChange('ACTIVE')}
              disabled={selectedCustomer?.status === 'ACTIVE'}
            >
              <ListItemIcon>
                <CheckCircle fontSize="small" color="success" />
              </ListItemIcon>
              <ListItemText>Activate</ListItemText>
            </MenuItem>
            <MenuItem
              onClick={() => handleStatusChange('SUSPENDED')}
              disabled={selectedCustomer?.status === 'SUSPENDED'}
            >
              <ListItemIcon>
                <Block fontSize="small" color="warning" />
              </ListItemIcon>
              <ListItemText>Suspend</ListItemText>
            </MenuItem>
          </>
        )}
        
        {hasPermission('CUSTOMER_KYC_UPDATE') && (
          <MenuItem onClick={() => navigate(`/customers/${selectedCustomer?.id}/kyc`)}>
            <ListItemIcon>
              <Edit fontSize="small" />
            </ListItemIcon>
            <ListItemText>Update KYC</ListItemText>
          </MenuItem>
        )}
      </Menu>
    </Box>
  );
};

export default Customers;
