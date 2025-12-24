import React from 'react';
import { Box, Typography, Paper, Button } from '@mui/material';
import { useParams, useNavigate } from 'react-router-dom';

const FraudAlertDetails: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  
  return (
    <Box>
      <Button variant="text" onClick={() => navigate('/fraud-alerts')} sx={{ mb: 2 }}>
        ← Back to Fraud Alerts
      </Button>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Fraud Alert Details - {id}
      </Typography>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="body1">
          Detailed investigation interface for fraud alert
        </Typography>
      </Paper>
    </Box>
  );
};

export default FraudAlertDetails;
