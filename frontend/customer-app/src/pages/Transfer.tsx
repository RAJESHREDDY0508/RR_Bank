import React, { useState, useEffect } from 'react';
import { ArrowLeftRight } from 'lucide-react';
import { useAccounts } from '../hooks/useAccounts';
import { useTransactions } from '../hooks/useTransactions';
import toast from 'react-hot-toast';
import { formatCurrency } from '../utils/format';

const Transfer: React.FC = () => {
  const { accounts, loadAccounts } = useAccounts();
  const { transfer, loading } = useTransactions();
  const [formData, setFormData] = useState({
    fromAccount: '',
    toAccount: '',
    amount: '',
    description: '',
  });

  useEffect(() => {
    loadAccounts();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.fromAccount || !formData.toAccount || !formData.amount) {
      toast.error('Please fill in all required fields');
      return;
    }

    if (formData.fromAccount === formData.toAccount) {
      toast.error('Cannot transfer to the same account');
      return;
    }

    const amount = parseFloat(formData.amount);
    if (amount <= 0) {
      toast.error('Amount must be greater than 0');
      return;
    }

    const success = await transfer({
      fromAccountNumber: formData.fromAccount,
      toAccountNumber: formData.toAccount,
      amount,
      description: formData.description || 'Transfer',
    });

    if (success) {
      toast.success('Transfer completed successfully!');
      setFormData({
        fromAccount: '',
        toAccount: '',
        amount: '',
        description: '',
      });
      loadAccounts();
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Transfer Money</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-1">
          Send money between your accounts
        </p>
      </div>

      <form onSubmit={handleSubmit} className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg space-y-6">
        {/* From Account */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            From Account *
          </label>
          <select
            value={formData.fromAccount}
            onChange={(e) => setFormData({ ...formData, fromAccount: e.target.value })}
            className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
            required
          >
            <option value="">Select account</option>
            {accounts.map((account) => (
              <option key={account.accountNumber} value={account.accountNumber}>
                {account.accountType} - {account.accountNumber} ({formatCurrency(account.balance)})
              </option>
            ))}
          </select>
        </div>

        {/* To Account */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            To Account *
          </label>
          <input
            type="text"
            value={formData.toAccount}
            onChange={(e) => setFormData({ ...formData, toAccount: e.target.value })}
            placeholder="Enter account number"
            className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
            required
          />
        </div>

        {/* Amount */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Amount *
          </label>
          <input
            type="number"
            step="0.01"
            min="0.01"
            value={formData.amount}
            onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
            placeholder="0.00"
            className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
            required
          />
        </div>

        {/* Description */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Description (Optional)
          </label>
          <textarea
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            placeholder="Enter a description..."
            rows={3}
            className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white resize-none"
          />
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition-colors flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? (
            <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
          ) : (
            <>
              <ArrowLeftRight size={20} />
              <span>Transfer Money</span>
            </>
          )}
        </button>
      </form>
    </div>
  );
};

export default Transfer;
