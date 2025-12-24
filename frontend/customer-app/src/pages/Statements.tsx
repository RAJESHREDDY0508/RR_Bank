import React from 'react';
import { FileText, Download } from 'lucide-react';

const Statements: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Statements</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-1">
          Download your account statements
        </p>
      </div>

      <div className="bg-white dark:bg-gray-800 rounded-xl p-12 shadow-lg text-center">
        <FileText className="w-16 h-16 text-gray-400 mx-auto mb-4" />
        <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-2">
          Statement Download Coming Soon
        </h3>
        <p className="text-gray-600 dark:text-gray-400">
          This feature will allow you to download monthly statements in PDF format
        </p>
      </div>
    </div>
  );
};

export default Statements;
