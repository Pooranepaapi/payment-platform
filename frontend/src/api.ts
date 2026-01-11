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

const API_BASE = 'http://localhost:8080';

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
  // UPI Collect APIs (v0 - existing)
  // ============================================

  async createPayment(
    paymentData: CreatePaymentRequest
  ): Promise<CreatePaymentResponse> {
    const response = await fetch(`${API_BASE}/api/payments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(paymentData),
    });
    return response.json();
  },

  async upiCollect(
    paymentId: number,
    customerVpa: string,
    contractId: string | null = null
  ): Promise<UpiCollectResponse> {
    const response = await fetch(`${API_BASE}/api/payments/upi/collect`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ paymentId, customerVpa, contractId }),
    });
    return response.json();
  },

  async simulateUpiApproval(
    transactionId: number | string,
    customerVpa: string = 'success@upi'
  ): Promise<TransactionResponse> {
    const response = await fetch(
      `${API_BASE}/api/transactions/${transactionId}/simulate-approval?customerVpa=${encodeURIComponent(customerVpa)}`,
      { method: 'POST' }
    );
    return response.json();
  },

  // ============================================
  // UPI QR APIs (v1 - NEW)
  // ============================================

  /**
   * Create a new payment order for QR payment
   * POST /api/v1/payments
   */
  async createPaymentV1(
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
};
