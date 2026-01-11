import type { StatusBadgeProps, PaymentStatus, TransactionStatus } from '../types';

type StatusConfigValue = {
  bg: string;
  text: string;
  label?: string;
};

const statusConfig: Record<PaymentStatus | TransactionStatus, StatusConfigValue> = {
  // Payment statuses
  PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-800' },
  PROCESSING: { bg: 'bg-blue-100', text: 'text-blue-800' },
  SUCCESS: { bg: 'bg-green-100', text: 'text-green-800' },
  FAILED: { bg: 'bg-red-100', text: 'text-red-800' },
  EXPIRED: { bg: 'bg-gray-100', text: 'text-gray-600' },
  REFUNDED: { bg: 'bg-purple-100', text: 'text-purple-800' },
  CREATED: { bg: 'bg-gray-100', text: 'text-gray-800' },
  QR_GENERATED: { bg: 'bg-blue-100', text: 'text-blue-800', label: 'QR Generated' },
  CANCELLED: { bg: 'bg-gray-100', text: 'text-gray-600' },

  // Transaction statuses
  PENDING_3DS: { bg: 'bg-yellow-100', text: 'text-yellow-800', label: 'Pending 3DS' },
  INITIATED: { bg: 'bg-yellow-100', text: 'text-yellow-800' },
  AUTHORIZED: { bg: 'bg-blue-100', text: 'text-blue-800' },
  CAPTURED: { bg: 'bg-green-100', text: 'text-green-800' },
  VOIDED: { bg: 'bg-gray-100', text: 'text-gray-800' },
  DECLINED: { bg: 'bg-red-100', text: 'text-red-800' },
};

const sizeClasses = {
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-sm',
  lg: 'px-3 py-1.5 text-base',
};

export default function StatusBadge({ status, size = 'md' }: StatusBadgeProps) {
  const config = statusConfig[status] || {
    bg: 'bg-gray-100',
    text: 'text-gray-800',
  };
  const label = config.label || status;

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full ${config.bg} ${config.text} ${sizeClasses[size]}`}
    >
      {label}
    </span>
  );
}
