# Backend CLAUDE.md

Backend-specific guidance. See root `CLAUDE.md` for monorepo overview, build commands, and architecture.

## Project Phases

### Phase 1: UPI Payments (CURRENT)
- UPI collect flow with QR code generation
- PostgreSQL database with `ddl-auto=update`
- Async simulator callbacks for payment resolution
- Scheduled `PaymentExpiryJob` expires stale QR codes

### Phase 2: Card Payments with 3DS (Future)
- Card authorization, capture, void, refund with 3DS authentication
- New endpoints: `/api/payments/auth`, `/api/payments/sale`, `/api/transactions/{id}/capture`

## Database

```
Type: PostgreSQL
URL: jdbc:postgresql://localhost:5432/payment_gateway
User: postgres / postgres
Docker: overridden via SPRING_DATASOURCE_URL env var
```

`DataInitializer` seeds merchants, contracts, and bank accounts on startup.

## Development Notes

- Lombok is used for boilerplate reduction (`@Data`, etc.) -- annotation processor configured in `pom.xml`
- CORS enabled for all origins
- Logging at DEBUG for `org.personal` package
- Test VPAs and seeded merchants: see `docs/TEST_DATA.md`
- API spec and ER diagram: see `backend/docs/ER_DIAGRAM_AND_API.md`
