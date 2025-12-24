import React, { useState, useEffect, useCallback } from 'react';
import { LogOut, Home, User, DollarSign, Repeat, FileText, Bell, Lock, UserPlus, Zap } from 'lucide-react';

// --- Firebase Initialization (Required by Canvas environment) ---
// Note: While this app simulates a Java/PostgreSQL backend as per architecture,
// this minimal setup is required for the React component to run in the environment.
const firebaseConfig = typeof __firebase_config !== 'undefined' ? JSON.parse(__firebase_config) : {};
const appId = typeof __app_id !== 'undefined' ? __app_id : 'default-app-id';

// Mocking required Firebase/Firestore dependencies and setup function
const initializeFirebase = () => {
  if (Object.keys(firebaseConfig).length === 0) {
    console.warn("Firebase configuration is missing. Mocking authentication.");
  }
};

initializeFirebase();

// --- 1. MOCK API UTILITIES (Simulating JWT Auth & Account Service) ---
// The API calls simulate interactions with the API Gateway (/auth/login, /accounts)
const API_BASE_URL = 'http://api-gateway.novabank.rr'; // Mock URL as per architecture

/**
 * Simulates an Axios Interceptor pattern for attaching the JWT token.
 * In a real app, Axios would handle this globally. Here, we manually add the header.
 */
const protectedFetch = async (endpoint, options = {}) => {
  const token = localStorage.getItem('novaBankToken');
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    // Architecture mandate: Attach Authorization: Bearer <token>
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(endpoint, { ...options, headers });

  if (response.status === 401 || response.status === 403) {
    console.error("JWT validation failed (401/403). Clearing session.");
    localStorage.removeItem('novaBankToken');
    // In a real app, this would trigger a global logout/redirect.
    throw new Error('Unauthorized or Forbidden Access');
  }

  return response;
};

/**
 * Simulates a call to the Auth Service via the API Gateway.
 */
const mockAuthService = {
  login: async (username, password) => {
    // POST /auth/login -> returns access + refresh JWT
    // Mock successful login and token storage
    if (username === 'customer@rr.com' && password === 'password123') {
      const mockToken = `mock-jwt-customer-${new Date().getTime()}`;
      // In a real app, the role would be decoded from the JWT payload
      const mockRole = 'CUSTOMER';
      localStorage.setItem('novaBankToken', mockToken);
      localStorage.setItem('novaBankRole', mockRole);
      return { token: mockToken, role: mockRole, userId: 'user-001' };
    }
    throw new Error('Invalid credentials');
  },

  register: async (email, password) => {
    // POST /auth/register -> simulates user creation
    if (!email.includes('@')) {
      throw new Error('Invalid email format');
    }
    // Simulate success
    return { success: true, message: 'Registration successful. Please log in.' };
  }
};

/**
 * Simulates a call to the Account Service via the API Gateway and LLM data generation.
 */
const fetchDashboardData = async (userId, token) => {
  if (!token) throw new Error("No token provided for protected call.");

  const systemPrompt = `You are a secure banking backend API. Generate structured JSON data containing a user's account summary and recent transactions. The user ID is ${userId}.`;
  const userQuery = "Generate a JSON response with two checking accounts and three recent transactions for each, including type (Deposit/Transfer/Payment), amount, and date.";
  const apiKey = ""
  const apiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=${apiKey}`;

  const schema = {
    type: "OBJECT",
    properties: {
      accountSummary: {
        type: "ARRAY",
        items: {
          type: "OBJECT",
          properties: {
            accountId: { "type": "STRING", "description": "Unique account identifier" },
            accountType: { "type": "STRING", "enum": ["Checking", "Savings"] },
            balance: { "type": "NUMBER", "description": "Current balance in USD" },
            currency: { "type": "STRING", "default": "USD" },
            transactions: {
              type: "ARRAY",
              items: {
                type: "OBJECT",
                properties: {
                  date: { "type": "STRING", "description": "YYYY-MM-DD format" },
                  description: { "type": "STRING" },
                  amount: { "type": "NUMBER" },
                  type: { "type": "STRING", "enum": ["Deposit", "Transfer", "Payment"] },
                }
              }
            }
          },
          "required": ["accountId", "accountType", "balance", "transactions"]
        }
      }
    }
  };

  const payload = {
    contents: [{ parts: [{ text: userQuery }] }],
    systemInstruction: { parts: [{ text: systemPrompt }] },
    generationConfig: {
      responseMimeType: "application/json",
      responseSchema: schema
    }
  };

  try {
    const response = await protectedFetch(apiUrl, {
      method: 'POST',
      body: JSON.stringify(payload),
      headers: {
         // Override the default headers to ensure API compatibility if necessary
        'Content-Type': 'application/json',
        // Note: The protectedFetch function adds the Auth header, 
        // but for this mock API call, we rely on the internal API key.
      }
    });

    if (!response.ok) {
        // This is a Gemini API error, not a simulated backend error
        throw new Error(`API call failed with status: ${response.status}`);
    }

    const result = await response.json();
    const jsonText = result.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!jsonText) throw new Error("Received empty content from generator.");

    const data = JSON.parse(jsonText);
    return data.accountSummary || [];

  } catch (error) {
    console.error("Error fetching mock dashboard data:", error);
    // Return mock fallback data if API fails
    return [
      { accountId: "CHK-001", accountType: "Checking", balance: 12450.75, currency: "USD", transactions: [] },
      { accountId: "SAV-002", accountType: "Savings", balance: 50890.11, currency: "USD", transactions: [] }
    ];
  }
};

// --- 2. COMPONENTS ---

// Navbar Component
const Navbar = ({ currentPage, setCurrentPage, onLogout }) => {
  const navItems = [
    { name: 'Dashboard', icon: Home, page: 'dashboard' },
    { name: 'Transfers', icon: Repeat, page: 'transfer' },
    { name: 'Payments', icon: Zap, page: 'payments' },
    { name: 'Statements', icon: FileText, page: 'statements' },
    { name: 'Profile', icon: User, page: 'profile' },
  ];

  return (
    <div className="bg-white shadow-xl w-64 p-4 flex flex-col h-full fixed md:static transition-all duration-300 ease-in-out z-10 rounded-r-2xl">
      <div className="text-2xl font-bold text-indigo-700 mb-8 p-2">NovaBank RR</div>
      <nav className="flex flex-col flex-grow space-y-2">
        {navItems.map((item) => (
          <button
            key={item.page}
            onClick={() => setCurrentPage(item.page)}
            className={`flex items-center space-x-3 p-3 rounded-xl transition-colors duration-200 ${
              currentPage === item.page
                ? 'bg-indigo-600 text-white shadow-lg'
                : 'text-gray-600 hover:bg-indigo-50 hover:text-indigo-700'
            }`}
          >
            <item.icon className="w-5 h-5" />
            <span className="font-medium">{item.name}</span>
          </button>
        ))}
      </nav>
      <div className="pt-4 border-t border-gray-200">
        <button
          onClick={onLogout}
          className="flex items-center space-x-3 p-3 rounded-xl w-full text-left text-red-500 hover:bg-red-50 transition-colors duration-200"
        >
          <LogOut className="w-5 h-5" />
          <span className="font-medium">Logout</span>
        </button>
      </div>
    </div>
  );
};

// --- Transaction History Card ---
const TransactionCard = ({ txn }) => (
  <div className="flex justify-between items-center p-4 border-b border-gray-100 hover:bg-indigo-50/50 transition-colors duration-150">
    <div className="flex items-center space-x-3">
      <div className={`p-2 rounded-full ${
        txn.type === 'Deposit' ? 'bg-green-100 text-green-600' :
        txn.type === 'Payment' ? 'bg-red-100 text-red-600' :
        'bg-yellow-100 text-yellow-600'
      }`}>
        {txn.type === 'Deposit' ? <DollarSign className="w-4 h-4" /> : <Repeat className="w-4 h-4" />}
      </div>
      <div>
        <p className="font-semibold text-gray-800">{txn.description}</p>
        <p className="text-xs text-gray-500">{txn.date}</p>
      </div>
    </div>
    <p className={`font-bold ${txn.amount > 0 ? 'text-green-600' : 'text-gray-800'}`}>
      {txn.amount > 0 ? '+' : ''}{new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(txn.amount)}
    </p>
  </div>
);

// --- Account Summary Card ---
const AccountSummaryCard = ({ account }) => (
  <div className="bg-white p-6 rounded-2xl shadow-lg border border-gray-100 flex flex-col h-full transition-shadow duration-300 hover:shadow-xl">
    <div className="flex justify-between items-center mb-4">
      <h3 className="text-lg font-bold text-gray-700 flex items-center">
        <DollarSign className="w-5 h-5 mr-2 text-indigo-500" />
        {account.accountType}
      </h3>
      <span className="text-sm font-mono text-gray-400">...{account.accountId.slice(-4)}</span>
    </div>

    <div className="flex flex-col flex-grow">
      <p className="text-sm text-gray-500 uppercase">Current Balance</p>
      <p className="text-4xl font-extrabold text-gray-900 mb-6">
        {new Intl.NumberFormat('en-US', { style: 'currency', currency: account.currency || 'USD' }).format(account.balance)}
      </p>

      <h4 className="text-md font-semibold text-gray-700 mb-2 border-t pt-4 mt-auto">Recent Activity</h4>
      {account.transactions && account.transactions.length > 0 ? (
        <div className="space-y-1 text-sm overflow-y-auto max-h-40">
          {account.transactions.slice(0, 3).map((txn, index) => (
            <div key={index} className="flex justify-between text-xs text-gray-600">
              <span className="truncate">{txn.description}</span>
              <span className={`font-semibold ${txn.amount > 0 ? 'text-green-600' : 'text-gray-800'}`}>
                {txn.amount > 0 ? '+' : ''}{txn.amount}
              </span>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-sm text-gray-500 italic">No recent transactions.</p>
      )}
    </div>
  </div>
);

// --- Dashboard Page ---
const DashboardPage = ({ userId, token }) => {
  const [accountData, setAccountData] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      if (!token) return;
      setIsLoading(true);
      try {
        const data = await fetchDashboardData(userId, token);
        setAccountData(data);
      } catch (e) {
        console.error("Dashboard data load error:", e);
      } finally {
        setIsLoading(false);
      }
    };
    loadData();
  }, [userId, token]);

  const totalBalance = accountData.reduce((sum, acc) => sum + acc.balance, 0);

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-full min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500"></div>
        <p className="ml-4 text-indigo-600">Loading Account Data...</p>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-8">
      <h1 className="text-3xl font-extrabold text-gray-800 mb-6">Welcome back, NovaBank Customer!</h1>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-indigo-600 text-white p-6 rounded-2xl shadow-xl">
          <DollarSign className="w-8 h-8 mb-2" />
          <p className="text-sm opacity-80">Total Balance</p>
          <p className="text-3xl font-bold">
            {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(totalBalance)}
          </p>
        </div>
        <div className="bg-white p-6 rounded-2xl shadow-xl border border-gray-100">
          <Repeat className="w-8 h-8 mb-2 text-indigo-600" />
          <p className="text-sm text-gray-500">Accounts Active</p>
          <p className="text-3xl font-bold text-gray-800">{accountData.length}</p>
        </div>
        <div className="bg-white p-6 rounded-2xl shadow-xl border border-gray-100">
          <Zap className="w-8 h-8 mb-2 text-indigo-600" />
          <p className="text-sm text-gray-500">Pending Payments</p>
          <p className="text-3xl font-bold text-gray-800">0</p>
        </div>
        <div className="bg-white p-6 rounded-2xl shadow-xl border border-gray-100">
          <Bell className="w-8 h-8 mb-2 text-indigo-600" />
          <p className="text-sm text-gray-500">Unread Notifications</p>
          <p className="text-3xl font-bold text-gray-800">2</p>
        </div>
      </div>

      {/* Account Summaries and Recent Transactions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Account Summaries */}
        <div className="lg:col-span-2 space-y-6">
          <h2 className="text-2xl font-bold text-gray-800">My Accounts</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {accountData.map((account) => (
              <AccountSummaryCard key={account.accountId} account={account} />
            ))}
          </div>
        </div>

        {/* Recent Transactions (Consolidated) */}
        <div className="lg:col-span-1 bg-white rounded-2xl shadow-xl border border-gray-100 p-6">
          <h2 className="text-2xl font-bold text-gray-800 mb-4">Recent Transactions</h2>
          <div className="divide-y divide-gray-100 -mx-6">
            {accountData.flatMap(acc => acc.transactions || []).sort((a, b) => new Date(b.date) - new Date(a.date)).slice(0, 5).map((txn, index) => (
              <TransactionCard key={index} txn={txn} />
            ))}
            {(accountData.length === 0 || accountData.flatMap(acc => acc.transactions || []).length === 0) && (
                <p className="p-4 text-center text-gray-500 italic">No transactions found across accounts.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

// --- Login Page ---
const LoginPage = ({ onAuthSuccess }) => {
  const [username, setUsername] = useState('customer@rr.com');
  const [password, setPassword] = useState('password123');
  const [error, setError] = useState(null);
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError(null);
    setIsLoggingIn(true);
    try {
      const result = await mockAuthService.login(username, password);
      // Success: JWT stored in localStorage by mockAuthService
      onAuthSuccess(result.userId, result.role);
    } catch (err) {
      setError(err.message || 'Login failed. Please check credentials.');
      console.error(err);
    } finally {
      setIsLoggingIn(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <div className="w-full max-w-md bg-white p-8 rounded-2xl shadow-2xl border border-gray-100">
        <div className="text-center mb-8">
          <Lock className="w-10 h-10 mx-auto text-indigo-600" />
          <h2 className="mt-4 text-3xl font-extrabold text-gray-900">Sign in to NovaBank</h2>
          <p className="mt-2 text-sm text-gray-600">Enter your customer portal credentials.</p>
          <p className="text-xs text-indigo-500 mt-2">Hint: Use customer@rr.com / password123</p>
        </div>
        <form onSubmit={handleLogin} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700">Email Address</label>
            <input
              type="email"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="mt-1 block w-full px-4 py-3 border border-gray-300 rounded-xl shadow-sm focus:ring-indigo-500 focus:border-indigo-500 transition duration-150"
              placeholder="customer@rr.com"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 block w-full px-4 py-3 border border-gray-300 rounded-xl shadow-sm focus:ring-indigo-500 focus:border-indigo-500 transition duration-150"
              placeholder="••••••••"
            />
          </div>
          {error && (
            <div className="text-sm text-red-600 bg-red-50 p-3 rounded-lg border border-red-200">
              {error}
            </div>
          )}
          <button
            type="submit"
            disabled={isLoggingIn}
            className="w-full flex justify-center py-3 px-4 border border-transparent rounded-xl shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition duration-150 disabled:bg-indigo-400"
          >
            {isLoggingIn ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>
        <p className="mt-6 text-center text-sm text-gray-600">
          New Customer?
          <button onClick={() => window.location.hash = '#/register'} className="font-medium text-indigo-600 hover:text-indigo-500 ml-1">
            Register Here
          </button>
        </p>
      </div>
    </div>
  );
};

// --- Registration Page (KYC-lite) ---
const RegisterPage = ({ onRegistrationSuccess }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isRegistering, setIsRegistering] = useState(false);
  const [message, setMessage] = useState(null);
  const [messageType, setMessageType] = useState('info');

  const handleRegister = async (e) => {
    e.preventDefault();
    setMessage(null);
    setIsRegistering(true);
    try {
      // POST /auth/register
      await mockAuthService.register(email, password);
      setMessage('Registration successful! Redirecting to login...');
      setMessageType('success');
      setTimeout(() => onRegistrationSuccess(), 2000);
    } catch (err) {
      setMessage(err.message || 'Registration failed. Please try again.');
      setMessageType('error');
    } finally {
      setIsRegistering(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <div className="w-full max-w-md bg-white p-8 rounded-2xl shadow-2xl border border-gray-100">
        <div className="text-center mb-8">
          <UserPlus className="w-10 h-10 mx-auto text-indigo-600" />
          <h2 className="mt-4 text-3xl font-extrabold text-gray-900">Customer Registration</h2>
          <p className="mt-2 text-sm text-gray-600">KYC-lite for NovaBank demo.</p>
        </div>
        <form onSubmit={handleRegister} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700">Email Address (Username)</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 block w-full px-4 py-3 border border-gray-300 rounded-xl shadow-sm focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="new.customer@rr.com"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 block w-full px-4 py-3 border border-gray-300 rounded-xl shadow-sm focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="••••••••"
            />
          </div>

          {message && (
            <div className={`text-sm p-3 rounded-lg border ${messageType === 'success' ? 'text-green-600 bg-green-50 border-green-200' : 'text-red-600 bg-red-50 border-red-200'}`}>
              {message}
            </div>
          )}

          <button
            type="submit"
            disabled={isRegistering}
            className="w-full flex justify-center py-3 px-4 border border-transparent rounded-xl shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition disabled:bg-indigo-400"
          >
            {isRegistering ? 'Processing...' : 'Register Account'}
          </button>
        </form>
        <p className="mt-6 text-center text-sm text-gray-600">
          Already have an account?
          <button onClick={() => window.location.hash = '#/login'} className="font-medium text-indigo-600 hover:text-indigo-500 ml-1">
            Sign In
          </button>
        </p>
      </div>
    </div>
  );
};

// --- Main App Component ---
const App = () => {
  const [token, setToken] = useState(localStorage.getItem('novaBankToken'));
  const [role, setRole] = useState(localStorage.getItem('novaBankRole'));
  const [userId, setUserId] = useState(localStorage.getItem('novaBankUserId') || crypto.randomUUID());
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [route, setRoute] = useState(window.location.hash.substring(1) || '/login');

  // Handle Hash-based routing (simulating react-router-dom)
  useEffect(() => {
    const handleHashChange = () => {
      setRoute(window.location.hash.substring(1) || '/login');
      // If the user navigates back to a protected route, reset to dashboard or login check
      if (token && (route.startsWith('/login') || route.startsWith('/register'))) {
         setCurrentPage('dashboard');
      }
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [token, route]);

  // Initial check and route setup
  useEffect(() => {
    if (token) {
      setRoute('/dashboard');
      setCurrentPage('dashboard');
    } else {
      // Set to login or register based on hash path
      if (!route.startsWith('/login') && !route.startsWith('/register')) {
        setRoute('/login');
      }
    }
  }, [token, route]);

  const handleLogout = useCallback(() => {
    localStorage.removeItem('novaBankToken');
    localStorage.removeItem('novaBankRole');
    localStorage.removeItem('novaBankUserId');
    setToken(null);
    setRole(null);
    setUserId(crypto.randomUUID()); // New anonymous ID
    setRoute('/login');
    window.location.hash = '#/login';
  }, []);

  const handleAuthSuccess = (newUserId, newRole) => {
    const newToken = localStorage.getItem('novaBankToken');
    setToken(newToken);
    setRole(newRole);
    localStorage.setItem('novaBankUserId', newUserId);
    setUserId(newUserId);
    setRoute('/dashboard');
    window.location.hash = '#/dashboard';
  };

  const renderContent = () => {
    if (token && role === 'CUSTOMER') {
      // Authenticated Customer View
      return (
        <div className="flex flex-col md:flex-row min-h-screen bg-gray-50 font-sans">
          <Navbar currentPage={currentPage} setCurrentPage={setCurrentPage} onLogout={handleLogout} />
          <main className="flex-grow md:ml-64 p-0">
            {currentPage === 'dashboard' && <DashboardPage userId={userId} token={token} />}
            {currentPage === 'transfer' && <PlaceholderPage title="Internal Transfers" icon={Repeat} />}
            {currentPage === 'payments' && <PlaceholderPage title="Bill/Merchant Payments" icon={Zap} />}
            {currentPage === 'statements' && <PlaceholderPage title="Statements & Reports" icon={FileText} />}
            {currentPage === 'profile' && <PlaceholderPage title="Customer Profile Management" icon={User} />}
          </main>
        </div>
      );
    }

    // Unauthenticated View (Login/Register)
    const currentPath = route.replace(/^\//, ''); // Remove leading slash
    
    if (currentPath.startsWith('register')) {
      return <RegisterPage onRegistrationSuccess={() => window.location.hash = '#/login'} />;
    }
    
    // Default to login page
    return <LoginPage onAuthSuccess={handleAuthSuccess} />;
  };

  const PlaceholderPage = ({ title, icon: Icon }) => (
    <div className="p-8 h-full min-h-[500px] flex flex-col justify-center items-center">
      <div className="text-center bg-white p-10 rounded-2xl shadow-lg border border-gray-100 max-w-lg w-full">
        <Icon className="w-12 h-12 mx-auto text-indigo-500 mb-4" />
        <h2 className="text-2xl font-bold text-gray-800">{title}</h2>
        <p className="mt-2 text-gray-600">
          This is a feature placeholder for the NovaBank architecture. The backend service ({title.split(' ')[0]} Service) and API endpoints are defined, but the frontend interaction is pending implementation.
        </p>
        <p className="mt-4 text-sm text-indigo-500 font-medium">Architecture Note: This will interact with the API Gateway endpoint `/transactions/**` or `/payments/**`.</p>
      </div>
    </div>
  );

  return (
    <>
      {/* Tailwind CSS Setup - Inter Font */}
      <script src="https://cdn.tailwindcss.com"></script>
      <style>
        {`
          @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap');
          body { font-family: 'Inter', sans-serif; margin: 0; padding: 0; }
        `}
      </style>
      {renderContent()}
    </>
  );
};

export default App;