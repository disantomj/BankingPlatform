'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useAccounts } from '@/hooks/useAccounts';
import { Button, Card, CardBody } from '@/components/ui';
import { formatCurrency, formatDate } from '@/lib/utils';
import { apiClient } from '@/lib/api/client';
import { AccountType } from '@/types';
import Link from 'next/link';

export default function AccountsPage() {
  const { user, logout, isAuthenticated, isLoading } = useAuth();
  const { accounts, isLoading: accountsLoading, error: accountsError, refetch: refetchAccounts } = useAccounts(user?.id);

  // Redirect to login if not authenticated
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      window.location.href = '/login';
    }
  }, [isAuthenticated, isLoading]);

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [createForm, setCreateForm] = useState({
    accountType: 'CHECKING' as AccountType,
    accountName: '',
    initialBalance: '',
    currency: 'USD'
  });
  const [createError, setCreateError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

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

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) return;

    setIsCreating(true);
    setCreateError('');
    setSuccessMessage('');

    try {
      const response = await apiClient.createAccount({
        userId: user.id,
        accountType: createForm.accountType,
        accountName: createForm.accountName,
        initialBalance: parseFloat(createForm.initialBalance) || 0,
        currency: createForm.currency
      });

      if (response.success) {
        setSuccessMessage('Account created successfully!');
        setCreateForm({
          accountType: 'CHECKING' as AccountType,
          accountName: '',
          initialBalance: '',
          currency: 'USD'
        });
        setShowCreateForm(false);
        await refetchAccounts();
      } else {
        setCreateError(response.error || 'Failed to create account');
      }
    } catch (error) {
      setCreateError('An error occurred while creating the account');
    } finally {
      setIsCreating(false);
    }
  };

  const getAccountTypeDisplay = (type: AccountType) => {
    switch (type) {
      case AccountType.CHECKING: return 'Checking Account';
      case AccountType.SAVINGS: return 'Savings Account';
      case AccountType.CREDIT: return 'Credit Account';
      case AccountType.INVESTMENT: return 'Investment Account';
      default: return type;
    }
  };

  const getAccountIcon = (type: AccountType) => {
    switch (type) {
      case AccountType.CHECKING:
        return (
          <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
            <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z"/>
            <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd"/>
          </svg>
        );
      case AccountType.SAVINGS:
        return (
          <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M3 3a1 1 0 000 2v8a2 2 0 002 2h2.586l-1.293 1.293a1 1 0 101.414 1.414L10 15.414l2.293 2.293a1 1 0 001.414-1.414L12.414 15H15a2 2 0 002-2V5a1 1 0 100-2H3zm11.707 4.707a1 1 0 00-1.414-1.414L10 9.586 8.707 8.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd"/>
          </svg>
        );
      case AccountType.CREDIT:
        return (
          <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
            <path d="M4 4a2 2 0 00-2 2v8a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2H4zm0 2h12v2H4V6zm0 4h12v4H4v-4z"/>
          </svg>
        );
      case AccountType.INVESTMENT:
        return (
          <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M3 3a1 1 0 000 2v8a2 2 0 002 2h2.586l-1.293 1.293a1 1 0 101.414 1.414L10 15.414l2.293 2.293a1 1 0 001.414-1.414L12.414 15H15a2 2 0 002-2V5a1 1 0 100-2H3zm11 4a1 1 0 10-2 0v4a1 1 0 102 0V7zm-3 1a1 1 0 10-2 0v3a1 1 0 102 0V8zM8 9a1 1 0 00-2 0v2a1 1 0 102 0V9z" clipRule="evenodd"/>
          </svg>
        );
      default:
        return (
          <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
            <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"/>
          </svg>
        );
    }
  };

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
                <Link href="/accounts" className="text-primary font-medium">Accounts</Link>
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
        {/* Page Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-3xl font-bold text-dark">My Accounts</h2>
            <p className="text-neutral-600 mt-1">Manage your banking accounts and view balances</p>
          </div>
          <Button
            variant="primary"
            onClick={() => setShowCreateForm(true)}
            disabled={showCreateForm}
          >
            + Open New Account
          </Button>
        </div>

        {/* Success Message */}
        {successMessage && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-6">
            {successMessage}
          </div>
        )}

        {/* Create Account Form */}
        {showCreateForm && (
          <Card className="mb-8">
            <CardBody>
              <h3 className="text-xl font-semibold text-dark mb-4">Open New Account</h3>
              <form onSubmit={handleCreateSubmit} className="space-y-4">
                {createError && (
                  <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
                    {createError}
                  </div>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-dark mb-1">
                      Account Type
                    </label>
                    <select
                      value={createForm.accountType}
                      onChange={(e) => setCreateForm(prev => ({ ...prev, accountType: e.target.value as AccountType }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      required
                    >
                      <option value="CHECKING">Checking Account</option>
                      <option value="SAVINGS">Savings Account</option>
                      <option value="CREDIT">Credit Account</option>
                      <option value="INVESTMENT">Investment Account</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-1">
                      Account Name
                    </label>
                    <input
                      type="text"
                      value={createForm.accountName}
                      onChange={(e) => setCreateForm(prev => ({ ...prev, accountName: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      placeholder="e.g., My Checking Account"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-1">
                      Initial Balance
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={createForm.initialBalance}
                      onChange={(e) => setCreateForm(prev => ({ ...prev, initialBalance: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      placeholder="0.00"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-1">
                      Currency
                    </label>
                    <select
                      value={createForm.currency}
                      onChange={(e) => setCreateForm(prev => ({ ...prev, currency: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      required
                    >
                      <option value="USD">USD - US Dollar</option>
                      <option value="EUR">EUR - Euro</option>
                      <option value="GBP">GBP - British Pound</option>
                      <option value="CAD">CAD - Canadian Dollar</option>
                    </select>
                  </div>
                </div>

                <div className="flex space-x-4">
                  <Button type="submit" variant="primary" disabled={isCreating}>
                    {isCreating ? 'Creating...' : 'Create Account'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setShowCreateForm(false)}
                    disabled={isCreating}
                  >
                    Cancel
                  </Button>
                </div>
              </form>
            </CardBody>
          </Card>
        )}

        {/* Accounts List */}
        <div className="space-y-6">
          {accountsLoading && (
            <Card>
              <CardBody className="text-center py-8">
                <p className="text-neutral-600">Loading your accounts...</p>
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
              <CardBody className="text-center py-12">
                <div className="text-neutral-400 mb-4">
                  <svg className="w-16 h-16 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z"/>
                    <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd"/>
                  </svg>
                </div>
                <h3 className="text-xl font-semibold text-dark mb-2">No accounts yet</h3>
                <p className="text-neutral-600 mb-4">Get started by opening your first account</p>
                <Button variant="primary" onClick={() => setShowCreateForm(true)}>
                  Open Your First Account
                </Button>
              </CardBody>
            </Card>
          )}

          {accounts.map((account) => (
            <Card key={account.id} variant="account">
              <CardBody>
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-4">
                    <div className="text-primary mt-1">
                      {getAccountIcon(account.accountType)}
                    </div>
                    <div>
                      <h3 className="text-xl font-semibold text-dark">{account.accountName}</h3>
                      <p className="text-neutral-600">{getAccountTypeDisplay(account.accountType)}</p>
                      <p className="text-sm text-neutral-500 mt-1">
                        Account #{account.accountNum} • Opened {formatDate(account.createdAt)}
                      </p>
                      <div className="flex items-center space-x-4 mt-2">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          account.status === 'ACTIVE' ? 'bg-green-100 text-green-800' :
                          account.status === 'PENDING_APPROVAL' ? 'bg-yellow-100 text-yellow-800' :
                          account.status === 'INACTIVE' ? 'bg-gray-100 text-gray-800' :
                          account.status === 'SUSPENDED' ? 'bg-red-100 text-red-800' :
                          account.status === 'FROZEN' ? 'bg-blue-100 text-blue-800' :
                          'bg-gray-100 text-gray-800'
                        }`}>
                          {account.status}
                        </span>
                        <span className="text-sm text-neutral-500">{account.currency}</span>
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-2xl font-bold text-dark">
                      {formatCurrency(account.balance)}
                    </p>
                    <p className="text-sm text-neutral-600">Available Balance</p>
                    {account.availableBalance !== account.balance && (
                      <p className="text-sm text-neutral-500">
                        Available: {formatCurrency(account.availableBalance)}
                      </p>
                    )}
                  </div>
                </div>

                <div className="mt-6 flex space-x-3">
                  <Link href={`/transactions?account=${account.id}`}>
                    <Button variant="primary" size="sm">View Transactions</Button>
                  </Link>
                  <Link href={`/transfer?from=${account.id}`}>
                    <Button variant="outline" size="sm">Transfer Money</Button>
                  </Link>
                  <Button variant="ghost" size="sm">Account Details</Button>
                </div>
              </CardBody>
            </Card>
          ))}
        </div>
      </main>
    </div>
  );
}