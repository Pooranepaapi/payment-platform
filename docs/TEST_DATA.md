# Test Data

## Seeded Merchants (via `DataInitializer` on startup)

| Merchant ID | VPA |
|-------------|-----|
| MER001 | storea@axis |
| MER002 | storeb@hdfc |
| MER003 | storec@icici |

## Simulator Test VPAs

### Common (All Banks)

| VPA | Behavior |
|-----|----------|
| `success@upi` | Succeeds after ~1.5-2.5s callback |
| `fail@upi` | Customer rejects payment |
| `timeout@upi` | No callback (stays PENDING) |
| `insufficient@upi` | Insufficient funds |

### Bank-Specific

| Bank | Success | Fail | Timeout | Other |
|------|---------|------|---------|-------|
| RBL | `rbl.success@rbl` | `rbl.fail@rbl` | `rbl.timeout@rbl` | `rbl.lowbal@rbl` |
| HDFC | `hdfc.success@hdfc` | `hdfc.fail@hdfc` | `hdfc.timeout@hdfc` | `hdfc.declined@hdfc`, `hdfc.nobal@hdfc` |
| Kotak | `kotak.success@kotak` | `kotak.fail@kotak` | `kotak.timeout@kotak` | `kotak.blocked@kotak` |

### Simulator Bank Callback Delays

| Bank | Delay |
|------|-------|
| RBLUPI (RBL) | 1.5s |
| HDFCUPI (HDFC) | 2.0s |
| KOTAKUPI (Kotak) | 2.5s |
