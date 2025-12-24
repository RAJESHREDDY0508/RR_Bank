import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  Wallet, 
  ArrowUpRight, 
  ArrowDownLeft, 
  TrendingUp,
  Eye,
  EyeOff,
} from 'lucide-react';
import { useAccounts } from '../hooks/useAccounts';
import { useTransactions } from '../hooks/useTransactions';
import { useAuth } from '../hooks/useAuth';
import Loading from '../components/common/Loading';
import { formatCurrency, formatDate } from '../utils/format';

const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const { accounts, totalBalance, loadAccounts, loading: accountsLoading } = useAccounts();
  const { recentTransactions, loadRecentTransactions } = useTransactions();
  const [showBalance, setShowBalance] = useState(true);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initDashboard = async () => {
      await loadAccounts();
      if (accounts.length > 0) {
        await loadRecentTransactions(accounts[0].accountNumber);
      }
      setLoading(false);
    };

    initDashboard();
  }, []);

  if (loading || accountsLoading) {
    return <Loading text="Loading dashboard..." />;
  }

  return (
    <div className="space-y-6">
      {/* Welcome Section */}
      <div className="bg-gradient-to-r from-blue-600 to-purple-600 rounded-xl p-6 text-white shadow-lg">
        <h1 className="text-3xl font-bold mb-2">
          Welcome back, {user?.firstName || user?.username}!
        </h1>
        <p className="text-blue-100">Here's your financial overview</p>
      </div>

      {/* Total Balance Card */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-700 dark:text-gray-300">
            Total Balance
          </h2>
          <button
            onClick={() => setShowBalance(!showBalance)}
            className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
          >
            {showBalance ? (
              <EyeOff className="w-5 h-5 text-gray-600 dark:text-gray-400" />
            ) : (
              <Eye className="w-5 h-5 text-gray-600 dark:text-gray-400" />
            )}
          </button>
        </div>
        <div className="text-4xl font-bold text-gray-900 dark:text-white">
          {showBalance ? formatCurrency(totalBalance) : '••••••'}
        </div>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">
          Across {accounts.length} account{accounts.length !== 1 ? 's' : ''}
        </p>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Link
          to="/transfer"
          className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg hover:shadow-xl transition-shadow flex items-center space-x-4"
        >
          <div className="p-3 bg-blue-100 dark:bg-blue-900 rounded-lg">
            <ArrowUpRight className="w-6 h-6 text-blue-600 dark:text-blue-400" />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white">Transfer Money</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">Send to any account</p>
          </div>
        </Link>

        <Link
          to="/payments/new"
          className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg hover:shadow-xl transition-shadow flex items-center space-x-4"
        >
          <div className="p-3 bg-green-100 dark:bg-green-900 rounded-lg">
            <ArrowDownLeft className="w-6 h-6 text-green-600 dark:text-green-400" />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white">Pay Bills</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">Quick bill payment</p>
          </div>
        </Link>

        <Link
          to="/accounts"
          className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg hover:shadow-xl transition-shadow flex items-center space-x-4"
        >
          <div className="p-3 bg-purple-100 dark:bg-purple-900 rounded-lg">
            <Wallet className="w-6 h-6 text-purple-600 dark:text-purple-400" />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white">My Accounts</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">View all accounts</p>
          </div>
        </Link>
      </div>

      {/* Accounts Overview */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">Your Accounts</h2>
          <Link
            to="/accounts"
            className="text-blue-600 dark:text-blue-400 hover:underline text-sm font-medium"
          >
            View All
          </Link>
        </div>
        <div className="space-y-3">
          {accounts.slice(0, 3).map((account) => (
            <Link
              key={account.accountNumber}
              to={`/accounts/${account.accountNumber}`}
              className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
            >
              <div>
                <p className="font-medium text-gray-900 dark:text-white">
                  {account.accountType}
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  {account.accountNumber}
                </p>
              </div>
              <div className="text-right">
                <p className="font-bold text-gray-900 dark:text-white">
                  {showBalance ? formatCurrency(account.balance) : '••••••'}
                </p>
                <span
                  className={`text-xs px-2 py-1 rounded-full ${
                    account.status === 'ACTIVE'
                      ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                      : 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200'
                  }`}
                >
                  {account.status}
                </span>
              </div>
            </Link>
          ))}
        </div>
      </div>

      {/* Recent Transactions */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">
            Recent Transactions
          </h2>
          <Link
            to="/transactions"
            className="text-blue-600 dark:text-blue-400 hover:underline text-sm font-medium"
          >
            View All
          </Link>
        </div>
        <div className="space-y-3">
          {recentTransactions.length > 0 ? (
            recentTransactions.map((transaction) => (
              <div
                key={transaction.transactionId}
                className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700 rounded-lg"
              >
                <div className="flex items-center space-x-3">
                  <div
                    className={`p-2 rounded-lg ${
                      transaction.transactionType === 'DEPOSIT'
                        ? 'bg-green-100 dark:bg-green-900'
                        : 'bg-red-100 dark:bg-red-900'
                    }`}
                  >
                    {transaction.transactionType === 'DEPOSIT' ? (
                      <ArrowDownLeft className="w-5 h-5 text-green-600 dark:text-green-400" />
                    ) : (
                      <ArrowUpRight className="w-5 h-5 text-red-600 dark:text-red-400" />
                    )}
                  </div>
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {transaction.transactionType}
                    </p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {formatDate(transaction.timestamp)}
                    </p>
                  </div>
                </div>
                <p
                  className={`font-bold ${
                    transaction.transactionType === 'DEPOSIT'
                      ? 'text-green-600 dark:text-green-400'
                      : 'text-red-600 dark:text-red-400'
                  }`}
                >
                  {transaction.transactionType === 'DEPOSIT' ? '+' : '-'}
                  {formatCurrency(transaction.amount)}
                </p>
              </div>
            ))
          ) : (
            <p className="text-center text-gray-500 dark:text-gray-400 py-8">
              No recent transactions
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
