import { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { api } from '../api';
import type { ThreeDSSessionResponse } from '../types';

export default function ThreeDSPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const sessionId = searchParams.get('sessionId');
  const txnId = searchParams.get('txnId');

  const [session, setSession] = useState<ThreeDSSessionResponse | null>(null);
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const completeTransaction = useCallback(async () => {
    if (!txnId) return;
    try {
      const response = await api.complete3DS(txnId);
      navigate(`/transaction/${response.transactionId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    }
  }, [txnId, navigate]);

  const loadSession = useCallback(async () => {
    if (!sessionId) return;
    try {
      const data = await api.get3DSSession(sessionId);
      setSession(data);
      if (data.status === 'AUTHENTICATED') {
        await completeTransaction();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  }, [sessionId, completeTransaction]);

  useEffect(() => {
    if (sessionId) {
      loadSession();
    }
  }, [sessionId, loadSession]);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!sessionId) return;

    setSubmitting(true);
    setError(null);

    try {
      const response = await api.authenticate3DS(sessionId, otp);

      if (response.status === 'AUTHENTICATED') {
        await completeTransaction();
      } else {
        setError(response.message || 'Authentication failed');
        setOtp(''); // Clear OTP for retry
        // Keep status as PENDING if there are remaining attempts
        if (response.remainingAttempts && response.remainingAttempts > 0) {
          setSession((prev) =>
            prev ? { ...prev, ...response, status: 'PENDING' } : null
          );
        } else {
          setSession((prev) => (prev ? { ...prev, ...response } : null));
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-gray-600">Loading...</div>
      </div>
    );
  }

  if (!session) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-red-600">Session not found</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-900 to-blue-700 flex items-center justify-center px-4">
      <div className="bg-white rounded-lg shadow-xl p-8 max-w-md w-full">
        {/* Bank Header */}
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-100 rounded-full mb-4">
            <svg
              className="w-8 h-8 text-blue-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
              />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-800">3D Secure</h1>
          <p className="text-gray-600">Verify your identity</p>
        </div>

        {/* Transaction Info */}
        <div className="bg-gray-50 rounded-lg p-4 mb-6">
          <div className="flex justify-between text-sm mb-2">
            <span className="text-gray-600">Card</span>
            <span className="font-mono">{session.maskedCardNumber}</span>
          </div>
          <div className="flex justify-between text-sm mb-2">
            <span className="text-gray-600">Amount</span>
            <span className="font-semibold">
              {session.amount} {session.currency}
            </span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Transaction</span>
            <span className="font-mono text-xs">{txnId}</span>
          </div>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4 text-sm">
            {error}
          </div>
        )}

        {session.status === 'PENDING' && (
          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Enter OTP sent to your phone
              </label>
              <input
                type="text"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                maxLength={6}
                placeholder="000000"
                className="w-full px-4 py-3 text-center text-2xl font-mono border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 tracking-widest"
                autoFocus
              />
              {session.remainingAttempts !== undefined && (
                <p className="text-xs text-gray-500 mt-2 text-center">
                  {session.remainingAttempts} attempts remaining
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={submitting || otp.length < 6}
              className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition disabled:opacity-50"
            >
              {submitting ? 'Verifying...' : 'Verify'}
            </button>
          </form>
        )}

        {session.status === 'FAILED' && (
          <div className="text-center">
            <div className="text-red-600 mb-4">Authentication Failed</div>
            <button
              onClick={() => navigate('/')}
              className="text-blue-600 hover:underline"
            >
              Return to payment
            </button>
          </div>
        )}

        <p className="text-xs text-gray-500 text-center mt-6 bg-yellow-50 p-2 rounded">
          Test OTP: <span className="font-mono font-bold">123456</span>
        </p>
      </div>
    </div>
  );
}
