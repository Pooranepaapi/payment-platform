import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useQRPayment } from '../hooks/useQRPayment';
import { QRCard, Timer, StatusBadge } from '../components';
import type {
  MerchantData,
  CardFormState,
  UpiFormState,
  TestCard,
  TestVpa,
  UpiProcessingStatus,
} from '../types';

type PaymentMethodType = 'card' | 'upi' | 'upi-qr';

const testCards: TestCard[] = [
  { number: '4111111111111111', label: '3DS Success', brand: 'visa' },
  { number: '4000000000000002', label: '3DS Decline', brand: 'visa' },
  { number: '5500000000000004', label: 'No 3DS', brand: 'mastercard' },
  { number: '4000000000000069', label: 'Insufficient Funds', brand: 'visa' },
];

const testVpas: TestVpa[] = [
  { vpa: 'success@upi', label: 'Success', description: 'Payment will succeed' },
  { vpa: 'fail@upi', label: 'Decline', description: 'Customer rejects' },
  { vpa: 'timeout@upi', label: 'Timeout', description: 'Stays pending' },
  {
    vpa: 'insufficient@upi',
    label: 'Insufficient',
    description: 'Insufficient funds',
  },
];

const MERCHANT_ID = 'MER001'; // Default test merchant (matches backend DataInitializer)

export default function CheckoutPage() {
  const navigate = useNavigate();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethodType>('upi');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [merchantData, setMerchantData] = useState<MerchantData | null>(null);

  // Card form state
  const [cardForm, setCardForm] = useState<CardFormState>({
    cardNumber: '4111111111111111',
    expiryMonth: '12',
    expiryYear: '2028',
    cvv: '123',
    cardholderName: 'Test User',
  });

  // UPI form state
  const [upiForm, setUpiForm] = useState<UpiFormState>({
    customerVpa: 'success@upi',
  });

  // UPI processing state
  const [upiStatus, setUpiStatus] = useState<UpiProcessingStatus>(null);
  const [upiMessage, setUpiMessage] = useState('');
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // UPI QR hook
  const qrPayment = useQRPayment({
    onSuccess: (paymentId, transactionId) => {
      sessionStorage.removeItem('merchantData');
      if (transactionId) {
        navigate(`/transaction/${transactionId}`);
      } else {
        // Navigate with paymentId if no transactionId available yet
        navigate(`/transaction/${paymentId}?type=payment`);
      }
    },
    onError: (errorMsg) => {
      setError(errorMsg);
    },
    onExpire: () => {
      // On expiry, reset and show message
      setError('Payment expired. Please try again.');
    },
  });

  useEffect(() => {
    const data = sessionStorage.getItem('merchantData');
    if (data) {
      setMerchantData(JSON.parse(data) as MerchantData);
    }

    // Cleanup polling on unmount
    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
      }
    };
  }, []);

  // Reset QR state when switching away from QR tab
  const handleMethodChange = useCallback(
    (method: PaymentMethodType) => {
      if (paymentMethod === 'upi-qr' && method !== 'upi-qr') {
        qrPayment.reset();
      }
      setPaymentMethod(method);
      setError(null);
    },
    [paymentMethod, qrPayment]
  );

  const handleCardChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCardForm({ ...cardForm, [e.target.name]: e.target.value });
  };

  const handleUpiChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setUpiForm({ ...upiForm, [e.target.name]: e.target.value });
  };

  const handleCardSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const payload = {
        ...cardForm,
        amount: parseFloat(merchantData?.amount || '100.00'),
        currency: merchantData?.currency || 'USD',
      };

      const response = await api.sale(payload);

      if (response.threeDSRequired) {
        navigate(
          `/3ds?sessionId=${response.threeDSSessionId}&txnId=${response.transactionId}`
        );
      } else if (response.transactionId) {
        sessionStorage.removeItem('merchantData');
        navigate(`/transaction/${response.transactionId}`);
      } else {
        setError(response.message || 'Payment failed');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleUpiSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setUpiStatus('processing');
    setUpiMessage('Creating payment...');

    try {
      // Step 1: Create Payment
      const paymentResponse = await api.createPayment({
        merchantId: MERCHANT_ID,
        merchantOrderId: merchantData?.orderId || `ORDER_${Date.now()}`,
        amount: parseFloat(merchantData?.amount || '100.00'),
        currency: merchantData?.currency || 'INR',
        testMode: true,
        customerMobile: '9999999999',
      });

      if (!paymentResponse.paymentId) {
        throw new Error(paymentResponse.message || 'Failed to create payment');
      }

      // Step 2: Initiate UPI Collect
      setUpiMessage('Sending UPI collect request...');
      const collectResponse = await api.upiCollect(
        paymentResponse.paymentId,
        upiForm.customerVpa
      );

      if (!collectResponse.transactionId) {
        throw new Error(
          collectResponse.message || 'Failed to initiate UPI collect'
        );
      }

      // Step 3: Poll for payment status (simulator sends callback automatically)
      setUpiStatus('polling');
      setUpiMessage('Waiting for payment confirmation...');

      const paymentId = paymentResponse.paymentId;

      const pollForStatus = () => {
        return new Promise<void>((resolve, reject) => {
          let attempts = 0;
          const maxAttempts = 15;

          pollingRef.current = setInterval(async () => {
            attempts++;
            try {
              const payment = await api.getPaymentStatus(paymentId);

              if (payment.status === 'SUCCESS') {
                if (pollingRef.current) clearInterval(pollingRef.current);
                resolve();
              } else if (payment.status === 'FAILED') {
                if (pollingRef.current) clearInterval(pollingRef.current);
                reject(new Error('Payment failed'));
              } else if (attempts >= maxAttempts) {
                if (pollingRef.current) clearInterval(pollingRef.current);
                resolve();
              }
            } catch (err) {
              if (pollingRef.current) clearInterval(pollingRef.current);
              reject(err);
            }
          }, 1000);
        });
      };

      await pollForStatus();

      // Success - navigate to transaction page
      sessionStorage.removeItem('merchantData');
      navigate(`/transaction/${paymentId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
      setUpiStatus(null);
      setUpiMessage('');
    } finally {
      setLoading(false);
    }
  };

  const handleQRSubmit = async () => {
    setError(null);

    await qrPayment.initiateQRPayment({
      merchantId: MERCHANT_ID,
      merchantOrderId: merchantData?.orderId || `ORDER_${Date.now()}`,
      amount: parseFloat(merchantData?.amount || '100.00'),
      currency: merchantData?.currency || 'INR',
      testMode: true,
      customerMobile: '9999999999',
    });
  };

  const getCardBrand = (number: string): 'visa' | 'mastercard' | null => {
    if (number.startsWith('4')) return 'visa';
    if (number.startsWith('5')) return 'mastercard';
    return null;
  };

  return (
    <div className="min-h-screen bg-gray-100 py-8 px-4">
      <div className="max-w-lg mx-auto">
        {/* Order Summary */}
        {merchantData && (
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-gray-700">
                Order Summary
              </h2>
              <span className="text-xs text-gray-500 font-mono">
                {merchantData.orderId}
              </span>
            </div>
            <div className="border-t border-gray-200 pt-4">
              <div className="flex justify-between text-sm mb-2">
                <span className="text-gray-600">{merchantData.description}</span>
              </div>
              <div className="flex justify-between text-sm mb-2">
                <span className="text-gray-600">Customer</span>
                <span>{merchantData.customerEmail}</span>
              </div>
              <div className="border-t border-gray-200 mt-4 pt-4 flex justify-between">
                <span className="text-lg font-semibold">Total</span>
                <span className="text-2xl font-bold text-gray-800">
                  {merchantData.currency}{' '}
                  {parseFloat(merchantData.amount).toFixed(2)}
                </span>
              </div>
            </div>
          </div>
        )}

        {/* Payment Method Tabs */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4 text-gray-700">
            Select Payment Method
          </h2>

          <div className="flex gap-2 mb-6">
            <button
              type="button"
              disabled
              title="Card payments coming soon"
              className="flex-1 py-3 px-4 rounded-lg font-medium flex items-center justify-center gap-2 bg-gray-100 text-gray-400 cursor-not-allowed relative"
            >
              <svg
                className="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"
                />
              </svg>
              Card
              <span className="absolute -top-2 -right-2 bg-yellow-400 text-yellow-900 text-xs font-bold px-1.5 py-0.5 rounded-full leading-none">
                Soon
              </span>
            </button>
            <button
              type="button"
              onClick={() => handleMethodChange('upi')}
              className={`flex-1 py-3 px-4 rounded-lg font-medium transition flex items-center justify-center gap-2 ${
                paymentMethod === 'upi'
                  ? 'bg-green-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              <svg
                className="w-5 h-5"
                viewBox="0 0 24 24"
                fill="currentColor"
              >
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" />
              </svg>
              UPI
            </button>
            <button
              type="button"
              onClick={() => handleMethodChange('upi-qr')}
              className={`flex-1 py-3 px-4 rounded-lg font-medium transition flex items-center justify-center gap-2 ${
                paymentMethod === 'upi-qr'
                  ? 'bg-purple-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              <svg
                className="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"
                />
              </svg>
              QR
            </button>
          </div>

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {/* Card Payment Form */}
          {paymentMethod === 'card' && (
            <form onSubmit={handleCardSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">
                  Card Number
                </label>
                <div className="relative">
                  <input
                    type="text"
                    name="cardNumber"
                    value={cardForm.cardNumber}
                    onChange={handleCardChange}
                    maxLength={16}
                    placeholder="1234 5678 9012 3456"
                    className="w-full px-3 py-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono"
                  />
                  {getCardBrand(cardForm.cardNumber) && (
                    <div className="absolute right-3 top-1/2 -translate-y-1/2">
                      {getCardBrand(cardForm.cardNumber) === 'visa' && (
                        <span className="text-blue-600 font-bold text-sm">
                          VISA
                        </span>
                      )}
                      {getCardBrand(cardForm.cardNumber) === 'mastercard' && (
                        <span className="text-orange-600 font-bold text-sm">
                          MC
                        </span>
                      )}
                    </div>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">
                  Cardholder Name
                </label>
                <input
                  type="text"
                  name="cardholderName"
                  value={cardForm.cardholderName}
                  onChange={handleCardChange}
                  placeholder="John Doe"
                  className="w-full px-3 py-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-600 mb-1">
                    Month
                  </label>
                  <input
                    type="text"
                    name="expiryMonth"
                    value={cardForm.expiryMonth}
                    onChange={handleCardChange}
                    maxLength={2}
                    placeholder="MM"
                    className="w-full px-3 py-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-center"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-600 mb-1">
                    Year
                  </label>
                  <input
                    type="text"
                    name="expiryYear"
                    value={cardForm.expiryYear}
                    onChange={handleCardChange}
                    maxLength={4}
                    placeholder="YYYY"
                    className="w-full px-3 py-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-center"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-600 mb-1">
                    CVV
                  </label>
                  <input
                    type="password"
                    name="cvv"
                    value={cardForm.cvv}
                    onChange={handleCardChange}
                    maxLength={4}
                    placeholder="***"
                    className="w-full px-3 py-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-center"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-600 text-white py-3 rounded-md font-semibold hover:bg-blue-700 transition disabled:opacity-50 flex items-center justify-center"
              >
                {loading ? (
                  <>
                    <svg
                      className="animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      ></circle>
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      ></path>
                    </svg>
                    Processing...
                  </>
                ) : (
                  <>
                    Pay {merchantData?.currency || 'USD'}{' '}
                    {parseFloat(merchantData?.amount || '100.00').toFixed(2)}
                  </>
                )}
              </button>
            </form>
          )}

          {/* UPI Payment Form */}
          {paymentMethod === 'upi' && (
            <form onSubmit={handleUpiSubmit} className="space-y-4">
              {upiStatus ? (
                // Processing State
                <div className="text-center py-8">
                  <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-100 mb-4">
                    <svg
                      className="animate-spin h-8 w-8 text-green-600"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      ></circle>
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      ></path>
                    </svg>
                  </div>
                  <p className="text-gray-700 font-medium mb-2">{upiMessage}</p>
                  <p className="text-sm text-gray-500">
                    {upiStatus === 'approving' &&
                      'In production, customer approves on their UPI app'}
                    {upiStatus === 'polling' && 'Please wait...'}
                  </p>
                </div>
              ) : (
                // Input Form
                <>
                  <div>
                    <label className="block text-sm font-medium text-gray-600 mb-1">
                      UPI ID (VPA)
                    </label>
                    <input
                      type="text"
                      name="customerVpa"
                      value={upiForm.customerVpa}
                      onChange={handleUpiChange}
                      placeholder="yourname@upi"
                      className="w-full px-3 py-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 font-mono"
                    />
                    <p className="text-xs text-gray-400 mt-1">
                      Enter your UPI ID (e.g., name@paytm, name@oksbi)
                    </p>
                  </div>

                  <button
                    type="submit"
                    disabled={loading || !upiForm.customerVpa}
                    className="w-full bg-green-600 text-white py-3 rounded-md font-semibold hover:bg-green-700 transition disabled:opacity-50 flex items-center justify-center"
                  >
                    Pay {merchantData?.currency || 'INR'}{' '}
                    {parseFloat(merchantData?.amount || '100.00').toFixed(2)}
                  </button>
                </>
              )}
            </form>
          )}

          {/* UPI QR Payment Section */}
          {paymentMethod === 'upi-qr' && (
            <div className="space-y-4">
              {qrPayment.status === null && (
                // Initial state - show button to generate QR
                <div className="text-center py-8">
                  <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-purple-100 mb-4">
                    <svg
                      className="w-8 h-8 text-purple-600"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"
                      />
                    </svg>
                  </div>
                  <h3 className="text-lg font-medium text-gray-800 mb-2">
                    Pay via UPI QR
                  </h3>
                  <p className="text-sm text-gray-500 mb-6">
                    Scan the QR code with any UPI app to complete payment
                  </p>
                  <button
                    onClick={handleQRSubmit}
                    className="w-full bg-purple-600 text-white py-3 rounded-md font-semibold hover:bg-purple-700 transition"
                  >
                    Generate QR Code
                  </button>
                </div>
              )}

              {(qrPayment.status === 'creating' ||
                qrPayment.status === 'generating') && (
                // Loading state
                <div className="text-center py-8">
                  <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-purple-100 mb-4">
                    <svg
                      className="animate-spin h-8 w-8 text-purple-600"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      ></circle>
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      ></path>
                    </svg>
                  </div>
                  <p className="text-gray-700 font-medium">
                    {qrPayment.status === 'creating'
                      ? 'Creating payment...'
                      : 'Generating QR code...'}
                  </p>
                </div>
              )}

              {qrPayment.status === 'polling' && qrPayment.qrData && (
                // QR Display state
                <div className="py-4">
                  {/* Timer */}
                  <div className="flex justify-center mb-4">
                    <Timer
                      expiresAt={qrPayment.qrData.expiresAt}
                      onExpire={qrPayment.handleExpiry}
                    />
                  </div>

                  {/* Status Badge */}
                  {qrPayment.paymentStatus && (
                    <div className="flex justify-center mb-4">
                      <StatusBadge status={qrPayment.paymentStatus} />
                    </div>
                  )}

                  {/* QR Card */}
                  <QRCard
                    qrImageBase64={qrPayment.qrData.qrImageBase64}
                    upiIntent={qrPayment.qrData.upiIntent}
                    amount={parseFloat(merchantData?.amount || '100.00')}
                    currency={merchantData?.currency || 'INR'}
                  />

                  {/* Simulate Payment Button (Test Mode Only) */}
                  {merchantData?.testMode && (
                    <button
                      onClick={async () => {
                        if (qrPayment.payment?.paymentId) {
                          try {
                            await api.settlePayment(qrPayment.payment.paymentId);
                          } catch (err) {
                            console.error('Failed to simulate payment:', err);
                          }
                        }
                      }}
                      className="mt-4 w-full bg-green-600 text-white py-2 rounded-md hover:bg-green-700 transition flex items-center justify-center"
                    >
                      <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      Simulate Payment Success
                    </button>
                  )}

                  {/* Cancel Button */}
                  <button
                    onClick={async () => {
                      try {
                        if (qrPayment.payment?.paymentId) {
                          await api.cancelPayment(qrPayment.payment.paymentId);
                        }
                      } catch (err) {
                        console.error('Failed to cancel payment:', err);
                      }
                      qrPayment.reset();
                      setError(null);
                    }}
                    className="mt-2 w-full bg-gray-200 text-gray-700 py-2 rounded-md hover:bg-gray-300 transition"
                  >
                    Cancel
                  </button>
                </div>
              )}

              {qrPayment.status === 'expired' && (
                // Expired state
                <div className="text-center py-8">
                  <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 mb-4">
                    <svg
                      className="w-8 h-8 text-gray-500"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                  </div>
                  <h3 className="text-lg font-medium text-gray-800 mb-2">
                    QR Code Expired
                  </h3>
                  <p className="text-sm text-gray-500 mb-6">
                    Please generate a new QR code to continue
                  </p>
                  <button
                    onClick={() => {
                      qrPayment.reset();
                      setError(null);
                    }}
                    className="w-full bg-purple-600 text-white py-3 rounded-md font-semibold hover:bg-purple-700 transition"
                  >
                    Try Again
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Test Cards */}
        {paymentMethod === 'card' && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-sm font-semibold mb-3 text-gray-500 uppercase tracking-wide">
              Test Cards
            </h3>
            <div className="grid grid-cols-2 gap-2">
              {testCards.map((card) => (
                <button
                  key={card.number}
                  onClick={() =>
                    setCardForm({ ...cardForm, cardNumber: card.number })
                  }
                  className={`text-left px-3 py-2 rounded border text-sm transition ${
                    cardForm.cardNumber === card.number
                      ? 'bg-blue-50 border-blue-300'
                      : 'bg-gray-50 border-gray-200 hover:bg-gray-100'
                  }`}
                >
                  <div className="font-mono text-xs text-gray-700">
                    {card.number}
                  </div>
                  <div className="text-gray-500 text-xs">{card.label}</div>
                </button>
              ))}
            </div>
            <p className="text-xs text-gray-400 mt-3 text-center">
              All cards: Expiry 12/2028, CVV 123
            </p>
          </div>
        )}

        {/* Test VPAs */}
        {paymentMethod === 'upi' && !upiStatus && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-sm font-semibold mb-3 text-gray-500 uppercase tracking-wide">
              Test UPI IDs
            </h3>
            <div className="grid grid-cols-2 gap-2">
              {testVpas.map((item) => (
                <button
                  key={item.vpa}
                  onClick={() =>
                    setUpiForm({ ...upiForm, customerVpa: item.vpa })
                  }
                  className={`text-left px-3 py-2 rounded border text-sm transition ${
                    upiForm.customerVpa === item.vpa
                      ? 'bg-green-50 border-green-300'
                      : 'bg-gray-50 border-gray-200 hover:bg-gray-100'
                  }`}
                >
                  <div className="font-mono text-xs text-gray-700">
                    {item.vpa}
                  </div>
                  <div className="text-gray-500 text-xs">{item.description}</div>
                </button>
              ))}
            </div>
            <p className="text-xs text-gray-400 mt-3 text-center">
              Test mode: approval is auto-simulated
            </p>
          </div>
        )}

        {/* UPI QR Test Info (only in test mode) */}
        {paymentMethod === 'upi-qr' && !qrPayment.status && merchantData?.testMode && (
          <div className="bg-amber-50 rounded-lg shadow-md p-6 border border-amber-200">
            <h3 className="text-sm font-semibold mb-3 text-amber-700 uppercase tracking-wide flex items-center">
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
              Test Mode Active
            </h3>
            <div className="text-sm text-amber-800 space-y-2">
              <p>In test mode, you can simulate payment completion by:</p>
              <ul className="list-disc list-inside text-xs space-y-1 mt-2">
                <li>Clicking the "Simulate Payment Success" button after QR is displayed</li>
                <li>Payment status will update automatically via polling</li>
              </ul>
            </div>
          </div>
        )}

        {/* Back Link */}
        <div className="text-center mt-6">
          <button
            onClick={() => navigate('/')}
            className="text-gray-500 hover:text-gray-700 text-sm"
          >
            &larr; Back to Merchant Portal
          </button>
        </div>
      </div>
    </div>
  );
}
