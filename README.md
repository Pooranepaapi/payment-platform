# Payment Platform

A full-stack UPI payment gateway with backend API and React frontend.

## Projects

| Project | Description | Port |
|---------|-------------|------|
| [backend](./backend) | Spring Boot payment API | 8080 |
| [frontend](./frontend) | React checkout UI | 5173 |

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Maven 3.6+

### Start Backend
```bash
cd backend
mvn spring-boot:run
```

### Start Frontend
```bash
cd frontend
npm install
npm run dev
```

### Access
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console

## Architecture

```
┌─────────────┐     HTTP      ┌─────────────┐     JPA      ┌──────────┐
│   Frontend  │ ──────────▶   │   Backend   │ ──────────▶  │    H2    │
│  (React)    │   REST API    │ (Spring)    │              │ Database │
└─────────────┘               └─────────────┘              └──────────┘
     :5173                         :8080
```

## Features

### Phase 1 (Current)
- UPI Collect payments
- Real-time status polling
- Multi-merchant support
- Transaction history
- Simulated PSP for testing

### Phase 2 (Planned)
- Card payments with 3DS
- Webhook notifications
- Settlement & payouts

## Test Data

| Merchant ID | VPA | Description |
|-------------|-----|-------------|
| MER001 | storea@axis | Test Store A |
| MER002 | storeb@hdfc | Test Store B |
| MER003 | storec@icici | Test Store C |

| Test VPA | Behavior |
|----------|----------|
| success@upi | Payment succeeds |
| fail@upi | Payment fails |
| timeout@upi | Stays pending |

## Documentation

- [Backend README](./backend/README.md)
- [Frontend README](./frontend/README.md)
- [API Specification](./backend/docs/ER_DIAGRAM_AND_API.md)
- [Database Schema](./backend/docs/DATABASE_SCHEMA.md)
- [Developer Guide](./backend/docs/DEVELOPER_GUIDE.md)

## License

Private - Internal Use Only
