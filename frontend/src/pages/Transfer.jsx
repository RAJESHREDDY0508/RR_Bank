import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { accountService, transactionService } from '../services/bankService';
import { useAuth } from '../context/AuthContext';
import Navbar from '../components/Navbar';
import KycBanner from '../components/KycBanner';
import { 
  ArrowUpRight, 
  ArrowDownLeft, 
  ArrowLeftRight,
  AlertCircle,
  CheckCircle,
  Wallet,
  Info,
  Copy,
  Check,
  RefreshCw,
  ShieldAlert
} from 'lucide-react';

const Transfer = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { canTransact, kycStatus, refreshKycStatus } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [limits, setLimits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [copiedAccountNumber, setCopiedAccountNumber] = useState(null);
  
  const urlPath = window.location.pathname;
  const initialType = urlPath.includes('deposit') ? 'deposit' : 'transfer';
  const [transactionType, setTransactionType] = useState(initialType);
  
  const [formData, setFormData] = useState({
    fromAccountId: searchParams.get('from') || searchParams.get('account') || '',
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
      setError(null);
      
      const [accountsData, limitsData] = await Promise.all([
        accountService.getAccounts().catch(() => []),
        transactionService.getLimits().catch(() => [])
      ]);
      
      const activeAccounts = (accountsData || []).filter(a => a.status === 'ACTIVE');
      setAccounts(activeAccounts);
      setLimits(limitsData || []);
      
      const fromParam = searchParams.get('from') || searchParams.get('account');
      if (fromParam) {
        setFormData(prev => ({ ...prev, fromAccountId: fromParam }));
      } else if (activeAccounts.length > 0 && !formData.fromAccountId) {
        setFormData(prev => ({ ...prev, fromAccountId: activeAccounts[0].id }));
      }
    } catch (err) {
      console.error('Error fetching data:', err);
      setError(err.response?.data?.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const refreshAccounts = async () => {
    try {
      const accountsData = await accountService.getAccounts().catch(() => []);
      const activeAccounts = (accountsData || []).filter(a => a.status === 'ACTIVE');
      setAccounts(activeAccounts);
    } catch (err) {
      console.error('Error refreshing accounts:', err);
    }
  };

  const getSelectedAccount = () => accounts.find(a => a.id === formData.fromAccountId);

  const getApplicableLimit = () => {
    const type = transactionType.toUpperCase();
    return limits.find(l => l.limitType === type || l.limitType === 'ALL');
  };

  const copyAccountNumber = (accountNumber) => {
    navigator.clipboard.writeText(accountNumber);
    setCopiedAccountNumber(accountNumber);
    setTimeout(() => setCopiedAccountNumber(null), 2000);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!canTransact) {
      setError('Your account is pending KYC verification. Transactions will be enabled once admin approves your KYC.');
      return;
    }
    
    setError(null);
    setSuccess(null);

    const amount = parseFloat(formData.amount);
    if (!amount || amount <= 0) {
      setError('Please enter a valid amount');
      return;
    }

    if (!formData.fromAccountId) {
      setError('Please select an account');
      return;
    }

    const selectedAccount = getSelectedAccount();
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

    if ((transactionType === 'withdraw' || transactionType === 'transfer') && selectedAccount) {
      const availableBalance = parseFloat(selectedAccount.availableBalance || selectedAccount.balance || 0);
      if (amount > availableBalance) {
        setError(`Insufficient funds. Available balance: $${availableBalance.toLocaleString()}`);
        return;
      }
    }

    try {
      setSubmitting(true);

      if (transactionType === 'deposit') {
        await transactionService.deposit(formData.fromAccountId, amount, formData.description || 'Deposit');
        setSuccess(`Successfully deposited $${amount.toLocaleString()}`);
      } else if (transactionType === 'withdraw') {
        await transactionService.withdraw(formData.fromAccountId, amount, formData.description || 'Withdrawal');
        setSuccess(`Successfully withdrew $${amount.toLocaleString()}`);
      } else {
        if (!formData.toAccountId && !formData.toAccountNumber) {
          setError('Please select or enter a destination account');
          setSubmitting(false);
          return;
        }
        
        let targetAccountId = formData.toAccountId;
        
        if (!targetAccountId && formData.toAccountNumber) {
          try {
            const targetAccount = await accountService.getAccountByNumber(formData.toAccountNumber);
            if (targetAccount?.id) {
              targetAccountId = targetAccount.id;
            } else {
              setError('Could not find the destination account');
              setSubmitting(false);
              return;
            }
          } catch (err) {
            setError('Invalid destination account number. Please check and try again.');
            setSubmitting(false);
            return;
          }
        }
        
        await transactionService.transfer(formData.fromAccountId, targetAccountId, amount, formData.description || 'Transfer');
        setSuccess(`Successfully transferred $${amount.toLocaleString()}`);
      }

      setFormData(prev => ({ ...prev, amount: '', description: '', toAccountId: '', toAccountNumber: '' }));
      await refreshAccounts();
      
    } catch (err) {
      console.error('Transaction error:', err);
      const errorData = err.response?.data;
      if (errorData?.error === 'KYC_REQUIRED') {
        setError(errorData.message || 'Your account is pending KYC verification.');
        refreshKycStatus();
      } else {
        setError(errorData?.message || errorData?.error || 'Transaction failed. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const formatCurrency = (amount) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount || 0);

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

  const selectedAccount = getSelectedAccount();
  const applicableLimit = getApplicableLimit();

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="py-8">
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* KYC Banner */}
          <KycBanner />

          <div className="bg-white rounded-xl shadow-sm">
            {/* Transaction Type Tabs */}
            <div className="border-b border-gray-200">
              <nav className="flex -mb-px">
                <button
                  onClick={() => { setTransactionType('transfer'); setError(null); setSuccess(null); }}
                  className={`flex-1 py-4 px-6 text-center border-b-2 font-medium text-sm ${transactionType === 'transfer' ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}`}
                >
                  <ArrowLeftRight className="w-5 h-5 mx-auto mb-1" />
                  Transfer
                </button>
                <button
                  onClick={() => { setTransactionType('deposit'); setError(null); setSuccess(null); }}
                  className={`flex-1 py-4 px-6 text-center border-b-2 font-medium text-sm ${transactionType === 'deposit' ? 'border-green-500 text-green-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}`}
                >
                  <ArrowDownLeft className="w-5 h-5 mx-auto mb-1" />
                  Deposit
                </button>
                <button
                  onClick={() => { setTransactionType('withdraw'); setError(null); setSuccess(null); }}
                  className={`flex-1 py-4 px-6 text-center border-b-2 font-medium text-sm ${transactionType === 'withdraw' ? 'border-red-500 text-red-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}`}
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
                <div className="mb-6 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg">
                  <div className="flex items-center mb-2">
                    <CheckCircle className="w-5 h-5 mr-2 flex-shrink-0" />
                    <span className="font-medium">{success}</span>
                  </div>
                  <p className="text-sm ml-7">Your account balances have been updated.</p>
                </div>
              )}

              {accounts.length === 0 && (
                <div className="mb-6 bg-yellow-50 border border-yellow-200 text-yellow-700 px-4 py-3 rounded-lg flex items-center">
                  <AlertCircle className="w-5 h-5 mr-2 flex-shrink-0" />
                  No active accounts found. Please create an account first.
                </div>
              )}

              {applicableLimit && (
                <div className="mb-6 bg-blue-50 border border-blue-200 text-blue-700 px-4 py-3 rounded-lg">
                  <div className="flex items-center mb-2">
                    <Info className="w-5 h-5 mr-2" />
                    <span className="font-medium">Transaction Limits</span>
                  </div>
                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div><span className="text-blue-500">Per Transaction:</span><br /><span className="font-medium">{formatCurrency(applicableLimit.perTransactionLimit)}</span></div>
                    <div><span className="text-blue-500">Daily Remaining:</span><br /><span className="font-medium">{formatCurrency(applicableLimit.remainingDaily)}</span></div>
                    <div><span className="text-blue-500">Monthly Remaining:</span><br /><span className="font-medium">{formatCurrency(applicableLimit.remainingMonthly)}</span></div>
                  </div>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">{transactionType === 'deposit' ? 'To Account' : 'From Account'}</label>
                  <select
                    value={formData.fromAccountId}
                    onChange={(e) => setFormData({ ...formData, fromAccountId: e.target.value })}
                    className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                    disabled={accounts.length === 0 || !canTransact}
                  >
                    <option value="">Select Account</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.accountType} - {account.accountNumber} ({formatCurrency(account.availableBalance || account.balance)})
                      </option>
                    ))}
                  </select>
                </div>

                {selectedAccount && (
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <Wallet className="w-8 h-8 text-blue-600 mr-3" />
                        <div>
                          <p className="text-sm text-gray-500">Available Balance</p>
                          <p className="text-xl font-bold text-gray-900">{formatCurrency(selectedAccount.availableBalance || selectedAccount.balance)}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-xs text-gray-500 mb-1">Account Number</p>
                        <div className="flex items-center space-x-2">
                          <span className="font-mono text-sm bg-white px-2 py-1 rounded border">{selectedAccount.accountNumber}</span>
                          <button type="button" onClick={() => copyAccountNumber(selectedAccount.accountNumber)} className="text-gray-500 hover:text-gray-700" title="Copy account number">
                            {copiedAccountNumber === selectedAccount.accountNumber ? <Check className="w-4 h-4 text-green-600" /> : <Copy className="w-4 h-4" />}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {transactionType === 'transfer' && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">To Account</label>
                    <select
                      value={formData.toAccountId}
                      onChange={(e) => setFormData({ ...formData, toAccountId: e.target.value, toAccountNumber: '' })}
                      className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 mb-2"
                      disabled={!canTransact}
                    >
                      <option value="">Select from your accounts</option>
                      {accounts.filter(a => a.id !== formData.fromAccountId).map((account) => (
                        <option key={account.id} value={account.id}>{account.accountType} - {account.accountNumber} ({formatCurrency(account.availableBalance || account.balance)})</option>
                      ))}
                    </select>
                    <div className="relative my-4">
                      <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-gray-300"></div></div>
                      <div className="relative flex justify-center text-sm"><span className="px-2 bg-white text-gray-500">Or enter account number</span></div>
                    </div>
                    <div>
                      <input
                        type="text"
                        value={formData.toAccountNumber}
                        onChange={(e) => setFormData({ ...formData, toAccountNumber: e.target.value, toAccountId: '' })}
                        className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono"
                        placeholder="e.g., CH12-3456-7890"
                        disabled={!canTransact}
                      />
                      <p className="mt-1 text-xs text-gray-500">Enter the recipient's account number (format: XX##-####-####)</p>
                    </div>
                  </div>
                )}

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Amount</label>
                  <div className="relative">
                    <span className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-500">$</span>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      max={transactionType === 'withdraw' ? (selectedAccount?.availableBalance || selectedAccount?.balance) : undefined}
                      value={formData.amount}
                      onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                      className="w-full border border-gray-300 rounded-lg pl-8 pr-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 text-lg"
                      placeholder="0.00"
                      required
                      disabled={accounts.length === 0 || !canTransact}
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Description (Optional)</label>
                  <input
                    type="text"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="e.g., Rent payment, Savings transfer"
                    disabled={accounts.length === 0 || !canTransact}
                  />
                </div>

                <button
                  type="submit"
                  disabled={submitting || accounts.length === 0 || !canTransact}
                  className={`w-full py-3 px-4 rounded-lg font-medium text-white transition-colors ${
                    transactionType === 'deposit' ? 'bg-green-600 hover:bg-green-700' :
                    transactionType === 'withdraw' ? 'bg-red-600 hover:bg-red-700' : 'bg-blue-600 hover:bg-blue-700'
                  } ${(submitting || accounts.length === 0 || !canTransact) ? 'opacity-50 cursor-not-allowed' : ''}`}
                >
                  {submitting ? (
                    <span className="flex items-center justify-center"><RefreshCw className="animate-spin h-5 w-5 mr-2" />Processing...</span>
                  ) : !canTransact ? (
                    <span className="flex items-center justify-center"><ShieldAlert className="w-5 h-5 mr-2" />KYC Verification Required</span>
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

          {transactionType === 'transfer' && accounts.length > 0 && (
            <div className="mt-6 bg-white rounded-xl shadow-sm p-4">
              <h3 className="text-sm font-medium text-gray-700 mb-3">Your Account Numbers (for sharing)</h3>
              <div className="space-y-2">
                {accounts.map((account) => (
                  <div key={account.id} className="flex items-center justify-between bg-gray-50 rounded-lg px-3 py-2">
                    <div className="flex items-center space-x-3">
                      <span className="text-sm text-gray-600">{account.accountType}</span>
                      <span className="font-mono text-sm">{account.accountNumber}</span>
                      <span className="text-sm text-gray-500">({formatCurrency(account.availableBalance || account.balance)})</span>
                    </div>
                    <button type="button" onClick={() => copyAccountNumber(account.accountNumber)} className="text-blue-600 hover:text-blue-800 text-sm flex items-center">
                      {copiedAccountNumber === account.accountNumber ? (<><Check className="w-4 h-4 mr-1" />Copied!</>) : (<><Copy className="w-4 h-4 mr-1" />Copy</>)}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Transfer;
