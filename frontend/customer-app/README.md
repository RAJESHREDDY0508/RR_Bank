# 🏦 RR-Bank Customer Web Application

## 📋 Overview

Modern, responsive React + TypeScript customer portal for RR-Bank with real-time updates, secure authentication, and comprehensive banking features.

## 🚀 Quick Start

### Prerequisites

- Node.js 18+ installed
- npm or yarn package manager
- RR-Bank Backend API running on http://localhost:8080

### Installation

```bash
# Navigate to customer app directory
cd C:\Users\rajes\Desktop\projects\RR-Bank\frontend\customer-app

# Install dependencies
npm install

# Start development server
npm run dev

# Open browser to http://localhost:3000
```

## 📁 Project Structure

```
customer-app/
├── public/                 # Static assets
├── src/
│   ├── api/               # API client and endpoints
│   │   ├── client.ts     # Axios configuration
│   │   ├── auth.ts       # Authentication APIs
│   │   ├── accounts.ts   # Account APIs
│   │   ├── transactions.ts # Transaction APIs
│   │   └── payments.ts   # Payment APIs
│   ├── components/        # Reusable components
│   │   ├── common/       # Common UI components
│   │   ├── layout/       # Layout components
│   │   └── features/     # Feature-specific components
│   ├── pages/            # Page components
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   ├── Dashboard.tsx
│   │   ├── Accounts.tsx
│   │   ├── Transfer.tsx
│   │   ├── Payments.tsx
│   │   ├── Transactions.tsx
│   │   ├── Statements.tsx
│   │   ├── Profile.tsx
│   │   ├── Notifications.tsx
│   │   └── Settings.tsx
│   ├── store/            # Redux store
│   │   ├── store.ts
│   │   ├── authSlice.ts
│   │   ├── accountSlice.ts
│   │   └── notificationSlice.ts
│   ├── hooks/            # Custom React hooks
│   ├── utils/            # Utility functions
│   ├── types/            # TypeScript types
│   ├── theme/            # Material-UI theme
│   ├── App.tsx           # Main App component
│   └── main.tsx          # Entry point
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 🎨 Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool
- **Material-UI v5** - Component library
- **Redux Toolkit** - State management
- **React Router v6** - Routing
- **Axios** - HTTP client
- **React Query** - Data fetching & caching
- **Chart.js** - Data visualization
- **React Hook Form** - Form management
- **React Toastify** - Toast notifications

## 📱 Features

### Authentication & Security
- ✅ JWT-based authentication
- ✅ Refresh token management
- ✅ Secure password requirements
- ✅ Session timeout handling
- ✅ Auto-logout on inactivity

### Dashboard
- ✅ Account overview with balances
- ✅ Recent transactions list
- ✅ Quick actions (transfer, pay bills)
- ✅ Spending analytics charts
- ✅ Notification center

### Accounts
- ✅ Multiple account management
- ✅ Real-time balance updates
- ✅ Account details and history
- ✅ Account statements download
- ✅ Account type indicators

### Transfers
- ✅ Internal account transfers
- ✅ External bank transfers
- ✅ Beneficiary management
- ✅ Transfer history
- ✅ Recurring transfers
- ✅ Real-time validation

### Payments
- ✅ Bill payment scheduler
- ✅ Saved payees
- ✅ Payment history
- ✅ Recurring payments
- ✅ Payment confirmations

### Transactions
- ✅ Complete transaction history
- ✅ Advanced filters (date, type, amount)
- ✅ Search functionality
- ✅ Export to CSV/PDF
- ✅ Transaction details modal

### Statements
- ✅ Monthly statement generation
- ✅ Date range selection
- ✅ PDF download
- ✅ Email delivery
- ✅ Statement history

### Profile
- ✅ Personal information management
- ✅ Contact details update
- ✅ Password change
- ✅ Security settings
- ✅ Two-factor authentication

### Notifications
- ✅ Real-time push notifications
- ✅ Transaction alerts
- ✅ Security alerts
- ✅ Mark as read/unread
- ✅ Notification preferences

### Settings
- ✅ Theme preferences (light/dark mode)
- ✅ Language selection
- ✅ Notification settings
- ✅ Privacy controls
- ✅ Session management

## 🎯 Pages & Routes

| Route | Page | Description |
|-------|------|-------------|
| `/` | Home | Landing page (redirects to dashboard if logged in) |
| `/login` | Login | User authentication |
| `/register` | Register | New customer registration |
| `/dashboard` | Dashboard | Main customer dashboard |
| `/accounts` | Accounts | Account list and overview |
| `/accounts/:id` | Account Details | Individual account details |
| `/transfer` | Transfer | Money transfer form |
| `/payments` | Payments | Payment list and management |
| `/payments/new` | New Payment | Create new payment |
| `/transactions` | Transactions | Transaction history |
| `/statements` | Statements | Account statements |
| `/profile` | Profile | User profile management |
| `/notifications` | Notifications | Notification center |
| `/settings` | Settings | App settings and preferences |

## 🔐 Authentication Flow

```typescript
// Login
POST /api/auth/login
{
  "username": "string",
  "password": "string"
}

// Response
{
  "token": "jwt-token",
  "refreshToken": "refresh-token",
  "user": {
    "id": "uuid",
    "username": "string",
    "email": "string",
    "role": "CUSTOMER"
  }
}

// Refresh Token
POST /api/auth/refresh
{
  "refreshToken": "refresh-token"
}

// Logout
POST /api/auth/logout
```

## 📊 State Management

### Redux Slices

**authSlice**
```typescript
- user: User | null
- token: string | null
- isAuthenticated: boolean
- loading: boolean
- error: string | null
```

**accountSlice**
```typescript
- accounts: Account[]
- selectedAccount: Account | null
- loading: boolean
- error: string | null
```

**transactionSlice**
```typescript
- transactions: Transaction[]
- filters: FilterState
- pagination: PaginationState
- loading: boolean
```

**notificationSlice**
```typescript
- notifications: Notification[]
- unreadCount: number
- loading: boolean
```

## 🎨 Theme Configuration

```typescript
// Light Mode
const lightTheme = {
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
};

// Dark Mode
const darkTheme = {
  palette: {
    mode: 'dark',
    primary: {
      main: '#90caf9',
    },
    secondary: {
      main: '#f48fb1',
    },
  },
};
```

## 📡 API Integration

```typescript
// Example: Fetch accounts
import { accountsApi } from '@/api/accounts';

const { data, isLoading, error } = useQuery({
  queryKey: ['accounts'],
  queryFn: accountsApi.getAll,
});

// Example: Create transfer
import { transferApi } from '@/api/transfers';

const mutation = useMutation({
  mutationFn: transferApi.create,
  onSuccess: () => {
    toast.success('Transfer successful!');
    queryClient.invalidateQueries(['accounts']);
  },
});
```

## 🧪 Testing

```bash
# Run unit tests
npm test

# Run tests with coverage
npm run test:coverage

# Run E2E tests
npm run test:e2e
```

## 🏗️ Build & Deployment

```bash
# Build for production
npm run build

# Preview production build
npm run preview

# Output directory: dist/
```

## 🔧 Environment Variables

Create `.env` file:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=RR-Bank
VITE_APP_VERSION=1.0.0
VITE_ENABLE_ANALYTICS=false
```

## 📱 Responsive Design

- ✅ Mobile-first approach
- ✅ Breakpoints: xs (0px), sm (600px), md (900px), lg (1200px), xl (1536px)
- ✅ Adaptive layouts for all screen sizes
- ✅ Touch-friendly interactions
- ✅ Optimized for tablets and mobile

## 🎯 Performance Optimizations

- ✅ Code splitting with React.lazy()
- ✅ Image optimization
- ✅ Tree shaking
- ✅ Lazy loading of routes
- ✅ React Query caching
- ✅ Memoization with useMemo/useCallback
- ✅ Virtual scrolling for large lists

## 🔒 Security Best Practices

- ✅ JWT tokens stored in httpOnly cookies
- ✅ CSRF protection
- ✅ XSS prevention
- ✅ Input validation
- ✅ Secure API communication (HTTPS)
- ✅ Content Security Policy
- ✅ Rate limiting on API calls

## 🐛 Troubleshooting

### Issue: Cannot connect to backend

**Solution:**
```bash
# Check backend is running
curl http://localhost:8080/actuator/health

# Check proxy configuration in vite.config.ts
```

### Issue: Authentication not working

**Solution:**
```bash
# Clear browser local storage
localStorage.clear()

# Check JWT token format
# Verify backend CORS settings
```

### Issue: Build fails

**Solution:**
```bash
# Clear node_modules
rm -rf node_modules package-lock.json

# Reinstall dependencies
npm install

# Try build again
npm run build
```

## 📚 Documentation

- [React Documentation](https://react.dev/)
- [TypeScript Documentation](https://www.typescriptlang.org/docs/)
- [Material-UI Documentation](https://mui.com/)
- [Redux Toolkit Documentation](https://redux-toolkit.js.org/)
- [React Query Documentation](https://tanstack.com/query)

## 🤝 Contributing

1. Create feature branch
2. Make changes
3. Write tests
4. Submit pull request

## 📄 License

Copyright © 2024 RR-Bank. All rights reserved.

## 📞 Support

For issues or questions:
- Email: support@rrbank.com
- Documentation: /docs
- Issue Tracker: GitHub Issues

---

**Version**: 1.0.0  
**Last Updated**: December 2, 2024  
**Status**: Production Ready
