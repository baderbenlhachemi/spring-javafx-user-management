# Implementation Plan — v0.2 Secure Baseline

Status: ready for execution

Last updated: 2026-07-12

## Scope

This plan converts the current application into the secure, reproducible baseline required before adding invitations, refresh-token sessions, audit history, or granular permissions.

Tasks are intentionally small and should be executed one at a time. Every task must update `docs/CHANGELOG.md`. Existing endpoint paths, HTTP methods, and successful-response JSON shapes must remain compatible throughout this milestone. Security-related failure behavior may become stricter where a task explicitly says so.

The task definition is the approved design. “Do not change architecture” means do not introduce a different architecture or broaden the refactor beyond the named files and responsibility. Database changes are allowed only in tasks explicitly marked as database work.

## Task status

- `[ ]` not started
- `[~]` in progress
- `[x]` complete and validated
- `[!]` blocked, with the reason recorded in `docs/CHANGELOG.md`

## Phase 1 — Establish safe verification and stop immediate leaks

### [x] Task 1.1 — Isolate backend tests with PostgreSQL Testcontainers

**Goal:** Make `mvn test` use a disposable PostgreSQL container instead of the developer's local database.

**Work type:** Test, Database, Infra

**Files likely touched:**

- `pom.xml`
- `src/test/java/com/badereddine/demo/DemoApplicationTests.java`
- `src/test/resources/application-test.properties`
- A shared Testcontainers support class under `src/test/java`
- `docs/CHANGELOG.md`

**Dependencies:** None. Docker Desktop or another Docker-compatible runtime must be available.

**Acceptance criteria:**

- The full backend test suite starts its own PostgreSQL container.
- Tests do not connect to `localhost:5432` or modify the developer database.
- The Spring context test uses the `test` profile and passes from a clean checkout.
- Container lifecycle is managed automatically by the test suite.

**Validation command:**

```powershell
docker info
.\mvnw.cmd test
```

### [x] Task 1.2 — Add a JavaFX client test harness

**Goal:** Establish JUnit 5 and OkHttp MockWebServer support so client behavior can be tested without a live backend.

**Work type:** Frontend, Test

**Files likely touched:**

- `javafx-client/pom.xml`
- `javafx-client/src/test/java/com/badereddine/client/service/ApiServiceTest.java`
- `docs/CHANGELOG.md`

**Dependencies:** None.

**Acceptance criteria:**

- Maven discovers and runs at least one client test.
- A smoke test starts MockWebServer and verifies a simple HTTP response can be consumed.
- Tests do not require a graphical display or a running backend.

**Validation command:**

```powershell
.\mvnw.cmd -f javafx-client\pom.xml test
```

### [x] Task 1.3 — Prevent password-hash serialization

**Goal:** Ensure the backend cannot serialize `User.password`, even when an entity is accidentally returned.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/model/User.java`
- `src/test/java/com/badereddine/demo/model/UserSerializationTest.java`
- `docs/CHANGELOG.md`

**Dependencies:** Task 1.1.

**Acceptance criteria:**

- Jackson serialization of a populated `User` never contains `password` or its BCrypt value.
- Deserialization behavior needed by the current code is documented and tested.
- A focused regression test would fail if the password property became serializable again.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=UserSerializationTest test
```

### [x] Task 1.4 — Remove sensitive JavaFX logging

**Goal:** Stop the desktop client from printing JWTs, authorization headers, authentication responses, and full profile responses.

**Work type:** Frontend, Test

**Files likely touched:**

- `javafx-client/src/main/java/com/badereddine/client/service/ApiService.java`
- `javafx-client/src/test/java/com/badereddine/client/service/ApiServiceTest.java`
- `docs/CHANGELOG.md`

**Dependencies:** Task 1.2.

**Acceptance criteria:**

- Authentication and profile operations emit no token, authorization header, password, hash, or response body to standard output/error.
- Any retained diagnostic logging contains only non-sensitive status information.
- MockWebServer tests cover successful authentication and profile retrieval.

**Validation command:**

```powershell
.\mvnw.cmd -f javafx-client\pom.xml test
rg -n "Auth response|Authorization:|Profile response" javafx-client\src\main\java
```

The `rg` command must return no sensitive debug statements.

### [x] Task 1.5 — Externalize runtime secrets and credentials

**Goal:** Remove committed database credentials and JWT signing material from production-capable configuration.

**Work type:** Backend, Infra, Test

**Files likely touched:**

- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/test/resources/application-test.properties`
- `.env.example`
- `.gitignore`
- `readme.md`
- `docs/CHANGELOG.md`

**Dependencies:** Task 1.1.

**Acceptance criteria:**

- Database URL, username, password, and JWT key are supplied through environment variables or profile-specific test wiring.
- No usable secret or production credential is committed.
- `.env.example` contains names and safe placeholders only.
- Application startup fails clearly when required non-development secrets are absent.
- Tests provide isolated values without relying on a developer `.env` file.

**Validation command:**

```powershell
.\mvnw.cmd test
rg -n "datasource\.password\s*=\s*postgres|jwtSecret\s*=\s*[^$]" src .env.example
```

The `rg` command must find no committed usable credential.

### [x] Task 1.6 — Restrict default admin initialization to development

**Goal:** Prevent production-capable profiles from creating or printing a known administrator account.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/config/DataInitializer.java`
- `src/main/resources/application-dev.properties`
- `src/test/resources/application-test.properties`
- Tests under `src/test/java/com/badereddine/demo/config`
- `readme.md`
- `docs/CHANGELOG.md`

**Dependencies:** Task 1.5.

**Acceptance criteria:**

- Default user creation runs only under an explicit `dev` or `demo` profile.
- Development credentials are configurable and are not printed to logs.
- The default profile never creates a known administrator automatically.
- Tests cover enabled and disabled initialization modes.

**Validation command:**

```powershell
.\mvnw.cmd test
rg -n "Password: admin|setPassword\(passwordEncoder\.encode\(\"admin\"\)" src\main
```

The `rg` command must return no hard-coded admin password or password log.

### [x] Task 1.7 — Remove generated credential artifacts from version control

**Goal:** Stop tracking runtime user exports and prevent future generated files from being committed.

**Work type:** Infra

**Files likely touched:**

- `.gitignore`
- `users.json` (removed)
- `new_users.json` (removed)
- `docs/PROJECT_CONTEXT.md`
- `docs/CHANGELOG.md`

**Dependencies:** Task 1.3.

**Acceptance criteria:**

- Tracked generated JSON files containing hashes are removed.
- Root and nested Maven `target/` directories and generated exports are ignored.
- Documentation no longer instructs users to depend on tracked generated data.
- No tracked JSON file contains a BCrypt hash.

**Validation command:**

```powershell
git ls-files
git grep -n '\$2[aby]\$'
```

Neither command may identify a tracked generated export or BCrypt hash.

## Phase 2 — Harden API boundaries and administrative invariants

### [x] Task 2.1 — Introduce explicit user response DTOs

**Goal:** Stop returning JPA `User` entities while preserving existing successful-response field names except the already-redacted password.

**Work type:** Backend, Test

**Files likely touched:**

- New response DTO and mapper classes under `src/main/java/com/badereddine/demo/payload/response`
- `src/main/java/com/badereddine/demo/controller/UserController.java`
- Controller/serialization tests under `src/test/java`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 1.1 and 1.3.

**Acceptance criteria:**

- Profile, update, list, and ID lookup responses use response DTOs.
- Existing client-consumed fields retain their names and compatible types.
- No persistence-only or password field is exposed.
- Controller tests verify representative single-user and paginated responses.

**Validation command:**

```powershell
.\mvnw.cmd test
rg -n "ResponseEntity\.ok\(user\)|getContent\(\)" src\main\java\com\badereddine\demo\controller
```

Any remaining match must not return a `User` entity directly.

### [x] Task 2.2 — Stream bounded fake-user generation

**Goal:** Generate the JSON download in memory or as a stream without writing a shared repository file, and reject unsafe counts.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/controller/UserController.java`
- `src/main/java/com/badereddine/demo/service/FakeDataService.java`
- A generation response DTO under `payload/response`
- Tests under `src/test/java/com/badereddine/demo/controller`
- `docs/CHANGELOG.md`

**Dependencies:** Task 2.1.

**Acceptance criteria:**

- The endpoint does not call `Files.write` or create `users.json` on disk.
- Count and admin count have documented lower and upper bounds.
- Invalid counts return a stable 400 response.
- Generated records contain no password/hash and are disabled until a future activation flow exists.
- The endpoint path, method, content type, and download filename remain compatible.

**Validation command:**

```powershell
.\mvnw.cmd test
rg -n "Files\.write|Paths\.get\(\"users\.json\"" src\main\java
```

The `rg` command must return no request-time disk write.

### [x] Task 2.3 — Restrict and validate batch imports

**Goal:** Replace direct entity deserialization with a restricted import DTO and always generate password hashes on the server.

**Work type:** Backend, Test

**Files likely touched:**

- A new import DTO under `src/main/java/com/badereddine/demo/payload/request`
- `src/main/java/com/badereddine/demo/controller/UserController.java`
- A focused import service under `src/main/java/com/badereddine/demo/service`
- Import tests under `src/test/java`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 1.3, 2.1, and 2.2.

**Acceptance criteria:**

- Uploaded JSON is never deserialized into a JPA entity.
- Imported role values are allow-listed and privileged entity fields are ignored or rejected.
- Password/hash input is ignored; the server assigns a random non-recoverable credential and imports the account disabled.
- Record count, file size, required fields, duplicates, and malformed input are handled deterministically.
- The existing endpoint, multipart field name, and result-count response shape remain compatible.

**Validation command:**

```powershell
.\mvnw.cmd test
rg -n "readValue\(.+User\[\]\.class|setPassword\(user\.getPassword\(\)\)" src\main\java
```

The `rg` command must return no direct entity import or password no-op.

### [x] Task 2.4 — Bound pagination and allow-list sorting

**Goal:** Prevent unbounded user queries and invalid or unsafe sort properties.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/controller/UserController.java`
- A pagination policy/helper under `src/main/java/com/badereddine/demo`
- Controller tests under `src/test/java`
- `docs/CHANGELOG.md`

**Dependencies:** Task 2.1.

**Acceptance criteria:**

- Page number cannot be negative and page size is capped at a documented maximum.
- Sort fields are selected from an explicit allow-list.
- Invalid pagination or sorting produces a stable 400 response.
- Existing defaults and response pagination fields remain compatible.

**Validation command:**

```powershell
.\mvnw.cmd test
```

### [x] Task 2.5 — Harden CSV export

**Goal:** Bound export size, use UTF-8 explicitly, and neutralize spreadsheet formula injection.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/controller/UserController.java`
- A CSV export service/helper under `src/main/java/com/badereddine/demo/service`
- CSV tests under `src/test/java`
- `docs/CHANGELOG.md`

**Dependencies:** Task 2.4.

**Acceptance criteria:**

- Fields beginning with `=`, `+`, `-`, or `@` cannot execute as spreadsheet formulas.
- CSV output uses UTF-8 and preserves structural escaping.
- Export rows are capped or streamed without requesting `Integer.MAX_VALUE` records.
- Endpoint path, parameters, filename, and CSV columns remain compatible.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=*Csv* test
rg -n "Integer\.MAX_VALUE" src\main\java
```

The `rg` command must not find an export page using `Integer.MAX_VALUE`.

### [x] Task 2.6 — Protect the last active administrator

**Goal:** Prevent deletion, disabling, or demotion from leaving the system without an active administrator.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/service/UserService.java`
- `src/main/java/com/badereddine/demo/repository/UserRepository.java`
- `src/main/java/com/badereddine/demo/controller/UserController.java`
- A domain exception and handler mapping
- Tests under `src/test/java/com/badereddine/demo/service`
- `docs/CHANGELOG.md`

**Dependencies:** Task 1.1.

**Acceptance criteria:**

- Deleting, disabling, or demoting the last active admin is rejected transactionally.
- Equivalent actions are allowed when another active admin exists.
- Concurrent updates cannot bypass the invariant in the tested database transaction model.
- Existing successful endpoint contracts remain unchanged.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=*AdminInvariant* test
```

### [x] Task 2.7 — Make registration and API documentation environment policies

**Goal:** Keep development usability while making public registration and Swagger explicit, disabled-by-default production choices.

**Work type:** Backend, Infra, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/security/WebSecurityConfig.java`
- New typed configuration properties under `src/main/java`
- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- Security tests under `src/test/java`
- `readme.md`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 1.5 and 1.6.

**Acceptance criteria:**

- Registration and Swagger exposure are controlled by typed configuration.
- Safe defaults disable both outside explicit development/demo configuration.
- When registration is enabled, its existing endpoint and successful response remain compatible.
- Security tests cover enabled and disabled policy states.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=*Security* test
```

### [x] Task 2.8 — Redact error handling without changing its contract

**Goal:** Prevent raw internal exception messages from reaching clients while preserving the existing error response shape and status mappings.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/exception/UserRestExceptionHandler.java`
- `src/main/java/com/badereddine/demo/exception/UserErrorResponse.java`
- Exception-handler tests under `src/test/java`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 2.3, 2.4, and 2.6.

**Acceptance criteria:**

- Existing custom exception-to-status mappings remain unchanged.
- Validation, malformed JSON, authentication, authorization, conflict, not-found, and unexpected failures return stable, non-sensitive messages.
- Unexpected failures log a redacted server-side diagnostic without exposing the exception message to the client.
- The existing `status`, `message`, and `timeStamp` fields and HTTP status behavior remain compatible.
- Tests cover each handled category.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=*ExceptionHandler* test
```

## Phase 3 — Make database state and security behavior reproducible

### [x] Task 3.1 — Add the initial Flyway migration

**Goal:** Replace Hibernate schema mutation with a versioned baseline matching the current PostgreSQL model.

**Work type:** Database, Backend, Test

**Files likely touched:**

- `pom.xml`
- `src/main/resources/db/migration/V1__baseline.sql`
- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/test/resources/application-test.properties`
- Migration tests under `src/test/java`
- `docs/CHANGELOG.md`

**Dependencies:** Task 1.1. This task explicitly requires database configuration and schema-management changes.

**Acceptance criteria:**

- Flyway creates roles, users, constraints, and indexes in an empty PostgreSQL database.
- Hibernate uses `ddl-auto=validate` and performs no schema update.
- A migration test boots twice against the same schema successfully.
- Existing development databases have a documented baseline/adoption procedure.

**Validation command:**

```powershell
.\mvnw.cmd test
```

### [x] Task 3.2 — Add authentication and authorization MVC tests

**Goal:** Prove the public/protected endpoint policy and role boundaries at the HTTP layer.

**Work type:** Backend, Test

**Files likely touched:**

- Security/controller tests under `src/test/java/com/badereddine/demo/security`
- Test fixtures/builders under `src/test/java`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 2.7, 2.8, and 3.1.

**Acceptance criteria:**

- Tests cover anonymous login, registration policy, authenticated profile access, admin-only endpoints, disabled users, invalid JWTs, and expired JWTs.
- Both 401 and 403 behavior are asserted where appropriate.
- No test depends on execution order or pre-existing rows.

**Validation command:**

```powershell
.\mvnw.cmd "-Dtest=*Security*,*Authorization*" test
```

### [x] Task 3.3 — Add user-management service tests

**Goal:** Cover business rules independently of controller rendering.

**Work type:** Backend, Test, Database

**Files likely touched:**

- Tests under `src/test/java/com/badereddine/demo/service`
- Test data builders under `src/test/java`
- Minor service changes required for deterministic testing
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 2.6 and 3.1.

**Acceptance criteria:**

- Tests cover uniqueness, role changes, status changes, last-admin protection, and new-user-today calculation.
- Time-dependent behavior uses an injectable `Clock` rather than the machine clock.
- Tests run against disposable PostgreSQL where repository behavior matters.

**Validation command:**

```powershell
.\mvnw.cmd "-Dtest=*UserService*,*AdminInvariant*" test
```

### [x] Task 3.4 — Add import, generation, and export integration tests

**Goal:** Lock down the secure file-processing behavior introduced in Phase 2.

**Work type:** Backend, Test, Database

**Files likely touched:**

- Integration tests under `src/test/java/com/badereddine/demo/importexport`
- Test JSON/CSV fixtures under `src/test/resources`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 2.2, 2.3, 2.5, and 3.1.

**Acceptance criteria:**

- Tests cover valid and malformed JSON, duplicates, privileged-field attempts, unsafe counts, disabled imported accounts, and generated output redaction.
- CSV tests cover commas, quotes, newlines, Unicode, and formula prefixes.
- Tests assert no request creates a repository-root export file.

**Validation command:**

```powershell
.\mvnw.cmd "-Dtest=*Import*,*Export*,*Generation*" test
```

## Phase 4 — Refactor responsibilities without changing contracts

### [x] Task 4.1 — Replace backend field injection with constructor injection

**Goal:** Make dependencies explicit and improve unit-test construction without changing behavior.

**Work type:** Backend, Test

**Files likely touched:**

- Backend controllers, services, security classes, and configuration classes using `@Autowired` fields
- Affected tests
- `docs/CHANGELOG.md`

**Dependencies:** Phase 3 complete.

**Acceptance criteria:**

- Application components use constructor injection.
- No required dependency is mutable or nullable after construction.
- Endpoint behavior and configuration remain unchanged.
- Existing tests pass without relaxed mocking.

**Validation command:**

```powershell
.\mvnw.cmd test
rg -n "@Autowired\s*$" src\main\java
```

Any remaining match must be justified in `docs/CHANGELOG.md`.

### [x] Task 4.2 — Extract authentication application logic

**Goal:** Move login and registration orchestration out of `UserController` into a focused service while preserving endpoints and payloads.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/controller/UserController.java`
- A new authentication service under `src/main/java/com/badereddine/demo/service`
- Authentication tests
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 4.1 and 3.2.

**Acceptance criteria:**

- The controller handles HTTP mapping and delegates authentication/registration use cases.
- Last-login updates and token generation remain transactional and tested.
- Endpoint paths, methods, request fields, and successful responses remain unchanged.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=*Auth* test
```

### [x] Task 4.3 — Extract self-service profile logic

**Goal:** Move current-user lookup, profile updates, and password changes into a focused application service.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/controller/UserController.java`
- A new profile service under `src/main/java/com/badereddine/demo/service`
- Profile tests
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 4.1, 2.1, and 2.8.

**Acceptance criteria:**

- Profile business rules are not implemented in the controller.
- Password verification and encoding remain server-side and tested.
- Endpoint contracts remain unchanged.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=*Profile* test
```

### [x] Task 4.4 — Extract administrator user-management logic

**Goal:** Move list, lookup, update, delete, role, status, and statistics orchestration into focused services.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/controller/UserController.java`
- New admin/statistics services under `src/main/java/com/badereddine/demo/service`
- Admin service/controller tests
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 4.1, 2.4, 2.6, and 3.3.

**Acceptance criteria:**

- Administrative invariants live in transactional services.
- Controller methods only parse HTTP input, delegate, and map responses.
- Search, pagination, statistics, and all endpoint contracts remain compatible.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=*Admin*,*Stats* test
```

### [x] Task 4.5 — Extract import/export orchestration

**Goal:** Move fake generation, import validation, and CSV production out of the controller into a cohesive service boundary.

**Work type:** Backend, Test

**Files likely touched:**

- `src/main/java/com/badereddine/demo/controller/UserController.java`
- Import/export services under `src/main/java/com/badereddine/demo/service`
- Existing integration tests
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 4.1 and 3.4.

**Acceptance criteria:**

- The controller contains no JSON parsing, CSV assembly, filesystem access, or per-record persistence loop.
- File-processing services have bounded inputs and focused tests.
- Download and import contracts remain unchanged.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=*Import*,*Export*,*Generation* test
```

### [x] Task 4.6 — Split the monolithic backend controller

**Goal:** Replace `UserController` with focused auth, profile, admin-user, transfer, and statistics controllers without changing the API.

**Work type:** Backend, Test

**Files likely touched:**

- Controllers under `src/main/java/com/badereddine/demo/controller`
- `src/main/java/com/badereddine/demo/controller/UserController.java` (removed after extraction)
- Controller tests
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 4.2 through 4.5.

**Acceptance criteria:**

- No replacement controller combines unrelated auth, profile, administration, and file-transfer responsibilities.
- Every existing endpoint retains its path, method, security policy, request shape, and successful-response shape.
- OpenAPI exposes the same operations grouped coherently.
- Full backend tests pass.

**Validation command:**

```powershell
.\mvnw.cmd test
```

## Phase 5 — Make development, delivery, and the desktop client reproducible

### [x] Task 5.1 — Make the JavaFX API base URL configurable

**Goal:** Remove the hard-coded localhost endpoint while retaining localhost as a development default.

**Work type:** Frontend, Test, Infra

**Files likely touched:**

- `javafx-client/src/main/java/com/badereddine/client/service/ApiService.java`
- A new client configuration class
- JavaFX client tests
- `javafx-client/README.md`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 1.2 and 1.4.

**Acceptance criteria:**

- Base URL can be supplied by environment variable or JVM property.
- URL normalization and invalid values are tested.
- Default local development behavior remains compatible.
- Tests inject MockWebServer without reflection or global environment mutation.

**Validation command:**

```powershell
.\mvnw.cmd -f javafx-client\pom.xml test
rg -n "static final String BASE_URL = \"http://localhost" javafx-client\src\main\java
```

The `rg` command must return no hard-coded service constant.

### [x] Task 5.2 — Separate JavaFX API transport from session state

**Goal:** Make API calls and authentication state independently testable without changing user-visible behavior.

**Work type:** Frontend, Test

**Files likely touched:**

- `javafx-client/src/main/java/com/badereddine/client/service/ApiService.java`
- `javafx-client/src/main/java/com/badereddine/client/service/SessionManager.java`
- New API client classes under `javafx-client/src/main/java/com/badereddine/client/api`
- Client tests
- `docs/CHANGELOG.md`

**Dependencies:** Task 5.1.

**Acceptance criteria:**

- HTTP client, base URL, JSON mapper, and session token can be injected in tests.
- API methods no longer depend on an unreplaceable singleton for transport configuration.
- Tokens remain memory-only and are never persisted or logged.
- Existing JavaFX controllers continue to behave the same.

**Validation command:**

```powershell
.\mvnw.cmd -f javafx-client\pom.xml test
```

### [x] Task 5.3 — Add application health and readiness endpoints

**Goal:** Expose minimal operational health signals without exposing sensitive actuator data.

**Work type:** Backend, Infra, Test

**Files likely touched:**

- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/java/com/badereddine/demo/security/WebSecurityConfig.java`
- Health security tests
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 2.7 and 3.1.

**Acceptance criteria:**

- Liveness and readiness endpoints are available for container orchestration.
- Only the required health endpoints are public; environment, beans, config, and metrics details remain protected or disabled.
- Database readiness is represented.
- Security tests assert the exposure policy.

**Validation command:**

```powershell
.\mvnw.cmd -Dtest=*Health*,*Security* test
```

### [x] Task 5.4 — Containerize the backend and PostgreSQL development stack

**Goal:** Start a reproducible API and database stack with health checks and no embedded credentials.

**Work type:** Infra, Backend, Database

**Files likely touched:**

- `Dockerfile`
- `compose.yaml`
- `.dockerignore`
- `.env.example`
- `readme.md`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 1.5, 3.1, and 5.3.

**Acceptance criteria:**

- The backend image uses a multi-stage build and a non-root runtime user.
- Compose starts PostgreSQL and the API with health checks and a persistent named volume.
- Secrets are supplied externally and no real values appear in the image or Compose file.
- A clean stack reaches a healthy state and Flyway initializes the schema.

**Validation command:**

```powershell
docker compose config
docker compose up --build -d
docker compose ps
Invoke-RestMethod http://localhost:9090/actuator/health/readiness
docker compose down
```

### [x] Task 5.5 — Add one-command repository verification

**Goal:** Provide a single PowerShell command that verifies backend and JavaFX modules in a predictable order.

**Work type:** Infra, Test

**Files likely touched:**

- `scripts/verify.ps1`
- `readme.md`
- `AGENTS.md`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 1.1 and 1.2.

**Acceptance criteria:**

- The script fails fast and returns a non-zero exit code on any failed module.
- It runs backend and client tests without changing developer configuration.
- Usage and prerequisites are documented.

**Validation command:**

```powershell
.\scripts\verify.ps1
```

### [x] Task 5.6 — Add continuous integration

**Goal:** Verify every push and pull request from a clean environment.

**Work type:** Infra, Test

**Files likely touched:**

- `.github/workflows/ci.yml`
- Optional dependency-update configuration under `.github`
- `readme.md`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 3.1, 5.5, and all tests in Phases 1–3.

**Acceptance criteria:**

- CI checks out the repository, installs the documented JDK, caches Maven safely, and runs `scripts/verify.ps1`.
- Testcontainers can access Docker in CI.
- Workflow permissions are read-only unless a job explicitly needs more.
- Dependency scanning or automated dependency updates are configured without blocking the base test job.

**Validation command:**

```powershell
.\scripts\verify.ps1
git diff --check
```

The workflow must also pass in the hosting provider before the task is marked complete.

### [x] Task 5.7 — Package the JavaFX desktop application

**Goal:** Produce a documented runnable desktop image or installer from the client module.

**Work type:** Frontend, Infra, Test

**Files likely touched:**

- `javafx-client/pom.xml`
- Java module metadata if required
- Packaging scripts/configuration under `javafx-client`
- `javafx-client/README.md`
- `docs/CHANGELOG.md`

**Dependencies:** Tasks 5.1, 5.2, and 5.5.

**Acceptance criteria:**

- A clean build produces a named JavaFX runtime image or installer artifact.
- The artifact accepts the configurable API URL.
- Packaging steps and platform limitations are documented.
- Client tests run before packaging.

**Validation command:**

```powershell
.\mvnw.cmd -f javafx-client\pom.xml clean test javafx:jlink
```

If a different packaging goal is selected, document and use its exact reproducible command.

### [x] Task 5.8 — Publish the secure-baseline documentation

**Goal:** Make the v0.2 repository truthful, navigable, and evaluable in under ten minutes.

**Work type:** Infra, Test

**Files likely touched:**

- `readme.md`
- `javafx-client/README.md`
- `docs/PROJECT_CONTEXT.md`
- `docs/ARCHITECTURE.md`
- `docs/PORTFOLIO_ROADMAP.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/CHANGELOG.md`

**Dependencies:** All preceding tasks.

**Acceptance criteria:**

- README explains the Team Access Hub direction, current secure-baseline scope, architecture, prerequisites, configuration, one-command verification, container startup, JavaFX startup, and known limitations.
- Endpoint documentation matches implemented methods and registration policy.
- No README advertises committed or production-safe default credentials.
- Completed implementation tasks are marked `[x]` and the changelog accurately summarizes v0.2.
- All links and commands are checked from a clean checkout.

**Validation command:**

```powershell
.\scripts\verify.ps1
docker compose config
git diff --check
rg -n "admin.*admin|Password.*admin|jwtSecret\s*=" readme.md docs javafx-client\README.md
```

Any credential-like documentation match must be a clearly marked historical warning, not a runnable default.

## v0.2 completion gate

The milestone is complete only when:

- Tasks 1.1 through 5.8 are marked `[x]`.
- `scripts/verify.ps1` passes from a clean checkout.
- CI passes.
- A clean Compose deployment reaches readiness.
- No tracked source, sample, log statement, API response, or documentation contains a usable secret, token, plaintext password, or password hash.
- Existing API endpoints remain compatible with the pre-milestone contract, apart from explicitly documented stricter security failures.

After this gate, create a new task-sized plan for Phase 2 of `PORTFOLIO_ROADMAP.md`: invitations, lifecycle states, revocable sessions, and audit history. Those features require additive API and database contracts and are intentionally outside this plan.

## Copy-paste executor prompt

Replace `[X.Y]` with one task identifier.

```text
Read:
- AGENTS.md
- docs/PROJECT_CONTEXT.md
- docs/ARCHITECTURE.md
- docs/PORTFOLIO_ROADMAP.md
- docs/IMPLEMENTATION_PLAN.md
- docs/CHANGELOG.md

Execute Task [X.Y] from `docs/IMPLEMENTATION_PLAN.md`.

Rules:
- Implement only this task.
- Do not change architecture.
- Do not change API contracts.
- Do not change database schema unless this task explicitly requires it.
- Add or update tests.
- Run the validation commands listed in the task.
- Update `docs/CHANGELOG.md`.
- Mark the task complete in `docs/IMPLEMENTATION_PLAN.md` only after every acceptance criterion and validation command passes.

Return:
1. Summary
2. Files changed
3. Tests run
4. Any unresolved issues
```
