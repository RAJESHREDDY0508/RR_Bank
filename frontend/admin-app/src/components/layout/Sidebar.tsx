/**
 * Responsive Sidebar with Permission-based Navigation
 * Supports desktop persistent drawer and mobile temporary drawer with bottom nav option
 */

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
  Typography,
  Collapse,
  IconButton,
  Tooltip,
  BottomNavigation,
  BottomNavigationAction,
  Paper,
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
  ExpandLess,
  ExpandMore,
  ChevronLeft,
  Menu as MenuIcon,
  VerifiedUser,
} from '@mui/icons-material';
import { useRBAC } from '../../hooks/useRBAC';
import { Permission } from '../../types/rbac';
import { accountRequestsApi } from '../../api/accountRequests';
import { kycApi } from '../../api/kyc';

interface NavItem {
  id: string;
  label: string;
  path: string;
  icon: React.ReactNode;
  permissions: Permission[];
  requireAll?: boolean;
  badge?: number;
  badgeColor?: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  children?: NavItem[];
  mobileNav?: boolean;
}

interface SidebarProps {
  open: boolean;
  onClose: () => void;
  collapsed?: boolean;
  onToggleCollapse?: () => void;
}

const DRAWER_WIDTH = 260;
const COLLAPSED_WIDTH = 72;

const Sidebar: React.FC<SidebarProps> = ({ open, onClose, collapsed = false, onToggleCollapse }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  const { hasAnyPermission, hasAllPermissions, isSuperAdmin } = useRBAC();
  
  const [pendingRequestsCount, setPendingRequestsCount] = useState(0);
  const [pendingKycCount, setPendingKycCount] = useState(0);
  const [expandedItems, setExpandedItems] = useState<string[]>([]);

  useEffect(() => {
    const fetchPendingCount = async () => {
      try {
        const data = await accountRequestsApi.getPendingCount();
        setPendingRequestsCount(data.pendingCount || 0);
      } catch (err) {
        console.error('Failed to fetch pending count:', err);
      }
    };
    fetchPendingCount();
    const interval = setInterval(fetchPendingCount, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const fetchPendingKycCount = async () => {
      try {
        const stats = await kycApi.getKycStats();
        setPendingKycCount(stats?.pending || 0);
      } catch (err) {
        // Silently handle - KYC stats might not be available
        setPendingKycCount(0);
      }
    };
    fetchPendingKycCount();
    const interval = setInterval(fetchPendingKycCount, 30000);
    return () => clearInterval(interval);
  }, []);

  const navItems: NavItem[] = [
    { id: 'dashboard', label: 'Dashboard', path: '/dashboard', icon: <Dashboard />, permissions: ['DASHBOARD_READ'], mobileNav: true },
    { id: 'customers', label: 'Customers', path: '/customers', icon: <People />, permissions: ['CUSTOMER_READ'], mobileNav: true },
    { id: 'kyc-requests', label: 'KYC Requests', path: '/kyc-requests', icon: <VerifiedUser />, permissions: ['CUSTOMER_READ'], badge: pendingKycCount, badgeColor: 'warning' },
    { id: 'accounts', label: 'Accounts', path: '/accounts', icon: <AccountBalance />, permissions: ['ACCOUNT_READ'] },
    { id: 'account-requests', label: 'Account Requests', path: '/account-requests', icon: <PendingActions />, permissions: ['ACCOUNT_APPROVE_REQUESTS'], badge: pendingRequestsCount, badgeColor: 'error' },
    { id: 'transactions', label: 'Transactions', path: '/transactions', icon: <Receipt />, permissions: ['TXN_READ'], mobileNav: true },
    { id: 'payments', label: 'Payments', path: '/payments', icon: <Payment />, permissions: ['PAYMENT_READ'] },
    { id: 'fraud-alerts', label: 'Fraud Alerts', path: '/fraud-alerts', icon: <Warning />, permissions: ['FRAUD_ALERT_READ'], mobileNav: true },
    { id: 'statements', label: 'Statements', path: '/statements', icon: <Description />, permissions: ['STATEMENT_READ'] },
    { id: 'audit-logs', label: 'Audit Logs', path: '/audit-logs', icon: <History />, permissions: ['AUDIT_READ'] },
    { id: 'admin-users', label: 'Admin Users', path: '/users', icon: <AdminPanelSettings />, permissions: ['ADMIN_USER_READ'] },
    { id: 'settings', label: 'Settings', path: '/settings', icon: <Settings />, permissions: ['SETTINGS_READ'] },
  ];

  const filterNavItems = (items: NavItem[]): NavItem[] => {
    return items.filter(item => {
      if (isSuperAdmin()) return true;
      if (item.permissions.length === 0) return true;
      if (item.requireAll) return hasAllPermissions(...item.permissions);
      return hasAnyPermission(...item.permissions);
    });
  };

  const filteredNavItems = filterNavItems(navItems);
  const mobileNavItems = filteredNavItems.filter(item => item.mobileNav).slice(0, 5);

  const handleNavigation = (path: string) => {
    navigate(path);
    if (isMobile) onClose();
  };

  const handleExpandToggle = (itemId: string) => {
    setExpandedItems(prev => prev.includes(itemId) ? prev.filter(id => id !== itemId) : [...prev, itemId]);
  };

  const isSelected = (path: string) => location.pathname === path || location.pathname.startsWith(path + '/');

  const renderNavItem = (item: NavItem, depth: number = 0) => {
    const hasChildren = item.children && item.children.length > 0;
    const isExpanded = expandedItems.includes(item.id);
    const selected = isSelected(item.path);
    const showLabel = !collapsed || isMobile;

    const buttonContent = (
      <ListItemButton
        selected={selected}
        onClick={() => hasChildren ? handleExpandToggle(item.id) : handleNavigation(item.path)}
        sx={{
          minHeight: 48, 
          justifyContent: showLabel ? 'initial' : 'center', 
          px: 2.5, 
          pl: depth > 0 ? 4 : 2.5,
          '&.Mui-selected': { 
            bgcolor: 'primary.main', 
            color: 'white', 
            '&:hover': { bgcolor: 'primary.dark' }, 
            '& .MuiListItemIcon-root': { color: 'white' } 
          },
        }}
      >
        <ListItemIcon sx={{ minWidth: 0, mr: showLabel ? 2 : 'auto', justifyContent: 'center', color: selected ? 'white' : 'inherit' }}>
          {item.badge && item.badge > 0 ? <Badge badgeContent={item.badge} color={item.badgeColor || 'error'}>{item.icon}</Badge> : item.icon}
        </ListItemIcon>
        {showLabel && <><ListItemText primary={item.label} />{hasChildren && (isExpanded ? <ExpandLess /> : <ExpandMore />)}</>}
      </ListItemButton>
    );

    return (
      <React.Fragment key={item.id}>
        <ListItem disablePadding sx={{ display: 'block' }}>
          {collapsed && !isMobile ? (
            <Tooltip title={item.label} placement="right">
              <span>{buttonContent}</span>
            </Tooltip>
          ) : (
            buttonContent
          )}
        </ListItem>
        {hasChildren && showLabel && (
          <Collapse in={isExpanded} timeout="auto" unmountOnExit>
            <List component="div" disablePadding>{filterNavItems(item.children!).map(child => renderNavItem(child, depth + 1))}</List>
          </Collapse>
        )}
      </React.Fragment>
    );
  };

  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: collapsed && !isMobile ? 'center' : 'space-between', p: 2, minHeight: 64, borderBottom: 1, borderColor: 'divider' }}>
        {(!collapsed || isMobile) && <Typography variant="h6" fontWeight="bold" color="primary">RR-Bank Admin</Typography>}
        {!isMobile && onToggleCollapse && <IconButton onClick={onToggleCollapse} size="small">{collapsed ? <MenuIcon /> : <ChevronLeft />}</IconButton>}
      </Box>
      <Box sx={{ flexGrow: 1, overflow: 'auto', mt: 1 }}><List>{filteredNavItems.map(item => renderNavItem(item))}</List></Box>
      {(!collapsed || isMobile) && <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}><Typography variant="caption" color="text.secondary">© 2024 RR-Bank</Typography></Box>}
    </Box>
  );

  const bottomNavigation = isMobile && (
    <Paper sx={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: theme.zIndex.appBar, display: { xs: 'block', md: 'none' } }} elevation={3}>
      <BottomNavigation value={mobileNavItems.findIndex(item => isSelected(item.path))} onChange={(_, newValue) => { if (mobileNavItems[newValue]) handleNavigation(mobileNavItems[newValue].path); }} showLabels>
        {mobileNavItems.map(item => <BottomNavigationAction key={item.id} label={item.label} icon={item.badge && item.badge > 0 ? <Badge badgeContent={item.badge} color={item.badgeColor || 'error'}>{item.icon}</Badge> : item.icon} />)}
      </BottomNavigation>
    </Paper>
  );

  return (
    <>
      <Drawer variant={isMobile ? 'temporary' : 'persistent'} open={open} onClose={onClose}
        sx={{ width: collapsed && !isMobile ? COLLAPSED_WIDTH : DRAWER_WIDTH, flexShrink: 0,
          '& .MuiDrawer-paper': { width: collapsed && !isMobile ? COLLAPSED_WIDTH : DRAWER_WIDTH, boxSizing: 'border-box',
            transition: theme.transitions.create('width', { easing: theme.transitions.easing.sharp, duration: theme.transitions.duration.enteringScreen }) } }}
        ModalProps={{ keepMounted: true }}>
        {drawerContent}
      </Drawer>
      {bottomNavigation}
    </>
  );
};

export default Sidebar;
