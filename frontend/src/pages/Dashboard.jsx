import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { accountService, transactionService } from '../services/bankService';
import { 
  Wallet, 
  TrendingUp, 
  TrendingDown, 
  ArrowUpRight, 
  ArrowDownLeft,
  CreditCard,
  Loader,
  AlertCircle,
  RefreshCw
} from 'lucide-react';
import { format, startOfMonth, endOfMonth } from 'date-fns';

const Dashboard = () => {
  const [accounts, setAccounts] = useState([]);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [monthlyStats, setMonthlyStats] = useState({ income: 0, expenses: 0 });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const fetchDashboardData = useCallback(async (showRefreshing = false) => {
    try {
      if (showRefreshing) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError('');
      
      // Fetch accounts
      const accountsData = await accountService.getAccounts().catch(err => {
        console.error('Error fetching accounts:', err);
        return [];
      });
      
      setAccounts(accountsData || []);

      // Fetch transactions for all accounts to calculate monthly stats
      const activeAccounts = (accountsData || []).filter(a => a.status === 'ACTIVE');
      
      if (activeAccounts.length > 0) {
        // Get current month date range
        const now = new Date();
        const monthStart = format(startOfMonth(now), 'yyyy-MM-dd');
        const monthEnd = format(endOfMonth(now), 'yyyy-MM-dd');
        
        let allTransactions = [];
        let totalIncome = 0;
        let totalExpenses = 0;
        
        // Fetch transactions for each account
        for (const account of activeAccounts) {
          try {
            const txData = await transactionService.getTransactions(
              account.id,
              0,
              100, // Get more transactions for stats
              { startDate: monthStart, endDate: monthEnd }
            );
            
            const transactions = txData?.content || txData || [];
            
            // Calculate income and expenses for this account
            transactions.forEach(tx => {
              const amount = parseFloat(tx.amount || 0);
              const type = tx.transactionType?.toUpperCase();
              const status = tx.status?.toUpperCase();
              
              // Only count completed transactions
              if (status === 'COMPLETED' || status === 'SUCCESS') {
                if (type === 'DEPOSIT') {
                  totalIncome += amount;
                } else if (type === 'WITHDRAWAL' || type === 'WITHDRAW') {
                  totalExpenses += amount;
                } else if (type === 'TRANSFER') {
                  // For transfers, check if this account is sender or receiver
                  if (tx.fromAccountId === account.id) {
                    totalExpenses += amount;
                  } else if (tx.toAccountId === account.id) {
                    totalIncome += amount;
                  }
                }
              }
            });
            
            allTransactions = [...allTransactions, ...transactions];
          } catch (txErr) {
            console.error(`Error fetching transactions for account ${account.id}:`, txErr);
          }
        }
        
        setMonthlyStats({ income: totalIncome, expenses: totalExpenses });
        
        // Sort all transactions by date and get most recent
        allTransactions.sort((a, b) => {
          const dateA = new Date(a.createdAt || a.timestamp || 0);
          const dateB = new Date(b.createdAt || b.timestamp || 0);
          return dateB - dateA;
        });
        
        setRecentTransactions(allTransactions.slice(0, 5));
      } else {
        setMonthlyStats({ income: 0, expenses: 0 });
        setRecentTransactions([]);
      }
    } catch (err) {
      console.error('Dashboard error:', err);
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const handleRefresh = () => {
    fetchDashboardData(true);
  };

  const totalBalance = accounts.reduce((sum, account) => {
    const balance = parseFloat(account.balance || account.availableBalance || 0);
    return sum + balance;
  }, 0);

  const getTransactionIcon = (type) => {
    const transactionType = type?.toUpperCase();
    switch (transactionType) {
      case 'DEPOSIT':
        return <ArrowDownLeft className="h-5 w-5 text-green-600" />;
      case 'WITHDRAWAL':
      case 'WITHDRAW':
        return <ArrowUpRight className="h-5 w-5 text-red-600" />;
      case 'TRANSFER':
        return <ArrowUpRight className="h-5 w-5 text-blue-600" />;
      default:
        return <CreditCard className="h-5 w-5 text-gray-600" />;
    }
  };

  const getTransactionColor = (type) => {
    const transactionType = type?.toUpperCase();
    switch (transactionType) {
      case 'DEPOSIT':
        return 'text-green-600';
      case 'WITHDRAWAL':
      case 'WITHDRAW':
        return 'text-red-600';
      case 'TRANSFER':
        return 'text-blue-600';
      default:
        return 'text-gray-600';
    }
  };

  const formatAmount = (amount) => {
    return parseFloat(amount || 0).toLocaleString('en-US', { 
      minimumFractionDigits: 2, 
      maximumFractionDigits: 2 
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="flex items-center justify-center h-96">
          <Loader className="h-12 w-12 animate-spin text-primary-600" />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
            <p className="text-gray-600 mt-1">Welcome back! Here's your financial overview.</p>
          </div>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="inline-flex items-center px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start">
            <AlertCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
            <span className="text-sm">{error}</span>
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Total Balance */}
          <div className="card bg-gradient-to-br from-primary-500 to-primary-600 text-white">
            <div className="flex items-center justify-between mb-4">
              <Wallet className="h-8 w-8 opacity-80" />
              <span className="text-sm opacity-90">Total Balance</span>
            </div>
            <div className="space-y-1">
              <p className="text-3xl font-bold">${formatAmount(totalBalance)}</p>
              <p className="text-sm opacity-90">Across {accounts.length} account{accounts.length !== 1 ? 's' : ''}</p>
            </div>
          </div>

          {/* Income */}
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <div className="bg-green-100 p-2 rounded-lg">
                <TrendingUp className="h-6 w-6 text-green-600" />
              </div>
              <span className="text-sm text-gray-600">This Month</span>
            </div>
            <div className="space-y-1">
              <p className="text-2xl font-bold text-green-600">${formatAmount(monthlyStats.income)}</p>
              <p className="text-sm text-gray-600">Total Income</p>
            </div>
          </div>

          {/* Expenses */}
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <div className="bg-red-100 p-2 rounded-lg">
                <TrendingDown className="h-6 w-6 text-red-600" />
              </div>
              <span className="text-sm text-gray-600">This Month</span>
            </div>
            <div className="space-y-1">
              <p className="text-2xl font-bold text-red-600">${formatAmount(monthlyStats.expenses)}</p>
              <p className="text-sm text-gray-600">Total Expenses</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Accounts */}
          <div className="card">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-semibold text-gray-900">Your Accounts</h2>
              <Link to="/accounts" className="text-sm text-primary-600 hover:text-primary-700 font-medium">
                View All
              </Link>
            </div>
            <div className="space-y-4">
              {accounts.length > 0 ? (
                accounts.slice(0, 3).map((account) => (
                  <div
                    key={account.id || account.accountNumber}
                    className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                  >
                    <div className="flex items-center space-x-4">
                      <div className="bg-primary-100 p-3 rounded-full">
                        <CreditCard className="h-6 w-6 text-primary-600" />
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">{account.accountType}</p>
                        <p className="text-sm text-gray-600">{account.accountNumber}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold text-gray-900">
                        ${formatAmount(account.balance || account.availableBalance)}
                      </p>
                      <p className="text-sm text-gray-600">{account.status}</p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <CreditCard className="h-12 w-12 mx-auto mb-3 text-gray-400" />
                  <p>No accounts found</p>
                  <Link 
                    to="/accounts" 
                    className="mt-2 inline-block text-primary-600 hover:text-primary-700 font-medium"
                  >
                    Open your first account
                  </Link>
                </div>
              )}
            </div>
          </div>

          {/* Recent Transactions */}
          <div className="card">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-semibold text-gray-900">Recent Transactions</h2>
              <Link to="/transactions" className="text-sm text-primary-600 hover:text-primary-700 font-medium">
                View All
              </Link>
            </div>
            <div className="space-y-3">
              {recentTransactions.length > 0 ? (
                recentTransactions.map((transaction, index) => (
                  <div
                    key={transaction.id || transaction.transactionId || transaction.transactionReference || index}
                    className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                  >
                    <div className="flex items-center space-x-3">
                      <div className={`p-2 rounded-full ${
                        transaction.transactionType?.toUpperCase() === 'DEPOSIT' ? 'bg-green-100' :
                        transaction.transactionType?.toUpperCase() === 'WITHDRAWAL' || transaction.transactionType?.toUpperCase() === 'WITHDRAW' ? 'bg-red-100' : 'bg-blue-100'
                      }`}>
                        {getTransactionIcon(transaction.transactionType)}
                      </div>
                      <div>
                        <p className="font-medium text-gray-900 text-sm">
                          {transaction.description || transaction.transactionType || 'Transaction'}
                        </p>
                        <p className="text-xs text-gray-600">
                          {transaction.createdAt ? format(new Date(transaction.createdAt), 'MMM dd, yyyy HH:mm') : 
                           transaction.timestamp ? format(new Date(transaction.timestamp), 'MMM dd, yyyy HH:mm') : 'N/A'}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className={`font-semibold text-sm ${getTransactionColor(transaction.transactionType)}`}>
                        {transaction.transactionType?.toUpperCase() === 'DEPOSIT' ? '+' : '-'}
                        ${formatAmount(transaction.amount)}
                      </p>
                      <p className="text-xs text-gray-600">{transaction.status}</p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <ArrowUpRight className="h-12 w-12 mx-auto mb-3 text-gray-400" />
                  <p>No recent transactions</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="mt-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Quick Actions</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Link
              to="/transfer"
              className="card hover:shadow-md transition-shadow cursor-pointer group"
            >
              <div className="flex items-center space-x-4">
                <div className="bg-primary-100 p-3 rounded-lg group-hover:bg-primary-200 transition-colors">
                  <ArrowUpRight className="h-6 w-6 text-primary-600" />
                </div>
                <div>
                  <p className="font-semibold text-gray-900">Transfer Money</p>
                  <p className="text-sm text-gray-600">Send to another account</p>
                </div>
              </div>
            </Link>

            <Link
              to="/transfer"
              className="card hover:shadow-md transition-shadow cursor-pointer group"
            >
              <div className="flex items-center space-x-4">
                <div className="bg-green-100 p-3 rounded-lg group-hover:bg-green-200 transition-colors">
                  <ArrowDownLeft className="h-6 w-6 text-green-600" />
                </div>
                <div>
                  <p className="font-semibold text-gray-900">Deposit Funds</p>
                  <p className="text-sm text-gray-600">Add money to account</p>
                </div>
              </div>
            </Link>

            <Link
              to="/transactions"
              className="card hover:shadow-md transition-shadow cursor-pointer group"
            >
              <div className="flex items-center space-x-4">
                <div className="bg-blue-100 p-3 rounded-lg group-hover:bg-blue-200 transition-colors">
                  <CreditCard className="h-6 w-6 text-blue-600" />
                </div>
                <div>
                  <p className="font-semibold text-gray-900">View Statements</p>
                  <p className="text-sm text-gray-600">Check transaction history</p>
                </div>
              </div>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
