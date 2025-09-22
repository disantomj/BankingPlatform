import { useState, useEffect } from 'react';
import { apiClient } from '@/lib/api/client';

export interface Loan {
  id: number;
  userId: number;
  disbursementAccountId?: number;
  loanType: string;
  principalAmount: number;
  currentBalance: number;
  interestRate: number;
  termMonths: number;
  paymentFrequency: string;
  monthlyPayment?: number;
  nextPaymentDate?: string;
  purpose: string;
  status: string;
  createdAt: string;
  approvedAt?: string;
  disbursedAt?: string;
  approvedBy?: string;
  daysDelinquent?: number;
}

interface UseLoanApplicationData {
  disbursementAccountId?: number;
  loanType: string;
  principalAmount: number;
  interestRate: number;
  termMonths: number;
  paymentFrequency: string;
  purpose: string;
}

interface UseLoansReturn {
  loans: Loan[];
  isLoading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
  applyForLoan: (userId: number, loanData: UseLoanApplicationData) => Promise<{ success: boolean; error?: string }>;
}

export function useLoans(userId?: number): UseLoansReturn {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadLoans = async () => {
    if (!userId) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await apiClient.getLoans(userId);
      if (response.success) {
        setLoans(response.data || []);
      } else {
        setError(response.error || 'Failed to load loans');
      }
    } catch (err) {
      setError('An error occurred while loading loans');
      console.error('Error loading loans:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const applyForLoan = async (userId: number, loanData: UseLoanApplicationData): Promise<{ success: boolean; error?: string }> => {
    try {
      const applicationData = {
        userId,
        ...loanData
      };

      const response = await apiClient.createLoanApplication(applicationData);

      if (response.success) {
        await loadLoans(); // Refresh loans after application
        return { success: true };
      } else {
        return { success: false, error: response.error || 'Failed to submit loan application' };
      }
    } catch (err) {
      return { success: false, error: 'An error occurred while submitting the loan application' };
    }
  };

  useEffect(() => {
    loadLoans();
  }, [userId]);

  return {
    loans,
    isLoading,
    error,
    refetch: loadLoans,
    applyForLoan
  };
}

// Helper functions for loan display
export const getLoanTypeDisplay = (type: string) => {
  switch (type) {
    case 'PERSONAL': return 'Personal Loan';
    case 'AUTO': return 'Auto Loan';
    case 'MORTGAGE': return 'Mortgage';
    case 'STUDENT': return 'Student Loan';
    case 'BUSINESS': return 'Business Loan';
    default: return type;
  }
};

export const getLoanStatusColor = (status: string) => {
  switch (status.toUpperCase()) {
    case 'ACTIVE': case 'DISBURSED': return 'bg-green-100 text-green-800';
    case 'PENDING': return 'bg-yellow-100 text-yellow-800';
    case 'APPROVED': return 'bg-blue-100 text-blue-800';
    case 'PAID_OFF': return 'bg-gray-100 text-gray-800';
    case 'DELINQUENT': case 'DEFAULTED': return 'bg-red-100 text-red-800';
    case 'REJECTED': return 'bg-red-100 text-red-800';
    default: return 'bg-gray-100 text-gray-800';
  }
};