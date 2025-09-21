'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useTransactions } from '@/hooks/useTransactions';
import { useAccounts } from '@/hooks/useAccounts';
import { Button, Card, CardBody } from '@/components/ui';
import { formatCurrency, formatDate } from '@/lib/utils';
import { apiClient } from '@/lib/api/client';
import Link from 'next/link';

export default function AdminDashboard() {
  const { user, logout } = useAuth();
  const { transactions, isLoading: transactionsLoading, error: transactionsError, refetch: refetchTransactions } = useTransactions(undefined, undefined);
  const { accounts, isLoading: accountsLoading } = useAccounts(undefined);

  const [audits, setAudits] = useState<any[]>([]);
  const [failedOps, setFailedOps] = useState<any[]>([]);
  const [highRiskActivities, setHighRiskActivities] = useState<any[]>([]);
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

  const pendingTransactions = transactions.filter(t => t.status === 'PENDING');
  const pendingAccounts = accounts.filter(a => a.status === 'PENDING_APPROVAL');
  const totalBalance = accounts.reduce((sum, account) => sum + (account.balance || 0), 0);

  useEffect(() => {
    const loadAuditData = async () => {
      try {
        // Load recent audits (this one works)
        const auditsRes = await apiClient.getRecentAudits();
        if (auditsRes.success) setAudits(auditsRes.data || []);

        // Try to load failed operations, but don't fail if it doesn't work
        try {
          const failedRes = await apiClient.getFailedOperations();
          if (failedRes.success) setFailedOps(failedRes.data || []);
        } catch (error) {
          console.log('Failed operations endpoint not available');
          setFailedOps([]);
        }

        // Try to load high risk activities, but don't fail if it doesn't work
        try {
          const riskRes = await apiClient.getHighRiskActivities();
          if (riskRes.success) setHighRiskActivities(riskRes.data || []);
        } catch (error) {
          console.log('High risk activities endpoint not available');
          setHighRiskActivities([]);
        }
      } catch (error) {
        console.error('Error loading audit data:', error);
      }
    };

    loadAuditData();
  }, []);

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
        <div className="grid grid-cols-1 md:grid-cols-5 gap-6 mb-8">
          <Card className="border-l-4 border-l-red-500">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Pending Transactions</p>
                  <p className="text-2xl font-semibold text-dark">
                    {transactionsLoading ? '...' : pendingTransactions.length}
                  </p>
                </div>
                <div className="text-red-500">
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-yellow-500">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Pending Accounts</p>
                  <p className="text-2xl font-semibold text-dark">
                    {accountsLoading ? '...' : pendingAccounts.length}
                  </p>
                </div>
                <div className="text-yellow-500">
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-secondary">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Total System Balance</p>
                  <p className="text-2xl font-semibold text-dark">
                    {accountsLoading ? '...' : formatCurrency(totalBalance)}
                  </p>
                </div>
                <div className="text-secondary">
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z"/>
                    <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-accent-600">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Failed Operations</p>
                  <p className="text-2xl font-semibold text-dark">{failedOps.length}</p>
                </div>
                <div className="text-accent-600">
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          <Card className="border-l-4 border-l-red-600">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">High Risk Activities</p>
                  <p className="text-2xl font-semibold text-dark">{highRiskActivities.length}</p>
                </div>
                <div className="text-red-600">
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M3 6a3 3 0 013-3h10a1 1 0 01.8 1.6L14.25 8l2.55 3.4A1 1 0 0116 13H6a1 1 0 00-1 1v3a1 1 0 11-2 0V6z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Pending Transactions */}
          <div>
            <h3 className="text-xl font-semibold text-dark mb-4">Pending Transaction Approvals</h3>
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

                {!transactionsLoading && !transactionsError && pendingTransactions.length === 0 && (
                  <div className="text-center py-8">
                    <p className="text-neutral-600">No pending transactions</p>
                  </div>
                )}

                <div className="space-y-4">
                  {pendingTransactions.map((transaction) => (
                    <div key={transaction.id} className="border border-accent-300 rounded-lg p-4">
                      <div className="flex justify-between items-start mb-3">
                        <div>
                          <h4 className="font-semibold text-dark">{transaction.transactionType}</h4>
                          <p className="text-sm text-neutral-600">{transaction.transactionReference}</p>
                          <p className="text-sm text-neutral-600">{formatDate(transaction.createdAt)}</p>
                        </div>
                        <div className="text-right">
                          <p className="text-lg font-semibold text-dark">{formatCurrency(transaction.amount)}</p>
                          <p className="text-sm text-neutral-600">{transaction.currency}</p>
                        </div>
                      </div>
                      {transaction.description && (
                        <p className="text-sm text-neutral-600 mb-3">{transaction.description}</p>
                      )}
                      <div className="flex space-x-2">
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleProcessTransaction(transaction.id)}
                          disabled={isProcessing[transaction.id]}
                        >
                          {isProcessing[transaction.id] ? 'Processing...' : 'Approve'}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleCancelTransaction(transaction.id)}
                          disabled={isProcessing[transaction.id]}
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
            <h3 className="text-xl font-semibold text-dark mb-4">Pending Account Approvals</h3>
            <Card>
              <CardBody>
                {accountsLoading && (
                  <div className="text-center py-8">
                    <p className="text-neutral-600">Loading accounts...</p>
                  </div>
                )}

                {!accountsLoading && pendingAccounts.length === 0 && (
                  <div className="text-center py-8">
                    <p className="text-neutral-600">No pending accounts</p>
                  </div>
                )}

                <div className="space-y-4">
                  {pendingAccounts.map((account) => (
                    <div key={account.id} className="border border-accent-300 rounded-lg p-4">
                      <div className="flex justify-between items-start mb-3">
                        <div>
                          <h4 className="font-semibold text-dark">{account.accountName}</h4>
                          <p className="text-sm text-neutral-600">{account.accountType} Account</p>
                          <p className="text-sm text-neutral-600">#{account.accountNum}</p>
                          <p className="text-sm text-neutral-500">Requested {formatDate(account.createdAt)}</p>
                        </div>
                        <div className="text-right">
                          <p className="text-lg font-semibold text-dark">
                            {formatCurrency(account.balance || 0)}
                          </p>
                          <p className="text-sm text-neutral-600">{account.currency}</p>
                        </div>
                      </div>
                      <div className="text-sm text-neutral-600 mb-3">
                        <p>Requested by: {account.user?.firstName} {account.user?.lastName}</p>
                        <p>Email: {account.user?.email}</p>
                      </div>
                      <div className="flex space-x-2">
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleApproveAccount(account.id)}
                          disabled={isProcessing[account.id]}
                        >
                          {isProcessing[account.id] ? 'Processing...' : 'Approve'}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleRejectAccount(account.id)}
                          disabled={isProcessing[account.id]}
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

          {/* Recent System Activity */}
          <div>
            <h3 className="text-xl font-semibold text-dark mb-4">Recent System Activity</h3>
            <Card>
              <CardBody>
                <div className="space-y-3">
                  {audits.slice(0, 8).map((audit) => (
                    <div key={audit.id} className="flex items-center justify-between py-2 border-b border-accent-200 last:border-b-0">
                      <div>
                        <p className="font-medium text-dark">{audit.action}</p>
                        <p className="text-sm text-neutral-600">User: {audit.username || 'System'}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-sm text-neutral-600">{formatDate(audit.timestamp)}</p>
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