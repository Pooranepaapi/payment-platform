import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import type { MerchantFormState, GenerateQRResponse } from '../types';

export default function MerchantPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<MerchantFormState>({
    merchantId: 'MER001',
    apiKey: 'sk_test_xxxxxxxxxxxx',
    orderId: `ORD-${Date.now()}`,
    amount: '100.00',
    currency: 'INR',
    description: 'Test Order',
    customerEmail: 'customer@example.com',
    testMode: true,
  });

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    // Store merchant data in sessionStorage for the payment page
    sessionStorage.setItem('merchantData', JSON.stringify(form));
    navigate('/checkout');
  };

  const [staticQr, setStaticQr] = useState<GenerateQRResponse | null>(null);
  const [staticQrLoading, setStaticQrLoading] = useState(false);

  const generateOrderId = () => {
    setForm({ ...form, orderId: `ORD-${Date.now()}` });
  };

  const handleGenerateStaticQR = async () => {
    setStaticQrLoading(true);
    try {
      const response = await api.generateStaticQR(form.merchantId);
      setStaticQr(response);
    } catch (err) {
      console.error('Failed to generate static QR:', err);
    } finally {
      setStaticQrLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 py-8 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">Merchant Portal</h1>
          <p className="text-slate-400">Configure payment request</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Test Mode Toggle */}
          <div className={`rounded-lg p-4 border ${form.testMode ? 'bg-amber-900/30 border-amber-700' : 'bg-slate-800 border-slate-700'}`}>
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <svg
                  className={`w-5 h-5 mr-3 ${form.testMode ? 'text-amber-400' : 'text-slate-400'}`}
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                  />
                </svg>
                <div>
                  <div className="font-semibold text-white">Test Mode</div>
                  <div className="text-sm text-slate-400">
                    {form.testMode
                      ? 'Simulate payments without real transactions'
                      : 'Live mode - real payments will be processed'}
                  </div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setForm({ ...form, testMode: !form.testMode })}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                  form.testMode ? 'bg-amber-500' : 'bg-slate-600'
                }`}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                    form.testMode ? 'translate-x-6' : 'translate-x-1'
                  }`}
                />
              </button>
            </div>
            {form.testMode && (
              <div className="mt-3 text-xs text-amber-300 bg-amber-900/50 rounded px-3 py-2">
                Test mode enabled: You can simulate payment success on the checkout page
              </div>
            )}
          </div>

          {/* Merchant Credentials */}
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <h2 className="text-lg font-semibold text-white mb-4 flex items-center">
              <svg
                className="w-5 h-5 mr-2 text-blue-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"
                />
              </svg>
              Merchant Credentials
            </h2>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">
                  Merchant ID
                </label>
                <input
                  type="text"
                  name="merchantId"
                  value={form.merchantId}
                  onChange={handleChange}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">
                  API Key
                </label>
                <input
                  type="password"
                  name="apiKey"
                  value={form.apiKey}
                  onChange={handleChange}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          </div>

          {/* Order Details */}
          <div className="bg-slate-800 rounded-lg p-6 border border-slate-700">
            <h2 className="text-lg font-semibold text-white mb-4 flex items-center">
              <svg
                className="w-5 h-5 mr-2 text-green-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                />
              </svg>
              Order Details
            </h2>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">
                  Order ID
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    name="orderId"
                    value={form.orderId}
                    onChange={handleChange}
                    className="flex-1 px-3 py-2 bg-slate-700 border border-slate-600 rounded-md text-white font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <button
                    type="button"
                    onClick={generateOrderId}
                    className="px-3 py-2 bg-slate-600 text-slate-300 rounded-md hover:bg-slate-500 transition"
                  >
                    Generate
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1">
                    Amount
                  </label>
                  <input
                    type="number"
                    name="amount"
                    value={form.amount}
                    onChange={handleChange}
                    step="0.01"
                    min="0.01"
                    className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1">
                    Currency
                  </label>
                  <select
                    name="currency"
                    value={form.currency}
                    onChange={handleChange}
                    className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="USD">USD - US Dollar</option>
                    <option value="EUR">EUR - Euro</option>
                    <option value="GBP">GBP - British Pound</option>
                    <option value="INR">INR - Indian Rupee</option>
                    <option value="JPY">JPY - Japanese Yen</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">
                  Description
                </label>
                <input
                  type="text"
                  name="description"
                  value={form.description}
                  onChange={handleChange}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">
                  Customer Email
                </label>
                <input
                  type="email"
                  name="customerEmail"
                  value={form.customerEmail}
                  onChange={handleChange}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          </div>

          {/* Summary */}
          <div className="bg-blue-900/30 rounded-lg p-6 border border-blue-800">
            <h2 className="text-lg font-semibold text-white mb-4">
              Payment Summary
            </h2>
            <div className="flex justify-between items-center">
              <div>
                <div className="text-slate-400 text-sm">Order: {form.orderId}</div>
                <div className="text-slate-400 text-sm">{form.description}</div>
              </div>
              <div className="text-right">
                <div className="text-3xl font-bold text-white">
                  {parseFloat(form.amount || '0').toFixed(2)}
                </div>
                <div className="text-slate-400">{form.currency}</div>
              </div>
            </div>
          </div>

          {/* Submit */}
          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition flex items-center justify-center"
          >
            <svg
              className="w-5 h-5 mr-2"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M17 8l4 4m0 0l-4 4m4-4H3"
              />
            </svg>
            Proceed to Checkout
          </button>
        </form>

        {/* Static QR Code */}
        <div className="mt-6 bg-slate-800 rounded-lg p-6 border border-slate-700">
          <h2 className="text-lg font-semibold text-white mb-4 flex items-center">
            <svg className="w-5 h-5 mr-2 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
            </svg>
            Static QR Code
          </h2>
          <p className="text-slate-400 text-sm mb-4">
            Generate a permanent QR code for countertop payments. Customers enter the amount in their UPI app.
          </p>
          <button
            type="button"
            onClick={handleGenerateStaticQR}
            disabled={staticQrLoading}
            className="w-full bg-purple-600 text-white py-2 rounded-lg font-semibold hover:bg-purple-700 transition disabled:opacity-50"
          >
            {staticQrLoading ? 'Generating...' : 'Generate Static QR'}
          </button>

          {staticQr && staticQr.qrImageBase64 && (
            <div className="mt-4 flex flex-col items-center">
              <img
                src={staticQr.qrImageBase64}
                alt="Static QR Code"
                className="w-48 h-48 rounded-lg border border-slate-600"
              />
              <p className="mt-2 text-slate-400 text-sm">Customer enters amount in UPI app</p>
              <p className="mt-1 text-slate-500 text-xs font-mono break-all">{staticQr.upiIntent}</p>
            </div>
          )}
        </div>

        {/* API Info */}
        <div className="mt-8 text-center text-slate-500 text-sm">
          <p>This simulates the merchant-side integration.</p>
          <p>In production, this data would come from your backend.</p>
        </div>
      </div>
    </div>
  );
}
