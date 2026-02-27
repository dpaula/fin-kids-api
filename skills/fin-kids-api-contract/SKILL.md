---
name: fin-kids-api-contract
description: Use when documenting, consulting, or updating the Fin Kids API contract for front-end integration. Trigger when someone asks for endpoint details, request/response payloads, params, status codes, error behavior, or when a new controller/endpoint is added and API docs must be synchronized.
---

# Fin Kids API Contract

Keep the API contract for this repository accurate and ready for front-end consumption.

## Workflow

1. Read endpoint documentation in `references/api-contract.md`.
2. If implementation changed, inspect controllers, DTOs, services, and exception handlers in `src/main/java`.
3. Update `references/api-contract.md` to match current behavior.
4. Keep examples in ISO-8601 UTC and enum values exactly as implemented.
5. Confirm OpenAPI output in `/v3/api-docs` remains aligned with the written contract.
6. Report breaking changes explicitly (renamed fields, removed params, status changes).

## Contract Rules

- Treat controller + DTO + service validation as source of truth.
- Document every endpoint with:
  - method and path
  - request (body/path/query)
  - success response
  - error responses and status codes
- Preserve enum names exactly (`DEPOSIT`, `WITHDRAW`, `MANUAL`, `WHATSAPP`, `BONUS`).
- When a new controller appears, append/update the corresponding section in `references/api-contract.md` in the same delivery.

## Scope

This skill documents API contract and integration behavior. It does not define UI or business roadmap.
