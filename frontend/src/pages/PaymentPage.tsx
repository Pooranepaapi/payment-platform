import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import type { TestCard } from '../types';

interface PaymentFormState {
  cardNumber: string;
  expiryMonth: string;
  expiryYear: string;
  cvv: string;
  amount: string;
  currency: string;
  cardholderName: string;
}

const testCards: TestCard[] = [
  { number: '4111111111111111', label: '3DS Success' },
  { number: '4000000000000002', label: '3DS Decline' },
  { number: '5500000000000004', label: 'No 3DS' },
  { number: '4000000000000069', label: 'Insufficient Funds' },
];

export default function PaymentPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<PaymentFormState>({
    cardNumber: '4111111111111111',
    expiryMonth: '12',
    expiryYear: '2028',
    cvv: '123',
    amount: '100.00',
    currency: 'USD',
    cardholderName: 'Test User',
  });

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (type: 'auth' | 'sale') => {
    setLoading(true);
    setError(null);

    try {
      const payload = {
        ...form,
        amount: parseFloat(form.amount),
      };

      const response =
        type === 'auth' ? await api.authorize(payload) : await api.sale(payload);

      if (response.threeDSRequired) {
        navigate(
          `/3ds?sessionId=${response.threeDSSessionId}&txnId=${response.transactionId}`
        );
      } else if (response.transactionId) {
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

  return (
    <div className="min-h-screen bg-gray-100 py-8 px-4">
      <div className="max-w-xl mx-auto">
        <h1 className="text-3xl font-bold text-center mb-8 text-gray-800">
          Payment Gateway
        </h1>

        {/* Payment Form */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4 text-gray-700">
            Make a Payment
          </h2>

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">
                Card Number
              </label>
              <input
                type="text"
                name="cardNumber"
                value={form.cardNumber}
                onChange={handleChange}
                maxLength={16}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">
                  Expiry MM
                </label>
                <input
                  type="text"
                  name="expiryMonth"
                  value={form.expiryMonth}
                  onChange={handleChange}
                  maxLength={2}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">
                  Expiry YYYY
                </label>
                <input
                  type="text"
                  name="expiryYear"
                  value={form.expiryYear}
                  onChange={handleChange}
                  maxLength={4}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">
                  CVV
                </label>
                <input
                  type="text"
                  name="cvv"
                  value={form.cvv}
                  onChange={handleChange}
                  maxLength={4}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">
                Cardholder Name
              </label>
              <input
                type="text"
                name="cardholderName"
                value={form.cardholderName}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">
                  Amount
                </label>
                <input
                  type="number"
                  name="amount"
                  value={form.amount}
                  onChange={handleChange}
                  step="0.01"
                  min="0.01"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">
                  Currency
                </label>
                <select
                  name="currency"
                  value={form.currency}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="USD">USD</option>
                  <option value="EUR">EUR</option>
                  <option value="GBP">GBP</option>
                </select>
              </div>
            </div>

            <div className="flex gap-4 pt-4">
              <button
                onClick={() => handleSubmit('auth')}
                disabled={loading}
                className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition disabled:opacity-50"
              >
                {loading ? 'Processing...' : 'Authorize'}
              </button>
              <button
                onClick={() => handleSubmit('sale')}
                disabled={loading}
                className="flex-1 bg-green-600 text-white py-2 px-4 rounded-md hover:bg-green-700 transition disabled:opacity-50"
              >
                {loading ? 'Processing...' : 'Sale'}
              </button>
            </div>
          </div>
        </div>

        {/* Test Cards */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h3 className="text-lg font-semibold mb-3 text-gray-700">Test Cards</h3>
          <div className="grid grid-cols-2 gap-2">
            {testCards.map((card) => (
              <button
                key={card.number}
                onClick={() => setForm({ ...form, cardNumber: card.number })}
                className={`text-left px-3 py-2 rounded border text-sm ${
                  form.cardNumber === card.number
                    ? 'bg-blue-50 border-blue-300'
                    : 'bg-gray-50 border-gray-200 hover:bg-gray-100'
                }`}
              >
                <div className="font-mono text-xs">{card.number}</div>
                <div className="text-gray-600">{card.label}</div>
              </button>
            ))}
          </div>
          <p className="text-xs text-gray-500 mt-3">
            All cards: Expiry 12/2028, CVV 123
          </p>
        </div>

        {/* Transaction Lookup */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold mb-3 text-gray-700">
            Lookup Transaction
          </h3>
          <div className="flex gap-2">
            <input
              type="text"
              id="lookupTxnId"
              placeholder="TXN-XXXXXXXX"
              className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              onClick={() => {
                const txnIdInput = document.getElementById(
                  'lookupTxnId'
                ) as HTMLInputElement;
                const txnId = txnIdInput?.value;
                if (txnId) navigate(`/transaction/${txnId}`);
              }}
              className="bg-gray-600 text-white py-2 px-4 rounded-md hover:bg-gray-700 transition"
            >
              Lookup
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
