import type {
  CreatePaymentRequest,
  CreatePaymentResponse,
  GenerateQRResponse,
  PaymentStatusResponse,
  UpiCollectResponse,
  TransactionResponse,
  CardPaymentRequest,
  CardPaymentResponse,
  ThreeDSSessionResponse,
  ThreeDSAuthResponse,
} from './types';

// In production, nginx proxies /api/ to the backend so we use a relative base.
// In local dev, set VITE_API_BASE=http://localhost:8080 in frontend/.env.local
const API_BASE = import.meta.env.VITE_API_BASE ?? '';

export const api = {
  // ============================================
  // Card Payment APIs (v0 - existing)
  // ============================================

  async authorize(paymentData: CardPaymentRequest): Promise<CardPaymentResponse> {
    const response = await fetch(`${API_BASE}/api/payments/auth`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(paymentData),
    });
    return response.json();
  },

  async sale(paymentData: CardPaymentRequest): Promise<CardPaymentResponse> {
    const response = await fetch(`${API_BASE}/api/payments/sale`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(paymentData),
    });
    return response.json();
  },

  async getTransaction(transactionId: number | string): Promise<TransactionResponse> {
    const response = await fetch(`${API_BASE}/api/transactions/${transactionId}`);
    return response.json();
  },

  async capture(
    transactionId: number | string,
    amount: number | null = null
  ): Promise<TransactionResponse> {
    const response = await fetch(
      `${API_BASE}/api/transactions/${transactionId}/capture`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(amount ? { amount } : {}),
      }
    );
    return response.json();
  },

  async voidTransaction(transactionId: number | string): Promise<TransactionResponse> {
    const response = await fetch(
      `${API_BASE}/api/transactions/${transactionId}/void`,
      {
        method: 'POST',
      }
    );
    return response.json();
  },

  async refund(
    transactionId: number | string,
    amount: number | null = null
  ): Promise<TransactionResponse> {
    const response = await fetch(
      `${API_BASE}/api/transactions/${transactionId}/refund`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(amount ? { amount } : {}),
      }
    );
    return response.json();
  },

  // ============================================
  // 3DS APIs (v0 - existing)
  // ============================================

  async complete3DS(transactionId: number | string): Promise<TransactionResponse> {
    const response = await fetch(
      `${API_BASE}/api/transactions/${transactionId}/complete-3ds`,
      {
        method: 'POST',
      }
    );
    return response.json();
  },

  async get3DSSession(sessionId: string): Promise<ThreeDSSessionResponse> {
    const response = await fetch(`${API_BASE}/api/3ds/${sessionId}`);
    return response.json();
  },

  async authenticate3DS(sessionId: string, otp: string): Promise<ThreeDSAuthResponse> {
    const response = await fetch(`${API_BASE}/api/3ds/${sessionId}/authenticate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ otp }),
    });
    return response.json();
  },

  // ============================================
  // UPI Payment APIs (v1)
  // ============================================

  async createPayment(
    paymentData: CreatePaymentRequest
  ): Promise<CreatePaymentResponse> {
    const response = await fetch(`${API_BASE}/api/v1/payments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(paymentData),
    });
    return response.json();
  },

  /**
   * Initiate UPI collect for a payment
   * POST /api/v1/payments/upi/collect
   */
  async upiCollect(
    paymentId: number,
    customerVpa: string
  ): Promise<UpiCollectResponse> {
    const response = await fetch(`${API_BASE}/api/v1/payments/upi/collect`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ paymentId, customerVpa }),
    });
    return response.json();
  },

  /**
   * Generate QR code for a payment
   * POST /api/v1/payments/{paymentId}/qr
   */
  async generateQR(paymentId: number | string): Promise<GenerateQRResponse> {
    const response = await fetch(`${API_BASE}/api/v1/payments/${paymentId}/qr`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });
    return response.json();
  },

  /**
   * Get payment status (with on-read expiry check)
   * GET /api/v1/payments/{paymentId}
   */
  async getPaymentStatus(paymentId: number | string): Promise<PaymentStatusResponse> {
    const response = await fetch(`${API_BASE}/api/v1/payments/${paymentId}`);
    return response.json();
  },

  /**
   * Settle payment (manual trigger for test mode)
   * POST /api/v1/payments/{paymentId}/settle
   */
  async settlePayment(paymentId: number | string): Promise<PaymentStatusResponse> {
    const response = await fetch(`${API_BASE}/api/v1/payments/${paymentId}/settle`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });
    return response.json();
  },

  /**
   * Cancel a payment
   * POST /api/v1/payments/{paymentId}/cancel
   */
  /**
   * Generate a static QR code for a merchant
   * POST /api/v1/payments/static-qr?merchantId={merchantId}
   */
  async generateStaticQR(merchantId: string): Promise<GenerateQRResponse> {
    const response = await fetch(`${API_BASE}/api/v1/payments/static-qr?merchantId=${encodeURIComponent(merchantId)}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });
    return response.json();
  },

  /**
   * Cancel a payment
   * POST /api/v1/payments/{paymentId}/cancel
   */
  async cancelPayment(paymentId: number | string): Promise<PaymentStatusResponse> {
    const response = await fetch(`${API_BASE}/api/v1/payments/${paymentId}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });
    return response.json();
  },
};
