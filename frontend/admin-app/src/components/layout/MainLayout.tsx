/**
 * Main Layout Component with Responsive Design
 * Handles desktop/tablet/mobile layouts with proper spacing
 */

import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, useTheme, useMediaQuery, Toolbar } from '@mui/material';
import Header from './Header';
import Sidebar from './Sidebar';

const DRAWER_WIDTH = 260;
const COLLAPSED_WIDTH = 72;
const MOBILE_BOTTOM_NAV_HEIGHT = 56;

const MainLayout: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const isTablet = useMediaQuery(theme.breakpoints.between('sm', 'md'));
  
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // Handle responsive sidebar state
  useEffect(() => {
    if (isMobile) {
      setSidebarOpen(false);
      setSidebarCollapsed(false);
    } else {
      setSidebarOpen(true);
    }
  }, [isMobile]);

  const toggleSidebar = () => {
    if (isMobile) {
      setSidebarOpen(!sidebarOpen);
    } else {
      setSidebarCollapsed(!sidebarCollapsed);
    }
  };

  const closeSidebar = () => {
    if (isMobile) {
      setSidebarOpen(false);
    }
  };

  // Calculate main content margin based on sidebar state
  const getMainMargin = () => {
    if (isMobile) {
      return 0;
    }
    if (!sidebarOpen) {
      return 0;
    }
    return sidebarCollapsed ? COLLAPSED_WIDTH : DRAWER_WIDTH;
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      {/* Header */}
      <Header onMenuClick={toggleSidebar} />

      {/* Sidebar */}
      <Sidebar
        open={sidebarOpen}
        onClose={closeSidebar}
        collapsed={sidebarCollapsed}
        onToggleCollapse={() => setSidebarCollapsed(!sidebarCollapsed)}
      />

      {/* Main Content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100vh',
          ml: { xs: 0, md: `${getMainMargin()}px` },
          transition: theme.transitions.create(['margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
        }}
      >
        {/* Toolbar spacer */}
        <Toolbar />

        {/* Page Content */}
        <Box
          sx={{
            flexGrow: 1,
            p: { xs: 2, sm: 3 },
            pb: { xs: `${MOBILE_BOTTOM_NAV_HEIGHT + 16}px`, md: 3 }, // Extra padding for mobile bottom nav
            overflow: 'auto',
          }}
        >
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
};

export default MainLayout;
