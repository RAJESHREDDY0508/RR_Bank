import React from "react";

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error("❌ ErrorBoundary Caught an Error:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 p-6">
          <div className="bg-white shadow-lg rounded-lg p-8 max-w-md text-center">
            <h1 className="text-2xl font-bold text-red-600 mb-3">
              Something went wrong
            </h1>
            <p className="text-gray-700 mb-4">
              We encountered an unexpected error. Please try again, or contact support if the issue
              persists.
            </p>

            <button
              onClick={() => window.location.reload()}
              className="bg-primary-600 text-white px-6 py-2 rounded-md hover:bg-primary-700 transition"
            >
              Reload App
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
