# 🏦 RR-Bank Admin Console

Professional admin console for RR-Bank with comprehensive customer management, fraud detection, and system monitoring capabilities.

![React](https://img.shields.io/badge/React-18-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-5.3-blue)
![Material-UI](https://img.shields.io/badge/Material--UI-5.14-blue)
![Redux](https://img.shields.io/badge/Redux_Toolkit-2.0-purple)

## ✨ Features

### 🔐 Authentication & Security
- Secure admin login with JWT
- Role-based access control
- Protected routes
- Session management
- Automatic token refresh

### 📊 Dashboard
- Real-time system metrics
- Customer statistics
- Transaction monitoring
- Fraud alert notifications
- Revenue tracking
- Growth indicators
- Recent activity feed

### 👥 Customer Management
- Search and filter customers
- View detailed customer profiles
- Account status management
- Customer activity history
- Bulk operations support

### 💰 Account Management
- View all customer accounts
- Account freeze/unfreeze
- Balance monitoring
- Account status tracking
- Transaction history

### 💳 Transaction Management
- Transaction investigation
- Advanced search and filters
- Transaction status tracking
- Suspicious activity detection
- Export capabilities

### 🚨 Fraud Detection
- Real-time fraud alerts
- Alert investigation dashboard
- Pattern recognition
- Risk scoring
- Alert status management
- Investigation workflow

### 📋 Audit Logging
- Complete system activity logs
- Admin action tracking
- Compliance reports
- Search and filter logs
- Export for compliance

### 👤 Admin User Management
- Manage admin users
- Role assignment
- Permission management
- Access control
- Activity monitoring

### ⚙️ System Settings
- Security preferences
- Notification configuration
- System parameters
- Theme customization
- Integration settings

## 🛠️ Tech Stack

- **React 18.2.0** - UI library
- **TypeScript 5.3.3** - Type safety
- **Redux Toolkit 2.0.1** - State management
- **React Router 6.20.1** - Routing
- **Material-UI 5.14.20** - UI components
- **React Query 5.14.2** - Data fetching
- **Axios 1.6.2** - HTTP client
- **React Toastify** - Notifications
- **Vite** - Build tool

## 🚀 Quick Start

### Prerequisites
- Node.js 18+
- npm or yarn
- Backend server running on http://localhost:8080

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at `http://localhost:3001`

### Build for Production

```bash
npm run build
```

## 📁 Project Structure

```
admin-app/
├── src/
│   ├── api/                    # API service layer
│   │   ├── client.ts          # Axios configuration
│   │   ├── auth.ts            # Authentication APIs
│   │   ├── customers.ts       # Customer APIs
│   │   ├── accounts.ts        # Account APIs
│   │   ├── transactions.ts    # Transaction APIs
│   │   ├── fraudAlerts.ts     # Fraud detection APIs
│   │   ├── auditLogs.ts       # Audit log APIs
│   │   ├── dashboard.ts       # Dashboard APIs
│   │   └── reports.ts         # Report generation APIs
│   │
│   ├── components/
│   │   ├── common/            # Reusable components
│   │   │   ├── Loading.tsx
│   │   │   └── PrivateRoute.tsx
│   │   │
│   │   └── layout/            # Layout components
│   │       ├── MainLayout.tsx
│   │       ├── AuthLayout.tsx
│   │       ├── Header.tsx
│   │       └── Sidebar.tsx
│   │
│   ├── hooks/                 # Custom React hooks
│   │   ├── useAuth.ts
│   │   └── useRedux.ts
│   │
│   ├── pages/                 # Page components
│   │   ├── Login.tsx
│   │   ├── Dashboard.tsx
│   │   ├── Customers.tsx
│   │   ├── CustomerDetails.tsx
│   │   ├── Accounts.tsx
│   │   ├── AccountDetails.tsx
│   │   ├── Transactions.tsx
│   │   ├── Payments.tsx
│   │   ├── FraudAlerts.tsx
│   │   ├── FraudAlertDetails.tsx
│   │   ├── Statements.tsx
│   │   ├── AuditLogs.tsx
│   │   ├── AdminUsers.tsx
│   │   └── Settings.tsx
│   │
│   ├── store/                 # Redux store
│   │   ├── store.ts
│   │   ├── authSlice.ts
│   │   ├── customerSlice.ts
│   │   ├── accountSlice.ts
│   │   ├── fraudAlertSlice.ts
│   │   └── auditLogSlice.ts
│   │
│   ├── theme/                 # Material-UI theme
│   │   └── theme.ts
│   │
│   ├── types/                 # TypeScript types
│   │   └── index.ts
│   │
│   ├── utils/                 # Utility functions
│   │   └── format.ts
│   │
│   ├── App.tsx               # Main app component
│   ├── main.tsx              # Entry point
│   └── index.css             # Global styles
│
├── public/                    # Static assets
├── index.html                # HTML template
├── package.json              # Dependencies
├── tsconfig.json             # TypeScript config
├── vite.config.ts            # Vite configuration
└── README.md                 # This file
```

## 🔌 API Integration

The admin console connects to your Spring Boot backend:

**Base URL**: `http://localhost:8080`

### Expected Endpoints

**Authentication**
- POST `/api/auth/login` - Admin login
- POST `/api/auth/refresh` - Token refresh

**Dashboard**
- GET `/api/admin/dashboard/stats` - System metrics

**Customers**
- GET `/api/admin/customers` - List customers
- GET `/api/admin/customers/{id}` - Customer details
- PUT `/api/admin/customers/{id}` - Update customer
- DELETE `/api/admin/customers/{id}` - Delete customer

**Accounts**
- GET `/api/admin/accounts` - List accounts
- GET `/api/admin/accounts/{id}` - Account details
- PUT `/api/admin/accounts/{id}/freeze` - Freeze account
- PUT `/api/admin/accounts/{id}/unfreeze` - Unfreeze account

**Transactions**
- GET `/api/admin/transactions` - List transactions
- GET `/api/admin/transactions/{id}` - Transaction details

**Fraud Alerts**
- GET `/api/admin/fraud-alerts` - List alerts
- GET `/api/admin/fraud-alerts/{id}` - Alert details
- PUT `/api/admin/fraud-alerts/{id}` - Update alert status

**Audit Logs**
- GET `/api/admin/audit-logs` - List logs
- GET `/api/admin/audit-logs/export` - Export logs

## ⚙️ Configuration

Create a `.env` file in the root directory:

```env
# API Configuration
VITE_API_BASE_URL=http://localhost:8080

# App Configuration
VITE_APP_NAME=RR-Bank Admin Console
VITE_APP_VERSION=1.0.0

# Optional
VITE_ENABLE_ANALYTICS=false
VITE_DEBUG_MODE=false
```

## 🎨 Customization

### Theme

Edit `src/theme/theme.ts` to customize colors:

```typescript
export const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2', // Change primary color
    },
    secondary: {
      main: '#dc004e', // Change secondary color
    },
  },
});
```

### Sidebar Menu

Edit `src/components/layout/Sidebar.tsx` to add/remove menu items:

```typescript
const menuItems = [
  { text: 'Dashboard', icon: <Dashboard />, path: '/dashboard' },
  // Add your custom menu items here
];
```

## 🧪 Testing

### Run Tests

```bash
npm test
```

### Run Tests with Coverage

```bash
npm test -- --coverage
```

## 📦 Build & Deploy

### Build for Production

```bash
npm run build
```

Output will be in the `dist/` folder.

### Deploy to Vercel

```bash
npm install -g vercel
vercel deploy
```

### Deploy to Netlify

```bash
npm run build
# Then drag and drop the dist/ folder to Netlify
```

## 🔒 Security Features

- JWT authentication with automatic token refresh
- Protected routes requiring authentication
- Role-based access control
- XSS protection
- CSRF protection
- Secure password handling
- Session timeout management
- Audit logging for all admin actions

## 🌐 Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## 📝 License

MIT License - see LICENSE file for details

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📞 Support

For support:
- Email: admin@rrbank.com
- Create an issue on GitHub
- Check the documentation

## 🙏 Acknowledgments

- Material-UI for the component library
- Redux Toolkit for state management
- React Router for navigation
- All open-source contributors

---

Made with ❤️ by the RR-Bank Team
