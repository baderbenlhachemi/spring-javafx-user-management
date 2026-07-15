# Project Context and Verified Baseline

Last verified: 2026-07-15

## Executive summary

Team Access Hub is a Spring Boot REST API with a JavaFX desktop administration
client. The v0.2 secure baseline preserves the original user-management API
while removing known credential and data leaks, enforcing safer administrative
invariants, moving orchestration into focused services, and adding reproducible
tests and deployment tooling.

The repository is now suitable as a secure foundation, not a finished identity
lifecycle product. Invitations, revocable sessions, explicit lifecycle states,
and audit history remain intentionally deferred to Phase 2.

## Repository map

| Area | Location | Current responsibility |
| --- | --- | --- |
| Backend application | `src/main/java/com/badereddine/demo` | REST API, security, application services, persistence |
| Backend configuration | `src/main/resources` | Environment-backed runtime policy and Flyway migration |
| Backend tests | `src/test/java` | Unit, MVC/security, PostgreSQL integration, configuration checks |
| Desktop client | `javafx-client` | Programmatic JavaFX UI, typed transport, in-memory session, packaging |
| Development stack | `compose.yaml`, `Dockerfile` | PostgreSQL 16 and non-root API container |
| Verification | `scripts/verify.ps1`, `.github/workflows/ci.yml` | Ordered local and hosted verification |

Generated JSON, CSV, Maven targets, local environment files, and native images
are runtime artifacts and are not source-controlled.

## Current backend boundaries

- Java 17, Spring Boot 3.2.4, Spring Security, Spring Data JPA, PostgreSQL,
  JJWT, Flyway, Actuator, springdoc-openapi, and JavaFaker.
- Focused controllers cover authentication, self-service profile,
  administration, statistics, and import/export.
- Application services own authentication, profile changes, administrative
  mutations, statistics, pagination policy, import, generation, and CSV export.
- JPA entities stay internal to the API. Explicit response DTOs redact password
  data and persistence-only role identifiers.
- Runtime schema changes are Flyway-managed; Hibernate uses
  `ddl-auto=validate`.
- Constructor injection is used throughout production backend code.

## Current JavaFX boundaries

- JavaFX 21, OkHttp, Gson, ControlsFX, and Ikonli.
- `ApiClient` owns HTTP transport; `ApiService` composes transport with an
  injectable `SessionManager`.
- API base URL resolution is centralized in `ClientConfiguration`.
- Access tokens and the current principal remain in memory and are cleared on
  logout.
- Headless tests use MockWebServer and do not require JavaFX initialization or a
  running backend.
- `package.ps1` builds a platform-native `TeamAccessHub` app image after a clean
  client test run.

The UI remains programmatic, and its controllers still carry substantial view
construction and workflow coordination. Screen-by-screen view-model extraction
is later roadmap work.

## Security baseline

- Database credentials and JWT signing material are required from the runtime
  environment and are never supplied by committed defaults.
- The default profile creates no account. Development initialization is
  opt-in, profile-scoped, and requires externally supplied values.
- Registration and Swagger are denied by default and controlled independently
  through typed policy properties.
- User responses, generated downloads, logs, and tracked fixtures contain no
  plaintext password, password hash, access token, or signing key.
- Imports use a restricted DTO, enforce size and record limits, ignore
  privileged input, create server-owned encoded credentials, and leave accounts
  disabled.
- CSV output is bounded and neutralizes common spreadsheet-formula prefixes.
- Deleting, disabling, or demoting the final active administrator returns a
  stable conflict and is protected by transactional locking.
- Public liveness and readiness responses are redacted. Aggregate health and
  other application routes require authentication.

## API compatibility

The established `/api` paths, methods, parameters, and successful response
shapes are preserved. Notable policy facts are:

- Login is `POST /api/auth`.
- Registration is `POST /api/auth/register` and is disabled by default.
- Password change is `PUT /api/users/me/password`.
- List pagination is bounded to sizes 1 through 100 with an allow-listed sort
  field and `asc` or `desc` direction.
- Generation accepts 1 through 1000 records.
- Import accepts at most 1 MiB and 1000 records.
- CSV export returns at most 10000 records.

Security failures are stricter where the implementation plan explicitly
allowed them. The existing `status`, `message`, and `timeStamp` error envelope
is retained with stable, redacted messages.

## Reproducible development and verification

Prerequisites are JDK 17 or later, PowerShell, and a running Docker-compatible
engine. From the repository root:

```powershell
.\scripts\verify.ps1
```

This runs backend tests against disposable PostgreSQL Testcontainers and then
the headless JavaFX tests. It does not depend on a developer database or local
environment file.

Focused commands:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -f javafx-client\pom.xml test
```

Container configuration and startup:

```powershell
Copy-Item .env.example .env
# Replace every placeholder in .env.
docker compose config
docker compose up --build -d
Invoke-RestMethod http://localhost:9090/actuator/health/readiness
```

Desktop source launch:

```powershell
Set-Location javafx-client
..\mvnw.cmd javafx:run
```

Desktop packaging:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\javafx-client\package.ps1
```

## Pre-Flyway development database adoption

New databases migrate automatically. For an existing development database that
was previously managed by Hibernate:

1. Back up the database.
2. Compare its `roles` and `users` schema with
   `src/main/resources/db/migration/V1__baseline.sql`.
3. Activate the `dev` profile and set `FLYWAY_BASELINE_ON_MIGRATE=true` for one
   startup only.
4. Stop the backend, remove the flag, and start it again.
5. Confirm Hibernate validation succeeds. If the schema differs from V1,
   reconcile it from the backup or use a new empty database instead of
   baselining an unknown shape.

Automatic baselining remains disabled by default.

## Known limitations and risks

- JWT access tokens default to 24 hours and there is no refresh-token store,
  rotation, per-session revocation, or active-session inventory.
- Registration can be enabled as public self-signup; invitation-only onboarding
  is not implemented yet.
- Account state remains a boolean rather than an explicit lifecycle model.
- Roles are limited to `ROLE_USER` and `ROLE_ADMIN`.
- No immutable security or administration audit history exists.
- The API remains unversioned and keeps its legacy error-envelope contract.
- The Compose stack has no synthetic interactive account by default.
- JavaFX controllers remain large and component-level UI coverage is limited.
- Desktop images are unsigned, platform-specific app images rather than
  installers.
- CI verifies compilation and tests but does not yet run formatting, static
  analysis, software-composition analysis, or container-image scanning.
- There is no hosted demo, tagged release, screenshot set, threat model, or
  explicit license file.

## Verification boundaries

The baseline evidence includes unit tests, MVC/security tests, PostgreSQL
Testcontainers integration tests, configuration tests, Compose deployment and
readiness checks, CI, headless client tests, and a Windows packaged-launcher
smoke check. It does not include penetration testing, cross-platform package
execution, load testing, accessibility testing, or production deployment.
