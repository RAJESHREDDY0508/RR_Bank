/**
 * Settings Page with Permission-based Management
 */

import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Button,
  Paper,
  Grid,
  TextField,
  Switch,
  FormControlLabel,
  Divider,
  useTheme,
  useMediaQuery,
  Tabs,
  Tab,
  Alert,
  Card,
  CardContent,
  CardActions,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Save,
  Edit,
  Security,
  Notifications,
  AccountBalance,
  Settings as SettingsIcon,
} from '@mui/icons-material';
import ErrorBanner, { parseApiError } from '../components/common/ErrorBanner';
import Loading from '../components/common/Loading';
import { useRBAC } from '../hooks/useRBAC';
import { ShowIfPermitted } from '../components/common/PrivateRoute';
import { toast } from 'react-toastify';
import apiClient from '../api/client';

interface Setting {
  id: string;
  category: string;
  key: string;
  value: string;
  description?: string;
  dataType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON';
  isEditable: boolean;
}

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => (
  <div role="tabpanel" hidden={value !== index}>
    {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
  </div>
);

const Settings: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { hasPermission } = useRBAC();
  const canManage = hasPermission('SETTINGS_MANAGE');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [settings, setSettings] = useState<Setting[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [tabValue, setTabValue] = useState(0);
  const [editedSettings, setEditedSettings] = useState<Record<string, string>>({});

  // Edit dialog
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editingSetting, setEditingSetting] = useState<Setting | null>(null);
  const [editValue, setEditValue] = useState('');

  const categories = [
    { id: 'general', label: 'General', icon: <SettingsIcon /> },
    { id: 'security', label: 'Security', icon: <Security /> },
    { id: 'notifications', label: 'Notifications', icon: <Notifications /> },
    { id: 'banking', label: 'Banking', icon: <AccountBalance /> },
  ];

  const loadSettings = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.get('/admin/settings');
      const data = response.data.data || response.data;
      setSettings(Array.isArray(data) ? data : []);
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      setError(errorInfo.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const getSettingsByCategory = (category: string) => {
    return settings.filter(s => s.category?.toLowerCase() === category);
  };

  const handleEditClick = (setting: Setting) => {
    setEditingSetting(setting);
    setEditValue(setting.value);
    setEditDialogOpen(true);
  };

  const handleSaveSetting = async () => {
    if (!editingSetting) return;

    try {
      setSaving(true);
      await apiClient.put(`/admin/settings/${editingSetting.id}`, {
        value: editValue,
      });
      toast.success('Setting updated successfully');
      setEditDialogOpen(false);
      loadSettings();
    } catch (err: any) {
      const errorInfo = parseApiError(err);
      toast.error(errorInfo.message);
    } finally {
      setSaving(false);
    }
  };

  const renderSettingValue = (setting: Setting) => {
    switch (setting.dataType) {
      case 'BOOLEAN':
        return (
          <FormControlLabel
            control={
              <Switch
                checked={setting.value === 'true'}
                disabled={!canManage || !setting.isEditable}
                onChange={async (e) => {
                  if (!canManage) return;
                  try {
                    await apiClient.put(`/admin/settings/${setting.id}`, {
                      value: e.target.checked.toString(),
                    });
                    toast.success('Setting updated');
                    loadSettings();
                  } catch (err: any) {
                    const errorInfo = parseApiError(err);
                    toast.error(errorInfo.message);
                  }
                }}
              />
            }
            label={setting.value === 'true' ? 'Enabled' : 'Disabled'}
          />
        );
      case 'NUMBER':
        return (
          <Typography variant="body1" fontWeight="medium">
            {setting.value}
          </Typography>
        );
      default:
        return (
          <Typography
            variant="body1"
            sx={{
              wordBreak: 'break-word',
              maxWidth: '100%',
            }}
          >
            {setting.value.length > 50 ? `${setting.value.substring(0, 50)}...` : setting.value}
          </Typography>
        );
    }
  };

  const renderSettingsCard = (setting: Setting) => (
    <Card key={setting.id} variant="outlined" sx={{ mb: 2 }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box sx={{ flex: 1 }}>
            <Typography variant="subtitle1" fontWeight="bold">
              {setting.key.replace(/_/g, ' ').replace(/\./g, ' > ')}
            </Typography>
            {setting.description && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {setting.description}
              </Typography>
            )}
            <Box sx={{ mt: 1 }}>
              {renderSettingValue(setting)}
            </Box>
          </Box>
          {canManage && setting.isEditable && setting.dataType !== 'BOOLEAN' && (
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => handleEditClick(setting)}>
                <Edit fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      </CardContent>
    </Card>
  );

  if (loading) {
    return <Loading text="Loading settings..." />;
  }

  return (
    <Box>
      {/* Header */}
      <Box
        sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          justifyContent: 'space-between',
          alignItems: { xs: 'stretch', sm: 'center' },
          mb: 3,
          gap: 2,
        }}
      >
        <Typography variant={isMobile ? 'h5' : 'h4'} fontWeight="bold">
          System Settings
        </Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={loadSettings}
          disabled={loading}
          size={isMobile ? 'small' : 'medium'}
        >
          Refresh
        </Button>
      </Box>

      {/* Error Banner */}
      {error && (
        <ErrorBanner
          error={error}
          onRetry={loadSettings}
          onClose={() => setError(null)}
        />
      )}

      {/* Permission Warning */}
      {!canManage && (
        <Alert severity="info" sx={{ mb: 3 }}>
          You have read-only access to settings. Contact an administrator to make changes.
        </Alert>
      )}

      {/* Settings Tabs */}
      <Paper elevation={0} variant="outlined">
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          variant={isMobile ? 'scrollable' : 'standard'}
          scrollButtons={isMobile ? 'auto' : false}
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          {categories.map((cat, index) => (
            <Tab
              key={cat.id}
              label={cat.label}
              icon={cat.icon}
              iconPosition="start"
              sx={{ minHeight: 48 }}
            />
          ))}
        </Tabs>

        <Box sx={{ p: { xs: 2, sm: 3 } }}>
          {categories.map((cat, index) => (
            <TabPanel key={cat.id} value={tabValue} index={index}>
              {getSettingsByCategory(cat.id).length === 0 ? (
                <Typography color="text.secondary" textAlign="center" py={4}>
                  No settings available in this category.
                </Typography>
              ) : (
                <Grid container spacing={2}>
                  {getSettingsByCategory(cat.id).map(setting => (
                    <Grid item xs={12} md={6} key={setting.id}>
                      {renderSettingsCard(setting)}
                    </Grid>
                  ))}
                </Grid>
              )}
            </TabPanel>
          ))}
        </Box>
      </Paper>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Setting</DialogTitle>
        <DialogContent>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 2 }}>
            {editingSetting?.key}
          </Typography>
          {editingSetting?.description && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {editingSetting.description}
            </Typography>
          )}
          <TextField
            fullWidth
            label="Value"
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            type={editingSetting?.dataType === 'NUMBER' ? 'number' : 'text'}
            multiline={editingSetting?.dataType === 'JSON'}
            rows={editingSetting?.dataType === 'JSON' ? 4 : 1}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSaveSetting}
            disabled={saving}
            startIcon={<Save />}
          >
            {saving ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Settings;
