'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useAccounts } from '@/hooks/useAccounts';
import { useTransactions } from '@/hooks/useTransactions';
import { Button, Card, CardBody } from '@/components/ui';
import { formatCurrency, formatDate } from '@/lib/utils';
import Link from 'next/link';

export default function DashboardPage() {
  const { user, logout, isAuthenticated, isLoading } = useAuth();
  const { accounts, isLoading: accountsLoading, error: accountsError } = useAccounts(user?.id);
  const { transactions, isLoading: transactionsLoading, error: transactionsError } = useTransactions(undefined, user?.id);

  // Redirect to login if not authenticated
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      window.location.href = '/login';
    }
  }, [isAuthenticated, isLoading]);

  const handleLogout = () => {
    logout();
    window.location.href = '/';
  };

  // Helper function to determine if a transfer is internal (between user's own accounts)
  const isInternalTransfer = (transaction: any) => {
    // For transfers, check if both accounts belong to the current user
    // If we don't have full account details, we can check if the transaction
    // involves accounts that belong to the current user
    if (transaction.transactionType === 'TRANSFER') {
      // Method 1: Check if we have full account objects with user data
      if (transaction.fromAccount?.user?.id && transaction.toAccount?.user?.id) {
        return transaction.fromAccount.user.id === user?.id &&
               transaction.toAccount.user.id === user?.id;
      }

      // Method 2: Check if both account IDs belong to user's accounts
      if (transaction.fromAccount?.id && transaction.toAccount?.id) {
        const userAccountIds = accounts.map(acc => acc.id);
        return userAccountIds.includes(transaction.fromAccount.id) &&
               userAccountIds.includes(transaction.toAccount.id);
      }

      // Method 3: Fallback - for now assume transfers are internal
      // (This is a safe assumption for most banking UIs)
      return true;
    }

    return false;
  };

  // Helper function to get transaction display color and amount
  const getTransactionDisplay = (transaction: any) => {
    if (transaction.transactionType === 'DEPOSIT') {
      return {
        color: 'text-green-600',
        prefix: '+',
        amount: transaction.amount
      };
    } else if (isInternalTransfer(transaction)) {
      return {
        color: 'text-neutral-600',
        prefix: '',
        amount: transaction.amount
      };
    } else {
      return {
        color: 'text-red-600',
        prefix: '-',
        amount: transaction.amount
      };
    }
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

  // Calculate total balance across active accounts only
  const activeAccounts = accounts.filter(account => account.status === 'ACTIVE');
  const totalBalance = activeAccounts.reduce((sum, account) => sum + (account.balance || 0), 0);

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
                <Link href="/dashboard" className="text-primary font-medium">Dashboard</Link>
                <Link href="/accounts" className="text-neutral-600 hover:text-primary">Accounts</Link>
                <Link href="/transactions" className="text-neutral-600 hover:text-primary">Transactions</Link>
                <Link href="/billing" className="text-neutral-600 hover:text-primary">Billing</Link>
                <Link href="/loans" className="text-neutral-600 hover:text-primary">Loans</Link>
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
        {/* Welcome Section */}
        <div className="mb-8">
          <h2 className="text-3xl font-bold text-dark mb-2">
            Good {new Date().getHours() < 12 ? 'Morning' : new Date().getHours() < 17 ? 'Afternoon' : 'Evening'}!
          </h2>
          <p className="text-neutral-600">Here's what's happening with your accounts today.</p>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Total Balance */}
          <Card variant="balance" className="text-white">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-accent-100 text-sm font-medium">Total Balance</p>
                  <p className="text-3xl font-bold">
                    {accountsLoading ? '...' : formatCurrency(totalBalance)}
                  </p>
                </div>
                <div className="text-accent-100">
                  <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z"/>
                    <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          {/* Active Accounts */}
          <Card className="border-l-4 border-l-secondary">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Active Accounts</p>
                  <p className="text-2xl font-semibold text-dark">
                    {accountsLoading ? '...' : activeAccounts.length}
                  </p>
                </div>
                <div className="text-secondary">
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>

          {/* Recent Transactions */}
          <Card className="border-l-4 border-l-primary">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Recent Transactions</p>
                  <p className="text-2xl font-semibold text-dark">
                    {transactionsLoading ? '...' : transactions.slice(0, 5).length}
                  </p>
                </div>
                <div className="text-primary">
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
                  </svg>
                </div>
              </div>
            </CardBody>
          </Card>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Accounts Overview */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-semibold text-dark">Your Accounts</h3>
              <Link href="/accounts">
                <Button variant="outline" size="sm">View All</Button>
              </Link>
            </div>

            <div className="space-y-4">
              {accountsLoading && (
                <Card>
                  <CardBody className="text-center py-8">
                    <p className="text-neutral-600">Loading accounts...</p>
                  </CardBody>
                </Card>
              )}

              {accountsError && (
                <Card>
                  <CardBody className="text-center py-8">
                    <p className="text-red-600">Error loading accounts: {accountsError}</p>
                  </CardBody>
                </Card>
              )}

              {!accountsLoading && !accountsError && accounts.length === 0 && (
                <Card>
                  <CardBody className="text-center py-8">
                    <p className="text-neutral-600 mb-4">No accounts found</p>
                    <Button variant="primary">Open New Account</Button>
                  </CardBody>
                </Card>
              )}

              {accounts.slice(0, 3).map((account) => (
                <Card key={account.id} variant="account">
                  <CardBody>
                    <div className="flex items-center justify-between">
                      <div>
                        <h4 className="font-semibold text-dark">{account.accountName || 'Account'}</h4>
                        <p className="text-sm text-neutral-600">{account.accountType} • {account.accountNum}</p>
                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium mt-1 ${
                          account.status === 'ACTIVE' ? 'bg-green-100 text-green-800' :
                          account.status === 'PENDING_APPROVAL' ? 'bg-yellow-100 text-yellow-800' :
                          account.status === 'INACTIVE' ? 'bg-gray-100 text-gray-800' :
                          account.status === 'SUSPENDED' ? 'bg-red-100 text-red-800' :
                          account.status === 'FROZEN' ? 'bg-blue-100 text-blue-800' :
                          'bg-gray-100 text-gray-800'
                        }`}>
                          {account.status}
                        </span>
                      </div>
                      <div className="text-right">
                        <p className={`text-lg font-semibold ${account.status === 'ACTIVE' ? 'text-dark' : 'text-neutral-400'}`}>
                          {account.status === 'ACTIVE' ? formatCurrency(account.balance) : '---'}
                        </p>
                        <p className="text-sm text-neutral-600">{account.currency}</p>
                      </div>
                    </div>
                  </CardBody>
                </Card>
              ))}
            </div>
          </div>

          {/* Recent Transactions */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-semibold text-dark">Recent Activity</h3>
              <Link href="/transactions">
                <Button variant="outline" size="sm">View All</Button>
              </Link>
            </div>

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

                {!transactionsLoading && !transactionsError && transactions.length === 0 && (
                  <div className="text-center py-8">
                    <p className="text-neutral-600">No recent transactions</p>
                  </div>
                )}

                <div className="space-y-1">
                  {transactions.slice(0, 5).map((transaction) => (
                    <div key={transaction.id} className="transaction-item">
                      <div className="flex items-center">
                        <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center mr-3">
                          <svg className="w-4 h-4 text-primary" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
                          </svg>
                        </div>
                        <div className="flex-1">
                          <p className="font-medium text-dark">{transaction.description || transaction.transactionType}</p>
                          <p className="text-sm text-neutral-600">{formatDate(transaction.createdAt)}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        {(() => {
                          const display = getTransactionDisplay(transaction);
                          return (
                            <p className={`font-semibold ${display.color}`}>
                              {display.prefix}{formatCurrency(display.amount)}
                            </p>
                          );
                        })()}
                        <p className="text-sm text-neutral-600">{transaction.status}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardBody>
            </Card>
          </div>
        </div>

        {/* Notifications & Alerts */}
        <div className="mt-8">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-xl font-semibold text-dark">Notifications & Alerts</h3>
            <Button variant="ghost" size="sm">
              <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
              </svg>
              Mark All Read
            </Button>
          </div>

          <div className="space-y-3">
            {/* Low Balance Alert */}
            {accounts.some(account => account.balance < 100) && (
              <Card className="border-l-4 border-l-amber-500">
                <CardBody className="py-3">
                  <div className="flex items-start">
                    <div className="text-amber-500 mr-3 mt-0.5">
                      <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                      </svg>
                    </div>
                    <div className="flex-1">
                      <h4 className="font-medium text-dark">Low Balance Warning</h4>
                      <p className="text-sm text-neutral-600">
                        {accounts.filter(account => account.balance < 100).length} of your accounts have low balances.
                        <Link href="/accounts" className="text-primary hover:underline ml-1">Review accounts</Link>
                      </p>
                      <p className="text-xs text-neutral-500 mt-1">Just now</p>
                    </div>
                  </div>
                </CardBody>
              </Card>
            )}

            {/* Recent Transaction Alert */}
            {transactions.length > 0 && transactions.slice(0, 1).map(transaction => (
              <Card key={transaction.id} className="border-l-4 border-l-primary">
                <CardBody className="py-3">
                  <div className="flex items-start">
                    <div className="text-primary mr-3 mt-0.5">
                      <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd"/>
                      </svg>
                    </div>
                    <div className="flex-1">
                      <h4 className="font-medium text-dark">Recent Transaction</h4>
                      <p className="text-sm text-neutral-600">
                        {transaction.transactionType} of {formatCurrency(transaction.amount)} was processed.
                        <Link href="/transactions" className="text-primary hover:underline ml-1">View details</Link>
                      </p>
                      <p className="text-xs text-neutral-500 mt-1">
                        {(() => {
                          try {
                            return transaction.transactionDate ? formatDate(transaction.transactionDate) : 'Recently';
                          } catch (error) {
                            return 'Recently';
                          }
                        })()}
                      </p>
                    </div>
                  </div>
                </CardBody>
              </Card>
            ))}

            {/* Security Notice */}
            <Card className="border-l-4 border-l-green-500">
              <CardBody className="py-3">
                <div className="flex items-start">
                  <div className="text-green-500 mr-3 mt-0.5">
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd"/>
                    </svg>
                  </div>
                  <div className="flex-1">
                    <h4 className="font-medium text-dark">Account Security</h4>
                    <p className="text-sm text-neutral-600">
                      Your account is secure. Last login: {new Date().toLocaleDateString()} at {new Date().toLocaleTimeString().split(' ')[0]}.
                    </p>
                    <p className="text-xs text-neutral-500 mt-1">2 hours ago</p>
                  </div>
                </div>
              </CardBody>
            </Card>

            {/* Banking Tips */}
            <Card className="border-l-4 border-l-blue-500">
              <CardBody className="py-3">
                <div className="flex items-start">
                  <div className="text-blue-500 mr-3 mt-0.5">
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd"/>
                    </svg>
                  </div>
                  <div className="flex-1">
                    <h4 className="font-medium text-dark">Banking Tip</h4>
                    <p className="text-sm text-neutral-600">
                      Set up automatic savings transfers to reach your financial goals faster.
                      <Link href="/accounts" className="text-primary hover:underline ml-1">Learn more</Link>
                    </p>
                    <p className="text-xs text-neutral-500 mt-1">1 day ago</p>
                  </div>
                </div>
              </CardBody>
            </Card>

            {/* Empty State when no alerts */}
            {accounts.every(account => account.balance >= 100) && transactions.length === 0 && (
              <Card>
                <CardBody className="text-center py-8">
                  <div className="text-neutral-400 mb-2">
                    <svg className="w-12 h-12 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd"/>
                    </svg>
                  </div>
                  <h4 className="font-medium text-neutral-600 mb-1">You're all caught up!</h4>
                  <p className="text-sm text-neutral-500">No new notifications or alerts at this time.</p>
                </CardBody>
              </Card>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}