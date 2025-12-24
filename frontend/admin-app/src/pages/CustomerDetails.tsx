import React from 'react';
import { Box, Typography, Paper, Grid, Chip, Button, Divider } from '@mui/material';
import { useParams, useNavigate } from 'react-router-dom';
import { Email, Phone, CalendarToday, AccountBalance, Block, CheckCircle } from '@mui/icons-material';

const CustomerDetails: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  // Mock data - replace with API call
  const customer = {
    userId: id,
    firstName: 'John',
    lastName: 'Doe',
    username: 'john_doe',
    email: 'john@example.com',
    phone: '(123) 456-7890',
    status: 'ACTIVE',
    createdAt: new Date().toISOString(),
    accounts: 3,
    totalBalance: 125000,
  };

  return (
    <Box>
      <Button variant="text" onClick={() => navigate('/customers')} sx={{ mb: 2 }}>
        ← Back to Customers
      </Button>

      <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
        <Box display="flex" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography variant="h4" fontWeight="bold" gutterBottom>
              {customer.firstName} {customer.lastName}
            </Typography>
            <Typography variant="body1" color="textSecondary">
              @{customer.username}
            </Typography>
          </Box>
          <Chip
            label={customer.status}
            color={customer.status === 'ACTIVE' ? 'success' : 'default'}
            icon={customer.status === 'ACTIVE' ? <CheckCircle /> : <Block />}
          />
        </Box>
        <Divider sx={{ my: 2 }} />
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Box display="flex" alignItems="center" gap={1} mb={1}>
              <Email fontSize="small" />
              <Typography variant="body2">{customer.email}</Typography>
            </Box>
            <Box display="flex" alignItems="center" gap={1}>
              <Phone fontSize="small" />
              <Typography variant="body2">{customer.phone}</Typography>
            </Box>
          </Grid>
          <Grid item xs={12} md={6}>
            <Box display="flex" alignItems="center" gap={1} mb={1}>
              <CalendarToday fontSize="small" />
              <Typography variant="body2">Joined: {new Date(customer.createdAt).toLocaleDateString()}</Typography>
            </Box>
            <Box display="flex" alignItems="center" gap={1}>
              <AccountBalance fontSize="small" />
              <Typography variant="body2">{customer.accounts} Accounts</Typography>
            </Box>
          </Grid>
        </Grid>
      </Paper>

      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Paper elevation={3} sx={{ p: 2 }}>
            <Typography variant="h6">Total Balance</Typography>
            <Typography variant="h4" color="primary">${customer.totalBalance.toLocaleString()}</Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper elevation={3} sx={{ p: 2 }}>
            <Typography variant="h6">Accounts</Typography>
            <Typography variant="h4">{customer.accounts}</Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper elevation={3} sx={{ p: 2 }}>
            <Typography variant="h6">Status</Typography>
            <Typography variant="h4">{customer.status}</Typography>
          </Paper>
        </Grid>
      </Grid>

      <Box mt={3}>
        <Button variant="contained" color="primary" sx={{ mr: 2 }}>
          Edit Customer
        </Button>
        <Button variant="outlined" color="error">
          Suspend Account
        </Button>
      </Box>
    </Box>
  );
};

export default CustomerDetails;
