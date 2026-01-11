import { useState, useEffect, useRef, useCallback } from 'react';
import { api } from '../api';
import type {
  PaymentStatus,
  CreatePaymentResponse,
  GenerateQRResponse,
  QRProcessingStatus,
  CreatePaymentRequest,
} from '../types';

interface UseQRPaymentOptions {
  onSuccess: (paymentId: number, transactionId?: number) => void;
  onError: (error: string) => void;
  onExpire: () => void;
  pollIntervalMs?: number;
}

interface QRPaymentState {
  status: QRProcessingStatus;
  payment: CreatePaymentResponse | null;
  qrData: GenerateQRResponse | null;
  error: string | null;
  paymentStatus: PaymentStatus | null;
}

export function useQRPayment({
  onSuccess,
  onError,
  onExpire,
  pollIntervalMs = 5000,
}: UseQRPaymentOptions) {
  const [state, setState] = useState<QRPaymentState>({
    status: null,
    payment: null,
    qrData: null,
    error: null,
    paymentStatus: null,
  });

  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const mountedRef = useRef(true);

  // Cleanup on unmount
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
      }
    };
  }, []);

  const stopPolling = useCallback(() => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  }, []);

  const startPolling = useCallback(
    (paymentId: number) => {
      stopPolling();

      pollingRef.current = setInterval(async () => {
        if (!mountedRef.current) return;

        try {
          const response = await api.getPaymentStatus(paymentId);

          if (!mountedRef.current) return;

          setState((prev) => ({ ...prev, paymentStatus: response.status }));

          if (response.status === 'SUCCESS') {
            stopPolling();
            onSuccess(paymentId, response.transactionId);
          } else if (response.status === 'FAILED') {
            stopPolling();
            onError(response.failureReason || 'Payment failed');
          } else if (response.status === 'EXPIRED') {
            stopPolling();
            setState((prev) => ({ ...prev, status: 'expired' }));
            onExpire();
          }
        } catch (err) {
          if (!mountedRef.current) return;
          console.error('Polling error:', err);
          // Continue polling on transient errors
        }
      }, pollIntervalMs);
    },
    [pollIntervalMs, onSuccess, onError, onExpire, stopPolling]
  );

  const initiateQRPayment = useCallback(
    async (paymentData: CreatePaymentRequest) => {
      setState({
        status: 'creating',
        payment: null,
        qrData: null,
        error: null,
        paymentStatus: null,
      });

      try {
        // Step 1: Create payment
        const paymentResponse = await api.createPaymentV1(paymentData);

        if (!mountedRef.current) return;

        if (!paymentResponse.paymentId) {
          throw new Error(
            (paymentResponse as { message?: string }).message ||
              'Failed to create payment'
          );
        }

        setState((prev) => ({
          ...prev,
          status: 'generating',
          payment: paymentResponse,
        }));

        // Step 2: Generate QR
        const qrResponse = await api.generateQR(paymentResponse.paymentId);

        if (!mountedRef.current) return;

        if (!qrResponse.qrImageBase64) {
          throw new Error(
            qrResponse.message || 'Failed to generate QR code'
          );
        }

        setState((prev) => ({
          ...prev,
          status: 'polling',
          qrData: qrResponse,
          paymentStatus: 'PENDING',
        }));

        // Step 3: Start polling
        startPolling(paymentResponse.paymentId);
      } catch (err) {
        if (!mountedRef.current) return;
        const message =
          err instanceof Error ? err.message : 'An error occurred';
        setState((prev) => ({
          ...prev,
          status: null,
          error: message,
        }));
        onError(message);
      }
    },
    [startPolling, onError]
  );

  const handleExpiry = useCallback(() => {
    stopPolling();
    setState((prev) => ({
      ...prev,
      status: 'expired',
      paymentStatus: 'EXPIRED',
    }));
    onExpire();
  }, [stopPolling, onExpire]);

  const reset = useCallback(() => {
    stopPolling();
    setState({
      status: null,
      payment: null,
      qrData: null,
      error: null,
      paymentStatus: null,
    });
  }, [stopPolling]);

  return {
    ...state,
    initiateQRPayment,
    handleExpiry,
    reset,
    stopPolling,
  };
}
