import { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import { accountService } from '../services/bankService';
import { 
  CreditCard, 
  Loader, 
  AlertCircle, 
  Plus, 
  Eye, 
  EyeOff,
  ArrowDownLeft,
  ArrowUpRight,
  CheckCircle
} from 'lucide-react';

const Accounts = () => {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showBalance, setShowBalance] = useState({});
  const [actionLoading, setActionLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  
  // Deposit/Withdraw state
  const [activeAction, setActiveAction] = useState(null);
  const [amount, setAmount] = useState('');
  const [selectedAccount, setSelectedAccount] = useState(null);

  useEffect(() => {
    fetchAccounts();
  }, []);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      const data = await accountService.getAccounts();
      setAccounts(data);
      // Initialize balance visibility
      const visibility = {};
      data.forEach(acc => visibility[acc.accountNumber] = true);
      setShowBalance(visibility);
    } catch (err) {
      setError('Failed to load accounts');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const toggleBalance = (accountNumber) => {
    setShowBalance(prev => ({
      ...prev,
      [accountNumber]: !prev[accountNumber]
    }));
  };

  const handleAction = (action, account) => {
    setActiveAction(action);
    setSelectedAccount(account);
    setAmount('');
    setError('');
    setSuccessMessage('');
  };

  const handleSubmitAction = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');

    const amountNum = parseFloat(amount);
    if (isNaN(amountNum) || amountNum <= 0) {
      setError('Please enter a valid amount');
      return;
    }

    setActionLoading(true);

    try {
      if (activeAction === 'deposit') {
        await accountService.deposit(selectedAccount.accountNumber, amountNum);
        setSuccessMessage(`Successfully deposited $${amountNum.toFixed(2)}`);
      } else if (activeAction === 'withdraw') {
        await accountService.withdraw(selectedAccount.accountNumber, amountNum);
        setSuccessMessage(`Successfully withdrew $${amountNum.toFixed(2)}`);
      }
      
      // Refresh accounts
      await fetchAccounts();
      
      // Reset form
      setTimeout(() => {
        setActiveAction(null);
        setSelectedAccount(null);
        setAmount('');
        setSuccessMessage('');
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Transaction failed');
    } finally {
      setActionLoading(false);
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
          <h1 className="text-3xl font-bold text-gray-900">Your Accounts</h1>
          <p className="text-gray-600 mt-1">Manage and monitor your bank accounts.</p>
        </div>

        {error && !activeAction && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start">
            <AlertCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
            <span className="text-sm">{error}</span>
          </div>
        )}

        {/* Accounts Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {accounts.map((account) => (
            <div key={account.accountNumber} className="card hover:shadow-lg transition-shadow">
              {/* Account Header */}
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center space-x-3">
                  <div className="bg-primary-100 p-3 rounded-full">
                    <CreditCard className="h-6 w-6 text-primary-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">{account.accountType}</h3>
                    <p className="text-sm text-gray-600">****{account.accountNumber.slice(-4)}</p>
                  </div>
                </div>
                <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                  account.status === 'ACTIVE' 
                    ? 'bg-green-100 text-green-700' 
                    : 'bg-gray-100 text-gray-700'
                }`}>
                  {account.status}
                </span>
              </div>

              {/* Balance */}
              <div className="mb-6">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-gray-600">Available Balance</span>
                  <button
                    onClick={() => toggleBalance(account.accountNumber)}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    {showBalance[account.accountNumber] ? (
                      <Eye className="h-4 w-4" />
                    ) : (
                      <EyeOff className="h-4 w-4" />
                    )}
                  </button>
                </div>
                <p className="text-3xl font-bold text-gray-900">
                  {showBalance[account.accountNumber] 
                    ? `$${parseFloat(account.balance || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                    : '••••••'
                  }
                </p>
              </div>

              {/* Account Details */}
              <div className="border-t border-gray-200 pt-4 mb-4">
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-gray-600">Account Number</p>
                    <p className="font-medium text-gray-900">{account.accountNumber}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Currency</p>
                    <p className="font-medium text-gray-900">{account.currency || 'USD'}</p>
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex space-x-3">
                <button
                  onClick={() => handleAction('deposit', account)}
                  className="flex-1 inline-flex items-center justify-center px-4 py-2 bg-green-600 hover:bg-green-700 text-white text-sm font-medium rounded-lg transition-colors"
                >
                  <ArrowDownLeft className="h-4 w-4 mr-2" />
                  Deposit
                </button>
                <button
                  onClick={() => handleAction('withdraw', account)}
                  className="flex-1 inline-flex items-center justify-center px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-lg transition-colors"
                >
                  <ArrowUpRight className="h-4 w-4 mr-2" />
                  Withdraw
                </button>
              </div>
            </div>
          ))}

          {/* Add New Account Card */}
          <div className="card border-2 border-dashed border-gray-300 hover:border-primary-400 transition-colors cursor-pointer flex items-center justify-center min-h-[300px]">
            <div className="text-center">
              <div className="bg-gray-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Plus className="h-8 w-8 text-gray-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Open New Account</h3>
              <p className="text-sm text-gray-600">Start managing your finances better</p>
            </div>
          </div>
        </div>

        {/* Action Modal */}
        {activeAction && selectedAccount && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-xl max-w-md w-full p-6">
              <h3 className="text-xl font-bold text-gray-900 mb-4">
                {activeAction === 'deposit' ? 'Deposit Funds' : 'Withdraw Funds'}
              </h3>

              {error && (
                <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start">
                  <AlertCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
                  <span className="text-sm">{error}</span>
                </div>
              )}

              {successMessage && (
                <div className="mb-4 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-start">
                  <CheckCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
                  <span className="text-sm">{successMessage}</span>
                </div>
              )}

              <div className="mb-4 p-4 bg-gray-50 rounded-lg">
                <p className="text-sm text-gray-600">Account</p>
                <p className="font-medium text-gray-900">{selectedAccount.accountType} - ****{selectedAccount.accountNumber.slice(-4)}</p>
                <p className="text-sm text-gray-600 mt-2">Current Balance</p>
                <p className="text-lg font-bold text-gray-900">${parseFloat(selectedAccount.balance || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
              </div>

              <form onSubmit={handleSubmitAction}>
                <div className="mb-4">
                  <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-2">
                    Amount
                  </label>
                  <input
                    id="amount"
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="input-field"
                    placeholder="0.00"
                    required
                  />
                </div>

                <div className="flex space-x-3">
                  <button
                    type="button"
                    onClick={() => {
                      setActiveAction(null);
                      setSelectedAccount(null);
                      setAmount('');
                      setError('');
                      setSuccessMessage('');
                    }}
                    className="flex-1 btn-secondary"
                    disabled={actionLoading}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={actionLoading}
                    className="flex-1 btn-primary"
                  >
                    {actionLoading ? (
                      <>
                        <Loader className="animate-spin h-5 w-5 mr-2 inline" />
                        Processing...
                      </>
                    ) : (
                      `${activeAction === 'deposit' ? 'Deposit' : 'Withdraw'} Funds`
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Accounts;
