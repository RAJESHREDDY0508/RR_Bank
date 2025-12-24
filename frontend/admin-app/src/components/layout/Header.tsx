import React, { useState } from 'react';
import {
  AppBar,
  Toolbar,
  IconButton,
  Typography,
  Badge,
  Menu,
  MenuItem,
  Avatar,
  Box,
  Divider,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Notifications,
  AccountCircle,
  Settings,
  Logout,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

interface HeaderProps {
  onMenuClick: () => void;
}

const Header: React.FC<HeaderProps> = ({ onMenuClick }) => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [notificationAnchor, setNotificationAnchor] = useState<null | HTMLElement>(null);

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleNotificationMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setNotificationAnchor(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setNotificationAnchor(null);
  };

  const handleLogout = () => {
    handleMenuClose();
    logout();
  };

  const handleSettings = () => {
    handleMenuClose();
    navigate('/settings');
  };

  return (
    <AppBar
      position="fixed"
      sx={{
        zIndex: (theme) => theme.zIndex.drawer + 1,
        bgcolor: 'white',
        color: 'text.primary',
        boxShadow: 1,
      }}
    >
      <Toolbar>
        <IconButton
          edge="start"
          color="inherit"
          aria-label="menu"
          onClick={onMenuClick}
          sx={{ mr: 2 }}
        >
          <MenuIcon />
        </IconButton>

        <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 0, fontWeight: 'bold', color: 'primary.main' }}>
          RR-Bank Admin
        </Typography>

        <Box sx={{ flexGrow: 1 }} />

        {/* Notifications */}
        <IconButton
          color="inherit"
          onClick={handleNotificationMenuOpen}
          sx={{ mr: 2 }}
        >
          <Badge badgeContent={3} color="error">
            <Notifications />
          </Badge>
        </IconButton>

        {/* Profile */}
        <IconButton
          edge="end"
          aria-label="account"
          aria-haspopup="true"
          onClick={handleProfileMenuOpen}
          color="inherit"
        >
          <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
            {user?.firstName?.[0] || user?.username?.[0] || 'A'}
          </Avatar>
        </IconButton>

        {/* Notification Menu */}
        <Menu
          anchorEl={notificationAnchor}
          open={Boolean(notificationAnchor)}
          onClose={handleMenuClose}
          onClick={handleMenuClose}
        >
          <MenuItem onClick={() => navigate('/fraud-alerts')}>
            <Typography variant="body2">3 new fraud alerts</Typography>
          </MenuItem>
          <MenuItem>
            <Typography variant="body2">New customer registration</Typography>
          </MenuItem>
          <MenuItem>
            <Typography variant="body2">System health check completed</Typography>
          </MenuItem>
          <Divider />
          <MenuItem onClick={() => navigate('/notifications')}>
            <Typography variant="body2" color="primary">View all notifications</Typography>
          </MenuItem>
        </Menu>

        {/* Profile Menu */}
        <Menu
          anchorEl={anchorEl}
          open={Boolean(anchorEl)}
          onClose={handleMenuClose}
          onClick={handleMenuClose}
        >
          <Box sx={{ px: 2, py: 1 }}>
            <Typography variant="subtitle1" fontWeight="bold">
              {user?.firstName || user?.username}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {user?.email}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Role: {user?.role || 'ADMIN'}
            </Typography>
          </Box>
          <Divider />
          <MenuItem onClick={handleSettings}>
            <Settings fontSize="small" sx={{ mr: 1 }} />
            Settings
          </MenuItem>
          <MenuItem onClick={handleLogout}>
            <Logout fontSize="small" sx={{ mr: 1 }} />
            Logout
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
};

export default Header;
