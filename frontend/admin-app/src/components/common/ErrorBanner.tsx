/**
 * Error Banner Component with Retry Functionality
 * Displays errors with request ID and retry option
 */

import React from 'react';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Typography,
  Collapse,
  IconButton,
} from '@mui/material';
import {
  Refresh,
  Close,
  ExpandMore,
  ContentCopy,
  Error as ErrorIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
} from '@mui/icons-material';
import { toast } from 'react-toastify';

interface ErrorBannerProps {
  error: string | null;
  requestId?: string | null;
  onRetry?: () => void;
  onClose?: () => void;
  severity?: 'error' | 'warning' | 'info';
  title?: string;
  showDetails?: boolean;
  details?: string;
}

const ErrorBanner: React.FC<ErrorBannerProps> = ({
  error,
  requestId,
  onRetry,
  onClose,
  severity = 'error',
  title,
  showDetails = false,
  details,
}) => {
  const [expanded, setExpanded] = React.useState(false);

  if (!error) return null;

  const handleCopyRequestId = () => {
    if (requestId) {
      navigator.clipboard.writeText(requestId);
      toast.info('Request ID copied to clipboard');
    }
  };

  const getDefaultTitle = () => {
    switch (severity) {
      case 'error':
        return 'Error';
      case 'warning':
        return 'Warning';
      case 'info':
        return 'Information';
      default:
        return 'Error';
    }
  };

  return (
    <Alert
      severity={severity}
      sx={{ mb: 2 }}
      action={
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {onRetry && (
            <Button
              size="small"
              color="inherit"
              startIcon={<Refresh />}
              onClick={onRetry}
            >
              Retry
            </Button>
          )}
          {onClose && (
            <IconButton size="small" color="inherit" onClick={onClose}>
              <Close fontSize="small" />
            </IconButton>
          )}
        </Box>
      }
    >
      <AlertTitle>{title || getDefaultTitle()}</AlertTitle>
      <Typography variant="body2">{error}</Typography>

      {/* Request ID */}
      {requestId && (
        <Box sx={{ mt: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="caption" color="text.secondary">
            Request ID: {requestId}
          </Typography>
          <IconButton size="small" onClick={handleCopyRequestId}>
            <ContentCopy fontSize="inherit" />
          </IconButton>
        </Box>
      )}

      {/* Expandable details */}
      {showDetails && details && (
        <>
          <Button
            size="small"
            color="inherit"
            onClick={() => setExpanded(!expanded)}
            endIcon={<ExpandMore sx={{ transform: expanded ? 'rotate(180deg)' : 'none' }} />}
            sx={{ mt: 1 }}
          >
            {expanded ? 'Hide Details' : 'Show Details'}
          </Button>
          <Collapse in={expanded}>
            <Box
              sx={{
                mt: 1,
                p: 1,
                bgcolor: 'action.hover',
                borderRadius: 1,
                fontFamily: 'monospace',
                fontSize: '0.75rem',
                overflow: 'auto',
                maxHeight: 200,
              }}
            >
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{details}</pre>
            </Box>
          </Collapse>
        </>
      )}

      {/* Help text */}
      <Typography variant="caption" display="block" sx={{ mt: 1 }}>
        If this problem persists, please contact support with the request ID.
      </Typography>
    </Alert>
  );
};

/**
 * API Error Handler - Converts API errors to banner-friendly format
 */
export interface ApiErrorInfo {
  message: string;
  requestId?: string;
  status?: number;
  code?: string;
  details?: string;
}

export const parseApiError = (error: any): ApiErrorInfo => {
  // Standard API error response
  if (error.response?.data) {
    const data = error.response.data;
    return {
      message: data.message || data.error || 'An unexpected error occurred',
      requestId: data.requestId || data.path,
      status: error.response.status,
      code: data.code || data.errorCode,
      details: data.details ? JSON.stringify(data.details, null, 2) : undefined,
    };
  }

  // Network error
  if (error.message === 'Network Error') {
    return {
      message: 'Unable to connect to the server. Please check your internet connection.',
      status: 0,
    };
  }

  // Timeout
  if (error.code === 'ECONNABORTED') {
    return {
      message: 'Request timed out. Please try again.',
      status: 408,
    };
  }

  // Generic error
  return {
    message: error.message || 'An unexpected error occurred',
    details: error.stack,
  };
};

/**
 * Inline Error Display for form fields
 */
interface FieldErrorProps {
  error?: string;
}

export const FieldError: React.FC<FieldErrorProps> = ({ error }) => {
  if (!error) return null;
  return (
    <Typography variant="caption" color="error" sx={{ mt: 0.5, display: 'block' }}>
      {error}
    </Typography>
  );
};

export default ErrorBanner;
