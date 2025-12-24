import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

const Accounts: React.FC = () => {
  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Accounts Management
      </Typography>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="body1">
          Account management interface - View and manage all customer accounts
        </Typography>
      </Paper>
    </Box>
  );
};

export default Accounts;
