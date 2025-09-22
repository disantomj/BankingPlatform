'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useTransactions } from '@/hooks/useTransactions';
import { useAccounts } from '@/hooks/useAccounts';
import { useApi } from '@/hooks/useApi';
import { Button, Card, CardBody } from '@/components/ui';
import { formatCurrency, formatDate } from '@/lib/utils';
import { apiClient } from '@/lib/api/client';
import Link from 'next/link';

export default function AdminDashboard() {
  const { user, logout } = useAuth();
  const { transactions, isLoading: transactionsLoading, error: transactionsError, refetch: refetchTransactions } = useTransactions(undefined, undefined);
  const { accounts, isLoading: accountsLoading } = useAccounts(undefined);

  const { data: audits = [], isLoading: auditsLoading } = useApi(() => apiClient.getRecentAudits());
  const { data: failedOps = [], isLoading: failedOpsLoading } = useApi(() => apiClient.getFailedOperations(), { immediate: false });
  const { data: highRiskActivities = [], isLoading: riskLoading } = useApi(() => apiClient.getHighRiskActivities(), { immediate: false });
  const { data: loans = [], isLoading: loansLoading, refetch: refetchLoans } = useApi(() => apiClient.getLoansByStatus('PENDING'));
  // For now, just focus on GENERATING reports since that's what we're testing
  const { data: allReports = [], isLoading: reportsLoading, refetch: refetchReports } = useApi(() => apiClient.getReportsByStatus('GENERATING'));
  const [isProcessing, setIsProcessing] = useState<{ [key: number]: boolean }>({});

  // Check if user is admin
  if (user && user.role !== 'ADMIN') {
    return (
      <div className="min-h-screen bg-accent-200 flex items-center justify-center">
        <Card>
          <CardBody className="text-center">
            <h1 className="text-2xl font-bold text-dark mb-4">Access Denied</h1>
            <p className="text-neutral-600 mb-4">You need admin privileges to access this page.</p>
            <Link href="/dashboard">
              <Button variant="primary">Back to Dashboard</Button>
            </Link>
          </CardBody>
        </Card>
      </div>
    );
  }

  const handleLogout = () => {
    logout();
    window.location.href = '/';
  };

  const pendingTransactions = (transactions || []).filter(t => t.status === 'PENDING');
  const pendingAccounts = (accounts || []).filter(a => a.status === 'PENDING_APPROVAL');
  const pendingLoans = (loans || []).filter(l => l.status === 'PENDING');
  const totalBalance = (accounts || []).reduce((sum, account) => sum + (account.balance || 0), 0);


  const handleProcessTransaction = async (transactionId: number) => {
    setIsProcessing(prev => ({ ...prev, [transactionId]: true }));
    try {
      const response = await apiClient.processTransaction(transactionId);
      if (response.success) {
        await refetchTransactions();
      } else {
        alert('Failed to process transaction: ' + response.error);
      }
    } catch (error) {
      alert('Error processing transaction');
    } finally {
      setIsProcessing(prev => ({ ...prev, [transactionId]: false }));
    }
  };

  const handleCancelTransaction = async (transactionId: number) => {
    setIsProcessing(prev => ({ ...prev, [transactionId]: true }));
    try {
      const response = await apiClient.cancelTransaction(transactionId);
      if (response.success) {
        await refetchTransactions();
      } else {
        alert('Failed to cancel transaction: ' + response.error);
      }
    } catch (error) {
      alert('Error canceling transaction');
    } finally {
      setIsProcessing(prev => ({ ...prev, [transactionId]: false }));
    }
  };

  const handleApproveAccount = async (accountId: number) => {
    setIsProcessing(prev => ({ ...prev, [accountId]: true }));
    try {
      const response = await apiClient.approveAccount(accountId);
      if (response.success) {
        // Refresh accounts data - we'll need to add this to useAccounts
        window.location.reload(); // Temporary solution
      } else {
        alert('Failed to approve account: ' + response.error);
      }
    } catch (error) {
      alert('Error approving account');
    } finally {
      setIsProcessing(prev => ({ ...prev, [accountId]: false }));
    }
  };

  const handleRejectAccount = async (accountId: number) => {
    setIsProcessing(prev => ({ ...prev, [accountId]: true }));
    try {
      const response = await apiClient.rejectAccount(accountId);
      if (response.success) {
        // Refresh accounts data - we'll need to add this to useAccounts
        window.location.reload(); // Temporary solution
      } else {
        alert('Failed to reject account: ' + response.error);
      }
    } catch (error) {
      alert('Error rejecting account');
    } finally {
      setIsProcessing(prev => ({ ...prev, [accountId]: false }));
    }
  };

  const handleApproveLoan = async (loanId: number) => {
    setIsProcessing(prev => ({ ...prev, [loanId]: true }));
    try {
      const response = await apiClient.approveLoan(loanId, user?.username || 'Admin');
      if (response.success) {
        // Refresh loans data
        await refetchLoans();
        alert('Loan approved successfully!');
      } else {
        alert('Failed to approve loan: ' + response.error);
      }
    } catch (error) {
      alert('Error approving loan');
    } finally {
      setIsProcessing(prev => ({ ...prev, [loanId]: false }));
    }
  };

  const handleRejectLoan = async (loanId: number) => {
    setIsProcessing(prev => ({ ...prev, [loanId]: true }));
    try {
      const response = await apiClient.rejectLoan(loanId, user?.username || 'Admin');
      if (response.success) {
        // Refresh loans data
        await refetchLoans();
        alert('Loan rejected successfully!');
      } else {
        alert('Failed to reject loan: ' + response.error);
      }
    } catch (error) {
      alert('Error rejecting loan');
    } finally {
      setIsProcessing(prev => ({ ...prev, [loanId]: false }));
    }
  };

  const handleDisburseLoan = async (loanId: number) => {
    setIsProcessing(prev => ({ ...prev, [loanId]: true }));
    try {
      const response = await apiClient.disburseLoan(loanId, user?.username || 'Admin');
      if (response.success) {
        // Refresh loans data
        await refetchLoans();
        alert('Loan disbursed successfully!');
      } else {
        alert('Failed to disburse loan: ' + response.error);
      }
    } catch (error) {
      alert('Error disbursing loan');
    } finally {
      setIsProcessing(prev => ({ ...prev, [loanId]: false }));
    }
  };

  const handleCompleteReport = async (reportId: number) => {
    setIsProcessing(prev => ({ ...prev, [reportId]: true }));
    try {
      const response = await apiClient.completeReportGeneration(reportId, {
        filePath: `/reports/report_${reportId}.pdf`,
        fileName: `report_${reportId}.pdf`,
        fileSizeBytes: 1024000,
        contentType: 'application/pdf',
        recordCount: 100
      });
      if (response.success) {
        await refetchReports();
        alert('Report completed successfully!');
      } else {
        alert('Failed to complete report: ' + response.error);
      }
    } catch (error) {
      alert('Error completing report');
    } finally {
      setIsProcessing(prev => ({ ...prev, [reportId]: false }));
    }
  };

  const handleFailReport = async (reportId: number, errorMessage: string) => {
    setIsProcessing(prev => ({ ...prev, [reportId]: true }));
    try {
      const response = await apiClient.failReportGeneration(reportId, errorMessage);
      if (response.success) {
        await refetchReports();
        alert('Report marked as failed!');
      } else {
        alert('Failed to mark report as failed: ' + response.error);
      }
    } catch (error) {
      alert('Error failing report');
    } finally {
      setIsProcessing(prev => ({ ...prev, [reportId]: false }));
    }
  };

  const handleStartReport = async (reportId: number) => {
    setIsProcessing(prev => ({ ...prev, [reportId]: true }));
    try {
      const response = await apiClient.startReportGeneration(reportId, user?.username || 'Admin');
      if (response.success) {
        await refetchReports();
        alert('Report generation started!');
      } else {
        alert('Failed to start report: ' + response.error);
      }
    } catch (error) {
      alert('Error starting report');
    } finally {
      setIsProcessing(prev => ({ ...prev, [reportId]: false }));
    }
  };

  const handleCancelReport = async (reportId: number) => {
    setIsProcessing(prev => ({ ...prev, [reportId]: true }));
    try {
      const response = await apiClient.cancelReport(reportId, user?.username || 'Admin');
      if (response.success) {
        await refetchReports();
        alert('Report cancelled successfully!');
      } else {
        alert('Failed to cancel report: ' + response.error);
      }
    } catch (error) {
      alert('Error cancelling report');
    } finally {
      setIsProcessing(prev => ({ ...prev, [reportId]: false }));
    }
  };

  return (
    <div className="min-h-screen bg-accent-200">
      {/* Admin Header */}
      <header className="bg-primary shadow-sm border-b border-accent-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-8">
              <Link href="/admin" className="flex items-center">
                <h1 className="text-2xl font-bold text-white">SecureBank Admin</h1>
              </Link>
              <nav className="hidden md:flex space-x-6">
                <Link href="/admin" className="text-white font-medium">Dashboard</Link>
                <Link href="/dashboard" className="text-accent-100 hover:text-white">User View</Link>
              </nav>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-accent-100">Admin: {user?.username || 'User'}</span>
              <Button variant="ghost" size="sm" onClick={handleLogout} className="text-white hover:text-accent-100">
                Logout
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Admin Stats */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-6 mb-8">
          <Card className="border-l-4 border-l-amber-500">
            <CardBody className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0 pr-2">
                  <p className="text-neutral-600 text-xs font-medium">Pending Transactions</p>
                  <p className="text-lg font-semibold text-dark">
                    {transactionsLoading ? '...' : pendingTransactions.length}
                  </p>
                </div>
                <div className="text-amber-500 flex-shrink-0">
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-amber-500">
            <CardBody className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0 pr-2">
                  <p className="text-neutral-600 text-xs font-medium">Pending Accounts</p>
                  <p className="text-lg font-semibold text-dark">
                    {accountsLoading ? '...' : pendingAccounts.length}
                  </p>
                </div>
                <div className="text-amber-500 flex-shrink-0">
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-amber-500">
            <CardBody className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0 pr-2">
                  <p className="text-neutral-600 text-xs font-medium">Pending Loans</p>
                  <p className="text-lg font-semibold text-dark">{(pendingLoans || []).length}</p>
                </div>
                <div className="text-amber-500 flex-shrink-0">
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M4 4a2 2 0 00-2 2v8a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2H4zm0 2h12v2H4V6zm0 4h12v4H4v-4z"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-primary">
            <CardBody className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0 pr-2">
                  <p className="text-neutral-600 text-xs font-medium">Total System Balance</p>
                  <p className="text-lg font-semibold text-dark truncate">
                    {accountsLoading ? '...' : formatCurrency(totalBalance)}
                  </p>
                </div>
                <div className="text-primary flex-shrink-0">
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z"/>
                    <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-red-500">
            <CardBody className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0 pr-2">
                  <p className="text-neutral-600 text-xs font-medium">Failed Operations</p>
                  <p className="text-lg font-semibold text-dark">{(failedOps || []).length}</p>
                </div>
                <div className="text-red-500 flex-shrink-0">
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-red-500">
            <CardBody className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0 pr-2">
                  <p className="text-neutral-600 text-xs font-medium">High Risk Activities</p>
                  <p className="text-lg font-semibold text-dark">{(highRiskActivities || []).length}</p>
                </div>
                <div className="text-red-500 flex-shrink-0">
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M3 6a3 3 0 013-3h10a1 1 0 01.8 1.6L14.25 8l2.55 3.4A1 1 0 0116 13H6a1 1 0 00-1 1v3a1 1 0 11-2 0V6z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-4 gap-8">
          {/* Pending Transactions */}
          <div>
            <h3 className="text-xl font-semibold text-dark mb-4">Pending Transactions</h3>
            <Card>
              <CardBody>
                {transactionsLoading && (
                  <div className="text-center py-8">
                    <p className="text-neutral-600">Loading transactions...</p>
                  </div>
                )}

                {transactionsError && (
                  <div className="text-center py-8">
                    <p className="text-red-600">Error loading transactions: {transactionsError}</p>
                  </div>
                )}

                {!transactionsLoading && !transactionsError && (pendingTransactions || []).length === 0 && (
                  <div className="text-center py-8">
                    <p className="text-neutral-600">No pending transactions</p>
                  </div>
                )}

                <div className="space-y-4">
                  {pendingTransactions.map((transaction) => (
                    <div key={transaction.id} className="border border-accent-300 rounded-lg p-4">
                      <div className="flex justify-between items-start mb-3">
                        <div className="flex-1 min-w-0 pr-4">
                          <h4 className="font-semibold text-dark text-sm leading-tight">{transaction.transactionType}</h4>
                          <p className="text-xs text-neutral-600 truncate">{transaction.transactionReference}</p>
                          <p className="text-xs text-neutral-600">{formatDate(transaction.createdAt)}</p>
                        </div>
                        <div className="text-right flex-shrink-0">
                          <p className="text-base font-semibold text-dark whitespace-nowrap">{formatCurrency(transaction.amount)}</p>
                          <p className="text-xs text-neutral-600">{transaction.currency}</p>
                        </div>
                      </div>
                      {transaction.description && (
                        <p className="text-xs text-neutral-600 mb-3 break-words">{transaction.description}</p>
                      )}
                      <div className="flex flex-wrap gap-2">
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleProcessTransaction(transaction.id)}
                          disabled={isProcessing[transaction.id]}
                          className="text-xs px-3 py-1"
                        >
                          {isProcessing[transaction.id] ? 'Processing...' : 'Approve'}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleCancelTransaction(transaction.id)}
                          disabled={isProcessing[transaction.id]}
                          className="text-xs px-3 py-1"
                        >
                          {isProcessing[transaction.id] ? 'Processing...' : 'Reject'}
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </CardBody>
            </Card>
          </div>

          {/* Pending Account Approvals */}
          <div>
            <h3 className="text-xl font-semibold text-dark mb-4">Pending Accounts</h3>
            <Card>
              <CardBody>
                {accountsLoading && (
                  <div className="text-center py-8">
                    <p className="text-neutral-600">Loading accounts...</p>
                  </div>
                )}

                {!accountsLoading && (pendingAccounts || []).length === 0 && (
                  <div className="text-center py-8">
                    <p className="text-neutral-600">No pending accounts</p>
                  </div>
                )}

                <div className="space-y-4">
                  {pendingAccounts.map((account) => (
                    <div key={account.id} className="border border-accent-300 rounded-lg p-4">
                      <div className="flex justify-between items-start mb-3">
                        <div className="flex-1 min-w-0 pr-4">
                          <h4 className="font-semibold text-dark text-sm leading-tight">{account.accountName}</h4>
                          <p className="text-xs text-neutral-600">{account.accountType} Account</p>
                          <p className="text-xs text-neutral-600 truncate">#{account.accountNum}</p>
                          <p className="text-xs text-neutral-500">Requested {formatDate(account.createdAt)}</p>
                        </div>
                        <div className="text-right flex-shrink-0">
                          <p className="text-base font-semibold text-dark whitespace-nowrap">
                            {formatCurrency(account.balance || 0)}
                          </p>
                          <p className="text-xs text-neutral-600">{account.currency}</p>
                        </div>
                      </div>
                      <div className="text-xs text-neutral-600 mb-3">
                        <p className="truncate">Requested by: {account.user?.firstName} {account.user?.lastName}</p>
                        <p className="truncate">Email: {account.user?.email}</p>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleApproveAccount(account.id)}
                          disabled={isProcessing[account.id]}
                          className="text-xs px-3 py-1"
                        >
                          {isProcessing[account.id] ? 'Processing...' : 'Approve'}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleRejectAccount(account.id)}
                          disabled={isProcessing[account.id]}
                          className="text-xs px-3 py-1"
                        >
                          {isProcessing[account.id] ? 'Processing...' : 'Reject'}
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </CardBody>
            </Card>
          </div>

          {/* Pending Loan Applications */}
          <div>
            <h3 className="text-xl font-semibold text-dark mb-4">Pending Loans</h3>
            <Card>
              <CardBody>
                {(pendingLoans || []).length === 0 ? (
                  <div className="text-center py-8">
                    <p className="text-neutral-600">No pending loan applications</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {pendingLoans.map((loan) => (
                      <div key={loan.id} className="border border-accent-300 rounded-lg p-4">
                        <div className="flex justify-between items-start mb-3">
                          <div className="flex-1 min-w-0 pr-4">
                            <h4 className="font-semibold text-dark text-sm leading-tight">
                              {loan.loanType?.replace(/_/g, ' ')?.replace(/\b\w/g, (l: string) => l.toUpperCase()) || 'Loan Application'}
                            </h4>
                            <p className="text-xs text-neutral-600">ID: {loan.id}</p>
                            <p className="text-xs text-neutral-600">Term: {loan.termMonths} months</p>
                            <p className="text-xs text-neutral-600">Rate: {loan.interestRate}%</p>
                            <p className="text-xs text-neutral-500">Applied {formatDate(loan.createdAt)}</p>
                          </div>
                          <div className="text-right flex-shrink-0">
                            <p className="text-base font-semibold text-dark whitespace-nowrap">{formatCurrency(loan.principalAmount)}</p>
                            <p className="text-xs text-neutral-600">Principal Amount</p>
                          </div>
                        </div>

                        <div className="text-xs text-neutral-600 mb-3">
                          <p className="font-medium text-sm">Purpose:</p>
                          <p className="break-words">{loan.purpose}</p>
                        </div>

                        <div className="text-xs text-neutral-600 mb-3">
                          <p className="truncate">Applicant: {loan.user?.firstName} {loan.user?.lastName}</p>
                          <p className="truncate">Email: {loan.user?.email}</p>
                          {loan.disbursementAccount && (
                            <p className="truncate">Account: {loan.disbursementAccount.accountNumber}</p>
                          )}
                        </div>

                        <div className="flex flex-wrap gap-2">
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleApproveLoan(loan.id)}
                            disabled={isProcessing[loan.id]}
                            className="text-xs px-3 py-1"
                          >
                            {isProcessing[loan.id] ? 'Processing...' : 'Approve'}
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleRejectLoan(loan.id)}
                            disabled={isProcessing[loan.id]}
                            className="text-xs px-3 py-1"
                          >
                            {isProcessing[loan.id] ? 'Processing...' : 'Reject'}
                          </Button>
                          {loan.status === 'APPROVED' && (
                            <Button
                              variant="secondary"
                              size="sm"
                              onClick={() => handleDisburseLoan(loan.id)}
                              disabled={isProcessing[loan.id]}
                              className="text-xs px-3 py-1"
                            >
                              {isProcessing[loan.id] ? 'Processing...' : 'Disburse'}
                            </Button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardBody>
            </Card>
          </div>


          {/* Recent System Activity */}
          <div>
            <h3 className="text-xl font-semibold text-dark mb-4">Recent Activity</h3>
            <Card>
              <CardBody>
                <div className="space-y-2">
                  {(audits || []).slice(0, 8).map((audit) => (
                    <div key={audit.id} className="flex items-start justify-between py-2 border-b border-accent-200 last:border-b-0">
                      <div className="flex-1 min-w-0 pr-3">
                        <p className="font-medium text-dark text-sm leading-tight truncate">{audit.action}</p>
                        <p className="text-xs text-neutral-600 truncate">User: {audit.username || 'System'}</p>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <p className="text-xs text-neutral-600 whitespace-nowrap">{formatDate(audit.timestamp)}</p>
                        <p className={`text-xs ${audit.success ? 'text-green-600' : 'text-red-600'}`}>
                          {audit.success ? 'Success' : 'Failed'}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardBody>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
}