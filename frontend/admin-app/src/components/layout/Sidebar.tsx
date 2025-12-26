import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Box,
  Badge,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import {
  Dashboard,
  People,
  AccountBalance,
  Receipt,
  Payment,
  Warning,
  Description,
  History,
  AdminPanelSettings,
  Settings,
  PendingActions,
} from '@mui/icons-material';
import { accountRequestsApi } from '../../api/accountRequests';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ open, onClose }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [pendingRequestsCount, setPendingRequestsCount] = useState(0);

  useEffect(() => {
    const fetchPendingCount = async () => {
      try {
        const data = await accountRequestsApi.getPendingCount();
        setPendingRequestsCount(data.pendingCount);
      } catch (err) {
        console.error('Failed to fetch pending count:', err);
      }
    };
    fetchPendingCount();
    
    // Refresh every 30 seconds
    const interval = setInterval(fetchPendingCount, 30000);
    return () => clearInterval(interval);
  }, []);

  const menuItems = [
    { text: 'Dashboard', icon: <Dashboard />, path: '/dashboard' },
    { text: 'Customers', icon: <People />, path: '/customers' },
    { text: 'Accounts', icon: <AccountBalance />, path: '/accounts' },
    { text: 'Account Requests', icon: <PendingActions />, path: '/account-requests', badge: pendingRequestsCount },
    { text: 'Transactions', icon: <Receipt />, path: '/transactions' },
    { text: 'Payments', icon: <Payment />, path: '/payments' },
    { text: 'Fraud Alerts', icon: <Warning />, path: '/fraud-alerts' },
    { text: 'Statements', icon: <Description />, path: '/statements' },
    { text: 'Audit Logs', icon: <History />, path: '/audit-logs' },
    { text: 'Admin Users', icon: <AdminPanelSettings />, path: '/users' },
    { text: 'Settings', icon: <Settings />, path: '/settings' },
  ];

  const handleNavigation = (path: string) => {
    navigate(path);
    if (isMobile) {
      onClose();
    }
  };

  const drawer = (
    <Box sx={{ overflow: 'auto', mt: 8 }}>
      <List>
        {menuItems.map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton
              selected={location.pathname === item.path || location.pathname.startsWith(item.path + '/')}
              onClick={() => handleNavigation(item.path)}
              sx={{
                '&.Mui-selected': {
                  bgcolor: 'primary.main',
                  color: 'white',
                  '&:hover': {
                    bgcolor: 'primary.dark',
                  },
                  '& .MuiListItemIcon-root': {
                    color: 'white',
                  },
                },
              }}
            >
              <ListItemIcon
                sx={{
                  color: location.pathname === item.path || location.pathname.startsWith(item.path + '/') 
                    ? 'white' 
                    : 'inherit',
                }}
              >
                {item.badge && item.badge > 0 ? (
                  <Badge badgeContent={item.badge} color="error">
                    {item.icon}
                  </Badge>
                ) : (
                  item.icon
                )}
              </ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Box>
  );

  return (
    <Drawer
      variant={isMobile ? 'temporary' : 'persistent'}
      open={open}
      onClose={onClose}
      sx={{
        width: 240,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: 240,
          boxSizing: 'border-box',
        },
      }}
    >
      {drawer}
    </Drawer>
  );
};

export default Sidebar;
