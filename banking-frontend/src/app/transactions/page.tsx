'use client';

import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useAccounts } from '@/hooks/useAccounts';
import { useTransactions } from '@/hooks/useTransactions';
import { Button, Input, Card, CardBody, ErrorAlert } from '@/components/ui';
import { formatCurrency, formatDateTime } from '@/lib/utils';
import { parseApiError, getValidationMessage } from '@/lib/errorUtils';
import { apiClient } from '@/lib/api/client';
import Link from 'next/link';

export default function TransactionsPage() {
  const { user, logout } = useAuth();
  const { accounts, isLoading: accountsLoading } = useAccounts(user?.id);
  const { transactions, isLoading: transactionsLoading, refetch: refetchTransactions } = useTransactions(undefined, user?.id);

  const [activeTab, setActiveTab] = useState<'history' | 'transfer' | 'deposit' | 'withdraw'>('history');
  const [errors, setErrors] = useState<{[key: string]: string}>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [transferLoading, setTransferLoading] = useState(false);
  const [depositLoading, setDepositLoading] = useState(false);
  const [withdrawalLoading, setWithdrawalLoading] = useState(false);

  // Form states
  const [transferForm, setTransferForm] = useState({
    fromAccountId: '',
    toAccountId: '',
    amount: '',
    description: '',
    channel: 'ONLINE_BANKING',
  });

  const [depositForm, setDepositForm] = useState({
    toAccountId: '',
    amount: '',
    description: '',
    channel: 'ONLINE_BANKING',
  });

  const [withdrawForm, setWithdrawForm] = useState({
    fromAccountId: '',
    amount: '',
    description: '',
    channel: 'ONLINE_BANKING',
  });

  const handleLogout = () => {
    logout();
    window.location.href = '/';
  };

  const clearMessages = () => {
    setErrors({});
    setApiError(null);
    setSuccessMessage('');
  };

  const handleTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) return;

    clearMessages();
    setTransferLoading(true);

    try {
      const response = await apiClient.createTransfer({
        fromAccountId: parseInt(transferForm.fromAccountId),
        toAccountId: parseInt(transferForm.toAccountId),
        amount: parseFloat(transferForm.amount),
        userId: user.id,
        description: transferForm.description || undefined,
        channel: transferForm.channel,
      });

      if (response.success) {
        setSuccessMessage('Transfer completed successfully!');
        setTransferForm({ fromAccountId: '', toAccountId: '', amount: '', description: '', channel: 'ONLINE_BANKING' });
        refetchTransactions();
      } else {
        setApiError(response.error || 'Transfer failed');
      }
    } catch (error) {
      setApiError('An unexpected error occurred');
    } finally {
      setTransferLoading(false);
    }
  };

  const handleDeposit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) return;

    clearMessages();
    setDepositLoading(true);

    try {
      const response = await apiClient.createDeposit({
        toAccountId: parseInt(depositForm.toAccountId),
        amount: parseFloat(depositForm.amount),
        userId: user.id,
        description: depositForm.description || undefined,
        channel: depositForm.channel,
      });

      if (response.success) {
        setSuccessMessage('Deposit completed successfully!');
        setDepositForm({ toAccountId: '', amount: '', description: '', channel: 'ONLINE_BANKING' });
        refetchTransactions();
      } else {
        setErrors({ general: response.error || 'Deposit failed' });
      }
    } catch (error) {
      setErrors({ general: 'An unexpected error occurred' });
    } finally {
      setDepositLoading(false);
    }
  };

  const handleWithdraw = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) return;

    clearMessages();
    setWithdrawalLoading(true);

    try {
      const response = await apiClient.createWithdrawal({
        fromAccountId: parseInt(withdrawForm.fromAccountId),
        amount: parseFloat(withdrawForm.amount),
        userId: user.id,
        description: withdrawForm.description || undefined,
        channel: withdrawForm.channel,
      });

      if (response.success) {
        setSuccessMessage('Withdrawal completed successfully!');
        setWithdrawForm({ fromAccountId: '', amount: '', description: '', channel: 'ONLINE_BANKING' });
        refetchTransactions();
      } else {
        setErrors({ general: response.error || 'Withdrawal failed' });
      }
    } catch (error) {
      setErrors({ general: 'An unexpected error occurred' });
    } finally {
      setWithdrawalLoading(false);
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
                <Link href="/accounts" className="text-neutral-600 hover:text-primary">Accounts</Link>
                <Link href="/transactions" className="text-primary font-medium">Transactions</Link>
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
        <div className="mb-8">
          <h2 className="text-3xl font-bold text-dark mb-2">Transactions</h2>
          <p className="text-neutral-600">Manage your money transfers and view transaction history.</p>
        </div>

        {/* Success/Error Messages */}
        {successMessage && (
          <div className="mb-6 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg">
            {successMessage}
          </div>
        )}

        {errors.general && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
            {errors.general}
          </div>
        )}

        {/* Tab Navigation */}
        <div className="mb-8">
          <div className="border-b border-neutral-200">
            <nav className="-mb-px flex space-x-8">
              <button
                onClick={() => setActiveTab('history')}
                className={`py-2 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'history'
                    ? 'border-primary text-primary'
                    : 'border-transparent text-neutral-500 hover:text-neutral-700 hover:border-neutral-300'
                }`}
              >
                Transaction History
              </button>
              <button
                onClick={() => setActiveTab('transfer')}
                className={`py-2 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'transfer'
                    ? 'border-primary text-primary'
                    : 'border-transparent text-neutral-500 hover:text-neutral-700 hover:border-neutral-300'
                }`}
              >
                Transfer Money
              </button>
              <button
                onClick={() => setActiveTab('deposit')}
                className={`py-2 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'deposit'
                    ? 'border-primary text-primary'
                    : 'border-transparent text-neutral-500 hover:text-neutral-700 hover:border-neutral-300'
                }`}
              >
                Deposit
              </button>
              <button
                onClick={() => setActiveTab('withdraw')}
                className={`py-2 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'withdraw'
                    ? 'border-primary text-primary'
                    : 'border-transparent text-neutral-500 hover:text-neutral-700 hover:border-neutral-300'
                }`}
              >
                Withdraw
              </button>
            </nav>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'history' && (
          <Card>
            <CardBody>
              <h3 className="text-xl font-semibold text-dark mb-4">Transaction History</h3>

              {transactionsLoading && (
                <div className="text-center py-8">
                  <p className="text-neutral-600">Loading transactions...</p>
                </div>
              )}

              {!transactionsLoading && transactions.length === 0 && (
                <div className="text-center py-8">
                  <p className="text-neutral-600">No transactions found</p>
                </div>
              )}

              <div className="space-y-1">
                {transactions.map((transaction) => (
                  <div key={transaction.id} className="transaction-item">
                    <div className="flex items-center">
                      <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center mr-4">
                        <svg className="w-5 h-5 text-primary" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
                        </svg>
                      </div>
                      <div className="flex-1">
                        <p className="font-medium text-dark">{transaction.description || transaction.transactionType}</p>
                        <p className="text-sm text-neutral-600">
                          {transaction.transactionReference} • {formatDateTime(transaction.createdAt)}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className={`font-semibold ${
                        transaction.transactionType === 'DEPOSIT' ? 'text-green-600' :
                        transaction.transactionType === 'WITHDRAWAL' ? 'text-red-600' :
                        'text-neutral-600'
                      }`}>
                        {transaction.transactionType === 'DEPOSIT' ? '+' :
                         transaction.transactionType === 'WITHDRAWAL' ? '-' : ''}
                        {formatCurrency(transaction.amount)}
                      </p>
                      <p className="text-sm text-neutral-600">{transaction.status}</p>
                    </div>
                  </div>
                ))}
              </div>
            </CardBody>
          </Card>
        )}

        {activeTab === 'transfer' && (
          <Card>
            <CardBody>
              <h3 className="text-xl font-semibold text-dark mb-4">Transfer Money</h3>

              {/* Transaction Limits Info */}
              <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-4">
                <h4 className="font-medium text-green-900 mb-2">Transfer Limits by Channel</h4>
                <div className="text-sm text-green-700 space-y-1">
                  <p><strong>ATM:</strong> $500 maximum</p>
                  <p><strong>Mobile App:</strong> $2,000 maximum</p>
                  <p><strong>Online Banking:</strong> $5,000 maximum (auto-processes ≤$2,500)</p>
                  <p><strong>Branch:</strong> $25,000 maximum (auto-processes all amounts)</p>
                  <p><strong>Wire Transfer:</strong> $50,000 maximum (manual review required)</p>
                </div>
              </div>

              <form onSubmit={handleTransfer} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-dark mb-2">From Account</label>
                    <select
                      value={transferForm.fromAccountId}
                      onChange={(e) => setTransferForm(prev => ({ ...prev, fromAccountId: e.target.value }))}
                      className="input"
                      required
                      disabled={transferLoading}
                    >
                      <option value="">Select account</option>
                      {accounts.map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.accountName} - {formatCurrency(account.balance)}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-2">To Account</label>
                    <select
                      value={transferForm.toAccountId}
                      onChange={(e) => setTransferForm(prev => ({ ...prev, toAccountId: e.target.value }))}
                      className="input"
                      required
                      disabled={transferLoading}
                    >
                      <option value="">Select account</option>
                      {accounts.map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.accountName} - {formatCurrency(account.balance)}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <Input
                  label="Amount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={transferForm.amount}
                  onChange={(e) => setTransferForm(prev => ({ ...prev, amount: e.target.value }))}
                  placeholder="0.00"
                  required
                  disabled={withdrawalLoading}
                />

                <div>
                  <label className="block text-sm font-medium text-dark mb-2">Transfer Channel</label>
                  <select
                    value={transferForm.channel}
                    onChange={(e) => setTransferForm(prev => ({ ...prev, channel: e.target.value }))}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                    required
                    disabled={transferLoading}
                  >
                    <option value="ONLINE_BANKING">Online Banking</option>
                    <option value="MOBILE_APP">Mobile App</option>
                    <option value="ATM">ATM</option>
                    <option value="BRANCH">Branch</option>
                    <option value="THIRD_PARTY">Wire Transfer</option>
                  </select>
                </div>

                <Input
                  label="Description (Optional)"
                  value={transferForm.description}
                  onChange={(e) => setTransferForm(prev => ({ ...prev, description: e.target.value }))}
                  placeholder="What's this transfer for?"
                  disabled={transferLoading}
                />

                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  className="w-full"
                  disabled={transferLoading || accountsLoading}
                >
                  {transferLoading ? 'Processing Transfer...' : 'Transfer Money'}
                </Button>
              </form>
            </CardBody>
          </Card>
        )}

        {activeTab === 'deposit' && (
          <Card>
            <CardBody>
              <h3 className="text-xl font-semibold text-dark mb-4">Deposit Money</h3>

              {/* Transaction Limits Info */}
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
                <h4 className="font-medium text-blue-900 mb-2">Deposit Limits by Channel</h4>
                <div className="text-sm text-blue-700 space-y-1">
                  <p><strong>ATM:</strong> $1,000 maximum</p>
                  <p><strong>Mobile App:</strong> $5,000 maximum</p>
                  <p><strong>Online Banking:</strong> $10,000 maximum (auto-processes ≤$2,500)</p>
                  <p><strong>Branch:</strong> $50,000 maximum (auto-processes all amounts)</p>
                  <p><strong>Wire Transfer:</strong> $100,000 maximum (manual review required)</p>
                </div>
              </div>

              <form onSubmit={handleDeposit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-dark mb-2">To Account</label>
                  <select
                    value={depositForm.toAccountId}
                    onChange={(e) => setDepositForm(prev => ({ ...prev, toAccountId: e.target.value }))}
                    className="input"
                    required
                    disabled={depositLoading}
                  >
                    <option value="">Select account</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.accountName} - {formatCurrency(account.balance)}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-dark mb-2">Deposit Channel</label>
                  <select
                    value={depositForm.channel}
                    onChange={(e) => setDepositForm(prev => ({ ...prev, channel: e.target.value }))}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                    required
                    disabled={depositLoading}
                  >
                    <option value="ONLINE_BANKING">Online Banking</option>
                    <option value="MOBILE_APP">Mobile App</option>
                    <option value="ATM">ATM</option>
                    <option value="BRANCH">Branch</option>
                    <option value="THIRD_PARTY">Wire Transfer</option>
                  </select>
                </div>

                <Input
                  label="Amount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={depositForm.amount}
                  onChange={(e) => setDepositForm(prev => ({ ...prev, amount: e.target.value }))}
                  placeholder="0.00"
                  required
                  disabled={depositLoading}
                />

                <Input
                  label="Description (Optional)"
                  value={depositForm.description}
                  onChange={(e) => setDepositForm(prev => ({ ...prev, description: e.target.value }))}
                  placeholder="What's this deposit for?"
                  disabled={withdrawalLoading}
                />

                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  className="w-full"
                  disabled={depositLoading || accountsLoading}
                >
                  {depositLoading ? 'Processing Deposit...' : 'Deposit Money'}
                </Button>
              </form>
            </CardBody>
          </Card>
        )}

        {activeTab === 'withdraw' && (
          <Card>
            <CardBody>
              <h3 className="text-xl font-semibold text-dark mb-4">Withdraw Money</h3>

              {/* Transaction Limits Info */}
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4">
                <h4 className="font-medium text-yellow-900 mb-2">Withdrawal Limits by Channel</h4>
                <div className="text-sm text-yellow-700 space-y-1">
                  <p><strong>ATM:</strong> $500 maximum</p>
                  <p><strong>Mobile App:</strong> $2,000 maximum</p>
                  <p><strong>Online Banking:</strong> $5,000 maximum (auto-processes ≤$2,500)</p>
                  <p><strong>Branch:</strong> $25,000 maximum (auto-processes all amounts)</p>
                  <p><strong>Wire Transfer:</strong> $50,000 maximum (manual review required)</p>
                </div>
              </div>

              <form onSubmit={handleWithdraw} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-dark mb-2">From Account</label>
                  <select
                    value={withdrawForm.fromAccountId}
                    onChange={(e) => setWithdrawForm(prev => ({ ...prev, fromAccountId: e.target.value }))}
                    className="input"
                    required
                    disabled={depositLoading}
                  >
                    <option value="">Select account</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.accountName} - {formatCurrency(account.balance)}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-dark mb-2">Withdrawal Channel</label>
                  <select
                    value={withdrawForm.channel}
                    onChange={(e) => setWithdrawForm(prev => ({ ...prev, channel: e.target.value }))}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                    required
                    disabled={withdrawalLoading}
                  >
                    <option value="ONLINE_BANKING">Online Banking</option>
                    <option value="MOBILE_APP">Mobile App</option>
                    <option value="ATM">ATM</option>
                    <option value="BRANCH">Branch</option>
                    <option value="THIRD_PARTY">Wire Transfer</option>
                  </select>
                </div>

                <Input
                  label="Amount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={withdrawForm.amount}
                  onChange={(e) => setWithdrawForm(prev => ({ ...prev, amount: e.target.value }))}
                  placeholder="0.00"
                  required
                  disabled={withdrawalLoading}
                />

                <Input
                  label="Description (Optional)"
                  value={withdrawForm.description}
                  onChange={(e) => setWithdrawForm(prev => ({ ...prev, description: e.target.value }))}
                  placeholder="What's this withdrawal for?"
                  disabled={withdrawalLoading}
                />

                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  className="w-full"
                  disabled={withdrawalLoading || accountsLoading}
                >
                  {withdrawalLoading ? 'Processing Withdrawal...' : 'Withdraw Money'}
                </Button>
              </form>
            </CardBody>
          </Card>
        )}
      </main>
    </div>
  );
}