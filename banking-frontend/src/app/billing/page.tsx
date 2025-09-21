'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { Button, Card, CardBody } from '@/components/ui';
import { formatCurrency, formatDate } from '@/lib/utils';
import { apiClient } from '@/lib/api/client';
import Link from 'next/link';

interface Billing {
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

export default function BillingPage() {
  const { user, logout, isAuthenticated, isLoading } = useAuth();
  const [activeTab, setActiveTab] = useState<'bills' | 'payments' | 'schedule'>('bills');
  const [bills, setBills] = useState<Billing[]>([]);
  const [paidBills, setPaidBills] = useState<Billing[]>([]);
  const [isLoadingBills, setIsLoadingBills] = useState(false);
  const [billsError, setBillsError] = useState('');
  const [showPayBillForm, setShowPayBillForm] = useState(false);
  const [showCreateBillForm, setShowCreateBillForm] = useState(false);
  const [selectedBill, setSelectedBill] = useState<Billing | null>(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // Create bill form
  const [createBillForm, setCreateBillForm] = useState({
    billingType: 'INVOICE',
    amount: '',
    description: '',
    dueDate: ''
  });

  // Redirect to login if not authenticated
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      window.location.href = '/login';
    }
  }, [isAuthenticated, isLoading]);

  // Load bills when user is authenticated
  useEffect(() => {
    if (isAuthenticated && user?.id) {
      loadBills();
    }
  }, [isAuthenticated, user?.id]);

  const loadBills = async () => {
    if (!user?.id) return;

    setIsLoadingBills(true);
    setBillsError('');

    try {
      // Load all bills and separate by status
      const [allBillsResponse, unpaidBillsResponse] = await Promise.all([
        apiClient.getBillings(user.id),
        apiClient.getUnpaidBillings(user.id)
      ]);

      if (allBillsResponse.success && allBillsResponse.data) {
        const allBills = allBillsResponse.data;
        const unpaidBills = unpaidBillsResponse.success ? unpaidBillsResponse.data : [];

        setBills(unpaidBills || []);
        setPaidBills(allBills.filter((bill: Billing) => bill.status === 'PAID') || []);
      } else {
        setBillsError(allBillsResponse.error || 'Failed to load bills');
      }
    } catch (error) {
      setBillsError('An error occurred while loading bills');
      console.error('Error loading bills:', error);
    } finally {
      setIsLoadingBills(false);
    }
  };

  const handleLogout = () => {
    logout();
    window.location.href = '/';
  };

  const handlePayBill = (bill: Billing) => {
    setSelectedBill(bill);
    setShowPayBillForm(true);
  };

  const handlePaymentSubmit = async () => {
    if (!selectedBill) return;

    try {
      const response = await apiClient.processBillingPayment(selectedBill.id, {
        paymentAmount: selectedBill.amount
      });

      if (response.success) {
        setSuccessMessage('Payment processed successfully!');
        setShowPayBillForm(false);
        setSelectedBill(null);
        loadBills(); // Reload bills
      } else {
        setErrorMessage(response.error || 'Payment failed');
      }
    } catch (error) {
      setErrorMessage('An error occurred while processing payment');
      console.error('Error processing payment:', error);
    }
  };

  const handleCreateBill = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) return;

    try {
      const response = await apiClient.createBilling({
        userId: user.id,
        billingType: createBillForm.billingType,
        amount: parseFloat(createBillForm.amount),
        description: createBillForm.description,
        dueDate: createBillForm.dueDate
      });

      if (response.success) {
        setSuccessMessage('Bill created successfully!');
        setShowCreateBillForm(false);
        setCreateBillForm({
          billingType: 'INVOICE',
          amount: '',
          description: '',
          dueDate: ''
        });
        loadBills(); // Reload bills
      } else {
        setErrorMessage(response.error || 'Failed to create bill');
      }
    } catch (error) {
      setErrorMessage('An error occurred while creating bill');
      console.error('Error creating bill:', error);
    }
  };

  const getBillingTypeDisplay = (type: string) => {
    switch (type) {
      case 'INVOICE': return 'Invoice';
      case 'SUBSCRIPTION': return 'Subscription';
      case 'SERVICE_FEE': return 'Service Fee';
      case 'MAINTENANCE_FEE': return 'Maintenance Fee';
      case 'OVERDRAFT_FEE': return 'Overdraft Fee';
      case 'TRANSACTION_FEE': return 'Transaction Fee';
      case 'LOAN_PAYMENT': return 'Loan Payment';
      case 'INTEREST_CHARGE': return 'Interest Charge';
      case 'PENALTY_FEE': return 'Penalty Fee';
      case 'MONTHLY_STATEMENT': return 'Monthly Statement';
      default: return type;
    }
  };

  const getBillStatusColor = (status: string) => {
    switch (status) {
      case 'PAID': return 'bg-green-100 text-green-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'SENT': return 'bg-blue-100 text-blue-800';
      case 'OVERDUE': return 'bg-red-100 text-red-800';
      case 'CANCELLED': return 'bg-gray-100 text-gray-800';
      case 'REFUNDED': return 'bg-purple-100 text-purple-800';
      case 'DISPUTED': return 'bg-orange-100 text-orange-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const clearMessages = () => {
    setSuccessMessage('');
    setErrorMessage('');
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
                <Link href="/billing" className="text-primary font-medium">Billing</Link>
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
            <h2 className="text-3xl font-bold text-dark">Bill Management</h2>
            <p className="text-neutral-600 mt-1">Manage your bills and payment history</p>
          </div>
          <Button variant="primary" onClick={() => setShowCreateBillForm(true)}>
            + Create New Bill
          </Button>
        </div>

        {/* Success/Error Messages */}
        {successMessage && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-6">
            {successMessage}
            <button onClick={clearMessages} className="ml-4 text-green-600 hover:text-green-800">×</button>
          </div>
        )}

        {errorMessage && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-6">
            {errorMessage}
            <button onClick={clearMessages} className="ml-4 text-red-600 hover:text-red-800">×</button>
          </div>
        )}

        {/* Tabs */}
        <div className="mb-8">
          <nav className="flex space-x-8">
            <button
              onClick={() => setActiveTab('bills')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'bills'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-neutral-500 hover:text-neutral-700 hover:border-neutral-300'
              }`}
            >
              Pending Bills ({bills.length})
            </button>
            <button
              onClick={() => setActiveTab('payments')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'payments'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-neutral-500 hover:text-neutral-700 hover:border-neutral-300'
              }`}
            >
              Payment History ({paidBills.length})
            </button>
            <button
              onClick={() => setActiveTab('schedule')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'schedule'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-neutral-500 hover:text-neutral-700 hover:border-neutral-300'
              }`}
            >
              Schedule Payments
            </button>
          </nav>
        </div>

        {/* Create Bill Modal */}
        {showCreateBillForm && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <Card className="w-full max-w-md mx-4">
              <CardBody>
                <h3 className="text-xl font-semibold text-dark mb-4">Create New Bill</h3>
                <form onSubmit={handleCreateBill} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-dark mb-1">Bill Type</label>
                    <select
                      value={createBillForm.billingType}
                      onChange={(e) => setCreateBillForm(prev => ({ ...prev, billingType: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      required
                    >
                      <option value="INVOICE">Invoice</option>
                      <option value="SUBSCRIPTION">Subscription</option>
                      <option value="SERVICE_FEE">Service Fee</option>
                      <option value="MAINTENANCE_FEE">Maintenance Fee</option>
                      <option value="OVERDRAFT_FEE">Overdraft Fee</option>
                      <option value="TRANSACTION_FEE">Transaction Fee</option>
                      <option value="LOAN_PAYMENT">Loan Payment</option>
                      <option value="INTEREST_CHARGE">Interest Charge</option>
                      <option value="PENALTY_FEE">Penalty Fee</option>
                      <option value="MONTHLY_STATEMENT">Monthly Statement</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-1">Amount</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={createBillForm.amount}
                      onChange={(e) => setCreateBillForm(prev => ({ ...prev, amount: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      placeholder="0.00"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-1">Description</label>
                    <textarea
                      value={createBillForm.description}
                      onChange={(e) => setCreateBillForm(prev => ({ ...prev, description: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      rows={3}
                      placeholder="Bill description"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-dark mb-1">Due Date</label>
                    <input
                      type="date"
                      value={createBillForm.dueDate}
                      onChange={(e) => setCreateBillForm(prev => ({ ...prev, dueDate: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-md focus:outline-none focus:ring-primary focus:border-primary"
                      required
                    />
                  </div>

                  <div className="flex space-x-4">
                    <Button type="submit" variant="primary" className="flex-1">
                      Create Bill
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setShowCreateBillForm(false)}
                      className="flex-1"
                    >
                      Cancel
                    </Button>
                  </div>
                </form>
              </CardBody>
            </Card>
          </div>
        )}

        {/* Pay Bill Modal */}
        {showPayBillForm && selectedBill && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <Card className="w-full max-w-md mx-4">
              <CardBody>
                <h3 className="text-xl font-semibold text-dark mb-4">Pay Bill</h3>
                <div className="space-y-4">
                  <div className="bg-accent-50 p-4 rounded-lg">
                    <p className="text-sm text-neutral-600">Bill Reference</p>
                    <p className="font-medium text-dark">{selectedBill.reference}</p>
                    <p className="text-sm text-neutral-600 mt-2">Amount Due</p>
                    <p className="text-2xl font-bold text-dark">{formatCurrency(selectedBill.amount)}</p>
                    <p className="text-sm text-neutral-600 mt-1">Due: {formatDate(selectedBill.dueDate)}</p>
                  </div>
                  <div className="flex space-x-4">
                    <Button
                      variant="primary"
                      onClick={handlePaymentSubmit}
                      className="flex-1"
                    >
                      Pay {formatCurrency(selectedBill.amount)}
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => {
                        setShowPayBillForm(false);
                        setSelectedBill(null);
                      }}
                      className="flex-1"
                    >
                      Cancel
                    </Button>
                  </div>
                </div>
              </CardBody>
            </Card>
          </div>
        )}

        {/* Tab Content */}
        {activeTab === 'bills' && (
          <div className="space-y-4">
            {isLoadingBills && (
              <Card>
                <CardBody className="text-center py-8">
                  <p className="text-neutral-600">Loading bills...</p>
                </CardBody>
              </Card>
            )}

            {billsError && (
              <Card>
                <CardBody className="text-center py-8">
                  <p className="text-red-600">Error: {billsError}</p>
                  <Button variant="outline" onClick={loadBills} className="mt-4">
                    Retry
                  </Button>
                </CardBody>
              </Card>
            )}

            {!isLoadingBills && !billsError && bills.length === 0 && (
              <Card>
                <CardBody className="text-center py-12">
                  <div className="text-neutral-400 mb-4">
                    <svg className="w-16 h-16 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M4 4a2 2 0 00-2 2v8a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2H4zm0 2h12v2H4V6zm0 4h12v4H4v-4z"/>
                    </svg>
                  </div>
                  <h3 className="text-xl font-semibold text-dark mb-2">No pending bills</h3>
                  <p className="text-neutral-600">All your bills are up to date!</p>
                </CardBody>
              </Card>
            )}

            {bills.map((bill) => (
              <Card key={bill.id}>
                <CardBody>
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-4">
                        <div>
                          <h4 className="font-semibold text-dark">{bill.description}</h4>
                          <p className="text-sm text-neutral-600">{getBillingTypeDisplay(bill.billingType)}</p>
                          <p className="text-sm text-neutral-500">Ref: {bill.reference}</p>
                          <div className="flex items-center space-x-4 mt-2">
                            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getBillStatusColor(bill.status)}`}>
                              {bill.status}
                            </span>
                            <span className="text-sm text-neutral-500">Created: {formatDate(bill.createdAt)}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-2xl font-bold text-dark">{formatCurrency(bill.amount)}</p>
                      <p className="text-sm text-neutral-600">Due: {formatDate(bill.dueDate)}</p>
                      {bill.status !== 'PAID' && (
                        <Button
                          variant="primary"
                          size="sm"
                          className="mt-2"
                          onClick={() => handlePayBill(bill)}
                        >
                          Pay Now
                        </Button>
                      )}
                    </div>
                  </div>
                </CardBody>
              </Card>
            ))}
          </div>
        )}

        {activeTab === 'payments' && (
          <div className="space-y-4">
            {paidBills.length === 0 ? (
              <Card>
                <CardBody className="text-center py-12">
                  <div className="text-neutral-400 mb-4">
                    <svg className="w-16 h-16 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
                    </svg>
                  </div>
                  <h3 className="text-xl font-semibold text-dark mb-2">No payment history</h3>
                  <p className="text-neutral-600">Your payment history will appear here</p>
                </CardBody>
              </Card>
            ) : (
              paidBills.map((bill) => (
                <Card key={bill.id}>
                  <CardBody>
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <div className="flex items-center space-x-4">
                          <div>
                            <h4 className="font-semibold text-dark">{bill.description}</h4>
                            <p className="text-sm text-neutral-600">{getBillingTypeDisplay(bill.billingType)}</p>
                            <p className="text-sm text-neutral-500">Ref: {bill.reference}</p>
                            <div className="flex items-center space-x-4 mt-2">
                              <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getBillStatusColor(bill.status)}`}>
                                {bill.status}
                              </span>
                              {bill.paidDate && (
                                <span className="text-sm text-neutral-500">Paid: {formatDate(bill.paidDate)}</span>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-2xl font-bold text-green-600">-{formatCurrency(bill.amount)}</p>
                        <p className="text-sm text-neutral-600">Payment Completed</p>
                      </div>
                    </div>
                  </CardBody>
                </Card>
              ))
            )}
          </div>
        )}

        {activeTab === 'schedule' && (
          <Card>
            <CardBody className="text-center py-12">
              <div className="text-neutral-400 mb-4">
                <svg className="w-16 h-16 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd"/>
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-dark mb-2">Schedule Payments</h3>
              <p className="text-neutral-600 mb-4">Set up automatic payments for your regular bills</p>
              <Button variant="primary">Set Up Auto-Pay</Button>
            </CardBody>
          </Card>
        )}
      </main>
    </div>
  );
}