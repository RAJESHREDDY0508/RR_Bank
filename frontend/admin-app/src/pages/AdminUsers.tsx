import React from 'react';
import { Box, Typography, Paper } from '@mui/material';
import { AdminPanelSettings } from '@mui/icons-material';

const AdminUsers: React.FC = () => {
  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <AdminPanelSettings fontSize="large" />
        <Typography variant="h4" fontWeight="bold">
          Admin User Management
        </Typography>
      </Box>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="body1">
          Manage admin users, roles, and permissions
        </Typography>
      </Paper>
    </Box>
  );
};

export default AdminUsers;
