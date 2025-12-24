import React from 'react';
import { Box, Typography, Paper } from '@mui/material';
import { History } from '@mui/icons-material';

const AuditLogs: React.FC = () => {
  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <History fontSize="large" />
        <Typography variant="h4" fontWeight="bold">
          Audit Logs
        </Typography>
      </Box>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="body1">
          Track all system activities and admin actions for security and compliance
        </Typography>
      </Paper>
    </Box>
  );
};

export default AuditLogs;
