# 🎨 Claude Frontend Prompt  
## UPI QR Checkout – Phase-1 (React + TypeScript)

---

## Role
You are a **Senior Frontend Engineer** building a **realistic payment gateway checkout UI**.

This is **not a demo UI**. It should feel like a stripped-down **Razorpay / PhonePe checkout**.

---

## Objective
Build a **Phase-1 React frontend** that integrates with a backend **UPI QR Payment Gateway**.

The frontend:
- Does **NOT** generate QR codes
- Only **renders backend-generated QR**
- Uses **polling** to track payment status
- Handles **QR expiry via backend response**

---

## Tech Stack (Strict)
- React 18+
- TypeScript
- Vite
- Axios
- Tailwind CSS
- React Router

---

## Application Flow (End-to-End)

```
Create Payment
   ↓
Show QR + Timer
   ↓
Poll Status (every 5s)
   ↓
SUCCESS / FAILED / EXPIRED
```

---

## Screens (Must Implement)

### 1️⃣ Payment Creation Page
**Route:** `/pay`

- Input:
  - Amount
  - Merchant selector (dropdown, static list for now)
- CTA:
  - “Pay via UPI QR”
- On submit:
  - Call `POST /api/v1/payments`
  - Navigate to `/pay/{paymentId}`

---

### 2️⃣ QR Checkout Page
**Route:** `/pay/:paymentId`

Display:
- Backend-generated **QR image** (Base64)
- Amount
- Merchant name
- Countdown timer (derived from `expiresAt`)
- Payment status badge

Behavior:
- Poll `GET /api/v1/payments/{paymentId}` every **5 seconds**
- Stop polling on:
  - SUCCESS
  - FAILED
  - EXPIRED
- If EXPIRED:
  - Show “QR expired” message
  - Disable payment
  - Show “Create New Payment” CTA

---

### 3️⃣ Payment Result Page
**Route:** `/result/:paymentId`

Display:
- Final payment status
- Transaction reference
- Merchant name
- Amount
- CTA:
  - “Start New Payment”

---

## API Contracts (Assume Backend)

### Create Payment
```
POST /api/v1/payments
```

Response:
```json
{
  "paymentId": "uuid",
  "amount": 250.00,
  "status": "PENDING",
  "expiresAt": "2026-01-10T14:30:00Z"
}
```

---

### Get Payment Status
```
GET /api/v1/payments/{paymentId}
```

Response:
```json
{
  "paymentId": "uuid",
  "status": "SUCCESS | FAILED | PENDING | EXPIRED",
  "amount": 250.00,
  "merchantName": "ABC Store",
  "expiresAt": "2026-01-10T14:30:00Z"
}
```

---

### Get QR
```
GET /api/v1/payments/{paymentId}/qr
```

Response:
```json
{
  "qrImageBase64": "data:image/png;base64,...",
  "upiIntent": "upi://pay?...",
  "expiresAt": "2026-01-10T14:30:00Z"
}
```

---

## State Management
- Use React hooks only
- Suggested structure:
  - `usePayment(paymentId)`
  - `usePolling(paymentId)`
- Stop polling on terminal states

---

## UX Requirements
- Mobile-first design
- Clean layout
- Visible countdown timer
- Clear status transitions
- No page refreshes
- Graceful error handling

---

## Folder Structure (Expected)

```
src/
 ├── api/
 ├── components/
 │   ├── QRCard.tsx
 │   ├── StatusBadge.tsx
 │   └── Timer.tsx
 ├── pages/
 │   ├── CreatePayment.tsx
 │   ├── Checkout.tsx
 │   └── Result.tsx
 ├── hooks/
 ├── routes/
 ├── types/
 ├── utils/
 └── App.tsx
```

---

## Non-Functional Requirements
- `.env.example` for backend URL
- Reusable components
- No hard-coded URLs
- Meaningful component names
- README with run instructions

---

## Coding Philosophy
- Simple > fancy
- Predictable state transitions
- UI reflects backend truth
- No business logic duplication

---

## Deliverables
- Fully runnable React app
- Clean TypeScript
- Tailwind styling
- Works seamlessly with Phase-1 backend
