# Fin Kids API Contract (Current)

Updated for the current codebase in this repository.

## 1) General

- Base path: `/api/v1`
- Content type: `application/json`
- Time format: ISO-8601 UTC (`Instant`), example: `2026-02-27T12:00:00Z`
- Monetary fields: decimal values with 2 fraction digits (domain allows `BigDecimal`)
- Automation auth: `Authorization: Bearer <token>` required for `/api/v1/automation/**`
- OpenAPI spec JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui/index.html`

## 2) Enums

### TransactionType
- `DEPOSIT`
- `WITHDRAW`

### TransactionOrigin
- `WHATSAPP`
- `MANUAL`
- `BONUS`

### BonusConditionType
- `NO_WITHDRAWALS_IN_MONTH`

### BonusBaseType
- `LAST_BALANCE`
- `LAST_ALLOWANCE`
- `MONTHLY_DEPOSITS`

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
- `400 Bad Request`: validation errors (`MethodArgumentNotValidException`, `HandlerMethodValidationException`, `ConstraintViolationException`, `ValidationException`)
- `401 Unauthorized`: missing/invalid automation token (`AutomationAuthenticationEntryPoint`)
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
- `description` (string, required, non-empty, max 255 chars)
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

## 5) Automation Transactions API

### 5.1 Create transaction via automation (n8n/WhatsApp)

- Method: `POST`
- Path: `/api/v1/automation/transactions`
- Security: `Authorization: Bearer <token>` (`AutomationBearerAuth`)

Request body:

```json
{
  "accountId": 1,
  "type": "DEPOSIT",
  "amount": 100.00,
  "description": "Deposito identificado via comprovante",
  "evidenceReference": "wa-media-001",
  "occurredAt": "2026-02-27T12:00:00Z"
}
```

Request fields:
- `accountId` (number, required, `> 0`)
- `type` (enum `TransactionType`, required)
- `amount` (decimal, required, `> 0`)
- `description` (string, required, non-empty, max 255 chars)
- `evidenceReference` (string, optional)
- `occurredAt` (timestamp, optional; if null server sets current timestamp)

Business behavior:
- `origin` is not accepted from payload; API always persists `WHATSAPP`.
- Same balance and withdraw validation rules from core transaction service apply.

Success response:
- Status: `201 Created`

```json
{
  "transactionId": 10,
  "updatedBalance": 150.00
}
```

Common errors:
- `400` invalid payload
- `401` token absent or invalid
- `404` account not found
- `422` insufficient balance on withdraw

---

## 6) Account Summary API

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

## 7) Goals API

### 6.1 Create goal

- Method: `POST`
- Path: `/api/v1/goals`

Request body:

```json
{
  "accountId": 1,
  "name": "Bicicleta",
  "targetAmount": 500.00
}
```

Request fields:
- `accountId` (number, required, `> 0`)
- `name` (string, required, non-empty)
- `name` max length: 120 chars
- `targetAmount` (decimal, required, `> 0`)

Success response:
- Status: `201 Created`

```json
{
  "goalId": 11,
  "accountId": 1,
  "name": "Bicicleta",
  "targetAmount": 500.00,
  "active": true,
  "createdAt": "2026-02-27T12:00:00Z",
  "updatedAt": "2026-02-27T12:00:00Z"
}
```

Common errors:
- `400` invalid payload
- `404` account not found

### 6.2 List active goals

- Method: `GET`
- Path: `/api/v1/goals`

Query params:
- `accountId` (required, number, `> 0`)

Success response:
- Status: `200 OK`

```json
{
  "accountId": 1,
  "goals": [
    {
      "goalId": 11,
      "accountId": 1,
      "name": "Bicicleta",
      "targetAmount": 500.00,
      "active": true,
      "createdAt": "2026-02-27T12:00:00Z",
      "updatedAt": "2026-02-27T12:00:00Z"
    }
  ]
}
```

Common errors:
- `400` invalid `accountId`
- `404` account not found

### 6.3 Update goal

- Method: `PUT`
- Path: `/api/v1/goals/{goalId}`

Path params:
- `goalId` (required, number, `> 0`)

Request body:

```json
{
  "accountId": 1,
  "name": "Notebook",
  "targetAmount": 3200.00
}
```

Request fields:
- `accountId` (required, number, `> 0`)
- `name` (required, non-empty)
- `name` max length: 120 chars
- `targetAmount` (required, `> 0`)

Success response:
- Status: `200 OK`
- Returns updated goal with same response shape from create.

Common errors:
- `400` invalid payload or `goalId`
- `404` account or goal not found

### 6.4 Delete goal (soft delete)

- Method: `DELETE`
- Path: `/api/v1/goals/{goalId}`

Path params:
- `goalId` (required, number, `> 0`)

Query params:
- `accountId` (required, number, `> 0`)

Behavior:
- Performs logical deletion by setting `active=false`.

Success response:
- Status: `204 No Content`

Common errors:
- `400` invalid params
- `404` account or goal not found

---

## 8) Bonus Rule API

### 7.1 Get bonus rule by account

- Method: `GET`
- Path: `/api/v1/accounts/{accountId}/bonus-rule`

Path params:
- `accountId` (required, number, `> 0`)

Success response:
- Status: `200 OK`

```json
{
  "bonusRuleId": 15,
  "accountId": 1,
  "percentage": 10.00,
  "conditionType": "NO_WITHDRAWALS_IN_MONTH",
  "baseType": "MONTHLY_DEPOSITS",
  "active": true,
  "createdAt": "2026-02-27T14:00:00Z",
  "updatedAt": "2026-02-27T14:00:00Z"
}
```

Common errors:
- `400` invalid `accountId`
- `404` account not found or bonus rule not configured for account

### 7.2 Create or update bonus rule (upsert)

- Method: `PUT`
- Path: `/api/v1/accounts/{accountId}/bonus-rule`

Path params:
- `accountId` (required, number, `> 0`)

Request body:

```json
{
  "percentage": 10.00,
  "conditionType": "NO_WITHDRAWALS_IN_MONTH",
  "baseType": "MONTHLY_DEPOSITS",
  "active": true
}
```

Request fields:
- `percentage` (required, decimal between `0.01` and `100.00`)
- `conditionType` (required, enum `BonusConditionType`)
- `baseType` (required, enum `BonusBaseType`)
- `active` (required, boolean)

Behavior:
- If rule does not exist for account: creates new bonus rule.
- If rule already exists for account: updates existing record.

Success response:
- Status: `200 OK`
- Returns the same response shape from `GET`.

Common errors:
- `400` invalid payload or `accountId`
- `404` account not found

---

## 9) Maintenance Checklist

When adding or changing any controller/endpoint:

1. Update this contract file in the same PR/commit.
2. Validate examples with current DTO field names.
3. Confirm status codes against `ApiExceptionHandler` and service rules.
4. Keep front-end visible breaking changes listed in the PR description.
