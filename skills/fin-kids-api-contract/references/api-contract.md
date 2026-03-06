# Fin Kids API Contract (Current)

Updated for the current codebase in this repository.

## 1) General

- Base path: `/api/v1`
- Content type: `application/json`
- Time format: ISO-8601 UTC (`Instant`), example: `2026-02-27T12:00:00Z`
- Monetary fields: decimal values with 2 fraction digits (domain allows `BigDecimal`)
- User auth (WebApp): `Authorization: Bearer <jwt-google>`
- Automation auth: `Authorization: Bearer <token>` required for `/api/v1/automation/**`
- OpenAPI spec JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui/index.html`

Authentication model:
- `/api/v1/automation/**`: dedicated automation token (`AutomationBearerAuth`)
- other `/api/v1/**`: user JWT validated by OAuth2 Resource Server (`UserBearerAuth`)

Authorization model by account:
- `CHILD`: read operations
- `PARENT`: read and write operations
- Role lookup source: `account_users.profile_role`

Database integrity guarantees (Liquibase):
- `transactions.amount` must be `> 0`
- `goals.target_amount` must be `> 0`
- `bonus_rules.percentage` must be between `0` and `100`
- Unique idempotency key for evidence: `transactions(account_id, origin, evidence_reference)`

Background execution (no HTTP endpoint):
- Monthly bonus job can be enabled by configuration (`BONUS_EXECUTION_ENABLED=true`)
- Scheduler calculates bonus for previous month (`referenceMonth = now(zone) - 1 month`)
- Applies only when account has active rule and no `WITHDRAW` in the reference month
- Persists a `DEPOSIT` with origin `BONUS` and evidence `bonus:YYYY-MM` (idempotent by month)
- Records audit trail event with action `BONUS_APPLIED`

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
- `401 Unauthorized`: missing/invalid user JWT on protected user endpoints
- `401 Unauthorized`: authenticated context without valid identity claims (`UnauthorizedException`)
- `403 Forbidden`: authenticated user without permission for account/operation (`AccessDeniedException`)
- `409 Conflict`: duplicated transaction evidence for same `accountId + origin + evidenceReference` (`DuplicateTransactionException`)
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
- If `evidenceReference` is provided, request is treated as idempotent by `accountId + origin + evidenceReference`.

Common errors:
- `400` invalid payload or invalid field values
- `401` missing/invalid user JWT
- `403` user without write permission on target account
- `409` duplicate evidence for the same `accountId + origin + evidenceReference`
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
- `401` missing/invalid user JWT
- `403` user without read permission on target account
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
- If the same `evidenceReference` is resent for the same account/origin, API returns conflict (idempotency guard).

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
- `409` duplicate evidence for the same `accountId + WHATSAPP + evidenceReference`
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
- `401` missing/invalid user JWT
- `403` user without read permission on target account
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
- `401` missing/invalid user JWT
- `403` user without read permission on target account
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
- `401` missing/invalid user JWT
- `403` user without write permission on target account
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
- `401` missing/invalid user JWT
- `403` user without read permission on target account
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
- `401` missing/invalid user JWT
- `403` user without write permission on target account
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
- `401` missing/invalid user JWT
- `403` user without write permission on target account
- `404` account or goal not found

---

## 8) Account Views API

### 8.1 Get child account view

- Method: `GET`
- Path: `/api/v1/accounts/{accountId}/child-view`

Path params:
- `accountId` (required, number, `> 0`)

Query params:
- `recentTransactionsLimit` (optional, integer between `1` and `50`, default `10`)

Success response:
- Status: `200 OK`

```json
{
  "accountId": 1,
  "childName": "Lucas",
  "currencyCode": "BRL",
  "currentBalance": 130.00,
  "goals": [
    {
      "goalId": 11,
      "name": "Bicicleta",
      "targetAmount": 200.00,
      "progressAmount": 130.00,
      "progressPercent": 65.00,
      "remainingAmount": 70.00,
      "achieved": false
    }
  ],
  "recentTransactions": [
    {
      "transactionId": 101,
      "type": "WITHDRAW",
      "amount": 20.00,
      "description": "Lanche",
      "occurredAt": "2026-03-06T10:00:00Z"
    }
  ]
}
```

Goal progress rule:
- `progressAmount = min(currentBalance, targetAmount)`
- `progressPercent = (progressAmount / targetAmount) * 100` rounded to 2 decimals (max `100`)
- `remainingAmount = max(targetAmount - progressAmount, 0)`
- `achieved = remainingAmount == 0`

Common errors:
- `400` invalid params (`accountId` or `recentTransactionsLimit`)
- `401` missing/invalid user JWT
- `403` user without read permission on target account
- `404` account not found

### 8.2 Get parent account view

- Method: `GET`
- Path: `/api/v1/accounts/{accountId}/parent-view`

Path params:
- `accountId` (required, number, `> 0`)

Query params:
- `year` (required, integer between `2000` and `2100`)
- `month` (required, integer between `1` and `12`)
- `recentTransactionsLimit` (optional, integer between `1` and `50`, default `20`)

Success response:
- Status: `200 OK`

```json
{
  "accountId": 1,
  "childName": "Nina",
  "currencyCode": "BRL",
  "currentBalance": 70.00,
  "monthlySummary": {
    "year": 2026,
    "month": 3,
    "periodStart": "2026-03-01T00:00:00Z",
    "periodEnd": "2026-04-01T00:00:00Z",
    "totalDeposits": 110.00,
    "totalWithdrawals": 40.00,
    "netChange": 70.00,
    "totalsByType": [
      { "type": "DEPOSIT", "total": 110.00 },
      { "type": "WITHDRAW", "total": 40.00 }
    ],
    "totalsByOrigin": [
      { "origin": "MANUAL", "total": 100.00 },
      { "origin": "BONUS", "total": 10.00 },
      { "origin": "WHATSAPP", "total": 40.00 }
    ]
  },
  "bonusRule": {
    "percentage": 5.00,
    "conditionType": "NO_WITHDRAWALS_IN_MONTH",
    "baseType": "MONTHLY_DEPOSITS",
    "active": true
  },
  "goals": [
    {
      "goalId": 21,
      "name": "Patins",
      "targetAmount": 150.00,
      "progressAmount": 70.00,
      "progressPercent": 46.67,
      "remainingAmount": 80.00,
      "achieved": false
    }
  ],
  "recentTransactions": [
    {
      "transactionId": 501,
      "type": "DEPOSIT",
      "origin": "BONUS",
      "amount": 10.00,
      "description": "Bonus",
      "evidenceReference": "bonus:2026-03",
      "occurredAt": "2026-03-15T10:00:00Z"
    }
  ]
}
```

Notes:
- `bonusRule` can be `null` when account has no configured bonus rule.
- `monthlySummary` follows the same monthly window rule used by `/monthly-summary`.

Common errors:
- `400` invalid params (`accountId`, `year`, `month`, `recentTransactionsLimit`)
- `401` missing/invalid user JWT
- `403` user without read permission on target account
- `404` account not found

---

## 9) Bonus Rule API

### 9.1 Get bonus rule by account

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
- `401` missing/invalid user JWT
- `403` user without write permission on target account
- `404` account not found or bonus rule not configured for account

### 9.2 Create or update bonus rule (upsert)

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
- `401` missing/invalid user JWT
- `403` user without write permission on target account
- `404` account not found

---

## 10) Users API

### 10.1 Get current authenticated user context

- Method: `GET`
- Path: `/api/v1/users/me`
- Security: `Authorization: Bearer <jwt-google>` (`UserBearerAuth`)

Success response:
- Status: `200 OK`

```json
{
  "userId": 10,
  "fullName": "Maria Silva",
  "email": "maria@email.com",
  "globalRole": "PARENT",
  "accounts": [
    {
      "accountId": 1,
      "childName": "Lucas",
      "profileRole": "PARENT"
    },
    {
      "accountId": 2,
      "childName": "Nina",
      "profileRole": "CHILD"
    }
  ]
}
```

Response fields:
- `userId` (number): internal user id from `app_users.id`
- `fullName` (string): user full name
- `email` (string): authenticated e-mail
- `globalRole` (enum `UserRole`): global role stored in `app_users.role`
- `accounts[]` (array): list of linked accounts from `account_users`
- `accounts[].accountId` (number): linked account id
- `accounts[].childName` (string): child name of linked account
- `accounts[].profileRole` (enum `UserRole`): role of user in that account (`CHILD` or `PARENT`)

Common errors:
- `401` missing/invalid JWT, or JWT without valid identity context
- `404` user not found by authenticated email
- `404` authenticated user without linked account

---

## 11) Account User Links API

### 11.1 List user links by account

- Method: `GET`
- Path: `/api/v1/accounts/{accountId}/user-links`
- Security: `Authorization: Bearer <jwt-google>` (`UserBearerAuth`)

Path params:
- `accountId` (required, number, `> 0`)

Success response:
- Status: `200 OK`

```json
{
  "accountId": 1,
  "links": [
    {
      "linkId": 10,
      "accountId": 1,
      "userId": 2,
      "userFullName": "Maria Silva",
      "userEmail": "maria@email.com",
      "profileRole": "PARENT",
      "linkedAt": "2026-03-02T12:00:00Z"
    }
  ]
}
```

Authorization rule:
- only `PARENT` on the account can list links.

Common errors:
- `400` invalid `accountId`
- `401` missing/invalid JWT
- `403` user without administrative permission on target account
- `404` account not found

### 11.2 Create or update account-user link (upsert)

- Method: `PUT`
- Path: `/api/v1/accounts/{accountId}/user-links/{userId}`
- Security: `Authorization: Bearer <jwt-google>` (`UserBearerAuth`)

Path params:
- `accountId` (required, number, `> 0`)
- `userId` (required, number, `> 0`)

Request body:

```json
{
  "profileRole": "CHILD"
}
```

Request fields:
- `profileRole` (required, enum `UserRole`: `CHILD` or `PARENT`)

Behavior:
- if link does not exist: creates `account_users` record.
- if link exists: updates `profile_role`.

Authorization rule:
- only `PARENT` on the account can create/update links.

Success response:
- Status: `200 OK`
- Returns the created/updated link payload.

Common errors:
- `400` invalid payload or invalid path params
- `401` missing/invalid JWT
- `403` user without administrative permission on target account
- `404` account or user not found

---

## 12) Audit Trail (Internal)

The API persists audit events for sensitive user writes in `audit_events`.

Audited operations:
- manual transaction creation (`POST /api/v1/transactions` with `origin=MANUAL`)
- goal create/update/delete (`POST/PUT/DELETE /api/v1/goals`)
- bonus rule upsert (`PUT /api/v1/accounts/{accountId}/bonus-rule`)
- account-user link upsert (`PUT /api/v1/accounts/{accountId}/user-links/{userId}`)

Stored audit fields:
- `account_id`
- `actor_email`
- `actor_user_id` (when actor exists in `app_users`)
- `actor_global_role` (when actor exists in `app_users`)
- `action_type`
- `resource_type`
- `resource_id`
- `payload_summary`
- `created_at`

Notes:
- Automation endpoint (`POST /api/v1/automation/transactions`) is not recorded in this user-sensitive audit trail.
- Audit events are created only after successful write operations.

---

## 13) Maintenance Checklist

When adding or changing any controller/endpoint:

1. Update this contract file in the same PR/commit.
2. Validate examples with current DTO field names.
3. Confirm status codes against `ApiExceptionHandler` and service rules.
4. Keep front-end visible breaking changes listed in the PR description.
