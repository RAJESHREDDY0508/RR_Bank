import { useState, useEffect } from 'react';
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
  AlertCircle
} from 'lucide-react';
import { format } from 'date-fns';

const Dashboard = () => {
  const [accounts, setAccounts] = useState([]);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const accountsData = await accountService.getAccounts();
      setAccounts(accountsData);

      // Fetch recent transactions for the first account
      if (accountsData.length > 0) {
        const transactionsData = await transactionService.getTransactions(
          accountsData[0].accountNumber,
          0,
          5
        );
        setRecentTransactions(transactionsData.content || []);
      }
    } catch (err) {
      setError('Failed to load dashboard data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const totalBalance = accounts.reduce((sum, account) => sum + parseFloat(account.balance || 0), 0);

  const getTransactionIcon = (type) => {
    switch (type) {
      case 'DEPOSIT':
        return <ArrowDownLeft className="h-5 w-5 text-green-600" />;
      case 'WITHDRAWAL':
        return <ArrowUpRight className="h-5 w-5 text-red-600" />;
      case 'TRANSFER':
        return <ArrowUpRight className="h-5 w-5 text-blue-600" />;
      default:
        return <CreditCard className="h-5 w-5 text-gray-600" />;
    }
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
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-600 mt-1">Welcome back! Here's your financial overview.</p>
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
              <p className="text-3xl font-bold">${totalBalance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
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
              <p className="text-2xl font-bold text-gray-900">$0.00</p>
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
              <p className="text-2xl font-bold text-gray-900">$0.00</p>
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
                accounts.map((account) => (
                  <div
                    key={account.accountNumber}
                    className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                  >
                    <div className="flex items-center space-x-4">
                      <div className="bg-primary-100 p-3 rounded-full">
                        <CreditCard className="h-6 w-6 text-primary-600" />
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">{account.accountType}</p>
                        <p className="text-sm text-gray-600">****{account.accountNumber.slice(-4)}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold text-gray-900">
                        ${parseFloat(account.balance || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </p>
                      <p className="text-sm text-gray-600">{account.status}</p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <CreditCard className="h-12 w-12 mx-auto mb-3 text-gray-400" />
                  <p>No accounts found</p>
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
                recentTransactions.map((transaction) => (
                  <div
                    key={transaction.transactionId}
                    className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                  >
                    <div className="flex items-center space-x-3">
                      <div className={`p-2 rounded-full ${
                        transaction.transactionType === 'DEPOSIT' ? 'bg-green-100' :
                        transaction.transactionType === 'WITHDRAWAL' ? 'bg-red-100' : 'bg-blue-100'
                      }`}>
                        {getTransactionIcon(transaction.transactionType)}
                      </div>
                      <div>
                        <p className="font-medium text-gray-900 text-sm">
                          {transaction.transactionType}
                        </p>
                        <p className="text-xs text-gray-600">
                          {format(new Date(transaction.timestamp), 'MMM dd, yyyy')}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className={`font-semibold text-sm ${
                        transaction.transactionType === 'DEPOSIT' ? 'text-green-600' :
                        transaction.transactionType === 'WITHDRAWAL' ? 'text-red-600' : 'text-blue-600'
                      }`}>
                        {transaction.transactionType === 'DEPOSIT' ? '+' : '-'}
                        ${parseFloat(transaction.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
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
              to="/accounts"
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
