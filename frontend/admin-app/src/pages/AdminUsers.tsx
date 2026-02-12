/**
 * Admin Users Page with RBAC Management
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
  OutlinedInput,
  Checkbox,
  ListItemText,
  Paper,
  Grid,
  Alert,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  PersonAdd,
  Edit,
  Delete,
  LockReset,
  Security,
} from '@mui/icons-material';
import ErrorBanner, { parseApiError } from '../components/common/ErrorBanner';
import FilterPanel, { FilterField, FilterValues } from '../components/common/FilterPanel';
import ResponsiveTable, { Column } from '../components/common/ResponsiveTable';
import { useRBAC } from '../hooks/useRBAC';
import { ShowIfPermitted } from '../components/common/PrivateRoute';
import { formatDateTime } from '../utils/format';
import { toast } from 'react-toastify';
import apiClient from '../api/client';
import { RoleName, ROLE_DESCRIPTIONS, formatRoleName } from '../types/rbac';

interface AdminUserData {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: RoleName[];
  status: string;
  department?: string;
  lastLogin?: string;
  createdAt: string;
}

interface CreateAdminRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  roles: RoleName[];
  department?: string;
}

const MENU_PROPS = {
  PaperProps: {
    style: {
      maxHeight: 300,
    },
  },
};

const AVAILABLE_ROLES: RoleName[] = [
  'SUPER_ADMIN',
  'SECURITY_ADMIN',
  'AUDITOR',
  'OPERATIONS_MANAGER',
  'KYC_COMPLIANCE',
  'FRAUD_ANALYST',
  'CUSTOMER_SUPPORT',
];

const AdminUsers: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { hasPermission, isSuperAdmin } = useRBAC();

  const [loading, setLoading] = useState(true);
  const [users, setUsers] = useState<AdminUserData[]>([]);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);

  // Create dialog state
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateAdminRequest>({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    roles: [],
    department: '',
  });

  // Edit roles dialog state
  const [editRolesDialogOpen, setEditRolesDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<AdminUserData | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<RoleName[]>([]);

  // Filter state
  const [filters, setFilters] = useState<FilterValues>({
    search: '',
    status: '',
    role: '',
  });

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
      id: 'role',
      label: 'Role',
      type: 'select',
      options: AVAILABLE_ROLES.map(r => ({ value: r, label: formatRoleName(r) })),
      gridSize: 2,
    },
  ];

  // Table columns
  const columns: Column<AdminUserData>[] = [
    {
      id: 'username',
      label: 'Username',
      accessor: 'username',
      priority: 1,
    },
    {
      id: 'name',
      label: 'Name',
      accessor: (row) => `${row.firstName} ${row.lastName}`,
      priority: 2,
    },
    {
      id: 'email',
      label: 'Email',
      accessor: 'email',
      priority: 3,
      hideOnMobile: true,
    },
    {
      id: 'roles',
      label: 'Roles',
      accessor: 'roles',
      format: (roles: RoleName[]) => (
        <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
          {roles.slice(0, 2).map(role => (
            <Chip
              key={role}
              label={formatRoleName(role)}
              size="small"
              color={role === 'SUPER_ADMIN' ? 'error' : 'primary'}
              variant="outlined"
            />
          ))}
          {roles.length > 2 && (
            <Chip label={`+${roles.length - 2}`} size="small" variant="outlined" />
          )}
        </Box>
      ),
      priority: 4,
    },
    {
      id: 'status',
      label: 'Status',
      accessor: 'status',
      format: (value: string) => (
        <Chip
          label={value}
          size="small"
          color={value === 'ACTIVE' ? 'success' : value === 'LOCKED' ? 'error' : 'default'}
        />
      ),
      priority: 5,
    },
    {
      id: 'lastLogin',
      label: 'Last Login',
      accessor: (row) => row.lastLogin ? formatDateTime(row.lastLogin) : 'Never',
      priority: 6,
      hideOnMobile: true,
    },
  ];

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.get('/admin/users', {
        params: {
          page,
          size: rowsPerPage,
          search: filters.search,
          status: filters.status,
          role: filters.role,
        },
      });

      const data = response.data.data || response.data;
      setUsers(data.content || []);
      setTotalCount(data.totalElements || 0);
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(errorInfo.message);
      setRequestId(errorInfo.requestId || null);
      setUsers([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, filters]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const handleSearch = () => {
    setPage(0);
    loadUsers();
  };

  const handleClearFilters = () => {
    setFilters({ search: '', status: '', role: '' });
    setPage(0);
  };

  const handleCreateUser = async () => {
    try {
      await apiClient.post('/admin/users', createForm);
      toast.success('Admin user created successfully');
      setCreateDialogOpen(false);
      setCreateForm({
        username: '',
        email: '',
        password: '',
        firstName: '',
        lastName: '',
        roles: [],
        department: '',
      });
      loadUsers();
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      toast.error(errorInfo.message);
    }
  };

  const handleEditRoles = (user: AdminUserData) => {
    setSelectedUser(user);
    setSelectedRoles(user.roles);
    setEditRolesDialogOpen(true);
  };

  const handleSaveRoles = async () => {
    if (!selectedUser) return;

    try {
      await apiClient.put(`/admin/users/${selectedUser.id}/roles`, {
        roles: selectedRoles,
      });
      toast.success('Roles updated successfully');
      setEditRolesDialogOpen(false);
      loadUsers();
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      toast.error(errorInfo.message);
    }
  };

  const handleResetPassword = async (user: AdminUserData) => {
    if (!confirm(`Reset password for ${user.username}?`)) return;

    try {
      const response = await apiClient.post(`/admin/users/${user.id}/reset-password`);
      const tempPassword = response.data.data || response.data.message;
      toast.success(`Password reset. Temporary password: ${tempPassword}`);
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      toast.error(errorInfo.message);
    }
  };

  const handleToggleStatus = async (user: AdminUserData) => {
    const newStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    
    try {
      await apiClient.put(`/admin/users/${user.id}/status`, { status: newStatus });
      toast.success(`User ${newStatus.toLowerCase()}`);
      loadUsers();
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      toast.error(errorInfo.message);
    }
  };

  // Render actions for each row
  const renderActions = (user: AdminUserData) => (
    <Box sx={{ display: 'flex', gap: 0.5 }}>
      <ShowIfPermitted permission="RBAC_MANAGE">
        <Tooltip title="Edit Roles">
          <IconButton size="small" onClick={() => handleEditRoles(user)}>
            <Security fontSize="small" />
          </IconButton>
        </Tooltip>
      </ShowIfPermitted>

      <ShowIfPermitted permission="ADMIN_USER_MANAGE">
        <Tooltip title="Reset Password">
          <IconButton size="small" onClick={() => handleResetPassword(user)}>
            <LockReset fontSize="small" />
          </IconButton>
        </Tooltip>
      </ShowIfPermitted>
    </Box>
  );

  const canManageRole = (role: RoleName) => {
    // Only SUPER_ADMIN can assign SUPER_ADMIN role
    if (role === 'SUPER_ADMIN') {
      return isSuperAdmin();
    }
    return hasPermission('RBAC_MANAGE');
  };

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
          Admin User Management
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadUsers}
            disabled={loading}
            size={isMobile ? 'small' : 'medium'}
          >
            Refresh
          </Button>
          <ShowIfPermitted permission="ADMIN_USER_MANAGE">
            <Button
              variant="contained"
              startIcon={<PersonAdd />}
              onClick={() => setCreateDialogOpen(true)}
              size={isMobile ? 'small' : 'medium'}
            >
              Add Admin
            </Button>
          </ShowIfPermitted>
        </Box>
      </Box>

      {/* Error Banner */}
      {error && (
        <ErrorBanner
          error={error}
          requestId={requestId}
          onRetry={loadUsers}
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
        searchPlaceholder="Search by username, email..."
      />

      {/* Data Table/Cards */}
      <ResponsiveTable
        columns={columns}
        data={users}
        loading={loading}
        error={null}
        keyExtractor={(row) => row.id}
        actions={renderActions}
        cardTitle={(row) => row.username}
        cardSubtitle={(row) => `${row.firstName} ${row.lastName}`}
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
        emptyMessage="No admin users found."
      />

      {/* Create Admin Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Admin User</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Username"
                value={createForm.username}
                onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Email"
                type="email"
                value={createForm.email}
                onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="First Name"
                value={createForm.firstName}
                onChange={(e) => setCreateForm({ ...createForm, firstName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Last Name"
                value={createForm.lastName}
                onChange={(e) => setCreateForm({ ...createForm, lastName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Password"
                type="password"
                value={createForm.password}
                onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Department"
                value={createForm.department}
                onChange={(e) => setCreateForm({ ...createForm, department: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Roles</InputLabel>
                <Select
                  multiple
                  value={createForm.roles}
                  onChange={(e) => setCreateForm({ ...createForm, roles: e.target.value as RoleName[] })}
                  input={<OutlinedInput label="Roles" />}
                  renderValue={(selected) => selected.map(formatRoleName).join(', ')}
                  MenuProps={MENU_PROPS}
                >
                  {AVAILABLE_ROLES.filter(canManageRole).map((role) => (
                    <MenuItem key={role} value={role}>
                      <Checkbox checked={createForm.roles.includes(role)} />
                      <ListItemText
                        primary={formatRoleName(role)}
                        secondary={ROLE_DESCRIPTIONS[role]}
                      />
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreateUser}
            disabled={!createForm.username || !createForm.email || !createForm.password || createForm.roles.length === 0}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Roles Dialog */}
      <Dialog open={editRolesDialogOpen} onClose={() => setEditRolesDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Roles for {selectedUser?.username}</DialogTitle>
        <DialogContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Select roles to assign to this user. Permissions are automatically granted based on roles.
          </Alert>
          <FormControl fullWidth sx={{ mt: 1 }}>
            <InputLabel>Roles</InputLabel>
            <Select
              multiple
              value={selectedRoles}
              onChange={(e) => setSelectedRoles(e.target.value as RoleName[])}
              input={<OutlinedInput label="Roles" />}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => (
                    <Chip key={value} label={formatRoleName(value)} size="small" />
                  ))}
                </Box>
              )}
              MenuProps={MENU_PROPS}
            >
              {AVAILABLE_ROLES.filter(canManageRole).map((role) => (
                <MenuItem key={role} value={role}>
                  <Checkbox checked={selectedRoles.includes(role)} />
                  <ListItemText
                    primary={formatRoleName(role)}
                    secondary={ROLE_DESCRIPTIONS[role]}
                  />
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditRolesDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSaveRoles}
            disabled={selectedRoles.length === 0}
          >
            Save Roles
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AdminUsers;
