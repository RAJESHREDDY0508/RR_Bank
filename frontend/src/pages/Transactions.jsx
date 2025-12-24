import { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import { accountService, transactionService } from '../services/bankService';
import { 
  Loader, 
  AlertCircle,
  ArrowUpRight,
  ArrowDownLeft,
  Search,
  Filter,
  Download
} from 'lucide-react';
import { format } from 'date-fns';

const Transactions = () => {
  const [accounts, setAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [transactionsLoading, setTransactionsLoading] = useState(false);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('ALL');

  useEffect(() => {
    fetchAccounts();
  }, []);

  useEffect(() => {
    if (selectedAccount) {
      fetchTransactions();
    }
  }, [selectedAccount]);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      const data = await accountService.getAccounts();
      setAccounts(data);
      if (data.length > 0) {
        setSelectedAccount(data[0].accountNumber);
      }
    } catch (err) {
      setError('Failed to load accounts');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchTransactions = async () => {
    try {
      setTransactionsLoading(true);
      const data = await transactionService.getTransactions(selectedAccount, 0, 50);
      setTransactions(data.content || []);
    } catch (err) {
      setError('Failed to load transactions');
      console.error(err);
    } finally {
      setTransactionsLoading(false);
    }
  };

  const getTransactionIcon = (type) => {
    switch (type) {
      case 'DEPOSIT':
        return <ArrowDownLeft className="h-5 w-5" />;
      case 'WITHDRAWAL':
        return <ArrowUpRight className="h-5 w-5" />;
      case 'TRANSFER':
        return <ArrowUpRight className="h-5 w-5" />;
      default:
        return <ArrowUpRight className="h-5 w-5" />;
    }
  };

  const getTransactionColor = (type) => {
    switch (type) {
      case 'DEPOSIT':
        return 'text-green-600 bg-green-100';
      case 'WITHDRAWAL':
        return 'text-red-600 bg-red-100';
      case 'TRANSFER':
        return 'text-blue-600 bg-blue-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const getAmountColor = (type) => {
    switch (type) {
      case 'DEPOSIT':
        return 'text-green-600';
      case 'WITHDRAWAL':
        return 'text-red-600';
      case 'TRANSFER':
        return 'text-blue-600';
      default:
        return 'text-gray-900';
    }
  };

  const filteredTransactions = transactions.filter(transaction => {
    const matchesSearch = transaction.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         transaction.transactionType.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filterType === 'ALL' || transaction.transactionType === filterType;
    return matchesSearch && matchesFilter;
  });

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
          <h1 className="text-3xl font-bold text-gray-900">Transaction History</h1>
          <p className="text-gray-600 mt-1">View and manage your account transactions.</p>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start">
            <AlertCircle className="h-5 w-5 mr-2 flex-shrink-0 mt-0.5" />
            <span className="text-sm">{error}</span>
          </div>
        )}

        {/* Filters */}
        <div className="card mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Account Selector */}
            <div>
              <label htmlFor="account" className="block text-sm font-medium text-gray-700 mb-2">
                Select Account
              </label>
              <select
                id="account"
                value={selectedAccount}
                onChange={(e) => setSelectedAccount(e.target.value)}
                className="input-field"
              >
                {accounts.map((account) => (
                  <option key={account.accountNumber} value={account.accountNumber}>
                    {account.accountType} - ****{account.accountNumber.slice(-4)}
                  </option>
                ))}
              </select>
            </div>

            {/* Search */}
            <div>
              <label htmlFor="search" className="block text-sm font-medium text-gray-700 mb-2">
                Search Transactions
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Search className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  id="search"
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="input-field pl-10"
                  placeholder="Search by description..."
                />
              </div>
            </div>

            {/* Filter by Type */}
            <div>
              <label htmlFor="filter" className="block text-sm font-medium text-gray-700 mb-2">
                Filter by Type
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Filter className="h-5 w-5 text-gray-400" />
                </div>
                <select
                  id="filter"
                  value={filterType}
                  onChange={(e) => setFilterType(e.target.value)}
                  className="input-field pl-10"
                >
                  <option value="ALL">All Types</option>
                  <option value="DEPOSIT">Deposits</option>
                  <option value="WITHDRAWAL">Withdrawals</option>
                  <option value="TRANSFER">Transfers</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        {/* Transactions List */}
        <div className="card">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-gray-900">
              Transactions ({filteredTransactions.length})
            </h2>
            <button className="inline-flex items-center px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors">
              <Download className="h-4 w-4 mr-2" />
              Export
            </button>
          </div>

          {transactionsLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader className="h-8 w-8 animate-spin text-primary-600" />
            </div>
          ) : filteredTransactions.length > 0 ? (
            <div className="space-y-3">
              {filteredTransactions.map((transaction) => (
                <div
                  key={transaction.transactionId}
                  className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                >
                  <div className="flex items-center space-x-4 flex-1">
                    <div className={`p-3 rounded-full ${getTransactionColor(transaction.transactionType)}`}>
                      {getTransactionIcon(transaction.transactionType)}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="font-medium text-gray-900">
                            {transaction.transactionType}
                          </p>
                          <p className="text-sm text-gray-600">
                            {transaction.description || 'No description'}
                          </p>
                        </div>
                        <div className="text-right ml-4">
                          <p className={`font-semibold ${getAmountColor(transaction.transactionType)}`}>
                            {transaction.transactionType === 'DEPOSIT' ? '+' : '-'}
                            ${parseFloat(transaction.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                          </p>
                          <p className="text-sm text-gray-600">
                            {format(new Date(transaction.timestamp), 'MMM dd, yyyy HH:mm')}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center mt-2 space-x-4 text-xs text-gray-500">
                        <span>ID: {transaction.transactionId}</span>
                        <span className={`px-2 py-1 rounded-full ${
                          transaction.status === 'COMPLETED' 
                            ? 'bg-green-100 text-green-700' 
                            : transaction.status === 'PENDING'
                            ? 'bg-yellow-100 text-yellow-700'
                            : 'bg-red-100 text-red-700'
                        }`}>
                          {transaction.status}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-12">
              <ArrowUpRight className="h-16 w-16 mx-auto text-gray-300 mb-4" />
              <p className="text-gray-500 text-lg font-medium mb-2">No transactions found</p>
              <p className="text-gray-400 text-sm">
                {searchTerm || filterType !== 'ALL' 
                  ? 'Try adjusting your search or filter' 
                  : 'Start making transactions to see them here'}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Transactions;
