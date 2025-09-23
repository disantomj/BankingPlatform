'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLoans, getLoanTypeDisplay, getLoanStatusColor } from '@/hooks/useLoans';
import { useAccounts } from '@/hooks/useAccounts';
import { Button, Card, CardBody } from '@/components/ui';
import { formatCurrency, formatDate } from '@/lib/utils';
import Link from 'next/link';


export default function LoansPage() {
  const { user, logout, isAuthenticated, isLoading } = useAuth();
  const { loans, isLoading: isLoadingLoans, error: loansError, applyForLoan } = useLoans(user?.id);
  const { accounts } = useAccounts(user?.id);
  const [activeTab, setActiveTab] = useState<'overview' | 'apply'>('overview');
  const [viewedApprovedLoans, setViewedApprovedLoans] = useState<Set<number>>(new Set());
  const [applicationForm, setApplicationForm] = useState({
    loanType: '',
    principalAmount: '',
    interestRate: '',
    termMonths: '',
    paymentFrequency: 'MONTHLY',
    purpose: '',
    disbursementAccountId: ''
  });
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Redirect to login if not authenticated
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      window.location.href = '/login';
    }
  }, [isAuthenticated, isLoading]);

  // Load viewed approved loans from localStorage on mount
  useEffect(() => {
    if (user?.id) {
      const stored = localStorage.getItem(`viewedApprovedLoans_${user.id}`);
      if (stored) {
        setViewedApprovedLoans(new Set(JSON.parse(stored)));
      }
    }
  }, [user?.id]);

  // Mark approved loans as viewed AFTER a delay to allow first render
  useEffect(() => {
    if (loans.length > 0 && user?.id) {
      const timer = setTimeout(() => {
        const approvedLoans = loans.filter(loan => loan.status?.toUpperCase() === 'APPROVED');
        const newViewedLoans = new Set(viewedApprovedLoans);
        let hasNewViews = false;

        approvedLoans.forEach(loan => {
          if (!newViewedLoans.has(loan.id)) {
            newViewedLoans.add(loan.id);
            hasNewViews = true;
          }
        });

        if (hasNewViews) {
          setViewedApprovedLoans(newViewedLoans);
          localStorage.setItem(`viewedApprovedLoans_${user.id}`, JSON.stringify([...newViewedLoans]));
        }
      }, 2000); // 2 second delay to allow user to see "APPROVED" status

      return () => clearTimeout(timer);
    }
  }, [loans, user?.id, viewedApprovedLoans]);


  const handleLogout = () => {
    logout();
    window.location.href = '/';
  };


  const handleApplicationSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) return;

    setMessage(null);
    const loanData = {
      disbursementAccountId: applicationForm.disbursementAccountId ? parseInt(applicationForm.disbursementAccountId) : undefined,
      loanType: applicationForm.loanType,
      principalAmount: parseFloat(applicationForm.principalAmount),
      interestRate: parseFloat(applicationForm.interestRate) / 100, // Convert percentage to decimal
      termMonths: parseInt(applicationForm.termMonths),
      paymentFrequency: applicationForm.paymentFrequency,
      purpose: applicationForm.purpose
    };

    const result = await applyForLoan(user.id, loanData);

    if (result.success) {
      setMessage({ type: 'success', text: 'Loan application submitted successfully!' });
      setApplicationForm({
        loanType: '',
        principalAmount: '',
        interestRate: '',
        termMonths: '',
        paymentFrequency: 'MONTHLY',
        purpose: '',
        disbursementAccountId: ''
      });
      setActiveTab('overview');
    } else {
      setMessage({ type: 'error', text: result.error || 'Failed to submit application' });
    }
  };

  const calculateMonthlyPayment = (principal: number, rate: number, termMonths: number): number => {
    if (rate === 0) return principal / termMonths;
    const monthlyRate = rate / 12; // rate is already a decimal (e.g., 0.055 for 5.5%)
    return (principal * monthlyRate * Math.pow(1 + monthlyRate, termMonths)) / (Math.pow(1 + monthlyRate, termMonths) - 1);
  };

  // Helper function to get the display status for a loan
  const getLoanDisplayStatus = (loan: any): string => {
    if (loan.status?.toUpperCase() === 'APPROVED') {
      // If this is the first time viewing this approved loan, show "APPROVED"
      if (!viewedApprovedLoans.has(loan.id)) {
        return 'APPROVED';
      }
      // If already viewed, show as "ACTIVE"
      return 'ACTIVE';
    }
    // For all other statuses, return as-is
    return loan.status?.toUpperCase() || 'UNKNOWN';
  };

  // Helper function to check if loan should be treated as active for calculations
  const isActiveForCalculations = (loan: any): boolean => {
    return ['ACTIVE', 'DISBURSED', 'APPROVED'].includes(loan.status?.toUpperCase());
  };

  // Show loading while checking authentication
  if (isLoading) {
    return (
      <div className="min-h-screen bg-accent-200 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-neutral-600">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-accent-200">
      {/* Header Navigation */}
      <header className="bg-white shadow-sm border-b border-accent-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-8">
              <Link href="/dashboard" className="flex items-center">
                <h1 className="text-2xl font-bold text-primary">SecureBank</h1>
              </Link>
              <nav className="hidden md:flex space-x-6">
                <Link href="/dashboard" className="text-neutral-600 hover:text-primary">Dashboard</Link>
                <Link href="/accounts" className="text-neutral-600 hover:text-primary">Accounts</Link>
                <Link href="/transactions" className="text-neutral-600 hover:text-primary">Transactions</Link>
                <Link href="/billing" className="text-neutral-600 hover:text-primary">Billing</Link>
                <Link href="/loans" className="text-primary font-medium">Loans</Link>
                <Link href="/reports" className="text-neutral-600 hover:text-primary">Reports</Link>
                {user?.role === 'ADMIN' && (
                  <Link href="/admin" className="text-red-600 hover:text-red-700 font-medium">Admin</Link>
                )}
              </nav>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-neutral-600">Welcome, {user?.username || 'User'}</span>
              <Button variant="ghost" size="sm" onClick={handleLogout}>
                Logout
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Success/Error Message */}
        {message && (
          <div className={`mb-6 p-4 rounded-md ${message.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : 'bg-red-50 text-red-800 border border-red-200'}`}>
            <div className="flex">
              <div className="flex-shrink-0">
                {message.type === 'success' ? (
                  <svg className="h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                  </svg>
                ) : (
                  <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                )}
              </div>
              <div className="ml-3">
                <p className="text-sm font-medium">{message.text}</p>
              </div>
              <div className="ml-auto pl-3">
                <button
                  onClick={() => setMessage(null)}
                  className="inline-flex text-gray-400 hover:text-gray-600"
                >
                  <span className="sr-only">Dismiss</span>
                  <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Page Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-3xl font-bold text-dark">Loan Management</h2>
            <p className="text-neutral-600 mt-1">Manage your loans and applications</p>
          </div>
          <Button variant="primary" onClick={() => setActiveTab('apply')}>
            + Apply for Loan
          </Button>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <Card className="border-l-4 border-l-primary">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Total Loan Balance</p>
                  <p className="text-2xl font-semibold text-dark">
                    {formatCurrency(loans.filter(l => ['ACTIVE', 'DISBURSED', 'APPROVED'].includes(l.status?.toUpperCase())).reduce((sum, loan) => {
                      // Use currentBalance for active/disbursed loans, principalAmount for approved loans
                      const balance = loan.status?.toUpperCase() === 'APPROVED' ? loan.principalAmount : (loan.currentBalance || 0);
                      return sum + balance;
                    }, 0))}
                  </p>
                </div>
                <div className="text-primary">
                  <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M4 4a2 2 0 00-2 2v8a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2H4zm0 2h12v2H4V6zm0 4h12v4H4v-4z"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-secondary">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Est. Monthly Payments</p>
                  <p className="text-2xl font-semibold text-dark">
                    {formatCurrency(loans.filter(l => ['ACTIVE', 'DISBURSED', 'APPROVED'].includes(l.status?.toUpperCase())).reduce((sum, loan) => {
                      const payment = loan.monthlyPayment || calculateMonthlyPayment(loan.principalAmount, loan.interestRate, loan.termMonths);
                      return sum + payment;
                    }, 0))}
                  </p>
                </div>
                <div className="text-secondary">
                  <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-primary">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Active & Approved Loans</p>
                  <p className="text-2xl font-semibold text-dark">{loans.filter(l => ['ACTIVE', 'DISBURSED', 'APPROVED'].includes(l.status?.toUpperCase())).length}</p>
                </div>
                <div className="text-primary">
                  <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M3 3a1 1 0 000 2v8a2 2 0 002 2h2.586l-1.293 1.293a1 1 0 101.414 1.414L10 15.414l2.293 2.293a1 1 0 001.414-1.414L12.414 15H15a2 2 0 002-2V5a1 1 0 100-2H3zm11.707 4.707a1 1 0 00-1.414-1.414L10 9.586 8.707 8.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>
        </div>

        {/* Tabs */}
        <div className="mb-8">
          <nav className="flex space-x-8">
            <button
              onClick={() => setActiveTab('overview')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'overview'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-neutral-500 hover:text-neutral-700 hover:border-neutral-300'
              }`}
            >
              Loan Overview ({loans.length})
            </button>
            <button
              onClick={() => setActiveTab('apply')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'apply'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-neutral-500 hover:text-neutral-700 hover:border-neutral-300'
              }`}
            >
              Apply for Loan
            </button>
          </nav>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {isLoadingLoans ? (
              <Card>
                <CardBody className="text-center py-12">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
                  <p className="text-neutral-600">Loading loans...</p>
                </CardBody>
              </Card>
            ) : loans.length === 0 ? (
              <Card>
                <CardBody className="text-center py-12">
                  <div className="text-neutral-400 mb-4">
                    <svg className="w-16 h-16 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M4 4a2 2 0 00-2 2v8a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2H4zm0 2h12v2H4V6zm0 4h12v4H4v-4z"/>
                    </svg>
                  </div>
                  <h3 className="text-xl font-semibold text-dark mb-2">No loans found</h3>
                  <p className="text-neutral-600 mb-4">You don't have any loans at the moment</p>
                  <Button variant="primary" onClick={() => setActiveTab('apply')}>
                    Apply for a Loan
                  </Button>
                </CardBody>
              </Card>
            ) : (
              loans.filter((loan) => {
                // Filter out rejected loans older than 24 hours
                if (loan.status?.toUpperCase() === 'REJECTED') {
                  const createdAt = new Date(loan.createdAt);
                  const now = new Date();
                  const hoursDiff = (now.getTime() - createdAt.getTime()) / (1000 * 60 * 60);
                  return hoursDiff < 24; // Only show rejected loans less than 24 hours old
                }
                return true; // Show all non-rejected loans
              }).map((loan) => {
                const monthlyPayment = loan.monthlyPayment || calculateMonthlyPayment(loan.principalAmount, loan.interestRate, loan.termMonths);
                const progressPercent = Math.round(((loan.principalAmount - (loan.currentBalance || 0)) / loan.principalAmount) * 100);

                return (
                  <Card key={loan.id}>
                    <CardBody>
                      <div className="flex items-start justify-between mb-4">
                        <div>
                          <h3 className="text-xl font-semibold text-dark">{getLoanTypeDisplay(loan.loanType)}</h3>
                          <p className="text-neutral-600">Loan ID: {loan.id}</p>
                          <p className="text-sm text-neutral-500">{loan.purpose}</p>
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium mt-2 ${getLoanStatusColor(getLoanDisplayStatus(loan))}`}>
                            {getLoanDisplayStatus(loan)}
                          </span>
                          {loan.status?.toUpperCase() === 'REJECTED' && (() => {
                            const createdAt = new Date(loan.createdAt);
                            const now = new Date();
                            const hoursRemaining = 24 - ((now.getTime() - createdAt.getTime()) / (1000 * 60 * 60));
                            if (hoursRemaining > 0) {
                              return (
                                <div className="mt-2 p-2 bg-red-50 border border-red-200 rounded-md">
                                  <p className="text-xs text-red-700 font-medium">
                                    ⚠️ This loan will terminate in {Math.ceil(hoursRemaining)} hour{Math.ceil(hoursRemaining) !== 1 ? 's' : ''}.
                                  </p>
                                </div>
                              );
                            }
                            return null;
                          })()}
                        </div>
                        <div className="text-right">
                          <p className="text-2xl font-bold text-dark">{formatCurrency(loan.currentBalance || 0)}</p>
                          <p className="text-sm text-neutral-600">Current Balance</p>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div>
                          <p className="text-sm text-neutral-600">Est. Monthly Payment</p>
                          <p className="font-semibold text-dark">{formatCurrency(monthlyPayment)}</p>
                        </div>
                        <div>
                          <p className="text-sm text-neutral-600">Interest Rate</p>
                          <p className="font-semibold text-dark">{(loan.interestRate * 100).toFixed(2)}%</p>
                        </div>
                        <div>
                          <p className="text-sm text-neutral-600">Term</p>
                          <p className="font-semibold text-dark">{loan.termMonths} months</p>
                        </div>
                        <div>
                          <p className="text-sm text-neutral-600">Payment Frequency</p>
                          <p className="font-semibold text-dark">{loan.paymentFrequency || 'Monthly'}</p>
                        </div>
                      </div>

                      <div className="mt-4">
                        <div className="flex justify-between text-sm text-neutral-600 mb-1">
                          <span>Loan Progress</span>
                          <span>{progressPercent}% paid</span>
                        </div>
                        <div className="w-full bg-neutral-200 rounded-full h-2">
                          <div
                            className="bg-primary h-2 rounded-full"
                            style={{ width: `${Math.max(0, Math.min(100, progressPercent))}%` }}
                          ></div>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mt-4 text-sm">
                        <div>
                          <p className="text-neutral-600">Principal Amount</p>
                          <p className="font-semibold text-dark">{formatCurrency(loan.principalAmount)}</p>
                        </div>
                        <div>
                          <p className="text-neutral-600">Created</p>
                          <p className="font-semibold text-dark">{formatDate(loan.createdAt)}</p>
                        </div>
                        {loan.approvedAt && (
                          <div>
                            <p className="text-neutral-600">Approved</p>
                            <p className="font-semibold text-dark">{formatDate(loan.approvedAt)}</p>
                          </div>
                        )}
                      </div>

                      <div className="flex space-x-3 mt-6">
                        {['ACTIVE', 'DISBURSED'].includes(loan.status?.toUpperCase()) && (
                          <Button variant="primary" size="sm">Make Payment</Button>
                        )}
                        <Button variant="outline" size="sm">View Details</Button>
                        {loan.status?.toUpperCase() === 'PENDING' && user?.role === 'ADMIN' && (
                          <>
                            <Button variant="primary" size="sm">Approve</Button>
                            <Button variant="outline" size="sm">Reject</Button>
                          </>
                        )}
                      </div>
                    </CardBody>
                  </Card>
                );
              })
            )}
          </div>
        )}

        {activeTab === 'apply' && (
          <Card>
            <CardBody>
              <h3 className="text-xl font-semibold text-dark mb-6">Apply for a New Loan</h3>
              <form onSubmit={handleApplicationSubmit} className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-dark mb-2">Loan Type</label>
                    <select
                      value={applicationForm.loanType}
                      onChange={(e) => setApplicationForm(prev => ({ ...prev, loanType: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      required
                    >
                      <option value="">Select loan type</option>
                      <option value="PERSONAL">Personal Loan</option>
                      <option value="AUTO">Auto Loan</option>
                      <option value="MORTGAGE">Home Mortgage</option>
                      <option value="STUDENT">Student Loan</option>
                      <option value="BUSINESS">Business Loan</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-2">Principal Amount</label>
                    <input
                      type="number"
                      step="0.01"
                      min="100"
                      value={applicationForm.principalAmount}
                      onChange={(e) => setApplicationForm(prev => ({ ...prev, principalAmount: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      placeholder="Enter loan amount"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-2">Interest Rate (%)</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0.1"
                      max="30"
                      value={applicationForm.interestRate}
                      onChange={(e) => setApplicationForm(prev => ({ ...prev, interestRate: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      placeholder="Enter interest rate"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-2">Term (Months)</label>
                    <input
                      type="number"
                      min="6"
                      max="360"
                      value={applicationForm.termMonths}
                      onChange={(e) => setApplicationForm(prev => ({ ...prev, termMonths: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      placeholder="Enter term in months"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-2">Payment Frequency</label>
                    <select
                      value={applicationForm.paymentFrequency}
                      onChange={(e) => setApplicationForm(prev => ({ ...prev, paymentFrequency: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      required
                    >
                      <option value="MONTHLY">Monthly</option>
                      <option value="QUARTERLY">Quarterly</option>
                      <option value="SEMI_ANNUALLY">Semi-Annually</option>
                      <option value="ANNUALLY">Annually</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-2">Disbursement Account (Optional)</label>
                    <select
                      value={applicationForm.disbursementAccountId}
                      onChange={(e) => setApplicationForm(prev => ({ ...prev, disbursementAccountId: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                    >
                      <option value="">Select account (optional)</option>
                      {accounts.map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.accountType} - {account.accountNum} ({formatCurrency(account.balance)})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-dark mb-2">Purpose of Loan</label>
                    <textarea
                      value={applicationForm.purpose}
                      onChange={(e) => setApplicationForm(prev => ({ ...prev, purpose: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      rows={3}
                      placeholder="Describe the purpose of this loan"
                      required
                    />
                  </div>
                </div>

                {/* Show estimated monthly payment */}
                {applicationForm.principalAmount && applicationForm.interestRate && applicationForm.termMonths && (
                  <div className="bg-accent-100 p-4 rounded-md">
                    <h4 className="font-medium text-dark mb-2">Loan Summary</h4>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <p className="text-neutral-600">Principal</p>
                        <p className="font-semibold">{formatCurrency(parseFloat(applicationForm.principalAmount))}</p>
                      </div>
                      <div>
                        <p className="text-neutral-600">Interest Rate</p>
                        <p className="font-semibold">{applicationForm.interestRate}%</p>
                      </div>
                      <div>
                        <p className="text-neutral-600">Term</p>
                        <p className="font-semibold">{applicationForm.termMonths} months</p>
                      </div>
                      <div>
                        <p className="text-neutral-600">Est. Monthly Payment</p>
                        <p className="font-semibold text-primary">
                          {formatCurrency(
                            calculateMonthlyPayment(
                              parseFloat(applicationForm.principalAmount),
                              parseFloat(applicationForm.interestRate) / 100, // Convert percentage to decimal for calculation
                              parseInt(applicationForm.termMonths)
                            )
                          )}
                        </p>
                      </div>
                    </div>
                  </div>
                )}

                <div className="flex space-x-4">
                  <Button type="submit" variant="primary" className="flex-1">
                    Submit Application
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setApplicationForm({
                      loanType: '',
                      principalAmount: '',
                      interestRate: '',
                      termMonths: '',
                      paymentFrequency: 'MONTHLY',
                      purpose: '',
                      disbursementAccountId: ''
                    })}
                    className="flex-1"
                  >
                    Clear Form
                  </Button>
                </div>
              </form>
            </CardBody>
          </Card>
        )}
      </main>
    </div>
  );
}