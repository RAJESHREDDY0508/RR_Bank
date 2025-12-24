import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Wallet,
  ArrowLeftRight,
  Receipt,
  FileText,
  History,
  User,
  Bell,
  Settings,
} from 'lucide-react';

interface SidebarProps {
  isOpen: boolean;
}

interface NavItem {
  name: string;
  path: string;
  icon: React.ReactNode;
}

const navItems: NavItem[] = [
  { name: 'Dashboard', path: '/dashboard', icon: <LayoutDashboard className="w-5 h-5" /> },
  { name: 'Accounts', path: '/accounts', icon: <Wallet className="w-5 h-5" /> },
  { name: 'Transfer', path: '/transfer', icon: <ArrowLeftRight className="w-5 h-5" /> },
  { name: 'Payments', path: '/payments', icon: <Receipt className="w-5 h-5" /> },
  { name: 'Transactions', path: '/transactions', icon: <History className="w-5 h-5" /> },
  { name: 'Statements', path: '/statements', icon: <FileText className="w-5 h-5" /> },
  { name: 'Profile', path: '/profile', icon: <User className="w-5 h-5" /> },
  { name: 'Notifications', path: '/notifications', icon: <Bell className="w-5 h-5" /> },
  { name: 'Settings', path: '/settings', icon: <Settings className="w-5 h-5" /> },
];

const Sidebar: React.FC<SidebarProps> = ({ isOpen }) => {
  return (
    <aside
      className={`fixed left-0 top-16 h-[calc(100vh-4rem)] bg-white dark:bg-gray-800 shadow-lg transition-transform duration-300 z-40 ${
        isOpen ? 'translate-x-0' : '-translate-x-full'
      } w-64`}
    >
      <nav className="p-4 space-y-2 overflow-y-auto h-full">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
                isActive
                  ? 'bg-blue-500 text-white'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
              }`
            }
          >
            {item.icon}
            <span className="font-medium">{item.name}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
};

export default Sidebar;
