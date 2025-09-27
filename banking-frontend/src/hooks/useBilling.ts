import { useState, useEffect } from 'react';
import { apiClient } from '@/lib/api/client';

export interface Billing {
  id: number;
  user: any;
  account?: any;
  billingType: string;
  status: string;
  amount: number;
  description: string;
  dueDate: string;
  paidDate?: string;
  transactionId?: number;
  reference: string;
  createdAt: string;
  updatedAt: string;
}

interface UseBillingReturn {
  bills: Billing[];
  paidBills: Billing[];
  isLoading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
  payBill: (billId: number, accountId: number) => Promise<{ success: boolean; error?: string }>;
  createBill: (billData: CreateBillData) => Promise<{ success: boolean; error?: string }>;
}

interface CreateBillData {
  billingType: string;
  amount: number;
  description: string;
  dueDate: string;
  frequency?: string;
  accountId?: number;
}

export function useBilling(userId?: number): UseBillingReturn {
  const [bills, setBills] = useState<Billing[]>([]);
  const [paidBills, setPaidBills] = useState<Billing[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadBills = async () => {
    if (!userId) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      // Load all bills and separate by status
      const [allBillsResponse, unpaidBillsResponse] = await Promise.all([
        apiClient.getBillings(userId),
        apiClient.getUnpaidBillings(userId)
      ]);

      if (allBillsResponse.success && allBillsResponse.data) {
        const allBills = allBillsResponse.data;
        const unpaidBills = unpaidBillsResponse.success ? unpaidBillsResponse.data : [];

        setBills(unpaidBills || []);
        setPaidBills(allBills.filter((bill: Billing) => bill.status === 'PAID') || []);
      } else {
        setError(allBillsResponse.error || 'Failed to load bills');
      }
    } catch (err) {
      setError('An error occurred while loading bills');
      console.error('Error loading bills:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const payBill = async (billId: number, accountId: number): Promise<{ success: boolean; error?: string }> => {
    try {
      const response = await apiClient.payBill(billId, accountId);
      if (response.success) {
        await loadBills(); // Refresh bills after payment
        return { success: true };
      } else {
        return { success: false, error: response.error || 'Failed to pay bill' };
      }
    } catch (err) {
      return { success: false, error: 'An error occurred while paying the bill' };
    }
  };

  const createBill = async (billData: CreateBillData): Promise<{ success: boolean; error?: string }> => {
    if (!userId) {
      return { success: false, error: 'User ID is required' };
    }

    try {
      const response = await apiClient.createBilling({
        userId,
        ...billData
      });
      if (response.success) {
        await loadBills(); // Refresh bills after creation
        return { success: true };
      } else {
        return { success: false, error: response.error || 'Failed to create bill' };
      }
    } catch (err) {
      return { success: false, error: 'An error occurred while creating the bill' };
    }
  };

  useEffect(() => {
    loadBills();
  }, [userId]);

  return {
    bills,
    paidBills,
    isLoading,
    error,
    refetch: loadBills,
    payBill,
    createBill
  };
}