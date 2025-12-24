import React from 'react';
import { Link } from 'react-router-dom';
import { Plus, Receipt } from 'lucide-react';

const Payments: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Payments</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-1">Manage your bill payments</p>
        </div>
        <Link
          to="/payments/new"
          className="flex items-center space-x-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors"
        >
          <Plus size={20} />
          <span>New Payment</span>
        </Link>
      </div>

      <div className="bg-white dark:bg-gray-800 rounded-xl p-12 shadow-lg text-center">
        <Receipt className="w-16 h-16 text-gray-400 mx-auto mb-4" />
        <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-2">No Payments Yet</h3>
        <p className="text-gray-600 dark:text-gray-400 mb-6">
          Start by creating your first payment
        </p>
        <Link
          to="/payments/new"
          className="inline-flex items-center space-x-2 bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg transition-colors"
        >
          <Plus size={20} />
          <span>Create Payment</span>
        </Link>
      </div>
    </div>
  );
};

export default Payments;
