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

  // Calculate total balance across all accounts
  const totalBalance = accounts.reduce((sum, account) => sum + (account.balance || 0), 0);

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
                    {accountsLoading ? '...' : accounts.length}
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
          <Card className="border-l-4 border-l-accent-600">
            <CardBody>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-neutral-600 text-sm font-medium">Recent Transactions</p>
                  <p className="text-2xl font-semibold text-dark">
                    {transactionsLoading ? '...' : transactions.slice(0, 5).length}
                  </p>
                </div>
                <div className="text-accent-600">
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
                      </div>
                      <div className="text-right">
                        <p className="text-lg font-semibold text-dark">
                          {formatCurrency(account.balance)}
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
                        <p className={`font-semibold ${transaction.transactionType === 'DEPOSIT' ? 'text-green-600' : 'text-red-600'}`}>
                          {transaction.transactionType === 'DEPOSIT' ? '+' : '-'}{formatCurrency(transaction.amount)}
                        </p>
                        <p className="text-sm text-neutral-600">{transaction.status}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardBody>
            </Card>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="mt-8">
          <h3 className="text-xl font-semibold text-dark mb-4">Quick Actions</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <Link href="/transfer">
              <Card variant="hover" className="text-center">
                <CardBody>
                  <div className="text-primary mb-2">
                    <svg className="w-8 h-8 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
                    </svg>
                  </div>
                  <p className="text-sm font-medium text-dark">Send Money</p>
                </CardBody>
              </Card>
            </Link>

            <Link href="/deposit">
              <Card variant="hover" className="text-center">
                <CardBody>
                  <div className="text-secondary mb-2">
                    <svg className="w-8 h-8 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-11a1 1 0 10-2 0v2H7a1 1 0 100 2h2v2a1 1 0 102 0v-2h2a1 1 0 100-2h-2V7z" clipRule="evenodd"/>
                    </svg>
                  </div>
                  <p className="text-sm font-medium text-dark">Deposit</p>
                </CardBody>
              </Card>
            </Link>

            <Link href="/bills">
              <Card variant="hover" className="text-center">
                <CardBody>
                  <div className="text-accent-600 mb-2">
                    <svg className="w-8 h-8 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z"/>
                      <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd"/>
                    </svg>
                  </div>
                  <p className="text-sm font-medium text-dark">Pay Bills</p>
                </CardBody>
              </Card>
            </Link>

            <Link href="/support">
              <Card variant="hover" className="text-center">
                <CardBody>
                  <div className="text-neutral-600 mb-2">
                    <svg className="w-8 h-8 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd"/>
                    </svg>
                  </div>
                  <p className="text-sm font-medium text-dark">Get Help</p>
                </CardBody>
              </Card>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}