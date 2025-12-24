import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

const Statements: React.FC = () => {
  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Statement Management
      </Typography>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="body1">
          Generate and download account statements
        </Typography>
      </Paper>
    </Box>
  );
};

export default Statements;
