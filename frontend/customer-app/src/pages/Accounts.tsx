import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Wallet, Eye, EyeOff, Plus } from 'lucide-react';
import { useAccounts } from '../hooks/useAccounts';
import Loading from '../components/common/Loading';
import { formatCurrency } from '../utils/format';

const Accounts: React.FC = () => {
  const { accounts, totalBalance, loadAccounts, loading } = useAccounts();
  const [showBalances, setShowBalances] = useState(true);

  useEffect(() => {
    loadAccounts();
  }, []);

  if (loading && accounts.length === 0) {
    return <Loading text="Loading accounts..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">My Accounts</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-1">
            Manage your accounts and balances
          </p>
        </div>
        <button
          onClick={() => setShowBalances(!showBalances)}
          className="p-3 bg-white dark:bg-gray-800 rounded-lg shadow hover:shadow-md transition-shadow"
        >
          {showBalances ? (
            <EyeOff className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          ) : (
            <Eye className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          )}
        </button>
      </div>

      {/* Total Balance */}
      <div className="bg-gradient-to-r from-blue-600 to-purple-600 rounded-xl p-6 text-white shadow-lg">
        <p className="text-blue-100 mb-2">Total Balance</p>
        <p className="text-4xl font-bold">
          {showBalances ? formatCurrency(totalBalance) : '••••••'}
        </p>
      </div>

      {/* Accounts Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {accounts.map((account) => (
          <Link
            key={account.accountNumber}
            to={`/accounts/${account.accountNumber}`}
            className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg hover:shadow-xl transition-shadow"
          >
            <div className="flex items-start justify-between mb-4">
              <div className="p-3 bg-blue-100 dark:bg-blue-900 rounded-lg">
                <Wallet className="w-6 h-6 text-blue-600 dark:text-blue-400" />
              </div>
              <span
                className={`px-3 py-1 rounded-full text-xs font-medium ${
                  account.status === 'ACTIVE'
                    ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                    : 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200'
                }`}
              >
                {account.status}
              </span>
            </div>

            <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-1">
              {account.accountType}
            </h3>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
              {account.accountNumber}
            </p>

            <div className="pt-4 border-t dark:border-gray-700">
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-1">Balance</p>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">
                {showBalances ? formatCurrency(account.balance) : '••••••'}
              </p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
};

export default Accounts;
