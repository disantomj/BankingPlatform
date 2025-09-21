'use client';

import { useState, useEffect } from 'react';
import { Account } from '@/types';
import { apiClient } from '@/lib/api/client';

interface UseAccountsReturn {
  accounts: Account[];
  isLoading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

export function useAccounts(userId?: number): UseAccountsReturn {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAccounts = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await apiClient.getAccounts(userId);

      if (response.success && response.data) {
        setAccounts(response.data);
      } else {
        setError(response.error || 'Failed to fetch accounts');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch accounts');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, [userId]);

  return {
    accounts,
    isLoading,
    error,
    refetch: fetchAccounts,
  };
}

interface UseAccountReturn {
  account: Account | null;
  isLoading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

export function useAccount(accountId: number): UseAccountReturn {
  const [account, setAccount] = useState<Account | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAccount = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await apiClient.getAccount(accountId);

      if (response.success && response.data) {
        setAccount(response.data);
      } else {
        setError(response.error || 'Failed to fetch account');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch account');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (accountId) {
      fetchAccount();
    }
  }, [accountId]);

  return {
    account,
    isLoading,
    error,
    refetch: fetchAccount,
  };
}