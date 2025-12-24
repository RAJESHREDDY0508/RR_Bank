import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

const Transactions: React.FC = () => {
  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Transaction Management
      </Typography>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="body1">
          View and investigate all transactions across the platform
        </Typography>
      </Paper>
    </Box>
  );
};

export default Transactions;
