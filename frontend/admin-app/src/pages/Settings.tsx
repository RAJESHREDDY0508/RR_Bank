import React from 'react';
import { Box, Typography, Paper, Grid, Switch, FormControlLabel } from '@mui/material';
import { Settings as SettingsIcon } from '@mui/icons-material';

const Settings: React.FC = () => {
  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <SettingsIcon fontSize="large" />
        <Typography variant="h4" fontWeight="bold">
          System Settings
        </Typography>
      </Box>
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper elevation={3} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Security Settings
            </Typography>
            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Two-Factor Authentication"
            />
            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Email Notifications"
            />
            <FormControlLabel
              control={<Switch />}
              label="SMS Notifications"
            />
          </Paper>
        </Grid>
        
        <Grid item xs={12} md={6}>
          <Paper elevation={3} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              System Preferences
            </Typography>
            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Dark Mode"
            />
            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Auto-refresh Dashboard"
            />
            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Enable Fraud Detection"
            />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Settings;
