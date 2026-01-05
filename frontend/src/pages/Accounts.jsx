import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { accountService, transactionService } from '../services/bankService';
import Navbar from '../components/Navbar';
import { 
  CreditCard, 
  Plus, 
  ArrowUpRight, 
  ArrowDownLeft, 
  CheckCircle,
  XCircle,
  AlertCircle,
  Wallet,
  TrendingUp,
  Loader,
  Copy,
  Check,
  RefreshCw
} from 'lucide-react';

const Accounts = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [accounts, setAccounts] = useState([]);
  const [limits, setLimits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [showNewAccountModal, setShowNewAccountModal] = useState(false);
  const [creating, setCreating] = useState(false);
  const [copiedAccountNumber, setCopiedAccountNumber] = useState(null);
  const [newAccount, setNewAccount] = useState({
    accountType: 'CHECKING',
    initialDeposit: 0,
    currency: 'USD'
  });

  const fetchData = useCallback(async (showRefreshing = false) => {
    try {
      if (showRefreshing) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);
      
      const [accountsData, limitsData] = await Promise.all([
        accountService.getAccounts().catch(err => {
          console.error('Error fetching accounts:', err);
          return [];
        }),
        transactionService.getLimits().catch(err => {
          console.error('Error fetching limits:', err);
          return [];
        })
      ]);
      
      setAccounts(accountsData || []);
      setLimits(limitsData || []);
    } catch (err) {
      console.error('Error fetching data:', err);
      setError(err.response?.data?.message || 'Failed to load accounts');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Fetch data on mount and when location changes (coming back from transfer page)
  useEffect(() => {
    fetchData();
  }, [fetchData, location.key]);

  // Also refresh when window gets focus (user comes back to tab)
  useEffect(() => {
    const handleFocus = () => {
      fetchData(true);
    };
    
    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [fetchData]);

  const handleRefresh = () => {
    fetchData(true);
  };

  const handleCreateAccount = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    
    try {
      setCreating(true);
      
      // Create the account
      const account = await accountService.createAccount(
        newAccount.accountType,
        newAccount.currency
      );
      
      // If there's an initial deposit and we got an account, make the deposit
      if (newAccount.initialDeposit > 0 && account?.id) {
        try {
          await transactionService.deposit(
            account.id, 
            newAccount.initialDeposit, 
            'Initial deposit'
          );
        } catch (depositErr) {
          console.error('Initial deposit failed:', depositErr);
          // Account was created, just the deposit failed
          setError('Account created but initial deposit failed. You can deposit later.');
        }
      }
      
      setShowNewAccountModal(false);
      setNewAccount({ accountType: 'CHECKING', initialDeposit: 0, currency: 'USD' });
      setSuccess('Account created successfully!');
      
      // Refresh the accounts list
      await fetchData(true);
      
    } catch (err) {
      console.error('Error creating account:', err);
      setError(err.response?.data?.message || 'Failed to create account');
    } finally {
      setCreating(false);
    }
  };

  const copyAccountNumber = (accountNumber) => {
    navigator.clipboard.writeText(accountNumber);
    setCopiedAccountNumber(accountNumber);
    setTimeout(() => setCopiedAccountNumber(null), 2000);
  };

  const getStatusBadge = (status) => {
    const badges = {
      ACTIVE: { color: 'bg-green-100 text-green-800', icon: CheckCircle },
      PENDING: { color: 'bg-yellow-100 text-yellow-800', icon: Loader },
      SUSPENDED: { color: 'bg-red-100 text-red-800', icon: XCircle },
      CLOSED: { color: 'bg-gray-100 text-gray-800', icon: XCircle }
    };
    const badge = badges[status] || badges.ACTIVE;
    const Icon = badge.icon;
    return (
      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${badge.color}`}>
        <Icon className="w-3 h-3 mr-1" />
        {status}
      </span>
    );
  };

  const formatCurrency = (amount, currency = 'USD') => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(amount || 0);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="flex items-center justify-center h-96">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="flex justify-between items-center mb-8">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">My Accounts</h1>
              <p className="mt-1 text-sm text-gray-500">Manage your bank accounts and view balances</p>
            </div>
            <div className="flex space-x-3">
              <button
                onClick={handleRefresh}
                disabled={refreshing}
                className="inline-flex items-center px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
              >
                <RefreshCw className={`w-4 h-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                {refreshing ? 'Refreshing...' : 'Refresh'}
              </button>
              <button
                onClick={() => setShowNewAccountModal(true)}
                className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                <Plus className="w-4 h-4 mr-2" />
                Open New Account
              </button>
            </div>
          </div>

          {/* Messages */}
          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center">
              <AlertCircle className="w-5 h-5 mr-2" />
              {error}
              <button onClick={() => setError(null)} className="ml-auto text-red-500 hover:text-red-700">
                <XCircle className="w-4 h-4" />
              </button>
            </div>
          )}

          {success && (
            <div className="mb-6 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-center">
              <CheckCircle className="w-5 h-5 mr-2" />
              {success}
              <button onClick={() => setSuccess(null)} className="ml-auto text-green-500 hover:text-green-700">
                <XCircle className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Transaction Limits Card */}
          {limits.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm p-6 mb-8">
              <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                <TrendingUp className="w-5 h-5 mr-2 text-blue-600" />
                Your Transaction Limits
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {limits.map((limit, index) => (
                  <div key={index} className="bg-gray-50 rounded-lg p-4">
                    <p className="text-sm text-gray-500">{limit.limitType} Limit</p>
                    <div className="mt-2 space-y-2">
                      <div className="flex justify-between">
                        <span className="text-xs text-gray-500">Per Transaction</span>
                        <span className="text-sm font-medium">{formatCurrency(limit.perTransactionLimit)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-xs text-gray-500">Daily Remaining</span>
                        <span className="text-sm font-medium text-green-600">{formatCurrency(limit.remainingDaily)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-xs text-gray-500">Monthly Remaining</span>
                        <span className="text-sm font-medium text-blue-600">{formatCurrency(limit.remainingMonthly)}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Active Accounts */}
          <div className="mb-8">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Active Accounts</h2>
            {accounts.length === 0 ? (
              <div className="bg-white rounded-xl shadow-sm p-8 text-center">
                <Wallet className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-500 mb-4">No accounts yet. Open your first account!</p>
                <button
                  onClick={() => setShowNewAccountModal(true)}
                  className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Open New Account
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {accounts.map((account) => (
                  <div
                    key={account.id}
                    className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow"
                  >
                    <div className="p-6">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center">
                          <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                            <CreditCard className="w-5 h-5 text-blue-600" />
                          </div>
                          <div className="ml-3">
                            <p className="text-sm font-medium text-gray-900">{account.accountType}</p>
                            <p className="text-xs text-gray-500">{account.currency}</p>
                          </div>
                        </div>
                        {getStatusBadge(account.status)}
                      </div>
                      
                      {/* Account Number with Copy */}
                      <div className="mb-4 bg-gray-50 rounded-lg px-3 py-2">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-xs text-gray-500">Account Number</p>
                            <p className="font-mono text-sm font-medium">{account.accountNumber}</p>
                          </div>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              copyAccountNumber(account.accountNumber);
                            }}
                            className="text-blue-600 hover:text-blue-800 p-1"
                            title="Copy account number"
                          >
                            {copiedAccountNumber === account.accountNumber ? (
                              <Check className="w-4 h-4 text-green-600" />
                            ) : (
                              <Copy className="w-4 h-4" />
                            )}
                          </button>
                        </div>
                      </div>
                      
                      <div className="mb-4">
                        <p className="text-sm text-gray-500">Available Balance</p>
                        <p className="text-2xl font-bold text-gray-900">
                          {formatCurrency(account.availableBalance || account.balance, account.currency)}
                        </p>
                      </div>
                      
                      <div className="flex space-x-2">
                        <button
                          onClick={() => navigate(`/transfer?from=${account.id}`)}
                          className="flex-1 inline-flex items-center justify-center px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                        >
                          <ArrowUpRight className="w-4 h-4 mr-1" />
                          Send
                        </button>
                        <button
                          onClick={() => navigate(`/transfer?account=${account.id}`)}
                          className="flex-1 inline-flex items-center justify-center px-3 py-2 border border-transparent rounded-md text-sm font-medium text-white bg-green-600 hover:bg-green-700"
                        >
                          <ArrowDownLeft className="w-4 h-4 mr-1" />
                          Deposit
                        </button>
                      </div>
                      
                      <button
                        onClick={() => navigate(`/transactions?accountId=${account.id}`)}
                        className="mt-2 w-full text-center text-sm text-blue-600 hover:text-blue-800 py-2"
                      >
                        View Transactions →
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* New Account Modal */}
          {showNewAccountModal && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Open New Account</h3>
                <form onSubmit={handleCreateAccount}>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Account Type</label>
                      <select
                        value={newAccount.accountType}
                        onChange={(e) => setNewAccount({ ...newAccount, accountType: e.target.value })}
                        className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        <option value="CHECKING">Checking Account</option>
                        <option value="SAVINGS">Savings Account</option>
                        <option value="BUSINESS">Business Account</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Currency</label>
                      <select
                        value={newAccount.currency}
                        onChange={(e) => setNewAccount({ ...newAccount, currency: e.target.value })}
                        className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        <option value="USD">USD - US Dollar</option>
                        <option value="EUR">EUR - Euro</option>
                        <option value="GBP">GBP - British Pound</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Initial Deposit (Optional)</label>
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={newAccount.initialDeposit}
                        onChange={(e) => setNewAccount({ ...newAccount, initialDeposit: parseFloat(e.target.value) || 0 })}
                        className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="0.00"
                      />
                      <p className="text-xs text-gray-500 mt-1">You can deposit money after account creation</p>
                    </div>
                  </div>
                  <div className="mt-6 flex space-x-3">
                    <button
                      type="button"
                      onClick={() => {
                        setShowNewAccountModal(false);
                        setNewAccount({ accountType: 'CHECKING', initialDeposit: 0, currency: 'USD' });
                      }}
                      className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                      disabled={creating}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="flex-1 px-4 py-2 border border-transparent rounded-md text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                      disabled={creating}
                    >
                      {creating ? (
                        <span className="flex items-center justify-center">
                          <Loader className="w-4 h-4 mr-2 animate-spin" />
                          Creating...
                        </span>
                      ) : (
                        'Create Account'
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Accounts;
