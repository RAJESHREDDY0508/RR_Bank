import React from 'react';
import { Box, Typography, Paper, Button } from '@mui/material';
import { useParams, useNavigate } from 'react-router-dom';

const AccountDetails: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  
  return (
    <Box>
      <Button variant="text" onClick={() => navigate('/accounts')} sx={{ mb: 2 }}>
        ← Back to Accounts
      </Button>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Account Details - {id}
      </Typography>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="body1">
          Account details and transaction history
        </Typography>
      </Paper>
    </Box>
  );
};

export default AccountDetails;
