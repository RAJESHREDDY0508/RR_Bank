import React from 'react';
import { Box, Typography, Paper, Chip } from '@mui/material';
import { Warning } from '@mui/icons-material';

const FraudAlerts: React.FC = () => {
  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Warning color="error" fontSize="large" />
        <Typography variant="h4" fontWeight="bold">
          Fraud Alerts
        </Typography>
        <Chip label="3 Pending" color="error" />
      </Box>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="body1">
          Monitor and investigate fraudulent activities and suspicious transactions
        </Typography>
      </Paper>
    </Box>
  );
};

export default FraudAlerts;
