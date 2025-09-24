'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Transaction } from '@/types';
import { apiClient } from '@/lib/api/client';

// Query keys for cache invalidation
export const transactionKeys = {
  all: ['transactions'] as const,
  lists: () => [...transactionKeys.all, 'list'] as const,
  list: (accountId?: number, userId?: number) => [...transactionKeys.lists(), { accountId, userId }] as const,
  details: () => [...transactionKeys.all, 'detail'] as const,
  detail: (id: number) => [...transactionKeys.details(), id] as const,
  search: (query: string) => [...transactionKeys.all, 'search', query] as const,
};

// Fetch transactions for user or account
export function useTransactions(accountId?: number, userId?: number) {
  return useQuery({
    queryKey: transactionKeys.list(accountId, userId),
    queryFn: async () => {
      const response = await apiClient.getTransactions(accountId, userId);
      if (!response.success) {
        throw new Error(response.error || 'Failed to fetch transactions');
      }
      return response.data || [];
    },
    enabled: !!(accountId || userId), // Only run if we have an ID
    staleTime: 1 * 60 * 1000, // 1 minute for transactions (they change frequently)
  });
}

// Fetch paginated transactions for user
export function useUserTransactionsPaginated(userId?: number, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: [...transactionKeys.list(undefined, userId), 'paginated', page, size],
    queryFn: async () => {
      if (!userId) throw new Error('User ID is required');
      const response = await apiClient.getUserTransactionsPaginated(userId, page, size);
      if (!response.success) {
        throw new Error(response.error || 'Failed to fetch transactions');
      }
      return response.data;
    },
    enabled: !!userId,
    staleTime: 1 * 60 * 1000, // 1 minute
    placeholderData: (previousData) => previousData, // Keep previous page data while loading new page
  });
}

// Fetch paginated transactions for account
export function useAccountTransactionsPaginated(accountId?: number, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: [...transactionKeys.list(accountId), 'paginated', page, size],
    queryFn: async () => {
      if (!accountId) throw new Error('Account ID is required');
      const response = await apiClient.getAccountTransactionsPaginated(accountId, page, size);
      if (!response.success) {
        throw new Error(response.error || 'Failed to fetch transactions');
      }
      return response.data;
    },
    enabled: !!accountId,
    staleTime: 1 * 60 * 1000, // 1 minute
    placeholderData: (previousData) => previousData, // Keep previous page data while loading new page
  });
}

// Fetch single transaction
export function useTransaction(transactionId: number) {
  return useQuery({
    queryKey: transactionKeys.detail(transactionId),
    queryFn: async () => {
      const response = await apiClient.getTransaction(transactionId);
      if (!response.success) {
        throw new Error(response.error || 'Failed to fetch transaction');
      }
      return response.data;
    },
    enabled: !!transactionId,
    staleTime: 5 * 60 * 1000, // 5 minutes for individual transactions (they don't change)
  });
}

// Search transactions
export function useTransactionSearch(query: string) {
  return useQuery({
    queryKey: transactionKeys.search(query),
    queryFn: async () => {
      const response = await apiClient.searchTransactions(query);
      if (!response.success) {
        throw new Error(response.error || 'Failed to search transactions');
      }
      return response.data || [];
    },
    enabled: query.length > 2, // Only search if query is meaningful
    staleTime: 30 * 1000, // 30 seconds for search results
  });
}

// Create transfer mutation
export function useCreateTransfer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (transferData: {
      fromAccountId: number;
      toAccountId: number;
      amount: number;
      userId: number;
      description?: string;
      channel?: string;
    }) => {
      const response = await apiClient.createTransfer(transferData);
      if (!response.success) {
        throw new Error(response.error || 'Failed to create transfer');
      }
      return response.data;
    },
    onSuccess: (data, variables) => {
      // Invalidate transactions for both accounts and user
      queryClient.invalidateQueries({
        queryKey: transactionKeys.list(variables.fromAccountId)
      });
      queryClient.invalidateQueries({
        queryKey: transactionKeys.list(variables.toAccountId)
      });
      queryClient.invalidateQueries({
        queryKey: transactionKeys.list(undefined, variables.userId)
      });

      // Also invalidate accounts to update balances
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
    },
  });
}

// Create deposit mutation
export function useCreateDeposit() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (depositData: {
      toAccountId: number;
      amount: number;
      userId: number;
      description?: string;
      channel?: string;
    }) => {
      const response = await apiClient.createDeposit(depositData);
      if (!response.success) {
        throw new Error(response.error || 'Failed to create deposit');
      }
      return response.data;
    },
    onSuccess: (data, variables) => {
      // Invalidate related queries
      queryClient.invalidateQueries({
        queryKey: transactionKeys.list(variables.toAccountId)
      });
      queryClient.invalidateQueries({
        queryKey: transactionKeys.list(undefined, variables.userId)
      });
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
    },
  });
}

// Create withdrawal mutation
export function useCreateWithdrawal() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (withdrawalData: {
      fromAccountId: number;
      amount: number;
      userId: number;
      description?: string;
      channel?: string;
    }) => {
      const response = await apiClient.createWithdrawal(withdrawalData);
      if (!response.success) {
        throw new Error(response.error || 'Failed to create withdrawal');
      }
      return response.data;
    },
    onSuccess: (data, variables) => {
      // Invalidate related queries
      queryClient.invalidateQueries({
        queryKey: transactionKeys.list(variables.fromAccountId)
      });
      queryClient.invalidateQueries({
        queryKey: transactionKeys.list(undefined, variables.userId)
      });
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
    },
  });
}