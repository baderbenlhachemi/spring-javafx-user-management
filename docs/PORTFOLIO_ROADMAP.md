# Portfolio Roadmap: Team Access Hub

## Current status

The approved v0.2 Secure Baseline is implemented, documented, and verified
through Task 5.8. This milestone delivers the approved security and
engineering-foundation slice across Phases 0 and 1. It does not claim every
later roadmap bullet, such as API versioning, static analysis, screenshots, or
synthetic demo seeding.

Phase 2 is the next product milestone and has not started. It requires a new
task-sized implementation plan because invitations, lifecycle states,
revocable sessions, and audit history add API and database contracts.

## Product direction

Team Access Hub is a security-conscious identity and access operations console
for a small organization:

> Administrators invite teammates, control access, review security-sensitive
> changes, revoke sessions, and understand account health from a polished
> desktop console backed by a production-style Spring Boot API.

The v0.2 repository establishes credible engineering evidence for that product
without pretending the full lifecycle already exists.

## Target users and journeys

### Team administrator

- Invite a teammate with a role and expiring activation link.
- Review invited, active, suspended, and deactivated accounts.
- Change access without removing the final active administrator.
- Revoke one session or every session for a user.
- Review who changed what and when.
- Export a safe filtered report and see the export in the audit trail.

### Team member

- Accept an invitation and set a strong password.
- Sign in, view active sessions, and revoke an unfamiliar device.
- Update profile information and change a password.
- Understand when access is suspended or a session expires.

## Evidence expected from the portfolio

- A live or recorded end-to-end demonstration with clearly synthetic data.
- An architecture diagram and concise decision records.
- A threat-aware security model with tested authorization rules.
- Integration tests against disposable PostgreSQL.
- Database migrations, CI, health checks, structured logs, and containerized
  startup.
- A versioned OpenAPI contract and JavaFX screenshots.
- Clear setup with no committed secrets.
- A short tradeoffs and lessons-learned section.

## Delivery phases

### Phase 0 - Stop leaks and make the baseline honest

Status: delivered by v0.2 for the existing feature set.

Delivered evidence:

- Explicit user response DTOs prevent password-hash serialization.
- JavaFX no longer logs tokens, authorization headers, or sensitive payloads.
- Database credentials and JWT signing material are externalized.
- Development initialization is opt-in and externally configured.
- Registration and Swagger default to disabled policies.
- Generated files are not tracked; generation, import, and export are bounded
  and credential-safe.
- The final active administrator is protected transactionally.
- Authentication, authorization, redaction, and data-transfer rules have
  automated coverage.

### Phase 1 - Build a reliable engineering foundation

Status: the v0.2 foundation slice is delivered; remaining quality tooling stays
on the roadmap.

Delivered evidence:

- Focused backend controllers and application services.
- Constructor injection and explicit DTO mapping.
- Flyway migrations with Hibernate schema validation.
- PostgreSQL Testcontainers integration tests.
- Root ordered verification for backend and JavaFX.
- Read-only GitHub Actions CI and weekly dependency updates.
- Docker Compose for PostgreSQL and a non-root API image with health checks.
- Configurable JavaFX API URL and a native app-image workflow.

Still planned within the broader engineering foundation:

- A versioned API and generated/verified OpenAPI contract.
- A standard problem-details error format.
- Formatting, static analysis, dependency/security scanning, and image scanning.
- Structured redacted application logs.
- Clearly synthetic, non-secret demo seeding.

### Phase 2 - Deliver the identity lifecycle

Goal: turn the secure demo into a coherent access-lifecycle product.

- Add invitation creation, expiry, resend, cancellation, and acceptance.
- Replace boolean-only state with `INVITED`, `ACTIVE`, `SUSPENDED`, and
  `DEACTIVATED` lifecycle states.
- Add revocable refresh-token sessions with device and user-agent metadata.
- Rotate refresh tokens on use and support member/admin revocation.
- Record immutable authentication and administration audit events.
- Add an audit timeline with actor, subject, action, timestamp, and non-secret
  metadata.
- Extend dashboard statistics to pending invitations, suspended users, recent
  sign-ins, and security events.

Exit evidence:

- Invite -> activate -> login -> change access -> revoke -> inspect audit trail
  works end to end.
- Suspension or session revocation demonstrably blocks continued access.

### Phase 3 - Make authorization and UX distinctive

Goal: add one carefully scoped differentiator and polish the desktop client.

- Evolve two fixed roles into roles with a small explicit permission catalog.
- Add a permission matrix and explain effective access in plain language.
- Refactor JavaFX one screen at a time into views and view models.
- Add loading, empty, offline, expired-session, validation, and partial-failure
  states.
- Improve keyboard navigation, focus indicators, contrast, and non-color cues.
- Add safe bulk operations with previews and confirmation summaries.
- Add client tests for view-model behavior and API error mapping.

Exit evidence:

- A reviewer can understand and verify why a user has access.
- Critical desktop workflows are polished for success, error, and empty states.

### Phase 4 - Publish the case study

Goal: make the engineering easy for a reviewer to evaluate.

- Add screenshots and a two-to-four-minute product demonstration.
- Add a permissive license if that matches the owner's intent.
- Publish a tagged release with API and desktop artifacts.
- Add a concise threat model, key tradeoffs, test strategy, and lessons learned.
- Deploy a low-cost demo or retain a reliable local Docker demonstration.

Exit evidence:

- A reviewer can understand the problem, run the system, see the main journey,
  and inspect its engineering evidence in under ten minutes.

## Features to defer

- Microservices, service discovery, Kafka, or distributed tracing.
- Social login before the native session lifecycle is secure.
- Multi-tenant organizations before a single organization is secure and
  audited.
- A web-client rewrite before JavaFX state boundaries and the API contract are
  stable.
- Machine-learning features unrelated to access management.

## Success measures

| Area | Target evidence |
| --- | --- |
| Security | No secret/hash/token exposure; protected last-admin invariant; revocation journey |
| Quality | Unit, MVC security, PostgreSQL integration, and client suites in CI |
| Reproducibility | Clean checkout to verification and running stack through documented commands |
| API | Versioned OpenAPI with consistent errors and bounded inputs |
| UX | Screenshots and tested success, empty, loading, validation, and expired-session states |
| Operations | Health/readiness, migrations, structured redacted logs, and scanned images |

## Naming note

`team_access_hub` is the local database name and Team Access Hub is the public
product identity. Repository/package renaming is optional and should happen
only alongside a deliberate release to avoid unnecessary source churn.
