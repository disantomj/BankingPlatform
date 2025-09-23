'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Account } from '@/types';
import { apiClient } from '@/lib/api/client';

// Query keys for cache invalidation
export const accountKeys = {
  all: ['accounts'] as const,
  lists: () => [...accountKeys.all, 'list'] as const,
  list: (userId?: number) => [...accountKeys.lists(), { userId }] as const,
  details: () => [...accountKeys.all, 'detail'] as const,
  detail: (id: number) => [...accountKeys.details(), id] as const,
};

// Fetch accounts for a user
export function useAccounts(userId?: number) {
  return useQuery({
    queryKey: accountKeys.list(userId),
    queryFn: async () => {
      const response = await apiClient.getAccounts(userId);
      if (!response.success) {
        throw new Error(response.error || 'Failed to fetch accounts');
      }
      return response.data || [];
    },
    enabled: !!userId, // Only run query if userId is provided
    staleTime: 2 * 60 * 1000, // 2 minutes for accounts (balance changes frequently)
  });
}

// Fetch single account
export function useAccount(accountId: number) {
  return useQuery({
    queryKey: accountKeys.detail(accountId),
    queryFn: async () => {
      const response = await apiClient.getAccount(accountId);
      if (!response.success) {
        throw new Error(response.error || 'Failed to fetch account');
      }
      return response.data;
    },
    enabled: !!accountId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Create account mutation
export function useCreateAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (accountData: Partial<Account>) => {
      const response = await apiClient.createAccount(accountData);
      if (!response.success) {
        throw new Error(response.error || 'Failed to create account');
      }
      return response.data;
    },
    onSuccess: () => {
      // Invalidate and refetch accounts list
      queryClient.invalidateQueries({ queryKey: accountKeys.lists() });
    },
  });
}

// Update account mutation
export function useUpdateAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ accountId, accountData }: { accountId: number; accountData: Partial<Account> }) => {
      const response = await apiClient.updateAccount(accountId, accountData);
      if (!response.success) {
        throw new Error(response.error || 'Failed to update account');
      }
      return response.data;
    },
    onSuccess: (data, variables) => {
      // Update the specific account in cache
      queryClient.setQueryData(accountKeys.detail(variables.accountId), data);
      // Invalidate accounts list to show updated data
      queryClient.invalidateQueries({ queryKey: accountKeys.lists() });
    },
  });
}