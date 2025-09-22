import { useState, useEffect, useCallback } from 'react';

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

interface UseApiOptions {
  immediate?: boolean; // Whether to call the API immediately on mount
}

interface UseApiReturn<T> {
  data: T | null;
  isLoading: boolean;
  error: string | null;
  execute: () => Promise<void>;
  refetch: () => Promise<void>;
}

export function useApi<T>(
  apiCall: () => Promise<ApiResponse<T>>,
  options: UseApiOptions = { immediate: true }
): UseApiReturn<T> {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const execute = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await apiCall();
      if (response.success) {
        setData(response.data || null);
      } else {
        setError(response.error || 'An error occurred');
      }
    } catch (err) {
      setError('An unexpected error occurred');
      console.error('API call error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (options.immediate) {
      execute();
    }
  }, [execute, options.immediate]);

  return {
    data,
    isLoading,
    error,
    execute,
    refetch: execute
  };
}

// Hook for API calls that don't auto-execute (useful for mutations)
export function useApiMutation<T, P = any>(
  apiCall: (params: P) => Promise<ApiResponse<T>>
): {
  data: T | null;
  isLoading: boolean;
  error: string | null;
  execute: (params: P) => Promise<{ success: boolean; data?: T; error?: string }>;
  reset: () => void;
} {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const execute = useCallback(async (params: P) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await apiCall(params);
      if (response.success) {
        setData(response.data || null);
        return { success: true, data: response.data };
      } else {
        setError(response.error || 'An error occurred');
        return { success: false, error: response.error };
      }
    } catch (err) {
      const errorMessage = 'An unexpected error occurred';
      setError(errorMessage);
      console.error('API call error:', err);
      return { success: false, error: errorMessage };
    } finally {
      setIsLoading(false);
    }
  }, []);

  const reset = useCallback(() => {
    setData(null);
    setError(null);
    setIsLoading(false);
  }, []);

  return {
    data,
    isLoading,
    error,
    execute,
    reset
  };
}

// Hook for API calls with dependencies
export function useApiWithDeps<T>(
  apiCall: () => Promise<ApiResponse<T>>,
  deps: any[],
  options: UseApiOptions = { immediate: true }
): UseApiReturn<T> {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const execute = useCallback(async () => {
    // Don't execute if any dependency is null/undefined
    if (deps.some(dep => dep == null)) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await apiCall();
      if (response.success) {
        setData(response.data || null);
      } else {
        setError(response.error || 'An error occurred');
      }
    } catch (err) {
      setError('An unexpected error occurred');
      console.error('API call error:', err);
    } finally {
      setIsLoading(false);
    }
  }, [...deps, apiCall]);

  useEffect(() => {
    if (options.immediate) {
      execute();
    }
  }, [execute, options.immediate]);

  return {
    data,
    isLoading,
    error,
    execute,
    refetch: execute
  };
}