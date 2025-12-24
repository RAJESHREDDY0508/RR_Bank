# RR Bank Frontend

A modern, responsive React-based frontend application for RR Bank's online banking platform.

## Features

- 🔐 **Secure Authentication**: JWT-based login and registration
- 💰 **Account Management**: View multiple accounts, check balances
- 💸 **Money Transfers**: Transfer funds between accounts instantly
- 📊 **Transaction History**: Track all your financial activities
- 💳 **Deposit & Withdraw**: Manage your account funds easily
- 📱 **Responsive Design**: Works perfectly on desktop and mobile devices
- 🎨 **Modern UI**: Clean, professional interface with TailwindCSS

## Tech Stack

- **React 18** - UI Library
- **React Router v6** - Navigation
- **Vite** - Build tool and dev server
- **TailwindCSS** - Styling
- **Axios** - HTTP client
- **Lucide React** - Icon library
- **date-fns** - Date formatting

## Prerequisites

- Node.js (v18 or higher)
- npm or yarn
- Backend API running on `http://localhost:8080`

## Installation

1. Navigate to the Frontend directory:
```bash
cd Frontend
```

2. Install dependencies:
```bash
npm install
```

## Running the Application

### Development Mode

Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:5173`

### Production Build

Build for production:
```bash
npm run build
```

Preview production build:
```bash
npm run preview
```

## Project Structure

```
Frontend/
├── src/
│   ├── components/          # Reusable components
│   │   ├── Navbar.jsx       # Navigation bar
│   │   └── PrivateRoute.jsx # Protected route wrapper
│   ├── context/             # React context providers
│   │   └── AuthContext.jsx  # Authentication state management
│   ├── pages/               # Page components
│   │   ├── Login.jsx        # Login page
│   │   ├── Register.jsx     # Registration page
│   │   ├── Dashboard.jsx    # Main dashboard
│   │   ├── Accounts.jsx     # Accounts management
│   │   ├── Transfer.jsx     # Money transfer
│   │   └── Transactions.jsx # Transaction history
│   ├── services/            # API services
│   │   ├── api.js           # Axios instance with interceptors
│   │   └── bankService.js   # Banking API calls
│   ├── App.jsx              # Main app component
│   ├── main.jsx             # Entry point
│   └── index.css            # Global styles
├── index.html               # HTML template
├── package.json             # Dependencies
├── vite.config.js           # Vite configuration
├── tailwind.config.js       # Tailwind configuration
└── postcss.config.js        # PostCSS configuration
```

## API Configuration

The frontend connects to the backend API at `http://localhost:8080/api`. This is configured in `vite.config.js` as a proxy:

```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

## Features Breakdown

### Authentication
- **Login**: Email and password authentication
- **Register**: Create new account with validation
- **Auto-login**: Persistent sessions using JWT tokens
- **Token Refresh**: Automatic token renewal on expiration

### Dashboard
- Overview of all accounts
- Total balance summary
- Recent transactions
- Quick action buttons

### Accounts
- View all bank accounts
- Show/hide balance
- Deposit funds
- Withdraw funds
- Account status indicators

### Transfer
- Transfer between your accounts or to other accounts
- Real-time balance validation
- Transaction confirmation
- Transfer description/notes

### Transactions
- Complete transaction history
- Filter by transaction type
- Search functionality
- Transaction status indicators
- Export functionality (UI ready)

## Environment Variables

Create a `.env` file in the Frontend directory (optional):

```env
VITE_API_URL=http://localhost:8080/api
```

## Security Features

- JWT token storage in localStorage
- Automatic token refresh
- Protected routes
- Secure HTTP-only requests
- XSS protection through React
- CORS configuration

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Development Tips

### Hot Module Replacement (HMR)
Vite provides instant HMR. Changes to your React components will reflect immediately without full page reload.

### Debugging
- React DevTools extension recommended
- Check browser console for errors
- Network tab for API calls

### Code Style
- Use functional components with hooks
- Follow React best practices
- Keep components small and focused
- Use TailwindCSS utility classes

## Common Issues

### CORS Errors
Make sure the backend is running and CORS is properly configured to allow `http://localhost:5173`

### API Connection Failed
1. Verify backend is running on port 8080
2. Check the proxy configuration in `vite.config.js`
3. Ensure Docker services (PostgreSQL, Redis, Kafka) are running

### Build Errors
1. Delete `node_modules` and `package-lock.json`
2. Run `npm install` again
3. Clear Vite cache: `rm -rf node_modules/.vite`

## Future Enhancements

- [ ] Multi-language support
- [ ] Dark mode
- [ ] Biometric authentication
- [ ] Push notifications
- [ ] Transaction export to PDF/CSV
- [ ] Bill payment integration
- [ ] Savings goals tracker
- [ ] Investment portfolio view

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

© 2024 RR Bank. All rights reserved.

## Support

For issues or questions:
- Create an issue in the GitHub repository
- Contact support at support@rrbank.com

---

Built with ❤️ using React and TailwindCSS
