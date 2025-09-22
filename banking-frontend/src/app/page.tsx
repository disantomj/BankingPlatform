'use client';

import { Button, Card, CardBody } from '@/components/ui';
import Link from 'next/link';

export default function Home() {

  return (
    <main className="min-h-screen bg-accent-200">
      {/* Header */}
      <div className="bg-white shadow-sm border-b border-accent-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-primary font-bold">SecureBank</h1>
              <p className="text-accent-600 mt-1 font-medium">Your trusted banking partner</p>
            </div>
            <div className="flex space-x-4">
              <Link href="/login">
                <Button variant="outline" size="sm">Login</Button>
              </Link>
              <Link href="/register">
                <Button variant="primary" size="sm">Get Started</Button>
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Hero Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="text-center">
          <h2 className="text-5xl font-bold text-dark mb-6">
            Modern Banking,{' '}
            <span className="text-primary">Simplified</span>
          </h2>
          <p className="text-xl text-neutral-600 mb-8 max-w-2xl mx-auto">
            Experience secure, fast, and intuitive banking with our next-generation platform.
            Manage your finances with confidence.
          </p>
          <div className="flex justify-center space-x-4">
            <Link href="/register">
              <Button variant="primary" size="lg">Open Account</Button>
            </Link>
            <Link href="/login">
              <Button variant="outline" size="lg">Sign In</Button>
            </Link>
          </div>
        </div>
      </div>

      {/* Feature Cards */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="cursor-pointer" onClick={() => window.location.href = '/register'}>
            <Card variant="hover" className="h-full">
              <CardBody>
                <div className="text-primary mb-4">
                  <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"/>
                  </svg>
                </div>
                <h3 className="text-xl font-semibold text-dark mb-2">Dashboard</h3>
                <p className="text-neutral-600 mb-3">
                  View your accounts, balances, and recent activity at a glance.
                </p>
                <p className="text-sm text-primary font-medium">
                  Sign up to access your dashboard →
                </p>
              </CardBody>
            </Card>
          </div>

          <div className="cursor-pointer" onClick={() => window.location.href = '/register'}>
            <Card variant="hover" className="h-full">
              <CardBody>
                <div className="text-secondary mb-4">
                  <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
                  </svg>
                </div>
                <h3 className="text-xl font-semibold text-dark mb-2">Transfers</h3>
                <p className="text-neutral-600 mb-3">
                  Send money, pay bills, and manage your transaction history.
                </p>
                <p className="text-sm text-secondary font-medium">
                  Sign up to start transferring →
                </p>
              </CardBody>
            </Card>
          </div>

          <div className="cursor-pointer" onClick={() => window.location.href = '/register'}>
            <Card variant="hover" className="h-full">
              <CardBody>
                <div className="text-primary mb-4">
                  <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z"/>
                    <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd"/>
                  </svg>
                </div>
                <h3 className="text-xl font-semibold text-dark mb-2">Accounts</h3>
                <p className="text-neutral-600 mb-3">
                  Manage multiple accounts including checking, savings, and more.
                </p>
                <p className="text-sm text-primary font-medium">
                  Sign up to open accounts →
                </p>
              </CardBody>
            </Card>
          </div>

          <Link href="/support">
            <Card variant="hover" className="h-full">
              <CardBody>
                <div className="text-neutral-600 mb-4">
                  <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd"/>
                  </svg>
                </div>
                <h3 className="text-xl font-semibold text-dark mb-2">Support</h3>
                <p className="text-neutral-600">
                  Get help when you need it with our 24/7 customer support.
                </p>
              </CardBody>
            </Card>
          </Link>
        </div>
      </div>
    </main>
  );
}