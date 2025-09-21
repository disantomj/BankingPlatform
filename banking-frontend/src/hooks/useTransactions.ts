'use client';

import { useState, useEffect } from 'react';
import { Transaction } from '@/types';
import { apiClient } from '@/lib/api/client';

interface UseTransactionsReturn {
  transactions: Transaction[];
  isLoading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

export function useTransactions(accountId?: number, userId?: number): UseTransactionsReturn {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTransactions = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await apiClient.getTransactions(accountId, userId);

      if (response.success && response.data) {
        setTransactions(response.data);
      } else {
        setError(response.error || 'Failed to fetch transactions');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch transactions');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [accountId, userId]);

  return {
    transactions,
    isLoading,
    error,
    refetch: fetchTransactions,
  };
}

interface UseTransactionReturn {
  transaction: Transaction | null;
  isLoading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

export function useTransaction(transactionId: number): UseTransactionReturn {
  const [transaction, setTransaction] = useState<Transaction | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTransaction = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await apiClient.getTransaction(transactionId);

      if (response.success && response.data) {
        setTransaction(response.data);
      } else {
        setError(response.error || 'Failed to fetch transaction');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch transaction');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (transactionId) {
      fetchTransaction();
    }
  }, [transactionId]);

  return {
    transaction,
    isLoading,
    error,
    refetch: fetchTransaction,
  };
}