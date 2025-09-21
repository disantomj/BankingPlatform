// User types
export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
  CUSTOMER = 'CUSTOMER'
}

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
  dateOfBirth?: string;
  role: UserRole;
  createdAt: string;
  updatedAt: string;
  isActive: boolean;
  lastLoginAt?: string;
}

// Account types
export enum AccountType {
  CHECKING = 'CHECKING',
  SAVINGS = 'SAVINGS',
  CREDIT = 'CREDIT',
  INVESTMENT = 'INVESTMENT'
}

export enum AccountStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  FROZEN = 'FROZEN',
  CLOSED = 'CLOSED',
  PENDING_APPROVAL = 'PENDING_APPROVAL',
  SUSPENDED = 'SUSPENDED'
}

export interface Account {
  id: number;
  accountNum: string;
  accountType: AccountType;
  status: AccountStatus;
  balance: number;
  availableBalance: number;
  minimumBalance: number;
  overdraftLimit: number;
  accountName: string;
  description?: string;
  currency: string;
  user: User;
  createdAt: string;
  updatedAt: string;
  closedAt?: string;
}

// Transaction types
export enum TransactionType {
  DEPOSIT = 'DEPOSIT',
  WITHDRAWAL = 'WITHDRAWAL',
  TRANSFER = 'TRANSFER',
  PAYMENT = 'PAYMENT',
  FEE = 'FEE',
  INTEREST = 'INTEREST',
  REFUND = 'REFUND'
}

export enum TransactionStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export interface Transaction {
  id: number;
  transactionReference: string;
  transactionType: TransactionType;
  status: TransactionStatus;
  amount: number;
  currency: string;
  description?: string;
  fromAccount?: Account;
  toAccount?: Account;
  balanceAfter?: number;
  fee?: number;
  processedAt?: string;
  createdAt: string;
  updatedAt: string;
}

// API Response types
export interface ApiResponse<T> {
  data?: T;
  message?: string;
  error?: string;
  success: boolean;
}

// Form types
export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
}