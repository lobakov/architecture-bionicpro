import React, { useState } from 'react';

interface ReportPageProps {
  authenticated: boolean;
  onLogin: () => void;
}

const ReportPage: React.FC<ReportPageProps> = ({ authenticated, onLogin }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reportUrl, setReportUrl] = useState<string | null>(null);

  const fetchReport = async () => {
    try {
      setLoading(true);
      setError(null);
      setReportUrl(null);

      const response = await fetch(`${process.env.REACT_APP_API_URL}/api/reports`, {
        credentials: 'include',
      });

      if (response.status === 401) {
        onLogin();
        return;
      }

      if (!response.ok) {
        throw new Error('Failed to generate report');
      }

      const data = await response.json();
      const url = data.reportUrl.replace('http://nginx:80', 'http://localhost:8081');
      setReportUrl(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Произошла ошибка');
    } finally {
      setLoading(false);
    }
  };

  if (!authenticated) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
        <button
          onClick={onLogin}
          className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          Login
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100 p-4">
      <div className="p-8 bg-white rounded-lg shadow-md w-full max-w-md">
        <h1 className="text-2xl font-bold mb-6 text-center">Usage Reports</h1>

        <button
          onClick={fetchReport}
          disabled={loading}
          className={`px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 w-full ${
            loading ? 'opacity-50 cursor-not-allowed' : ''
          }`}
        >
          {loading ? 'Generating Report...' : 'Download Report'}
        </button>

        {error && <div className="mt-4 p-4 bg-red-100 text-red-700 rounded">{error}</div>}

        {reportUrl && (
          <div className="mt-6 text-center">
            <a
              href={reportUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 underline hover:text-blue-800"
            >
              Открыть отчёт
            </a>
          </div>
        )}
      </div>
    </div>
  );
};

export default ReportPage;
