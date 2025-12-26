import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { accountService, transactionService } from '../services/bankService';
import { 
  CreditCard, 
  Plus, 
  ArrowUpRight, 
  ArrowDownLeft, 
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Wallet,
  TrendingUp,
  Eye
} from 'lucide-react';

const Accounts = () => {
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState([]);
  const [requests, setRequests] = useState([]);
  const [limits, setLimits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showNewAccountModal, setShowNewAccountModal] = useState(false);
  const [newAccount, setNewAccount] = useState({
    accountType: 'CHECKING',
    initialDeposit: 0,
    currency: 'USD',
    notes: ''
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [accountsData, requestsData, limitsData] = await Promise.all([
        accountService.getAccounts(),
        accountService.getMyRequests(),
        transactionService.getLimits()
      ]);
      setAccounts(accountsData);
      setRequests(requestsData);
      setLimits(limitsData);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  const handleRequestAccount = async (e) => {
    e.preventDefault();
    try {
      await accountService.requestAccount(
        newAccount.accountType,
        newAccount.initialDeposit,
        newAccount.currency,
        newAccount.notes
      );
      setShowNewAccountModal(false);
      setNewAccount({ accountType: 'CHECKING', initialDeposit: 0, currency: 'USD', notes: '' });
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit request');
    }
  };

  const handleCancelRequest = async (requestId) => {
    if (window.confirm('Are you sure you want to cancel this request?')) {
      try {
        await accountService.cancelRequest(requestId);
        fetchData();
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to cancel request');
      }
    }
  };

  const getStatusBadge = (status) => {
    const badges = {
      ACTIVE: { color: 'bg-green-100 text-green-800', icon: CheckCircle },
      PENDING: { color: 'bg-yellow-100 text-yellow-800', icon: Clock },
      APPROVED: { color: 'bg-green-100 text-green-800', icon: CheckCircle },
      REJECTED: { color: 'bg-red-100 text-red-800', icon: XCircle },
      CANCELLED: { color: 'bg-gray-100 text-gray-800', icon: XCircle }
    };
    const badge = badges[status] || badges.PENDING;
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
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">My Accounts</h1>
            <p className="mt-1 text-sm text-gray-500">Manage your bank accounts and view balances</p>
          </div>
          <button
            onClick={() => setShowNewAccountModal(true)}
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            <Plus className="w-4 h-4 mr-2" />
            Open New Account
          </button>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center">
            <AlertCircle className="w-5 h-5 mr-2" />
            {error}
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
              <p className="text-gray-500">No active accounts yet. Open your first account!</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {accounts.map((account) => (
                <div
                  key={account.id}
                  className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow cursor-pointer"
                  onClick={() => navigate(`/accounts/${account.id}`)}
                >
                  <div className="p-6">
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center">
                        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                          <CreditCard className="w-5 h-5 text-blue-600" />
                        </div>
                        <div className="ml-3">
                          <p className="text-sm font-medium text-gray-900">{account.accountType}</p>
                          <p className="text-xs text-gray-500">****{account.accountNumber?.slice(-4)}</p>
                        </div>
                      </div>
                      {getStatusBadge(account.status)}
                    </div>
                    <div className="mt-4">
                      <p className="text-sm text-gray-500">Available Balance</p>
                      <p className="text-2xl font-bold text-gray-900">
                        {formatCurrency(account.availableBalance || account.balance, account.currency)}
                      </p>
                    </div>
                    <div className="mt-4 flex space-x-2">
                      <button
                        onClick={(e) => { e.stopPropagation(); navigate(`/transfer?from=${account.id}`); }}
                        className="flex-1 inline-flex items-center justify-center px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                      >
                        <ArrowUpRight className="w-4 h-4 mr-1" />
                        Send
                      </button>
                      <button
                        onClick={(e) => { e.stopPropagation(); navigate(`/deposit?account=${account.id}`); }}
                        className="flex-1 inline-flex items-center justify-center px-3 py-2 border border-transparent rounded-md text-sm font-medium text-white bg-green-600 hover:bg-green-700"
                      >
                        <ArrowDownLeft className="w-4 h-4 mr-1" />
                        Deposit
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Pending Requests */}
        {requests.length > 0 && (
          <div>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Account Requests</h2>
            <div className="bg-white rounded-xl shadow-sm overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Initial Deposit</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {requests.map((request) => (
                    <tr key={request.id}>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="text-sm font-medium text-gray-900">{request.accountType}</span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="text-sm text-gray-900">{formatCurrency(request.initialDeposit)}</span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getStatusBadge(request.status)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(request.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {request.status === 'PENDING' && (
                          <button
                            onClick={() => handleCancelRequest(request.id)}
                            className="text-red-600 hover:text-red-800 text-sm font-medium"
                          >
                            Cancel
                          </button>
                        )}
                        {request.adminNotes && (
                          <span className="text-sm text-gray-500 ml-2" title={request.adminNotes}>
                            <Eye className="w-4 h-4 inline" />
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* New Account Modal */}
        {showNewAccountModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Open New Account</h3>
              <form onSubmit={handleRequestAccount}>
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
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Notes (Optional)</label>
                    <textarea
                      value={newAccount.notes}
                      onChange={(e) => setNewAccount({ ...newAccount, notes: e.target.value })}
                      className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      rows="3"
                      placeholder="Any additional information..."
                    />
                  </div>
                </div>
                <div className="mt-6 flex space-x-3">
                  <button
                    type="button"
                    onClick={() => setShowNewAccountModal(false)}
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="flex-1 px-4 py-2 border border-transparent rounded-md text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
                  >
                    Submit Request
                  </button>
                </div>
              </form>
              <p className="mt-4 text-xs text-gray-500 text-center">
                Your request will be reviewed by our team. This usually takes 1-2 business days.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Accounts;
