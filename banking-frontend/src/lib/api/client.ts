import { LoginRequest, RegisterRequest, ApiResponse, User, Account, Transaction, CreateAccountRequest } from '@/types';

class ApiClient {
  private baseURL: string;
  private token: string | null = null;

  constructor() {
    // Default to localhost:8080 where Spring Boot runs
    this.baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

    // Try to load token from localStorage on initialization
    if (typeof window !== 'undefined') {
      this.token = localStorage.getItem('auth_token');
    }
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseURL}${endpoint}`;

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    // Add JWT token if available
    if (this.token) {
      headers.Authorization = `Bearer ${this.token}`;
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      });

      let data;
      const contentType = response.headers.get('content-type');

      try {
        if (contentType && contentType.includes('application/json')) {
          data = await response.json();
        } else {
          data = await response.text();
        }
      } catch (parseError) {
        data = await response.text();
      }

      if (!response.ok) {
        return {
          success: false,
          error: data.message || data || `HTTP error! status: ${response.status}`,
        };
      }

      return {
        success: true,
        data,
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Network error occurred',
      };
    }
  }

  // Auth methods
  async login(credentials: LoginRequest): Promise<ApiResponse<{ token: string; username: string; role: string }>> {
    const response = await this.request<{ token: string; username: string; role: string }>('/api/users/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });

    // Store token if login successful
    if (response.success && response.data?.token) {
      this.setToken(response.data.token);
    }

    return response;
  }

  async register(userData: RegisterRequest): Promise<ApiResponse<{ token: string; username: string; role: string }>> {
    const response = await this.request<{ token: string; username: string; role: string }>('/api/users/signup', {
      method: 'POST',
      body: JSON.stringify(userData),
    });

    // Store token if registration successful
    if (response.success && response.data?.token) {
      this.setToken(response.data.token);
    }

    return response;
  }

  async checkUsername(username: string): Promise<ApiResponse<{ available: boolean }>> {
    return this.request<{ available: boolean }>(`/api/users/check-username?username=${encodeURIComponent(username)}`);
  }

  async checkEmail(email: string): Promise<ApiResponse<{ available: boolean }>> {
    return this.request<{ available: boolean }>(`/api/users/check-email?email=${encodeURIComponent(email)}`);
  }

  // User methods
  async getCurrentUser(): Promise<ApiResponse<User>> {
    return this.request<User>('/api/users/profile');
  }

  async updateUser(userData: Partial<User>): Promise<ApiResponse<User>> {
    return this.request<User>('/api/users/me', {
      method: 'PUT',
      body: JSON.stringify(userData),
    });
  }

  // Account methods
  async getAccounts(userId?: number): Promise<ApiResponse<Account[]>> {
    if (userId) {
      return this.request<Account[]>(`/api/accounts/user/${userId}`);
    }
    // For admin/system-wide access, get all accounts
    return this.request<Account[]>('/api/accounts/admin/all');
  }

  async getAccount(accountId: number): Promise<ApiResponse<Account>> {
    return this.request<Account>(`/api/accounts/${accountId}`);
  }

  async createAccount(accountData: CreateAccountRequest): Promise<ApiResponse<Account>> {
    return this.request<Account>('/api/accounts', {
      method: 'POST',
      body: JSON.stringify(accountData),
    });
  }

  async updateAccount(accountId: number, accountData: Partial<Account>): Promise<ApiResponse<Account>> {
    return this.request<Account>(`/api/accounts/${accountId}`, {
      method: 'PUT',
      body: JSON.stringify(accountData),
    });
  }

  // Transaction methods
  async getTransactions(accountId?: number, userId?: number): Promise<ApiResponse<Transaction[]>> {
    if (userId) {
      return this.request<Transaction[]>(`/api/transactions/user/${userId}/recent`);
    }
    if (accountId) {
      return this.request<Transaction[]>(`/api/transactions/account/${accountId}`);
    }
    // For admin/system-wide access, use date range with wide dates to get all transactions
    const startDate = '2020-01-01T00:00:00';
    const endDate = '2030-01-01T00:00:00';
    return this.request<Transaction[]>(`/api/transactions/date-range?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`);
  }

  async getTransaction(transactionId: number): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>(`/api/transactions/${transactionId}`);
  }

  async createTransaction(transactionData: Partial<Transaction>): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>('/api/transactions', {
      method: 'POST',
      body: JSON.stringify(transactionData),
    });
  }

  async createDeposit(depositData: {
    toAccountId: number;
    amount: number;
    userId: number;
    description?: string;
    channel?: string;
  }): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>('/api/transactions/deposit', {
      method: 'POST',
      body: JSON.stringify({
        ...depositData,
        channel: depositData.channel || 'ONLINE_BANKING'
      }),
    });
  }

  async createWithdrawal(withdrawalData: {
    fromAccountId: number;
    amount: number;
    userId: number;
    description?: string;
    channel?: string;
  }): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>('/api/transactions/withdrawal', {
      method: 'POST',
      body: JSON.stringify({
        ...withdrawalData,
        channel: withdrawalData.channel || 'ONLINE_BANKING'
      }),
    });
  }

  async createTransfer(transferData: {
    fromAccountId: number;
    toAccountId: number;
    amount: number;
    userId: number;
    description?: string;
    channel?: string;
  }): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>('/api/transactions/transfer', {
      method: 'POST',
      body: JSON.stringify({
        ...transferData,
        channel: transferData.channel || 'ONLINE_BANKING'
      }),
    });
  }

  async searchTransactions(query: string): Promise<ApiResponse<Transaction[]>> {
    return this.request<Transaction[]>(`/api/transactions/search?q=${encodeURIComponent(query)}`);
  }

  // Paginated transaction methods
  async getUserTransactionsPaginated(userId: number, page: number = 0, size: number = 20): Promise<ApiResponse<any>> {
    return this.request(`/api/transactions/user/${userId}/paginated?page=${page}&size=${size}&sort=createdAt,desc`);
  }

  async getAccountTransactionsPaginated(accountId: number, page: number = 0, size: number = 20): Promise<ApiResponse<any>> {
    return this.request(`/api/transactions/account/${accountId}/paginated?page=${page}&size=${size}&sort=createdAt,desc`);
  }

  // Admin transaction management
  async processTransaction(transactionId: number): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>(`/api/transactions/${transactionId}/process`, {
      method: 'PUT',
    });
  }

  async cancelTransaction(transactionId: number): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>(`/api/transactions/${transactionId}/cancel`, {
      method: 'PUT',
    });
  }

  // Admin audit endpoints
  async getRecentAudits(): Promise<ApiResponse<any[]>> {
    return this.request<any[]>('/api/audits/recent');
  }

  async getFailedOperations(): Promise<ApiResponse<any[]>> {
    // This endpoint doesn't exist, return empty array
    return Promise.resolve({ success: true, data: [] });
  }

  async getHighRiskActivities(): Promise<ApiResponse<any[]>> {
    // This endpoint doesn't exist, return empty array
    return Promise.resolve({ success: true, data: [] });
  }

  async getCriticalEvents(): Promise<ApiResponse<any[]>> {
    return this.request<any[]>('/api/audits/security/critical-events');
  }

  // Admin account management
  async updateAccountStatus(accountId: number, status: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/accounts/${accountId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
  }

  async updateAccountBalance(accountId: number, balance: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/accounts/${accountId}/balance`, {
      method: 'PUT',
      body: JSON.stringify({ balance }),
    });
  }

  // Admin account approval methods
  async approveAccount(accountId: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/accounts/${accountId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status: 'ACTIVE' }),
    });
  }

  async rejectAccount(accountId: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/accounts/${accountId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status: 'INACTIVE' }),
    });
  }

  // Token management
  setToken(token: string): void {
    this.token = token;
    if (typeof window !== 'undefined') {
      localStorage.setItem('auth_token', token);
    }
  }

  getToken(): string | null {
    return this.token;
  }

  clearToken(): void {
    this.token = null;
    if (typeof window !== 'undefined') {
      localStorage.removeItem('auth_token');
    }
  }

  // Health check
  async healthCheck(): Promise<ApiResponse<{ status: string }>> {
    return this.request<{ status: string }>('/api/users/health');
  }

  // Billing methods
  async getBillings(userId: number): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/billings/user/${userId}`);
  }

  async getUnpaidBillings(userId: number): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/billings/user/${userId}/unpaid`);
  }

  async getOverdueBillings(): Promise<ApiResponse<any[]>> {
    return this.request<any[]>('/api/billings/overdue');
  }

  async getBilling(id: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/billings/${id}`);
  }

  async createBilling(billingData: {
    userId: number;
    accountId?: number;
    billingType: string;
    amount: number;
    description: string;
    dueDate: string;
    frequency?: string;
  }): Promise<ApiResponse<any>> {
    return this.request<any>('/api/billings', {
      method: 'POST',
      body: JSON.stringify(billingData),
    });
  }

  async createSubscription(subscriptionData: {
    userId: number;
    accountId?: number;
    amount: number;
    description: string;
    frequency: string;
    startDate: string;
    endDate?: string;
  }): Promise<ApiResponse<any>> {
    return this.request<any>('/api/billings/subscription', {
      method: 'POST',
      body: JSON.stringify(subscriptionData),
    });
  }

  async processBillingPayment(id: number, paymentData: {
    paymentAmount: number;
    transactionId?: number;
  }): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/billings/${id}/payment`, {
      method: 'PUT',
      body: JSON.stringify(paymentData),
    });
  }

  // New method for realistic payment processing from account
  async payBillFromAccount(id: number, accountId: number, paymentAmount: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/billings/${id}/pay-from-account`, {
      method: 'POST',
      body: JSON.stringify({
        accountId: accountId,
        paymentAmount: paymentAmount
      }),
    });
  }

  async payBill(billId: number, accountId: number): Promise<ApiResponse<any>> {
    // Get bill details to determine payment amount
    const billResponse = await this.getBilling(billId);
    if (!billResponse.success || !billResponse.data) {
      return { success: false, error: 'Failed to get bill details' };
    }

    const bill = billResponse.data;

    // First create a withdrawal transaction from the specified account
    const withdrawalResponse = await this.createWithdrawal({
      fromAccountId: accountId,
      amount: bill.amount,
      userId: bill.user.id, // Use the bill owner's user ID
      description: `Payment for ${bill.description || bill.billingType}`,
      channel: 'ONLINE_BANKING'
    });

    if (!withdrawalResponse.success || !withdrawalResponse.data) {
      return { success: false, error: withdrawalResponse.error || 'Failed to create payment transaction' };
    }

    // Then process the billing payment with the transaction ID
    return this.processBillingPayment(billId, {
      paymentAmount: bill.amount,
      transactionId: withdrawalResponse.data.id
    });
  }

  async updateBillingStatus(id: number, status: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/billings/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
  }

  async applyBillingDiscount(id: number, discountAmount: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/billings/${id}/discount`, {
      method: 'PUT',
      body: JSON.stringify({ discountAmount }),
    });
  }

  async deleteBilling(id: number): Promise<ApiResponse<void>> {
    return this.request<void>(`/api/billings/${id}`, {
      method: 'DELETE',
    });
  }

  // Report methods
  async requestReport(reportData: {
    userId: number;
    reportType: string;
    title: string;
    format: string;
    startDate?: string;
    endDate?: string;
    description?: string;
    parameters?: string;
  }): Promise<ApiResponse<any>> {
    return this.request<any>('/api/reports', {
      method: 'POST',
      body: JSON.stringify(reportData),
    });
  }

  async getReport(id: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/reports/${id}`);
  }

  async getReportByReference(reference: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/reports/reference/${reference}`);
  }

  async getReportsByUser(userId: number): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/reports/user/${userId}`);
  }

  async getRecentReportsByUser(userId: number): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/reports/user/${userId}/recent`);
  }

  async getReportsByStatus(status: string): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/reports/status/${status}`);
  }

  async getReportsByType(reportType: string): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/reports/type/${reportType}`);
  }

  async getReportsByFormat(format: string): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/reports/format/${format}`);
  }

  async getPendingReports(): Promise<ApiResponse<any[]>> {
    return this.request<any[]>('/api/reports/pending');
  }

  async getExpiredReports(): Promise<ApiResponse<any[]>> {
    return this.request<any[]>('/api/reports/expired');
  }

  async getStuckReports(minutesAgo: number = 30): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/reports/stuck?minutesAgo=${minutesAgo}`);
  }

  async startReportGeneration(id: number, generatedBy: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/reports/${id}/start`, {
      method: 'PUT',
      body: JSON.stringify({ generatedBy }),
    });
  }

  async completeReportGeneration(id: number, completionData: {
    filePath: string;
    fileName: string;
    fileSizeBytes: number;
    contentType: string;
    recordCount: number;
  }): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/reports/${id}/complete`, {
      method: 'PUT',
      body: JSON.stringify(completionData),
    });
  }

  async failReportGeneration(id: number, errorMessage: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/reports/${id}/fail`, {
      method: 'PUT',
      body: JSON.stringify({ errorMessage }),
    });
  }

  async cancelReport(id: number, cancelledBy: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/reports/${id}/cancel`, {
      method: 'PUT',
      body: JSON.stringify({ cancelledBy }),
    });
  }

  async logReportDownload(id: number, downloadedBy: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/reports/${id}/download`, {
      method: 'POST',
      body: JSON.stringify({ downloadedBy }),
    });
  }

  async cleanupExpiredReports(): Promise<ApiResponse<any>> {
    return this.request<any>('/api/reports/cleanup-expired', {
      method: 'POST',
    });
  }

  async deleteReport(id: number): Promise<ApiResponse<void>> {
    return this.request<void>(`/api/reports/${id}`, {
      method: 'DELETE',
    });
  }

  // Loan methods
  async getLoans(userId: number): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/loans/user/${userId}`);
  }

  async getActiveLoans(userId: number): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/loans/user/${userId}/active`);
  }

  async getLoan(id: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/loans/${id}`);
  }

  async getLoansByStatus(status: string): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/loans/status/${status}`);
  }

  async getLoansByType(loanType: string): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/loans/type/${loanType}`);
  }

  async getDelinquentLoans(): Promise<ApiResponse<any[]>> {
    return this.request<any[]>('/api/loans/delinquent');
  }

  async getLoansMaturingSoon(days?: number): Promise<ApiResponse<any[]>> {
    const params = days ? `?days=${days}` : '';
    return this.request<any[]>(`/api/loans/maturing${params}`);
  }

  async createLoanApplication(loanData: {
    userId: number;
    disbursementAccountId?: number;
    loanType: string;
    principalAmount: number;
    interestRate: number;
    termMonths: number;
    paymentFrequency: string;
    purpose: string;
  }): Promise<ApiResponse<any>> {
    return this.request<any>('/api/loans', {
      method: 'POST',
      body: JSON.stringify(loanData),
    });
  }

  async approveLoan(id: number, approvedBy: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/loans/${id}/approve`, {
      method: 'PUT',
      body: JSON.stringify({ approvedBy }),
    });
  }

  async rejectLoan(id: number, approvedBy: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/loans/${id}/reject`, {
      method: 'PUT',
      body: JSON.stringify({ approvedBy }),
    });
  }

  async disburseLoan(id: number, approvedBy: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/loans/${id}/disburse`, {
      method: 'PUT',
      body: JSON.stringify({ approvedBy }),
    });
  }

  async processLoanPayment(id: number, paymentData: {
    paymentAmount: number;
    transactionId?: number;
  }): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/loans/${id}/payment`, {
      method: 'PUT',
      body: JSON.stringify(paymentData),
    });
  }

  async updateLoanStatus(id: number, status: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/loans/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
  }

  async markLoanAsDelinquent(id: number, daysDelinquent: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/loans/${id}/delinquent`, {
      method: 'PUT',
      body: JSON.stringify({ daysDelinquent }),
    });
  }

  async deleteLoan(id: number): Promise<ApiResponse<void>> {
    return this.request<void>(`/api/loans/${id}`, {
      method: 'DELETE',
    });
  }

  // Credit scoring methods
  async getCreditScore(userId: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/loans/user/${userId}/credit-score`);
  }

  async previewLoanDecision(previewData: {
    userId: number;
    loanAmount: number;
    loanType: string;
  }): Promise<ApiResponse<any>> {
    return this.request<any>('/api/loans/preview', {
      method: 'POST',
      body: JSON.stringify(previewData),
    });
  }

  // Loan payment methods
  async makeLoanPayment(loanId: number, paymentData: {
    paymentAmount: number;
    description?: string;
  }): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/loans/${loanId}/make-payment`, {
      method: 'POST',
      body: JSON.stringify({
        paymentAmount: paymentData.paymentAmount,
        description: paymentData.description || 'Manual loan payment',
      }),
    });
  }

  // Notification methods
  async getUserNotifications(userId: number): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/notifications/user/${userId}`);
  }

  async getUnreadNotifications(userId: number): Promise<ApiResponse<any[]>> {
    return this.request<any[]>(`/api/notifications/user/${userId}/unread`);
  }

  async getUnreadNotificationsCount(userId: number): Promise<ApiResponse<number>> {
    return this.request<number>(`/api/notifications/user/${userId}/unread-count`);
  }

  async markNotificationAsRead(notificationId: number): Promise<ApiResponse<any>> {
    return this.request<any>(`/api/notifications/${notificationId}/mark-read`, {
      method: 'PUT',
    });
  }

  async markAllNotificationsAsRead(userId: number): Promise<ApiResponse<void>> {
    return this.request<void>(`/api/notifications/user/${userId}/mark-all-read`, {
      method: 'PUT',
    });
  }

  async deleteNotification(notificationId: number): Promise<ApiResponse<void>> {
    return this.request<void>(`/api/notifications/${notificationId}`, {
      method: 'DELETE',
    });
  }
}

// Export singleton instance
export const apiClient = new ApiClient();
export default apiClient;