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
} from '@mui/material';
import {
  Search,
  Visibility,
  Block,
  CheckCircle,
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

  useEffect(() => {
    loadCustomers();
  }, [page, rowsPerPage, searchTerm]);

  const loadCustomers = async () => {
    try {
      setLoading(true);
      const response = await customersApi.getAll({ page, size: rowsPerPage, search: searchTerm });
      setCustomers(response.data.content || []);
      setTotalCount(response.data.totalElements || 0);
    } catch (error) {
      console.error('Error loading customers:', error);
      // Mock data for demonstration
      setCustomers([
        { userId: 1, username: 'john_doe', email: 'john@example.com', firstName: 'John', lastName: 'Doe', phone: '1234567890', status: 'ACTIVE', createdAt: new Date().toISOString() },
        { userId: 2, username: 'jane_smith', email: 'jane@example.com', firstName: 'Jane', lastName: 'Smith', phone: '0987654321', status: 'ACTIVE', createdAt: new Date().toISOString() },
      ]);
      setTotalCount(2);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
    setPage(0);
  };

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  if (loading && customers.length === 0) {
    return <Loading text="Loading customers..." />;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight="bold">
          Customer Management
        </Typography>
        <Button variant="contained" color="primary">
          Add Customer
        </Button>
      </Box>

      <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
        <TextField
          fullWidth
          placeholder="Search by name, email, or username..."
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
                <TableCell>ID</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Joined</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {customers.map((customer) => (
                <TableRow key={customer.userId} hover>
                  <TableCell>{customer.userId}</TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight="medium">
                      {customer.firstName} {customer.lastName}
                    </Typography>
                    <Typography variant="caption" color="textSecondary">
                      @{customer.username}
                    </Typography>
                  </TableCell>
                  <TableCell>{customer.email}</TableCell>
                  <TableCell>{formatPhoneNumber(customer.phone || 'N/A')}</TableCell>
                  <TableCell>
                    <Chip
                      label={customer.status}
                      size="small"
                      color={customer.status === 'ACTIVE' ? 'success' : 'default'}
                      icon={customer.status === 'ACTIVE' ? <CheckCircle /> : <Block />}
                    />
                  </TableCell>
                  <TableCell>{formatDate(customer.createdAt)}</TableCell>
                  <TableCell align="right">
                    <IconButton
                      size="small"
                      color="primary"
                      onClick={() => navigate(`/customers/${customer.userId}`)}
                    >
                      <Visibility />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
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
        />
      </Paper>
    </Box>
  );
};

export default Customers;
