// ============================================
// Payment Status Types
// ============================================

export type PaymentStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'SUCCESS'
  | 'FAILED'
  | 'EXPIRED'
  | 'REFUNDED'
  | 'CREATED'
  | 'QR_GENERATED'
  | 'CANCELLED';

export type TransactionStatus =
  | 'PENDING'
  | 'PENDING_3DS'
  | 'INITIATED'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'SUCCESS'
  | 'VOIDED'
  | 'REFUNDED'
  | 'DECLINED'
  | 'FAILED';

export type PaymentMethod = 'CARD' | 'UPI' | 'UPI_QR';

export type TransactionType =
  | 'AUTH'
  | 'SALE'
  | 'CAPTURE'
  | 'VOID'
  | 'REFUND'
  | 'UPI_COLLECT'
  | 'UPI_QR';

// ============================================
// Merchant Data (Session Storage)
// ============================================

export interface MerchantData {
  merchantId: string;
  apiKey: string;
  orderId: string;
  amount: string;
  currency: string;
  description: string;
  customerEmail: string;
  testMode: boolean;
}

// ============================================
// API Request Types
// ============================================

export interface CreatePaymentRequest {
  merchantId: string;
  merchantOrderId: string;
  amount: number;
  currency: string;
  testMode?: boolean;
  customerMobile?: string;
  description?: string;
}

export interface UpiCollectRequest {
  paymentId: number;
  customerVpa: string;
  contractId?: string | null;
}

export interface CardPaymentRequest {
  cardNumber: string;
  expiryMonth: string;
  expiryYear: string;
  cvv: string;
  cardholderName: string;
  amount: number;
  currency: string;
}

export interface Authenticate3DSRequest {
  otp: string;
}

// ============================================
// API Response Types
// ============================================

export interface CreatePaymentResponse {
  paymentId: number;
  paymentUuid: string;
  status: PaymentStatus;
  dueAmount?: number;
  paidAmount?: number;
  refundedAmount?: number;
  currency: string;
  merchantOrderId: string;
  testMode?: boolean;
  createdAt: string;
  expiresAt?: string;
  message?: string;
}

export interface GenerateQRResponse {
  paymentId: number;
  qrType?: string;
  qrImageBase64: string;
  qrImageSvg?: string;
  upiIntent: string;
  expiresAt: string;
  status: PaymentStatus;
  message?: string;
}

export interface PaymentStatusResponse {
  paymentId: number;
  paymentUuid: string;
  status: PaymentStatus;
  dueAmount?: number;
  paidAmount?: number;
  refundedAmount?: number;
  currency: string;
  merchantOrderId: string;
  transactionId?: number;
  transactionUuid?: string;
  failureReason?: string;
  createdAt: string;
  updatedAt?: string;
  expiresAt?: string;
}

export interface UpiCollectResponse {
  transactionId: number;
  transactionUuid: string;
  paymentId: number;
  status: TransactionStatus;
  pspReferenceId?: string;
  bankReferenceId?: string;
  failureReason?: string;
  createdAt?: string;
  message?: string;
}

export interface TransactionResponse {
  transactionId: number;
  transactionUuid?: string;
  paymentId?: number;
  paymentUuid?: string;
  status: TransactionStatus;
  txnType?: TransactionType;
  type?: TransactionType;
  paymentMethod?: PaymentMethod;
  amount: number;
  currency?: string;
  maskedCardNumber?: string;
  authCode?: string;
  rrn?: string;
  pspReferenceId?: string;
  bankReferenceId?: string;
  responseCode?: string;
  responseMessage?: string;
  failureReason?: string;
  success?: boolean;
  message?: string;
  createdAt?: string;
}

export interface CardPaymentResponse {
  transactionId: number;
  transactionUuid: string;
  status: TransactionStatus;
  threeDSRequired?: boolean;
  threeDSSessionId?: string;
  message?: string;
}

export interface ThreeDSSessionResponse {
  sessionId: string;
  status: 'PENDING' | 'AUTHENTICATED' | 'FAILED';
  maskedCardNumber: string;
  amount: number;
  currency: string;
  remainingAttempts?: number;
  message?: string;
}

export interface ThreeDSAuthResponse {
  status: 'PENDING' | 'AUTHENTICATED' | 'FAILED';
  remainingAttempts?: number;
  message?: string;
}

// ============================================
// Component Props Types
// ============================================

export interface QRCardProps {
  qrImageBase64: string;
  upiIntent: string;
  amount: number;
  currency: string;
  merchantName?: string;
}

export interface TimerProps {
  expiresAt: string;
  onExpire: () => void;
  className?: string;
}

export interface StatusBadgeProps {
  status: PaymentStatus | TransactionStatus;
  size?: 'sm' | 'md' | 'lg';
}

// ============================================
// Form State Types
// ============================================

export interface CardFormState {
  cardNumber: string;
  expiryMonth: string;
  expiryYear: string;
  cvv: string;
  cardholderName: string;
}

export interface UpiFormState {
  customerVpa: string;
}

export interface MerchantFormState {
  merchantId: string;
  apiKey: string;
  orderId: string;
  amount: string;
  currency: string;
  description: string;
  customerEmail: string;
  testMode: boolean;
}

// ============================================
// Test Data Types
// ============================================

export interface TestCard {
  number: string;
  label: string;
  brand?: 'visa' | 'mastercard';
}

export interface TestVpa {
  vpa: string;
  label: string;
  description: string;
}

// ============================================
// UI State Types
// ============================================

export type UpiProcessingStatus = 'processing' | 'approving' | 'polling' | null;

export type QRProcessingStatus =
  | 'creating'
  | 'generating'
  | 'polling'
  | 'expired'
  | null;

// ============================================
// Utility Types
// ============================================

export interface ApiError {
  message: string;
  code?: string;
  status?: number;
}
