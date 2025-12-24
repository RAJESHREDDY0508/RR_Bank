import { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import { accountService, transactionService } from '../services/bankService';
import { 
  ArrowRight, 
  Loader, 
  AlertCircle, 
  CheckCircle,
  CreditCard
} from 'lucide-react';

const Transfer = () => {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [transferLoading, setTransferLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  
  const [formData, setFormData] = useState({
    fromAccountNumber: '',
    toAccountNumber: '',
    amount: '',
    description: '',
    currency: 'USD'
  });

  useEffect(() => {
    fetchAccounts();
  }, []);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      const data = await accountService.getAccounts();
      setAccounts(data);
      if (data.length > 0) {
        setFormData(prev => ({ ...prev, fromAccountNumber: data[0].accountNumber }));
      }
    } catch (err) {
      setError('Failed to load accounts');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
    setError('');
    setSuccess(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);

    // Validation
    const amount = parseFloat(formData.amount);
    if (isNaN(amount) || amount <= 0) {
      setError('Please enter a valid amount');
      return;
    }

    if (formData.fromAccountNumber === formData.toAccountNumber) {
      setError('Source and destination accounts must be different');
      return;
    }

    const fromAccount = accounts.find(acc => acc.accountNumber === formData.fromAccountNumber);
    if (fromAccount && parseFloat(fromAccount.balance) < amount) {
      setError('Insufficient funds in source account');
      return;
    }

    setTransferLoading(true);

    try {
      await transactionService.transfer(formData);
      setSuccess(true);
      
      // Reset form
      setFormData({
        fromAccountNumber: accounts[0]?.accountNumber || '',
        toAccountNumber: '',
        amount: '',
        description: '',
        currency: 'USD'
      });

      // Refresh accounts
      await fetchAccounts();

      // Clear success message after 5 seconds
      setTimeout(() => {
        setSuccess(false);
      }, 5000);
    } catch (err) {
      setError(err.response?.data?.message || 'Transfer failed. Please try again.');
    } finally {
      setTransferLoading(false);
    }
  };

  const getAccountBalance = (accountNumber) => {
    const account = accounts.find(acc => acc.accountNumber === accountNumber);
    return account ? parseFloat(account.balance || 0) : 0;
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
      
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Transfer Funds</h1>
          <p className="text-gray-600 mt-1">Send money to another account securely.</p>
        </div>

        {/* Transfer Form */}
        <div className="card">
          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start">
              <AlertCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
              <span className="text-sm">{error}</span>
            </div>
          )}

          {success && (
            <div className="mb-6 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-start">
              <CheckCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
              <span className="text-sm">Transfer completed successfully!</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* From Account */}
            <div>
              <label htmlFor="fromAccountNumber" className="block text-sm font-medium text-gray-700 mb-2">
                From Account
              </label>
              <select
                id="fromAccountNumber"
                name="fromAccountNumber"
                value={formData.fromAccountNumber}
                onChange={handleChange}
                className="input-field"
                required
              >
                <option value="">Select source account</option>
                {accounts.map((account) => (
                  <option key={account.accountNumber} value={account.accountNumber}>
                    {account.accountType} - ****{account.accountNumber.slice(-4)} 
                    (${parseFloat(account.balance || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })})
                  </option>
                ))}
              </select>
              {formData.fromAccountNumber && (
                <p className="mt-2 text-sm text-gray-600">
                  Available Balance: ${getAccountBalance(formData.fromAccountNumber).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </p>
              )}
            </div>

            {/* Arrow Indicator */}
            <div className="flex justify-center">
              <div className="bg-primary-100 p-3 rounded-full">
                <ArrowRight className="h-6 w-6 text-primary-600" />
              </div>
            </div>

            {/* To Account */}
            <div>
              <label htmlFor="toAccountNumber" className="block text-sm font-medium text-gray-700 mb-2">
                To Account Number
              </label>
              <input
                id="toAccountNumber"
                name="toAccountNumber"
                type="text"
                value={formData.toAccountNumber}
                onChange={handleChange}
                className="input-field"
                placeholder="Enter destination account number"
                required
              />
              <p className="mt-2 text-sm text-gray-600">
                Enter the complete account number of the recipient
              </p>
            </div>

            {/* Amount */}
            <div>
              <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-2">
                Amount
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <span className="text-gray-500 sm:text-sm">$</span>
                </div>
                <input
                  id="amount"
                  name="amount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={formData.amount}
                  onChange={handleChange}
                  className="input-field pl-7"
                  placeholder="0.00"
                  required
                />
              </div>
            </div>

            {/* Description */}
            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
                Description (Optional)
              </label>
              <textarea
                id="description"
                name="description"
                rows="3"
                value={formData.description}
                onChange={handleChange}
                className="input-field resize-none"
                placeholder="Add a note about this transfer..."
                maxLength="255"
              />
              <p className="mt-2 text-sm text-gray-600">
                {formData.description.length}/255 characters
              </p>
            </div>

            {/* Submit Button */}
            <div className="flex space-x-3 pt-4">
              <button
                type="button"
                onClick={() => {
                  setFormData({
                    fromAccountNumber: accounts[0]?.accountNumber || '',
                    toAccountNumber: '',
                    amount: '',
                    description: '',
                    currency: 'USD'
                  });
                  setError('');
                  setSuccess(false);
                }}
                className="flex-1 btn-secondary"
                disabled={transferLoading}
              >
                Clear
              </button>
              <button
                type="submit"
                disabled={transferLoading}
                className="flex-1 btn-primary"
              >
                {transferLoading ? (
                  <>
                    <Loader className="animate-spin h-5 w-5 mr-2 inline" />
                    Processing Transfer...
                  </>
                ) : (
                  'Transfer Funds'
                )}
              </button>
            </div>
          </form>
        </div>

        {/* Recent Transfers Info */}
        <div className="mt-8 card bg-blue-50 border-blue-200">
          <div className="flex items-start space-x-3">
            <CreditCard className="h-6 w-6 text-blue-600 flex-shrink-0 mt-1" />
            <div>
              <h3 className="font-semibold text-blue-900 mb-1">Transfer Information</h3>
              <ul className="text-sm text-blue-800 space-y-1">
                <li>• Transfers are processed instantly within RR Bank</li>
                <li>• You can transfer to any valid account number</li>
                <li>• All transfers are securely encrypted</li>
                <li>• View your transfer history in the Transactions page</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Transfer;
