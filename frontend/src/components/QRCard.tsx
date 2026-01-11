import type { QRCardProps } from '../types';

export default function QRCard({
  qrImageBase64,
  upiIntent,
  amount,
  currency,
  merchantName,
}: QRCardProps) {
  const handleOpenUpiApp = () => {
    // Try to open UPI app on mobile devices
    window.location.href = upiIntent;
  };

  // Ensure the QR image has the proper data URI prefix
  const qrSrc = qrImageBase64.startsWith('data:')
    ? qrImageBase64
    : `data:image/png;base64,${qrImageBase64}`;

  return (
    <div className="flex flex-col items-center">
      {/* QR Code Display */}
      <div className="bg-white p-4 rounded-xl shadow-lg border-2 border-purple-100">
        <img
          src={qrSrc}
          alt="UPI QR Code"
          className="w-48 h-48 md:w-56 md:h-56"
        />
      </div>

      {/* Amount Display */}
      <div className="mt-4 text-center">
        {merchantName && (
          <p className="text-sm text-gray-600 font-medium mb-1">{merchantName}</p>
        )}
        <p className="text-sm text-gray-500">Scan to pay</p>
        <p className="text-2xl font-bold text-gray-800">
          {currency} {amount.toFixed(2)}
        </p>
      </div>

      {/* Mobile UPI Intent Link */}
      <button
        onClick={handleOpenUpiApp}
        className="mt-4 w-full bg-purple-600 text-white py-3 px-6 rounded-lg font-medium hover:bg-purple-700 transition flex items-center justify-center gap-2"
      >
        <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" />
        </svg>
        Open UPI App
      </button>

      {/* Instructions */}
      <div className="mt-4 text-xs text-gray-400 text-center">
        <p>Or scan with any UPI app:</p>
        <p className="mt-1">Google Pay, PhonePe, Paytm, etc.</p>
      </div>
    </div>
  );
}
