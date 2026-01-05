import React, { useState, useEffect } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { transactionService, accountService } from '../services/bankService';
import Navbar from '../components/Navbar';
import { 
  ArrowUpRight, 
  ArrowDownLeft, 
  ArrowLeftRight,
  Download,
  ChevronLeft,
  ChevronRight,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Eye,
  Search,
  RefreshCw
} from 'lucide-react';

const Transactions = () => {
  const { accountId: routeAccountId } = useParams();
  const [searchParams] = useSearchParams();
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState(null);
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  
  const [pagination, setPagination] = useState({
    page: 0,
    size: 20,
    totalPages: 0,
    totalElements: 0
  });

  const [filters, setFilters] = useState({
    accountId: routeAccountId || searchParams.get('accountId') || '',
    startDate: '',
    endDate: '',
    type: ''
  });

  useEffect(() => {
    fetchAccounts();
  }, []);

  useEffect(() => {
    if (filters.accountId) {
      fetchTransactions();
    }
  }, [filters.accountId, pagination.page]);

  const fetchAccounts = async () => {
    try {
      const data = await accountService.getAccounts();
      setAccounts(data || []);
      if (!filters.accountId && data && data.length > 0) {
        setFilters(prev => ({ ...prev, accountId: data[0].id }));
      } else {
        setLoading(false);
      }
    } catch (err) {
      console.error('Error fetching accounts:', err);
      setError(err.response?.data?.message || 'Failed to load accounts');
      setLoading(false);
    }
  };

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const filterParams = {};
      if (filters.startDate) filterParams.startDate = filters.startDate;
      if (filters.endDate) filterParams.endDate = filters.endDate;
      if (filters.type) filterParams.type = filters.type;
      
      const data = await transactionService.getTransactions(
        filters.accountId,
        pagination.page,
        pagination.size,
        filterParams
      );
      
      // Handle both paginated and array responses
      if (data && typeof data === 'object') {
        if (Array.isArray(data)) {
          setTransactions(data);
          setPagination(prev => ({
            ...prev,
            totalPages: 1,
            totalElements: data.length
          }));
        } else {
          setTransactions(data.content || []);
          setPagination(prev => ({
            ...prev,
            totalPages: data.totalPages || 1,
            totalElements: data.totalElements || (data.content?.length || 0)
          }));
        }
      } else {
        setTransactions([]);
      }
    } catch (err) {
      console.error('Error fetching transactions:', err);
      setError(err.response?.data?.message || 'Failed to load transactions');
      setTransactions([]);
    } finally {
      setLoading(false);
    }
  };

  const handleFilter = () => {
    setPagination(prev => ({ ...prev, page: 0 }));
    fetchTransactions();
  };

  const handleClearFilters = () => {
    setFilters(prev => ({
      ...prev,
      startDate: '',
      endDate: '',
      type: ''
    }));
    setPagination(prev => ({ ...prev, page: 0 }));
    // Fetch will be triggered by useEffect
  };

  const handleExport = async () => {
    if (!filters.accountId) {
      setError('Please select an account first');
      return;
    }
    
    try {
      setExporting(true);
      setError(null);
      
      const blob = await transactionService.exportTransactions(
        filters.accountId,
        filters.startDate || null,
        filters.endDate || null
      );
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `transactions_${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Error exporting transactions:', err);
      setError('Failed to export transactions. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  const formatCurrency = (amount, currency = 'USD') => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(amount || 0);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getTransactionIcon = (type) => {
    const transactionType = type?.toUpperCase();
    switch (transactionType) {
      case 'DEPOSIT':
        return <ArrowDownLeft className="w-5 h-5 text-green-500" />;
      case 'WITHDRAWAL':
      case 'WITHDRAW':
        return <ArrowUpRight className="w-5 h-5 text-red-500" />;
      case 'TRANSFER':
        return <ArrowLeftRight className="w-5 h-5 text-blue-500" />;
      default:
        return <Clock className="w-5 h-5 text-gray-500" />;
    }
  };

  const getStatusBadge = (status) => {
    const statusUpper = status?.toUpperCase();
    const badges = {
      COMPLETED: { color: 'bg-green-100 text-green-800', icon: CheckCircle },
      SUCCESS: { color: 'bg-green-100 text-green-800', icon: CheckCircle },
      PENDING: { color: 'bg-yellow-100 text-yellow-800', icon: Clock },
      PROCESSING: { color: 'bg-blue-100 text-blue-800', icon: Clock },
      FAILED: { color: 'bg-red-100 text-red-800', icon: XCircle },
      CANCELLED: { color: 'bg-gray-100 text-gray-800', icon: XCircle }
    };
    const badge = badges[statusUpper] || badges.PENDING;
    const Icon = badge.icon;
    return (
      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${badge.color}`}>
        <Icon className="w-3 h-3 mr-1" />
        {status}
      </span>
    );
  };

  const getAmountClass = (type) => {
    const transactionType = type?.toUpperCase();
    switch (transactionType) {
      case 'DEPOSIT':
        return 'text-green-600';
      case 'WITHDRAWAL':
      case 'WITHDRAW':
        return 'text-red-600';
      default:
        return 'text-gray-900';
    }
  };

  const getAmountPrefix = (type) => {
    const transactionType = type?.toUpperCase();
    switch (transactionType) {
      case 'DEPOSIT':
        return '+';
      case 'WITHDRAWAL':
      case 'WITHDRAW':
        return '-';
      default:
        return '';
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="flex justify-between items-center mb-8">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Transactions</h1>
              <p className="mt-1 text-sm text-gray-500">View your transaction history</p>
            </div>
            <button 
              onClick={handleExport}
              disabled={exporting || !filters.accountId}
              className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {exporting ? (
                <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
              ) : (
                <Download className="w-4 h-4 mr-2" />
              )}
              {exporting ? 'Exporting...' : 'Export CSV'}
            </button>
          </div>

          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center">
              <AlertCircle className="w-5 h-5 mr-2" />
              {error}
              <button onClick={() => setError(null)} className="ml-auto">
                <XCircle className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Filters */}
          <div className="bg-white rounded-xl shadow-sm p-4 mb-6">
            <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Account</label>
                <select
                  value={filters.accountId}
                  onChange={(e) => {
                    setFilters({ ...filters, accountId: e.target.value });
                    setPagination(p => ({ ...p, page: 0 }));
                  }}
                  className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select Account</option>
                  {accounts.map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.accountType} - {account.accountNumber}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
                <select
                  value={filters.type}
                  onChange={(e) => setFilters({ ...filters, type: e.target.value })}
                  className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">All Types</option>
                  <option value="DEPOSIT">Deposit</option>
                  <option value="WITHDRAWAL">Withdrawal</option>
                  <option value="TRANSFER">Transfer</option>
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
              <div className="flex items-end space-x-2">
                <button
                  onClick={handleFilter}
                  disabled={!filters.accountId}
                  className="flex-1 inline-flex items-center justify-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
                >
                  <Search className="w-4 h-4 mr-2" />
                  Filter
                </button>
                <button
                  onClick={handleClearFilters}
                  className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                >
                  Clear
                </button>
              </div>
            </div>
          </div>

          {/* Transactions Table */}
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            {loading ? (
              <div className="flex items-center justify-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              </div>
            ) : !filters.accountId ? (
              <div className="text-center py-12 text-gray-500">
                <AlertCircle className="w-12 h-12 mx-auto mb-3 text-gray-400" />
                <p>Please select an account to view transactions</p>
              </div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Reference</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amount</th>
                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {transactions.length === 0 ? (
                        <tr>
                          <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                            No transactions found
                          </td>
                        </tr>
                      ) : (
                        transactions.map((txn) => (
                          <tr key={txn.id || txn.transactionId || txn.transactionReference} className="hover:bg-gray-50">
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                              {formatDate(txn.createdAt || txn.timestamp)}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="flex items-center">
                                {getTransactionIcon(txn.transactionType)}
                                <span className="ml-2 text-sm font-medium text-gray-900">
                                  {txn.transactionType}
                                </span>
                              </div>
                            </td>
                            <td className="px-6 py-4 text-sm text-gray-900 max-w-xs truncate">
                              {txn.description || '-'}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono text-xs">
                              {txn.transactionReference?.slice(0, 8) || '-'}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              {getStatusBadge(txn.status)}
                            </td>
                            <td className={`px-6 py-4 whitespace-nowrap text-sm font-medium text-right ${getAmountClass(txn.transactionType)}`}>
                              {getAmountPrefix(txn.transactionType)}{formatCurrency(txn.amount)}
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
                </div>

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
              </>
            )}
          </div>
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
              <div className="flex justify-between py-2 border-b">
                <span className="text-gray-500">Type</span>
                <div className="flex items-center">
                  {getTransactionIcon(selectedTransaction.transactionType)}
                  <span className="ml-2 font-medium">{selectedTransaction.transactionType}</span>
                </div>
              </div>
              <div className="flex justify-between py-2 border-b">
                <span className="text-gray-500">Amount</span>
                <span className={`font-medium ${getAmountClass(selectedTransaction.transactionType)}`}>
                  {getAmountPrefix(selectedTransaction.transactionType)}{formatCurrency(selectedTransaction.amount)}
                </span>
              </div>
              <div className="flex justify-between py-2 border-b">
                <span className="text-gray-500">Status</span>
                {getStatusBadge(selectedTransaction.status)}
              </div>
              <div className="flex justify-between py-2 border-b">
                <span className="text-gray-500">Date</span>
                <span className="font-medium">{formatDate(selectedTransaction.createdAt || selectedTransaction.timestamp)}</span>
              </div>
              {selectedTransaction.description && (
                <div className="flex justify-between py-2 border-b">
                  <span className="text-gray-500">Description</span>
                  <span className="font-medium text-right max-w-[200px]">{selectedTransaction.description}</span>
                </div>
              )}
              {selectedTransaction.transactionReference && (
                <div className="flex justify-between py-2 border-b">
                  <span className="text-gray-500">Reference</span>
                  <span className="font-mono text-xs">{selectedTransaction.transactionReference}</span>
                </div>
              )}
              <div className="flex justify-between py-2">
                <span className="text-gray-500">Transaction ID</span>
                <span className="font-mono text-xs">{selectedTransaction.id || selectedTransaction.transactionId}</span>
              </div>
            </div>
            <button
              onClick={() => setSelectedTransaction(null)}
              className="mt-6 w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Transactions;
