package org.personal.enums;

/**
 * QR Code Types
 *
 * Phase-1: Only supports DYNAMIC QR codes
 * Phase-2+: Can add STATIC QR codes
 *
 * DYNAMIC QR:
 * - Generated per payment
 * - Includes amount, merchant details
 * - Expires with payment (15 minutes default)
 * - Used for one-time transactions
 *
 * STATIC QR (Phase-2):
 * - Generated once per merchant
 * - No amount in QR code
 * - Never expires
 * - Customer enters amount manually
 */
public enum QrType {
    DYNAMIC    // Phase-1: Dynamic QR with payment amount
    // STATIC  // Phase-2: Static merchant QR
}
