'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { Button, Card, CardBody } from '@/components/ui';
import { formatCurrency, formatDate } from '@/lib/utils';
import { apiClient } from '@/lib/api/client';
import Link from 'next/link';

interface Report {
  id: number;
  reference: string;
  requestedBy: {
    id: number;
    username: string;
  };
  reportType: string;
  title: string;
  format: string;
  status: string;
  requestDate: string;
  startDate?: string;
  endDate?: string;
  description?: string;
  fileName?: string;
  fileSizeBytes?: number;
  recordCount?: number;
  generatedBy?: string;
  generationStartTime?: string;
  generationEndTime?: string;
  errorMessage?: string;
  downloadCount: number;
  expirationDate?: string;
}


const reportTypes = [
  'ACCOUNT_SUMMARY',
  'TRANSACTION_HISTORY',
  'LOAN_PORTFOLIO',
  'BILLING_SUMMARY',
  'AUDIT_LOG',
  'RISK_ASSESSMENT',
  'COMPLIANCE_REPORT',
  'PERFORMANCE_METRICS'
];

const reportFormats = ['PDF', 'CSV', 'EXCEL', 'JSON'];

export default function ReportsPage() {
  const { user, logout, isAuthenticated, isLoading } = useAuth();
  const [reports, setReports] = useState<Report[]>([]);
  const [reportsLoading, setReportsLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [newReport, setNewReport] = useState({
    reportType: '',
    title: '',
    format: '',
    startDate: '',
    endDate: '',
    description: '',
    parameters: ''
  });

  const [requestingReport, setRequestingReport] = useState(false);
  const [cancellingReport, setCancellingReport] = useState(false);
  const [downloadingReport, setDownloadingReport] = useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      window.location.href = '/login';
      return;
    }

    if (user && user.id) {
      loadReports(user.id);
    }
  }, [user, isAuthenticated, isLoading]);

  const loadReports = async (userId: number) => {
    try {
      setReportsLoading(true);
      const response = await apiClient.getReportsByUser(userId);
      if (response.success && response.data) {
        setReports(response.data);
      } else {
        setMessage({ type: 'error', text: 'Failed to load reports' });
      }
    } catch (error) {
      setMessage({ type: 'error', text: 'Failed to load reports' });
    } finally {
      setReportsLoading(false);
    }
  };

  const checkRateLimit = async (userId: number): Promise<boolean> => {
    try {
      const response = await apiClient.getReportsByUser(userId);
      if (response.success && response.data) {
        const oneWeekAgo = new Date();
        oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);

        const recentReports = response.data.filter(report =>
          new Date(report.requestDate) > oneWeekAgo
        );

        return recentReports.length === 0;
      }
      return true;
    } catch (error) {
      return true;
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    const canRequest = await checkRateLimit(user.id);
    if (!canRequest) {
      setMessage({ type: 'error', text: 'You can only request one report per week. Please wait before requesting another report.' });
      return;
    }

    const reportData = {
      userId: user.id,
      reportType: newReport.reportType,
      title: newReport.title,
      format: newReport.format,
      startDate: newReport.startDate || undefined,
      endDate: newReport.endDate || undefined,
      description: newReport.description || undefined,
      parameters: newReport.parameters || undefined
    };

    try {
      setRequestingReport(true);
      const response = await apiClient.requestReport(reportData);

      if (response.success && response.data) {
        setMessage({ type: 'success', text: 'Report auto-approved and generation started! Report will be completed by background process.' });

        try {
          await apiClient.startReportGeneration(response.data.id, user.username);
        } catch (startError) {
          setMessage({ type: 'error', text: 'Report requested but failed to auto-start generation. Check admin panel.' });
        }

        setNewReport({
          reportType: '',
          title: '',
          format: '',
          startDate: '',
          endDate: '',
          description: '',
          parameters: ''
        });
        setShowForm(false);
        await loadReports(user.id);
      } else {
        setMessage({ type: 'error', text: response.error || 'Failed to request report' });
      }
    } catch (error) {
      setMessage({ type: 'error', text: 'Failed to request report' });
    } finally {
      setRequestingReport(false);
    }
  };

  const handleCancelReport = async (reportId: number) => {
    if (!user) return;

    try {
      setCancellingReport(true);
      const response = await apiClient.cancelReport(reportId, user.username);

      if (response.success) {
        setMessage({ type: 'success', text: 'Report cancelled successfully!' });
        await loadReports(user.id);
      } else {
        setMessage({ type: 'error', text: response.error || 'Failed to cancel report' });
      }
    } catch (error) {
      setMessage({ type: 'error', text: 'Failed to cancel report' });
    } finally {
      setCancellingReport(false);
    }
  };

  const handleDownloadReport = async (reportId: number) => {
    if (!user) return;

    try {
      setDownloadingReport(true);
      const response = await apiClient.logReportDownload(reportId, user.username);

      if (response.success) {
        setMessage({ type: 'success', text: 'Download logged successfully!' });
        await loadReports(user.id);
      } else {
        setMessage({ type: 'error', text: response.error || 'Failed to log download' });
      }
    } catch (error) {
      setMessage({ type: 'error', text: 'Failed to log download' });
    } finally {
      setDownloadingReport(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'text-green-600';
      case 'PENDING': return 'text-yellow-600';
      case 'GENERATING': return 'text-blue-600';
      case 'FAILED': return 'text-red-600';
      case 'CANCELLED': return 'text-gray-600';
      case 'EXPIRED': return 'text-red-400';
      default: return 'text-gray-600';
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  if (isLoading || reportsLoading) {
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
                <Link href="/billing" className="text-neutral-600 hover:text-primary">Billing</Link>
                <Link href="/loans" className="text-neutral-600 hover:text-primary">Loans</Link>
                <Link href="/reports" className="text-primary font-medium">Reports</Link>
                {user?.role === 'ADMIN' && (
                  <Link href="/admin" className="text-red-600 hover:text-red-700 font-medium">Admin</Link>
                )}
              </nav>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-neutral-600">Welcome, {user?.username || 'User'}</span>
              <Button variant="ghost" size="sm" onClick={logout}>
                Logout
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-dark">Reports</h1>
          <Button
            onClick={() => setShowForm(!showForm)}
            variant={showForm ? "ghost" : "primary"}
          >
            {showForm ? 'Cancel' : 'Request New Report'}
          </Button>
        </div>

        {message && (
          <div className={`mb-6 p-4 rounded-lg ${message.type === 'success' ? 'bg-success-100 text-success-700' : 'bg-danger-100 text-danger-700'}`}>
            {message.text}
          </div>
        )}

        {showForm && (
          <Card className="mb-8">
            <CardBody>
              <h2 className="text-xl font-semibold text-dark mb-4">Request New Report</h2>
              <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Report Type</label>
                <select
                  value={newReport.reportType}
                  onChange={(e) => setNewReport(prev => ({ ...prev, reportType: e.target.value }))}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  required
                >
                  <option value="">Select Report Type</option>
                  {reportTypes.map(type => (
                    <option key={type} value={type}>{type.replace(/_/g, ' ')}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Format</label>
                <select
                  value={newReport.format}
                  onChange={(e) => setNewReport(prev => ({ ...prev, format: e.target.value }))}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  required
                >
                  <option value="">Select Format</option>
                  {reportFormats.map(format => (
                    <option key={format} value={format}>{format}</option>
                  ))}
                </select>
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
                <input
                  type="text"
                  value={newReport.title}
                  onChange={(e) => setNewReport(prev => ({ ...prev, title: e.target.value }))}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
                <input
                  type="date"
                  value={newReport.startDate}
                  onChange={(e) => setNewReport(prev => ({ ...prev, startDate: e.target.value }))}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
                <input
                  type="date"
                  value={newReport.endDate}
                  onChange={(e) => setNewReport(prev => ({ ...prev, endDate: e.target.value }))}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  value={newReport.description}
                  onChange={(e) => setNewReport(prev => ({ ...prev, description: e.target.value }))}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  rows={3}
                />
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Parameters (JSON)</label>
                <textarea
                  value={newReport.parameters}
                  onChange={(e) => setNewReport(prev => ({ ...prev, parameters: e.target.value }))}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  rows={2}
                  placeholder='{"key": "value"}'
                />
              </div>

              <div className="md:col-span-2">
                <Button type="submit" variant="primary">
                  Request Report
                </Button>
              </div>
              </form>
            </CardBody>
          </Card>
        )}

        <Card className="overflow-hidden">
          <div className="px-6 py-4 border-b border-accent-200">
            <h2 className="text-xl font-semibold text-dark">Your Reports</h2>
          </div>

          {!reports || reports.length === 0 ? (
            <CardBody>
              <div className="text-center text-neutral-600">
                No reports found. Request your first report above!
              </div>
            </CardBody>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-accent-200">
                <thead className="bg-accent-100">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-neutral-600 uppercase tracking-wider">Reference</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-neutral-600 uppercase tracking-wider">Title</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-neutral-600 uppercase tracking-wider">Type</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-neutral-600 uppercase tracking-wider">Format</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-neutral-600 uppercase tracking-wider">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-neutral-600 uppercase tracking-wider">Requested</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-neutral-600 uppercase tracking-wider">File Info</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-neutral-600 uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-accent-200">
                  {reports?.map((report) => (
                    <tr key={report.id} className="hover:bg-accent-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-dark">
                        {report.reference}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-dark">
                        {report.title}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-dark">
                        {report.reportType.replace(/_/g, ' ')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-dark">
                        {report.format}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`text-sm font-medium ${getStatusColor(report.status)}`}>
                          {report.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-dark">
                        {new Date(report.requestDate).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-dark">
                        {report.fileName ? (
                          <div>
                            <div>{report.fileName}</div>
                            {report.fileSizeBytes && (
                              <div className="text-xs text-neutral-500">
                                {formatFileSize(report.fileSizeBytes)} • {report.recordCount} records
                              </div>
                            )}
                            <div className="text-xs text-neutral-500">
                              Downloads: {report.downloadCount}
                            </div>
                          </div>
                        ) : (
                          <span className="text-neutral-400">Not generated</span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                        {report.status === 'COMPLETED' && (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => handleDownloadReport(report.id)}
                            className="text-primary hover:text-primary-dark"
                          >
                            Download
                          </Button>
                        )}
                        {(report.status === 'PENDING' || report.status === 'GENERATING') && (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => handleCancelReport(report.id)}
                            className="text-danger hover:text-danger-dark"
                          >
                            Cancel
                          </Button>
                        )}
                        {report.errorMessage && (
                          <span className="text-danger text-xs" title={report.errorMessage}>
                            Error
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </main>
    </div>
  );
}