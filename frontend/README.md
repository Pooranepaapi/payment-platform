# Payment Gateway Frontend

A React-based checkout interface for the UPI Payment Gateway. Supports UPI collect payments with real-time status polling.

---

## Quick Start

### Prerequisites
- **Node.js 18+**
- **npm** or **yarn**
- Backend running at `http://localhost:8080`

### Run the Application

```bash
# Install dependencies
npm install

# Start development server (port 5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

---

## Tech Stack

- **React 19** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool & dev server
- **React Router v6** - Client-side routing
- **Tailwind CSS** - Styling

---

## Project Structure

```
src/
├── api.ts              # API client for backend communication
├── App.tsx             # Main app with routing
├── main.tsx            # Entry point
├── index.css           # Global styles (Tailwind)
│
├── pages/
│   ├── MerchantPage.tsx    # Merchant selection & order creation
│   ├── CheckoutPage.tsx    # UPI payment flow
│   ├── PaymentPage.tsx     # Payment details view
│   ├── TransactionPage.tsx # Transaction status & details
│   └── ThreeDSPage.tsx     # 3DS authentication (Phase 2)
│
├── components/
│   ├── QRCard.tsx          # QR code display component
│   ├── StatusBadge.tsx     # Payment status indicator
│   ├── Timer.tsx           # Countdown timer for expiry
│   └── index.ts            # Component exports
│
├── hooks/
│   └── useQRPayment.ts     # QR payment polling hook
│
└── types/
    └── index.ts            # TypeScript type definitions
```

---

## Pages & Flow

### 1. Merchant Page (`/`)
- Enter merchant details (ID, order ID, amount)
- Stores data in sessionStorage
- Navigates to checkout

### 2. Checkout Page (`/checkout`)
- Displays payment summary
- UPI VPA input field
- Initiates UPI collect request
- Polls for payment status
- Redirects to transaction page on completion

### 3. Transaction Page (`/transaction/:id`)
- Shows final payment status (SUCCESS/FAILED)
- Displays transaction details
- PSP reference numbers

---

## API Integration

The frontend communicates with the backend via `src/api.ts`:

```typescript
// Create payment
api.createPayment({ merchantId, amount, currency })

// Initiate UPI collect
api.upiCollect(paymentId, customerVpa, contractId)

// Get transaction status
api.getTransaction(transactionId)

// Simulate approval (test mode)
api.simulateUpiApproval(transactionId, customerVpa)
```

### Backend URL
Default: `http://localhost:8080`

To change, update `API_BASE` in `src/api.ts`.

---

## Test Flow

1. Start the backend (`mvn spring-boot:run` in payment-backend)
2. Start the frontend (`npm run dev`)
3. Navigate to `http://localhost:5173`
4. Enter test merchant: `MER001`
5. Enter amount: `250.00`
6. Click "Proceed to Payment"
7. Enter test VPA: `success@upi`
8. Click "Pay via UPI"
9. Wait for auto-approval simulation
10. View transaction result

### Test VPAs

| VPA | Behavior |
|-----|----------|
| `success@upi` | Payment succeeds |
| `fail@upi` | Payment fails |
| `timeout@upi` | Payment stays pending |
| `insufficient@upi` | Insufficient funds error |

---

## Development

### Scripts

```bash
npm run dev      # Start dev server with HMR
npm run build    # Production build to dist/
npm run preview  # Preview production build
npm run lint     # Run ESLint
```

### Code Style

- TypeScript strict mode enabled
- ESLint with React Hooks plugin
- Tailwind for styling (no CSS modules)

---

## Configuration

### Vite Config (`vite.config.ts`)
```typescript
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173
  }
})
```

### Tailwind Config (`tailwind.config.js`)
```javascript
module.exports = {
  content: ['./src/**/*.{js,ts,jsx,tsx}'],
  // ... theme customization
}
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **CORS errors** | Ensure backend has CORS enabled for `localhost:5173` |
| **API connection refused** | Check backend is running on port 8080 |
| **Payment stuck on PENDING** | Use `success@upi` VPA for auto-approval |
| **Build errors** | Run `npm install` to ensure dependencies |

---

## Related

- **Backend**: See `../payment-backend/README.md`
- **API Docs**: See `../payment-backend/docs/ER_DIAGRAM_AND_API.md`

---

**Port**: 5173
**Backend Port**: 8080
