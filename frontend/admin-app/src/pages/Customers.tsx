import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  IconButton,
  InputAdornment,
  Alert,
} from '@mui/material';
import {
  Search,
  Visibility,
  Block,
  CheckCircle,
  Refresh as RefreshIcon,
  PersonAdd as PersonAddIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { customersApi } from '../api/customers';
import { formatDate, formatPhoneNumber } from '../utils/format';
import Loading from '../components/common/Loading';

const Customers: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [customers, setCustomers] = useState<any[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadCustomers();
  }, [page, rowsPerPage]);

  // Debounced search
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (page === 0) {
        loadCustomers();
      } else {
        setPage(0); // This will trigger loadCustomers via the other useEffect
      }
    }, 300);
    return () => clearTimeout(timeoutId);
  }, [searchTerm]);

  const loadCustomers = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await customersApi.getAll({ 
        page, 
        size: rowsPerPage, 
        search: searchTerm 
      });
      
      // Handle the response - it's paginated
      const data = response.data;
      setCustomers(data.content || []);
      setTotalCount(data.totalElements || 0);
    } catch (err: any) {
      console.error('Error loading customers:', err);
      setError(err.response?.data?.message || 'Failed to load customers. Please try again.');
      setCustomers([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
  };

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const getKycStatusColor = (status: string) => {
    switch (status) {
      case 'VERIFIED': return 'success';
      case 'PENDING': return 'warning';
      case 'IN_PROGRESS': return 'info';
      case 'REJECTED': return 'error';
      case 'EXPIRED': return 'default';
      default: return 'default';
    }
  };

  if (loading && customers.length === 0 && !error) {
    return <Loading text="Loading customers..." />;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight="bold">
          Customer Management
        </Typography>
        <Box>
          <Button 
            variant="outlined" 
            startIcon={<RefreshIcon />}
            onClick={loadCustomers}
            sx={{ mr: 1 }}
          >
            Refresh
          </Button>
          <Button 
            variant="contained" 
            color="primary"
            startIcon={<PersonAddIcon />}
          >
            Add Customer
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
        <TextField
          fullWidth
          placeholder="Search by name, email, or phone..."
          value={searchTerm}
          onChange={handleSearch}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search />
              </InputAdornment>
            ),
          }}
        />
      </Paper>

      <Paper elevation={3}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Customer ID</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>KYC Status</TableCell>
                <TableCell>Segment</TableCell>
                <TableCell>Joined</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 5 }}>
                    <Loading text="Loading..." />
                  </TableCell>
                </TableRow>
              ) : customers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 5 }}>
                    <Typography color="text.secondary">
                      {searchTerm ? 'No customers found matching your search.' : 'No customers registered yet.'}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                customers.map((customer) => (
                  <TableRow key={customer.id || customer.userId} hover>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                        {(customer.id || customer.userId || '').toString().substring(0, 8)}...
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {customer.firstName} {customer.lastName}
                      </Typography>
                    </TableCell>
                    <TableCell>{customer.email || 'N/A'}</TableCell>
                    <TableCell>{formatPhoneNumber(customer.phone || customer.phoneNumber || 'N/A')}</TableCell>
                    <TableCell>
                      <Chip
                        label={customer.kycStatus || 'PENDING'}
                        size="small"
                        color={getKycStatusColor(customer.kycStatus) as any}
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={customer.customerSegment || 'REGULAR'}
                        size="small"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>{formatDate(customer.createdAt)}</TableCell>
                    <TableCell align="right">
                      <IconButton
                        size="small"
                        color="primary"
                        onClick={() => navigate(`/customers/${customer.id || customer.userId}`)}
                        title="View Details"
                      >
                        <Visibility />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={totalCount}
          page={page}
          onPageChange={handleChangePage}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={handleChangeRowsPerPage}
          rowsPerPageOptions={[5, 10, 25, 50]}
        />
      </Paper>
    </Box>
  );
};

export default Customers;
