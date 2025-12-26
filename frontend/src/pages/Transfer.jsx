import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { accountService, transactionService } from '../services/bankService';
import { 
  ArrowUpRight, 
  ArrowDownLeft, 
  ArrowLeftRight,
  AlertCircle,
  CheckCircle,
  Wallet,
  Info
} from 'lucide-react';

const Transfer = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [accounts, setAccounts] = useState([]);
  const [limits, setLimits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [transactionType, setTransactionType] = useState('transfer');
  
  const [formData, setFormData] = useState({
    fromAccountId: searchParams.get('from') || '',
    toAccountId: '',
    toAccountNumber: '',
    amount: '',
    description: ''
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [accountsData, limitsData] = await Promise.all([
        accountService.getAccounts(),
        transactionService.getLimits()
      ]);
      setAccounts(accountsData.filter(a => a.status === 'ACTIVE'));
      setLimits(limitsData);
      
      // Set default from account
      if (!formData.fromAccountId && accountsData.length > 0) {
        setFormData(prev => ({ ...prev, fromAccountId: accountsData[0].id }));
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const getSelectedAccount = () => {
    return accounts.find(a => a.id === formData.fromAccountId);
  };

  const getApplicableLimit = () => {
    const type = transactionType.toUpperCase();
    return limits.find(l => l.limitType === type || l.limitType === 'ALL');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    const amount = parseFloat(formData.amount);
    if (!amount || amount <= 0) {
      setError('Please enter a valid amount');
      return;
    }

    const limit = getApplicableLimit();
    if (limit) {
      if (amount > limit.perTransactionLimit) {
        setError(`Amount exceeds per-transaction limit of $${limit.perTransactionLimit.toLocaleString()}`);
        return;
      }
      if (amount > limit.remainingDaily) {
        setError(`Amount exceeds remaining daily limit of $${limit.remainingDaily.toLocaleString()}`);
        return;
      }
    }

    try {
      setSubmitting(true);

      if (transactionType === 'deposit') {
        await transactionService.deposit(formData.fromAccountId, amount, formData.description);
        setSuccess(`Successfully deposited $${amount.toLocaleString()}`);
      } else if (transactionType === 'withdraw') {
        await transactionService.withdraw(formData.fromAccountId, amount, formData.description);
        setSuccess(`Successfully withdrew $${amount.toLocaleString()}`);
      } else {
        // Transfer
        if (!formData.toAccountId && !formData.toAccountNumber) {
          setError('Please select or enter a destination account');
          return;
        }
        await transactionService.transfer(
          formData.fromAccountId,
          formData.toAccountId || formData.toAccountNumber,
          amount,
          formData.description
        );
        setSuccess(`Successfully transferred $${amount.toLocaleString()}`);
      }

      // Reset form and refresh data
      setFormData({ ...formData, amount: '', description: '' });
      fetchData();
      
      // Redirect after success
      setTimeout(() => navigate('/accounts'), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Transaction failed');
    } finally {
      setSubmitting(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount || 0);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const selectedAccount = getSelectedAccount();
  const applicableLimit = getApplicableLimit();

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-white rounded-xl shadow-sm">
          {/* Transaction Type Tabs */}
          <div className="border-b border-gray-200">
            <nav className="flex -mb-px">
              <button
                onClick={() => setTransactionType('transfer')}
                className={`flex-1 py-4 px-6 text-center border-b-2 font-medium text-sm ${
                  transactionType === 'transfer'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <ArrowLeftRight className="w-5 h-5 mx-auto mb-1" />
                Transfer
              </button>
              <button
                onClick={() => setTransactionType('deposit')}
                className={`flex-1 py-4 px-6 text-center border-b-2 font-medium text-sm ${
                  transactionType === 'deposit'
                    ? 'border-green-500 text-green-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <ArrowDownLeft className="w-5 h-5 mx-auto mb-1" />
                Deposit
              </button>
              <button
                onClick={() => setTransactionType('withdraw')}
                className={`flex-1 py-4 px-6 text-center border-b-2 font-medium text-sm ${
                  transactionType === 'withdraw'
                    ? 'border-red-500 text-red-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <ArrowUpRight className="w-5 h-5 mx-auto mb-1" />
                Withdraw
              </button>
            </nav>
          </div>

          <div className="p-6">
            {error && (
              <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center">
                <AlertCircle className="w-5 h-5 mr-2 flex-shrink-0" />
                {error}
              </div>
            )}

            {success && (
              <div className="mb-6 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-center">
                <CheckCircle className="w-5 h-5 mr-2 flex-shrink-0" />
                {success}
              </div>
            )}

            {/* Limits Info */}
            {applicableLimit && (
              <div className="mb-6 bg-blue-50 border border-blue-200 text-blue-700 px-4 py-3 rounded-lg">
                <div className="flex items-center mb-2">
                  <Info className="w-5 h-5 mr-2" />
                  <span className="font-medium">Transaction Limits</span>
                </div>
                <div className="grid grid-cols-3 gap-4 text-sm">
                  <div>
                    <span className="text-blue-500">Per Transaction:</span>
                    <br />
                    <span className="font-medium">{formatCurrency(applicableLimit.perTransactionLimit)}</span>
                  </div>
                  <div>
                    <span className="text-blue-500">Daily Remaining:</span>
                    <br />
                    <span className="font-medium">{formatCurrency(applicableLimit.remainingDaily)}</span>
                  </div>
                  <div>
                    <span className="text-blue-500">Monthly Remaining:</span>
                    <br />
                    <span className="font-medium">{formatCurrency(applicableLimit.remainingMonthly)}</span>
                  </div>
                </div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* From Account */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {transactionType === 'deposit' ? 'To Account' : 'From Account'}
                </label>
                <select
                  value={formData.fromAccountId}
                  onChange={(e) => setFormData({ ...formData, fromAccountId: e.target.value })}
                  className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                >
                  <option value="">Select Account</option>
                  {accounts.map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.accountType} - ****{account.accountNumber?.slice(-4)} ({formatCurrency(account.availableBalance)})
                    </option>
                  ))}
                </select>
              </div>

              {/* Selected Account Balance */}
              {selectedAccount && (
                <div className="bg-gray-50 rounded-lg p-4 flex items-center justify-between">
                  <div className="flex items-center">
                    <Wallet className="w-8 h-8 text-blue-600 mr-3" />
                    <div>
                      <p className="text-sm text-gray-500">Available Balance</p>
                      <p className="text-xl font-bold text-gray-900">
                        {formatCurrency(selectedAccount.availableBalance)}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* To Account (for transfers) */}
              {transactionType === 'transfer' && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">To Account</label>
                  <select
                    value={formData.toAccountId}
                    onChange={(e) => setFormData({ ...formData, toAccountId: e.target.value, toAccountNumber: '' })}
                    className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 mb-2"
                  >
                    <option value="">Select your account or enter external</option>
                    {accounts
                      .filter(a => a.id !== formData.fromAccountId)
                      .map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.accountType} - ****{account.accountNumber?.slice(-4)}
                        </option>
                      ))}
                  </select>
                  <p className="text-sm text-gray-500 text-center my-2">OR</p>
                  <input
                    type="text"
                    value={formData.toAccountNumber}
                    onChange={(e) => setFormData({ ...formData, toAccountNumber: e.target.value, toAccountId: '' })}
                    className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Enter external account number"
                  />
                </div>
              )}

              {/* Amount */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Amount</label>
                <div className="relative">
                  <span className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-500">$</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    max={transactionType === 'withdraw' ? selectedAccount?.availableBalance : undefined}
                    value={formData.amount}
                    onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                    className="w-full border border-gray-300 rounded-lg pl-8 pr-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 text-lg"
                    placeholder="0.00"
                    required
                  />
                </div>
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Description (Optional)</label>
                <input
                  type="text"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g., Rent payment, Savings transfer"
                />
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={submitting}
                className={`w-full py-3 px-4 rounded-lg font-medium text-white transition-colors ${
                  transactionType === 'deposit'
                    ? 'bg-green-600 hover:bg-green-700'
                    : transactionType === 'withdraw'
                    ? 'bg-red-600 hover:bg-red-700'
                    : 'bg-blue-600 hover:bg-blue-700'
                } ${submitting ? 'opacity-50 cursor-not-allowed' : ''}`}
              >
                {submitting ? (
                  <span className="flex items-center justify-center">
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                    Processing...
                  </span>
                ) : (
                  <span className="flex items-center justify-center">
                    {transactionType === 'deposit' && <ArrowDownLeft className="w-5 h-5 mr-2" />}
                    {transactionType === 'withdraw' && <ArrowUpRight className="w-5 h-5 mr-2" />}
                    {transactionType === 'transfer' && <ArrowLeftRight className="w-5 h-5 mr-2" />}
                    {transactionType === 'deposit' && 'Deposit Money'}
                    {transactionType === 'withdraw' && 'Withdraw Money'}
                    {transactionType === 'transfer' && 'Transfer Money'}
                  </span>
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Transfer;
