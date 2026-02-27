# GranaGalaxy Domain Rules Reference

Use this reference as the detailed source when auditing implementation changes.

## Product Scope
- Product: GranaGalaxy
- Core modules:
  - API (`fin-kids-api`)
  - Automation (n8n + WhatsApp + OCR + IA)
  - Frontend (React with Google OAuth)
- API role in ecosystem:
  - source of truth for data and business rules
  - authorization boundary by role
  - transaction audit backbone

## Fundamental Business Rules
### Child Account
- Keep one primary child account context.
- Derive balance from full transaction history.
- Prevent negative balance in initial phase.

### Transactions
- Allowed types: `DEPOSIT`, `WITHDRAW`.
- Required fields:
  - amount
  - short description
  - datetime
  - origin (`WHATSAPP`, `MANUAL`, `BONUS`)
- Optional field:
  - evidence reference (id/hash/internal URL)
- Immutability rules:
  - do not delete transaction history
  - corrections must create new records (for example, reversal)
  - keep every change auditable

### Roles
- Valid roles:
  - `CHILD`
  - `PARENT`
- Access policy:
  - `CHILD`: read-only access to balance/goals/history views
  - `PARENT`: write access for transactions, rules, goals
  - automation must use protected dedicated endpoint

### Bonus Rules
- Make bonus percentage configurable by parents.
- Apply bonus only under explicit condition.
- Register bonus as auditable transaction with origin `BONUS`.

### Goals
- Model goals with:
  - name
  - target amount
  - progress derived from current balance/domain rule

## Integration Contracts
### Frontend Integration
- Validate Google OAuth token.
- Resolve user role correctly.
- Enforce endpoint authorization by role.

### n8n Integration
- Deposit flow:
  - accept classified payload
  - create `DEPOSIT`
  - return updated balance
- Withdraw flow:
  - accept classified payload
  - create `WITHDRAW` only if valid
  - reject insufficient balance
  - return clear and consistent error payload
- Security and validation:
  - require protected automation endpoint
  - enforce origin `WHATSAPP`
  - validate payload schema

## Architecture Constraints
- Keep API independent from frontend and n8n runtime.
- Keep balance consistency and transaction auditability non-negotiable.
- Keep future bonus logic explicit and versionable.

## Audit Checklist
- [ ] Entities represent the domain correctly.
- [ ] Transaction origin enum is defined and enforced.
- [ ] Transaction type enum is consistent (`DEPOSIT`, `WITHDRAW`).
- [ ] Negative balance is blocked.
- [ ] Role-based access is enforced.
- [ ] Automation endpoint is protected.
- [ ] Bonus rules are configurable and auditable.
- [ ] Goals are modeled coherently.
- [ ] README is aligned with real implementation.
- [ ] Roadmap is coherent with code status.
- [ ] Critical business rules are covered by tests.
- [ ] Regressions are called out before acceptance.
