export interface ErrorInfo {
  type: 'error' | 'warning' | 'info';
  title: string;
  message: string;
  field?: string;
}

export const parseApiError = (error: any): ErrorInfo => {
  // Handle structured error responses from our GlobalExceptionHandler
  if (error && typeof error === 'object') {
    if (error.error && error.message) {
      return parseBackendError(error);
    }

    // Handle validation errors with field-specific messages
    if (error.fieldErrors && typeof error.fieldErrors === 'object') {
      const firstField = Object.keys(error.fieldErrors)[0];
      return {
        type: 'error',
        title: 'Validation Error',
        message: error.fieldErrors[firstField] || 'Please check your input',
        field: firstField
      };
    }
  }

  // Handle string errors
  if (typeof error === 'string') {
    return parseStringError(error);
  }

  // Default fallback
  return {
    type: 'error',
    title: 'Error',
    message: 'An unexpected error occurred. Please try again.'
  };
};

const parseBackendError = (error: { error: string; message: string }): ErrorInfo => {
  const errorCode = error.error;

  // Map backend error codes to user-friendly messages
  const errorMappings: Record<string, ErrorInfo> = {
    'ACCOUNT_NOT_FOUND': {
      type: 'error',
      title: 'Account Not Found',
      message: 'The requested account could not be found. Please verify the account details.'
    },
    'INSUFFICIENT_FUNDS': {
      type: 'error',
      title: 'Insufficient Funds',
      message: 'Your account does not have sufficient funds for this transaction.'
    },
    'ACCOUNT_CLOSED': {
      type: 'error',
      title: 'Account Closed',
      message: 'This account is closed and cannot be used for transactions. Please contact customer service.'
    },
    'TRANSACTION_FAILED': {
      type: 'error',
      title: 'Transaction Failed',
      message: 'Your transaction could not be processed at this time. Please try again or contact customer service.'
    },
    'RATE_LIMIT_EXCEEDED': {
      type: 'warning',
      title: 'Too Many Requests',
      message: 'You\'ve made too many requests. Please wait a moment before trying again.'
    },
    'USER_ALREADY_EXISTS': {
      type: 'error',
      title: 'Account Already Exists',
      message: 'An account with this username or email already exists. Please use different credentials.'
    },
    'INVALID_CREDENTIALS': {
      type: 'error',
      title: 'Invalid Login',
      message: 'The username or password you entered is incorrect. Please try again.'
    },
    'ACCESS_DENIED': {
      type: 'error',
      title: 'Access Denied',
      message: 'You don\'t have permission to access this resource.'
    },
    'VALIDATION_FAILED': {
      type: 'error',
      title: 'Validation Error',
      message: 'Please check your input data and try again.'
    },
    'INTERNAL_SERVER_ERROR': {
      type: 'error',
      title: 'Server Error',
      message: 'A server error occurred. Please try again later or contact support if the problem persists.'
    }
  };

  return errorMappings[errorCode] || {
    type: 'error',
    title: 'Error',
    message: error.message || 'An error occurred while processing your request.'
  };
};

const parseStringError = (error: string): ErrorInfo => {
  // Handle common HTTP errors
  if (error.includes('NetworkError') || error.includes('Failed to fetch')) {
    return {
      type: 'error',
      title: 'Connection Error',
      message: 'Unable to connect to the server. Please check your internet connection and try again.'
    };
  }

  if (error.includes('Unauthorized') || error.includes('401')) {
    return {
      type: 'error',
      title: 'Session Expired',
      message: 'Your session has expired. Please log in again.'
    };
  }

  if (error.includes('Forbidden') || error.includes('403')) {
    return {
      type: 'error',
      title: 'Access Denied',
      message: 'You don\'t have permission to perform this action.'
    };
  }

  if (error.includes('Not Found') || error.includes('404')) {
    return {
      type: 'error',
      title: 'Not Found',
      message: 'The requested resource could not be found.'
    };
  }

  if (error.includes('timeout') || error.includes('Timeout')) {
    return {
      type: 'warning',
      title: 'Request Timeout',
      message: 'The request took too long to complete. Please try again.'
    };
  }

  // Return the original string if no specific pattern matches
  return {
    type: 'error',
    title: 'Error',
    message: error
  };
};

export const getValidationMessage = (field: string, value: any): string | null => {
  switch (field) {
    case 'username':
      if (!value || value.trim().length === 0) return 'Username is required';
      if (value.length < 3) return 'Username must be at least 3 characters';
      if (value.length > 50) return 'Username must be less than 50 characters';
      if (!/^[a-zA-Z0-9_-]+$/.test(value)) return 'Username can only contain letters, numbers, hyphens, and underscores';
      break;

    case 'email':
      if (!value || value.trim().length === 0) return 'Email is required';
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'Please enter a valid email address';
      break;

    case 'password':
      if (!value || value.length === 0) return 'Password is required';
      if (value.length < 8) return 'Password must be at least 8 characters';
      if (!/(?=.*[a-z])/.test(value)) return 'Password must contain at least one lowercase letter';
      if (!/(?=.*[A-Z])/.test(value)) return 'Password must contain at least one uppercase letter';
      if (!/(?=.*\d)/.test(value)) return 'Password must contain at least one number';
      if (!/(?=.*[@$!%*?&])/.test(value)) return 'Password must contain at least one special character (@$!%*?&)';
      break;

    case 'amount':
      if (!value || isNaN(Number(value))) return 'Please enter a valid amount';
      if (Number(value) <= 0) return 'Amount must be greater than 0';
      if (Number(value) > 1000000) return 'Amount cannot exceed $1,000,000';
      break;

    case 'accountNumber':
      if (!value || value.trim().length === 0) return 'Account number is required';
      if (!/^\d{10,12}$/.test(value)) return 'Account number must be 10-12 digits';
      break;

    default:
      if (!value || (typeof value === 'string' && value.trim().length === 0)) {
        return `${field.charAt(0).toUpperCase() + field.slice(1)} is required`;
      }
      break;
  }

  return null;
};