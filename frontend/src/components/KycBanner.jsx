import { useAuth } from '../context/AuthContext';

/**
 * KYC Banner component that shows the user's KYC verification status
 * and blocks them from performing transactions if not approved.
 */
const KycBanner = ({ showOnApproved = false }) => {
  const { kycStatus, kycRejectionReason, refreshKycStatus } = useAuth();

  // Don't show anything if KYC is approved and showOnApproved is false
  if (kycStatus === 'APPROVED' && !showOnApproved) {
    return null;
  }

  const handleRefresh = async () => {
    await refreshKycStatus();
  };

  if (kycStatus === 'PENDING') {
    return (
      <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 mb-4">
        <div className="flex">
          <div className="flex-shrink-0">
            <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-yellow-800">
              KYC Verification Pending
            </h3>
            <div className="mt-2 text-sm text-yellow-700">
              <p>
                Your account is pending KYC verification. Transactions (deposit, withdraw, transfer) 
                will be enabled once an admin approves your KYC.
              </p>
            </div>
            <div className="mt-3">
              <button
                onClick={handleRefresh}
                className="text-sm font-medium text-yellow-800 hover:text-yellow-600 underline"
              >
                Check status again
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (kycStatus === 'REJECTED') {
    return (
      <div className="bg-red-50 border-l-4 border-red-400 p-4 mb-4">
        <div className="flex">
          <div className="flex-shrink-0">
            <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
            </svg>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">
              KYC Verification Rejected
            </h3>
            <div className="mt-2 text-sm text-red-700">
              <p>
                Your KYC verification was rejected. Transactions are disabled.
              </p>
              {kycRejectionReason && (
                <p className="mt-1">
                  <strong>Reason:</strong> {kycRejectionReason}
                </p>
              )}
              <p className="mt-1">
                Please contact support for assistance.
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (kycStatus === 'APPROVED' && showOnApproved) {
    return (
      <div className="bg-green-50 border-l-4 border-green-400 p-4 mb-4">
        <div className="flex">
          <div className="flex-shrink-0">
            <svg className="h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
            </svg>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-green-800">
              KYC Verified
            </h3>
            <div className="mt-2 text-sm text-green-700">
              <p>Your account is verified. All features are enabled.</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return null;
};

export default KycBanner;
