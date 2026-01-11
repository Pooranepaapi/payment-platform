import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import type { TransactionResponse, TransactionStatus, PaymentStatus } from '../types';

const statusColors: Record<TransactionStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  PENDING_3DS: 'bg-yellow-100 text-yellow-800',
  INITIATED: 'bg-yellow-100 text-yellow-800',
  AUTHORIZED: 'bg-blue-100 text-blue-800',
  CAPTURED: 'bg-green-100 text-green-800',
  SUCCESS: 'bg-green-100 text-green-800',
  VOIDED: 'bg-gray-100 text-gray-800',
  REFUNDED: 'bg-purple-100 text-purple-800',
  DECLINED: 'bg-red-100 text-red-800',
  FAILED: 'bg-red-100 text-red-800',
};

// Payment status colors (for QR payments)
const paymentStatusColors: Record<PaymentStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  PROCESSING: 'bg-yellow-100 text-yellow-800',
  SUCCESS: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
  EXPIRED: 'bg-gray-100 text-gray-800',
  REFUNDED: 'bg-purple-100 text-purple-800',
  CREATED: 'bg-blue-100 text-blue-800',
  QR_GENERATED: 'bg-blue-100 text-blue-800',
  CANCELLED: 'bg-gray-100 text-gray-800',
};

// Payment response type for QR payments
interface PaymentData {
  paymentId: number;
  paymentUuid: string;
  status: PaymentStatus;
  dueAmount?: number;
  paidAmount?: number;
  currency: string;
  merchantOrderId?: string;
  createdAt: string;
  expiresAt?: string;
}

export default function TransactionPage() {
  const { transactionId } = useParams<{ transactionId: string }>();
  const [searchParams] = useSearchParams();
  const isPaymentType = searchParams.get('type') === 'payment';
  const navigate = useNavigate();
  const [transaction, setTransaction] = useState<TransactionResponse | null>(
    null
  );
  const [payment, setPayment] = useState<PaymentData | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadTransaction = useCallback(async () => {
    if (!transactionId) return;
    try {
      if (isPaymentType) {
        // Load payment status for QR payments
        const data = await api.getPaymentStatus(transactionId);
        setPayment(data as unknown as PaymentData);
      } else {
        // Load transaction for card/UPI collect
        const data = await api.getTransaction(transactionId);
        setTransaction(data);
      }
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  }, [transactionId, isPaymentType]);

  useEffect(() => {
    loadTransaction();
  }, [loadTransaction]);

  const handleAction = async (action: 'capture' | 'void' | 'refund') => {
    if (!transactionId) return;
    setActionLoading(action);
    setError(null);

    try {
      let response: TransactionResponse;
      switch (action) {
        case 'capture':
          response = await api.capture(transactionId);
          break;
        case 'void':
          response = await api.voidTransaction(transactionId);
          break;
        case 'refund':
          response = await api.refund(transactionId);
          break;
        default:
          return;
      }

      if (response.transactionId) {
        setTransaction(response);
      } else {
        setError(response.message || 'Action failed');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-gray-600">Loading...</div>
      </div>
    );
  }

  // Handle QR Payment display
  if (isPaymentType && payment) {
    return (
      <div className="min-h-screen bg-gray-100 py-8 px-4">
        <div className="max-w-xl mx-auto">
          <div className="flex items-center justify-between mb-6">
            <h1 className="text-2xl font-bold text-gray-800">Payment Details</h1>
            <button
              onClick={() => navigate('/')}
              className="text-blue-600 hover:underline text-sm"
            >
              New Payment
            </button>
          </div>

          {/* Status Banner */}
          <div
            className={`rounded-lg p-4 mb-6 ${
              paymentStatusColors[payment.status] || 'bg-gray-100'
            }`}
          >
            <div className="flex items-center justify-between">
              <div>
                <div className="text-sm opacity-75">Status</div>
                <div className="text-xl font-bold">{payment.status}</div>
              </div>
              <div className="text-right">
                <div className="text-sm opacity-75">Type</div>
                <div className="text-lg font-semibold">UPI QR</div>
              </div>
            </div>
          </div>

          {/* Payment Info */}
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-lg font-semibold mb-4 text-gray-700">
              Payment Info
            </h2>

            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600">Payment ID</span>
                <span className="font-mono text-sm">{payment.paymentId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Payment UUID</span>
                <span className="font-mono text-xs">{payment.paymentUuid}</span>
              </div>
              {payment.merchantOrderId && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Order ID</span>
                  <span className="font-mono text-sm">{payment.merchantOrderId}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-gray-600">Amount</span>
                <span className="font-semibold">
                  {payment.dueAmount} {payment.currency}
                </span>
              </div>
              {payment.paidAmount !== undefined && payment.paidAmount > 0 && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Paid Amount</span>
                  <span className="font-semibold text-green-600">
                    {payment.paidAmount} {payment.currency}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Success Message */}
          {payment.status === 'SUCCESS' && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4 text-center">
              <div className="text-green-600 font-semibold">
                Payment completed successfully via UPI QR!
              </div>
            </div>
          )}

          {/* Refresh Button */}
          <div className="mt-4">
            <button
              onClick={loadTransaction}
              className="w-full bg-gray-200 text-gray-700 py-2 px-4 rounded-md hover:bg-gray-300 transition"
            >
              Refresh
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Handle not found cases
  if (isPaymentType && !payment) {
    return (
      <div className="min-h-screen bg-gray-100 py-8 px-4">
        <div className="max-w-xl mx-auto">
          <div className="bg-white rounded-lg shadow-md p-6 text-center">
            <div className="text-red-600 mb-4">
              {error || 'Payment not found'}
            </div>
            <button
              onClick={() => navigate('/')}
              className="text-blue-600 hover:underline"
            >
              Back to payment
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!transaction || transaction.success === false) {
    return (
      <div className="min-h-screen bg-gray-100 py-8 px-4">
        <div className="max-w-xl mx-auto">
          <div className="bg-white rounded-lg shadow-md p-6 text-center">
            <div className="text-red-600 mb-4">
              {transaction?.message || 'Transaction not found'}
            </div>
            <button
              onClick={() => navigate('/')}
              className="text-blue-600 hover:underline"
            >
              Back to payment
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 py-8 px-4">
      <div className="max-w-xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-800">Transaction Details</h1>
          <button
            onClick={() => navigate('/')}
            className="text-blue-600 hover:underline text-sm"
          >
            New Payment
          </button>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        {/* Status Banner */}
        <div
          className={`rounded-lg p-4 mb-6 ${
            statusColors[transaction.status] || 'bg-gray-100'
          }`}
        >
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm opacity-75">Status</div>
              <div className="text-xl font-bold">{transaction.status}</div>
            </div>
            <div className="text-right">
              <div className="text-sm opacity-75">Type</div>
              <div className="text-lg font-semibold">
                {transaction.txnType || transaction.type}
              </div>
            </div>
          </div>
        </div>

        {/* Transaction Info */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-lg font-semibold mb-4 text-gray-700">
            Transaction Info
          </h2>

          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Transaction ID</span>
              <span className="font-mono text-sm">{transaction.transactionId}</span>
            </div>
            {transaction.paymentId && (
              <div className="flex justify-between">
                <span className="text-gray-600">Payment ID</span>
                <span className="font-mono text-sm">{transaction.paymentId}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-gray-600">Payment Method</span>
              <span
                className={`font-semibold ${
                  transaction.paymentMethod === 'UPI'
                    ? 'text-green-600'
                    : 'text-blue-600'
                }`}
              >
                {transaction.paymentMethod || 'CARD'}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Amount</span>
              <span className="font-semibold">
                {transaction.amount} {transaction.currency}
              </span>
            </div>

            {/* Card-specific fields */}
            {transaction.maskedCardNumber && (
              <div className="flex justify-between">
                <span className="text-gray-600">Card</span>
                <span className="font-mono">{transaction.maskedCardNumber}</span>
              </div>
            )}
            {transaction.authCode && (
              <div className="flex justify-between">
                <span className="text-gray-600">Auth Code</span>
                <span className="font-mono">{transaction.authCode}</span>
              </div>
            )}
            {transaction.rrn && (
              <div className="flex justify-between">
                <span className="text-gray-600">RRN</span>
                <span className="font-mono text-sm">{transaction.rrn}</span>
              </div>
            )}

            {/* UPI-specific fields */}
            {transaction.pspReferenceId && (
              <div className="flex justify-between">
                <span className="text-gray-600">PSP Reference</span>
                <span className="font-mono text-sm">
                  {transaction.pspReferenceId}
                </span>
              </div>
            )}
            {transaction.bankReferenceId && (
              <div className="flex justify-between">
                <span className="text-gray-600">Bank Reference</span>
                <span className="font-mono text-sm">
                  {transaction.bankReferenceId}
                </span>
              </div>
            )}

            {/* Response info (for card transactions) */}
            {transaction.responseCode && (
              <div className="flex justify-between">
                <span className="text-gray-600">Response</span>
                <span
                  className={
                    transaction.responseCode === '00'
                      ? 'text-green-600'
                      : 'text-red-600'
                  }
                >
                  {transaction.responseCode} - {transaction.responseMessage}
                </span>
              </div>
            )}

            {/* Failure reason (for UPI/any failed transactions) */}
            {transaction.failureReason && (
              <div className="flex justify-between">
                <span className="text-gray-600">Failure Reason</span>
                <span className="text-red-600">{transaction.failureReason}</span>
              </div>
            )}
          </div>
        </div>

        {/* Actions */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-lg font-semibold mb-4 text-gray-700">Actions</h2>

          <div className="flex flex-wrap gap-3">
            {/* PHASE 2: Capture & Void buttons are for Card transactions with AUTHORIZED status */}
            {transaction.status === 'AUTHORIZED' && (
              <>
                <button
                  onClick={() => handleAction('capture')}
                  disabled={actionLoading !== null}
                  className="flex-1 bg-green-600 text-white py-2 px-4 rounded-md hover:bg-green-700 transition disabled:opacity-50"
                  title="Phase 2: Card transactions only"
                >
                  {actionLoading === 'capture' ? 'Processing...' : 'Capture'}
                </button>
                <button
                  onClick={() => handleAction('void')}
                  disabled={actionLoading !== null}
                  className="flex-1 bg-red-600 text-white py-2 px-4 rounded-md hover:bg-red-700 transition disabled:opacity-50"
                  title="Phase 2: Card transactions only"
                >
                  {actionLoading === 'void' ? 'Processing...' : 'Void'}
                </button>
              </>
            )}

            {/* Refund button for CAPTURED status */}
            {transaction.status === 'CAPTURED' && (
              <button
                onClick={() => handleAction('refund')}
                disabled={actionLoading !== null}
                className="flex-1 bg-orange-600 text-white py-2 px-4 rounded-md hover:bg-orange-700 transition disabled:opacity-50"
                title="Phase 2: Card transactions only (for UPI, use payment-level refund)"
              >
                {actionLoading === 'refund' ? 'Processing...' : 'Refund'}
              </button>
            )}

            <button
              onClick={loadTransaction}
              disabled={actionLoading !== null}
              className="bg-gray-200 text-gray-700 py-2 px-4 rounded-md hover:bg-gray-300 transition"
            >
              Refresh
            </button>
          </div>

          {(transaction.status === 'VOIDED' ||
            transaction.status === 'REFUNDED' ||
            transaction.status === 'DECLINED' ||
            transaction.status === 'FAILED') && (
            <p className="text-sm text-gray-500 mt-4 text-center">
              No further actions available for this transaction.
            </p>
          )}

          {transaction.status === 'SUCCESS' &&
            transaction.paymentMethod === 'UPI' && (
              <p className="text-sm text-green-600 mt-4 text-center">
                Payment completed successfully via UPI.
              </p>
            )}
        </div>
      </div>
    </div>
  );
}
