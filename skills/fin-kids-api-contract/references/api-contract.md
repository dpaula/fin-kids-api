# Fin Kids API Contract (Current)

Updated for the current codebase in this repository.

## 1) General

- Base path: `/api/v1`
- Content type: `application/json`
- Time format: ISO-8601 UTC (`Instant`), example: `2026-02-27T12:00:00Z`
- Monetary fields: decimal values with 2 fraction digits (domain allows `BigDecimal`)

## 2) Enums

### TransactionType
- `DEPOSIT`
- `WITHDRAW`

### TransactionOrigin
- `WHATSAPP`
- `MANUAL`
- `BONUS`

## 3) Error Model

Standard error body (handled by `ApiExceptionHandler` for domain errors):

```json
{
  "timestamp": "2026-02-27T13:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "accountId deve ser informado e maior que zero.",
  "path": "/api/v1/transactions"
}
```

Mapped statuses:
- `400 Bad Request`: validation errors (`ValidationException`)
- `404 Not Found`: missing resources (`ResourceNotFoundException`)
- `422 Unprocessable Entity`: business rule conflict (`BusinessRuleException`)

---

## 4) Transactions API

### 4.1 Create transaction

- Method: `POST`
- Path: `/api/v1/transactions`

Request body:

```json
{
  "accountId": 1,
  "type": "DEPOSIT",
  "origin": "MANUAL",
  "amount": 100.00,
  "description": "Mesada",
  "evidenceReference": "optional-reference",
  "occurredAt": "2026-02-27T12:00:00Z"
}
```

Request fields:
- `accountId` (number, required, `> 0`)
- `type` (enum `TransactionType`, required)
- `origin` (enum `TransactionOrigin`, required)
- `amount` (decimal, required, `> 0`)
- `description` (string, required, non-empty)
- `evidenceReference` (string, optional)
- `occurredAt` (timestamp, optional; if null server sets current timestamp)

Success response:
- Status: `201 Created`

```json
{
  "transactionId": 10,
  "updatedBalance": 150.00
}
```

Business behavior:
- `WITHDRAW` is rejected when current balance is insufficient.
- Balance is calculated from transaction history (`DEPOSIT` positive, `WITHDRAW` negative).

Common errors:
- `400` invalid payload or invalid field values
- `404` account not found
- `422` insufficient balance on withdraw

### 4.2 List transactions by period

- Method: `GET`
- Path: `/api/v1/transactions`

Query params:
- `accountId` (required, number, `> 0`)
- `start` (required, ISO-8601 UTC timestamp)
- `end` (required, ISO-8601 UTC timestamp)

Rules:
- `start <= end`

Success response:
- Status: `200 OK`

```json
{
  "currentBalance": 90.00,
  "transactions": [
    {
      "transactionId": 77,
      "accountId": 1,
      "type": "DEPOSIT",
      "origin": "MANUAL",
      "amount": 40.00,
      "description": "Mesada",
      "evidenceReference": null,
      "occurredAt": "2026-02-20T10:00:00Z"
    }
  ]
}
```

Common errors:
- `400` invalid params (`accountId`, `start`, `end`, or range)
- `404` account not found

---

## 5) Account Summary API

### 5.1 Get current balance

- Method: `GET`
- Path: `/api/v1/accounts/{accountId}/balance`

Path params:
- `accountId` (required, number, `> 0`)

Success response:
- Status: `200 OK`

```json
{
  "accountId": 1,
  "currentBalance": 123.45
}
```

Common errors:
- `400` invalid accountId
- `404` account not found

### 5.2 Get monthly summary

- Method: `GET`
- Path: `/api/v1/accounts/{accountId}/monthly-summary`

Path params:
- `accountId` (required, number, `> 0`)

Query params:
- `year` (required, integer between 2000 and 2100)
- `month` (required, integer between 1 and 12)

Monthly window rule:
- `periodStart` inclusive, `periodEnd` exclusive
- UTC boundaries based on `year/month`

Success response:
- Status: `200 OK`

```json
{
  "accountId": 1,
  "year": 2026,
  "month": 2,
  "periodStart": "2026-02-01T00:00:00Z",
  "periodEnd": "2026-03-01T00:00:00Z",
  "currentBalance": 180.00,
  "totalDeposits": 250.00,
  "totalWithdrawals": 70.00,
  "netChange": 180.00,
  "totalsByType": [
    { "type": "DEPOSIT", "total": 250.00 },
    { "type": "WITHDRAW", "total": 70.00 }
  ],
  "totalsByOrigin": [
    { "origin": "WHATSAPP", "total": 70.00 },
    { "origin": "MANUAL", "total": 200.00 },
    { "origin": "BONUS", "total": 50.00 }
  ]
}
```

Common errors:
- `400` invalid `accountId`, `year`, or `month`
- `404` account not found

---

## 6) Maintenance Checklist

When adding or changing any controller/endpoint:

1. Update this contract file in the same PR/commit.
2. Validate examples with current DTO field names.
3. Confirm status codes against `ApiExceptionHandler` and service rules.
4. Keep front-end visible breaking changes listed in the PR description.
