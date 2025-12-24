import React, { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { useAccounts } from '../hooks/useAccounts';
import { useTransactions } from '../hooks/useTransactions';
import Loading from '../components/common/Loading';
import { formatCurrency, formatDate } from '../utils/format';

const AccountDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { selectedAccount, loadAccountById, loading: accountLoading } = useAccounts();
  const { transactions, loadTransactions, loading: transactionsLoading } = useTransactions();

  useEffect(() => {
    if (id) {
      loadAccountById(id);
      loadTransactions(id, 0, 10);
    }
  }, [id]);

  if (accountLoading || !selectedAccount) {
    return <Loading text="Loading account details..." />;
  }

  return (
    <div className="space-y-6">
      {/* Back Button */}
      <button
        onClick={() => navigate('/accounts')}
        className="flex items-center space-x-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white"
      >
        <ArrowLeft className="w-5 h-5" />
        <span>Back to Accounts</span>
      </button>

      {/* Account Info */}
      <div className="bg-gradient-to-r from-blue-600 to-purple-600 rounded-xl p-6 text-white shadow-lg">
        <h1 className="text-2xl font-bold mb-2">{selectedAccount.accountType}</h1>
        <p className="text-blue-100 mb-4">{selectedAccount.accountNumber}</p>
        <div className="text-4xl font-bold">{formatCurrency(selectedAccount.balance)}</div>
        <span
          className={`inline-block mt-4 px-3 py-1 rounded-full text-xs font-medium ${
            selectedAccount.status === 'ACTIVE'
              ? 'bg-green-500 text-white'
              : 'bg-gray-500 text-white'
          }`}
        >
          {selectedAccount.status}
        </span>
      </div>

      {/* Recent Transactions */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg">
        <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
          Recent Transactions
        </h2>
        {transactionsLoading ? (
          <Loading size="sm" />
        ) : transactions.length > 0 ? (
          <div className="space-y-3">
            {transactions.map((transaction) => (
              <div
                key={transaction.transactionId}
                className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700 rounded-lg"
              >
                <div>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {transaction.transactionType}
                  </p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    {formatDate(transaction.timestamp)}
                  </p>
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
            ))}
          </div>
        ) : (
          <p className="text-center text-gray-500 dark:text-gray-400 py-8">
            No transactions yet
          </p>
        )}
      </div>
    </div>
  );
};

export default AccountDetails;
