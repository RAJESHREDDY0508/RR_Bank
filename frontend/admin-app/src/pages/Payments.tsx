import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

const Payments: React.FC = () => {
  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Payment Management
      </Typography>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="body1">
          View and manage all payment transactions
        </Typography>
      </Paper>
    </Box>
  );
};

export default Payments;
