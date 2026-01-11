import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MerchantPage from './pages/MerchantPage';
import CheckoutPage from './pages/CheckoutPage';
import PaymentPage from './pages/PaymentPage';
import ThreeDSPage from './pages/ThreeDSPage';
import TransactionPage from './pages/TransactionPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MerchantPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/payment" element={<PaymentPage />} />
        <Route path="/3ds" element={<ThreeDSPage />} />
        <Route path="/transaction/:transactionId" element={<TransactionPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
