import React, { useState, useEffect } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { transactionService, accountService } from '../services/bankService';
import { 
  ArrowUpRight, 
  ArrowDownLeft, 
  ArrowLeftRight,
  Search,
  Filter,
  Download,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Eye
} from 'lucide-react';

const Transactions = () => {
  const { accountId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [ledgerEntries, setLedgerEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewMode, setViewMode] = useState('transactions'); // 'transactions' or 'ledger'
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  
  const [pagination, setPagination] = useState({
    page: 0,
    size: 20,
    totalPages: 0,
    totalElements: 0
  });

  const [filters, setFilters] = useState({
    accountId: accountId || searchParams.get('accountId') || '',
    startDate: '',
    endDate: '',
    type: '',
    status: '',
    search: ''
  });

  useEffect(() => {
    fetchAccounts();
  }, []);

  useEffect(() => {
    if (filters.accountId) {
      fetchTransactions();
    }
  }, [filters.accountId, pagination.page, viewMode]);

  const fetchAccounts = async () => {
    try {
      const data = await accountService.getAccounts();
      setAccounts(data);
      if (!filters.accountId && data.length > 0) {
        setFilters(prev => ({ ...prev, accountId: data[0].id }));
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load accounts');
    }
  };

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      if (viewMode === 'ledger') {
        const data = await transactionService.getLedgerEntries(
          filters.accountId,
          pagination.page,
          pagination.size
        );
        setLedgerEntries(data.content || []);
        setPagination(prev => ({
          ...prev,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0
        }));
      } else {
        const data = await transactionService.getTransactions(
          filters.accountId,
          pagination.page,
          pagination.size
        );
        setTransactions(data.content || []);
        setPagination(prev => ({
          ...prev,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0
        }));
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount, currency = 'USD') => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(amount || 0);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getTransactionIcon = (type) => {
    switch (type?.toUpperCase()) {
      case 'DEPOSIT':
        return <ArrowDownLeft className="w-5 h-5 text-green-500" />;
      case 'WITHDRAWAL':
        return <ArrowUpRight className="w-5 h-5 text-red-500" />;
      case 'TRANSFER':
        return <ArrowLeftRight className="w-5 h-5 text-blue-500" />;
      default:
        return <Clock className="w-5 h-5 text-gray-500" />;
    }
  };

  const getStatusBadge = (status) => {
    const badges = {
      COMPLETED: { color: 'bg-green-100 text-green-800', icon: CheckCircle },
      PENDING: { color: 'bg-yellow-100 text-yellow-800', icon: Clock },
      FAILED: { color: 'bg-red-100 text-red-800', icon: XCircle },
      CANCELLED: { color: 'bg-gray-100 text-gray-800', icon: XCircle }
    };
    const badge = badges[status] || badges.PENDING;
    const Icon = badge.icon;
    return (
      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${badge.color}`}>
        <Icon className="w-3 h-3 mr-1" />
        {status}
      </span>
    );
  };

  const getLedgerEntryBadge = (entryType) => {
    return entryType === 'CREDIT' ? (
      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">
        <ArrowDownLeft className="w-3 h-3 mr-1" />
        CREDIT
      </span>
    ) : (
      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-800">
        <ArrowUpRight className="w-3 h-3 mr-1" />
        DEBIT
      </span>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Transactions</h1>
            <p className="mt-1 text-sm text-gray-500">View your transaction history and ledger</p>
          </div>
          <button className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">
            <Download className="w-4 h-4 mr-2" />
            Export
          </button>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center">
            <AlertCircle className="w-5 h-5 mr-2" />
            {error}
          </div>
        )}

        {/* Filters */}
        <div className="bg-white rounded-xl shadow-sm p-4 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Account</label>
              <select
                value={filters.accountId}
                onChange={(e) => setFilters({ ...filters, accountId: e.target.value })}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Select Account</option>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.accountType} - ****{account.accountNumber?.slice(-4)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">View Mode</label>
              <select
                value={viewMode}
                onChange={(e) => { setViewMode(e.target.value); setPagination(p => ({ ...p, page: 0 })); }}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="transactions">Transactions</option>
                <option value="ledger">Ledger Entries</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
              <input
                type="date"
                value={filters.startDate}
                onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
              <input
                type="date"
                value={filters.endDate}
                onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
        </div>

        {/* Transactions/Ledger Table */}
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          ) : viewMode === 'ledger' ? (
            /* Ledger Entries View */
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Reference</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amount</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Balance</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {ledgerEntries.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-12 text-center text-gray-500">
                      No ledger entries found
                    </td>
                  </tr>
                ) : (
                  ledgerEntries.map((entry) => (
                    <tr key={entry.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {formatDate(entry.createdAt)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getLedgerEntryBadge(entry.entryType)}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900">
                        {entry.description || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono">
                        {entry.referenceId || '-'}
                      </td>
                      <td className={`px-6 py-4 whitespace-nowrap text-sm font-medium text-right ${
                        entry.entryType === 'CREDIT' ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {entry.entryType === 'CREDIT' ? '+' : '-'}{formatCurrency(entry.amount)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-right text-gray-900">
                        {formatCurrency(entry.runningBalance)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          ) : (
            /* Transactions View */
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amount</th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {transactions.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-12 text-center text-gray-500">
                      No transactions found
                    </td>
                  </tr>
                ) : (
                  transactions.map((txn) => (
                    <tr key={txn.transactionId || txn.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {formatDate(txn.createdAt)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          {getTransactionIcon(txn.transactionType)}
                          <span className="ml-2 text-sm font-medium text-gray-900">
                            {txn.transactionType}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900">
                        {txn.description || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getStatusBadge(txn.status)}
                      </td>
                      <td className={`px-6 py-4 whitespace-nowrap text-sm font-medium text-right ${
                        txn.transactionType === 'DEPOSIT' ? 'text-green-600' : 
                        txn.transactionType === 'WITHDRAWAL' ? 'text-red-600' : 'text-gray-900'
                      }`}>
                        {txn.transactionType === 'DEPOSIT' ? '+' : 
                         txn.transactionType === 'WITHDRAWAL' ? '-' : ''}
                        {formatCurrency(txn.amount)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-center">
                        <button
                          onClick={() => setSelectedTransaction(txn)}
                          className="text-blue-600 hover:text-blue-800"
                        >
                          <Eye className="w-5 h-5" />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}

          {/* Pagination */}
          {pagination.totalPages > 1 && (
            <div className="bg-white px-4 py-3 border-t border-gray-200 flex items-center justify-between">
              <div className="text-sm text-gray-500">
                Showing {pagination.page * pagination.size + 1} to{' '}
                {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of{' '}
                {pagination.totalElements} entries
              </div>
              <div className="flex space-x-2">
                <button
                  onClick={() => setPagination(p => ({ ...p, page: p.page - 1 }))}
                  disabled={pagination.page === 0}
                  className="px-3 py-1 border border-gray-300 rounded-md text-sm disabled:opacity-50"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <span className="px-3 py-1 text-sm">
                  Page {pagination.page + 1} of {pagination.totalPages}
                </span>
                <button
                  onClick={() => setPagination(p => ({ ...p, page: p.page + 1 }))}
                  disabled={pagination.page >= pagination.totalPages - 1}
                  className="px-3 py-1 border border-gray-300 rounded-md text-sm disabled:opacity-50"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Transaction Detail Modal */}
      {selectedTransaction && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Transaction Details</h3>
              <button
                onClick={() => setSelectedTransaction(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                <XCircle className="w-6 h-6" />
              </button>
            </div>
            <div className="space-y-4">
              <div className="flex justify-between">
                <span className="text-gray-500">Type</span>
                <span className="font-medium">{selectedTransaction.transactionType}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Amount</span>
                <span className="font-medium">{formatCurrency(selectedTransaction.amount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Status</span>
                {getStatusBadge(selectedTransaction.status)}
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Date</span>
                <span className="font-medium">{formatDate(selectedTransaction.createdAt)}</span>
              </div>
              {selectedTransaction.description && (
                <div className="flex justify-between">
                  <span className="text-gray-500">Description</span>
                  <span className="font-medium">{selectedTransaction.description}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-gray-500">Transaction ID</span>
                <span className="font-mono text-xs">{selectedTransaction.transactionId || selectedTransaction.id}</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Transactions;
