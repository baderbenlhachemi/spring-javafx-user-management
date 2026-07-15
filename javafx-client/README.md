# Team Access Hub JavaFX Client

This module is the desktop administration client for Team Access Hub. It uses
JavaFX 21, OkHttp, Gson, ControlsFX, and Ikonli to call the Spring Boot API. The
API remains the system of record and independently enforces authorization.

The client does not contain or create a default login. Use an account supplied
through the backend's documented development initialization or existing data.

## Capabilities

- JWT login with an in-memory session.
- Profile display and editing.
- Password change.
- Administrator user search, pagination, editing, deletion, role changes, and
  status changes.
- User statistics.
- Bounded fake-user JSON generation and restricted JSON import.
- Bounded UTF-8 CSV export.

## Prerequisites

- JDK 17 or later.
- The repository Maven wrapper.
- A reachable Team Access Hub backend.
- PowerShell and a JDK containing `jpackage` for native packaging.

See the [root evaluator guide](../readme.md) for backend configuration,
verification, Compose startup, security scope, and known server limitations.

## Run from source

The client connects to `http://localhost:9090/api` by default. From the
repository root:

```powershell
Set-Location javafx-client
..\mvnw.cmd javafx:run
```

The Windows-only `run.bat` wrapper runs the same Maven goal.

## API URL configuration

Override the complete API base URL with either:

- Environment variable: `TEAM_ACCESS_HUB_API_BASE_URL`
- JVM property: `teamaccesshub.api.base-url`

The JVM property takes precedence. Values are trimmed, trailing slashes are
removed, and the result must be an absolute HTTP or HTTPS URL with a host. URLs
containing credentials, a query string, or a fragment are rejected.

Environment example:

```powershell
$env:TEAM_ACCESS_HUB_API_BASE_URL = "https://access.example.com/api"
Set-Location javafx-client
..\mvnw.cmd javafx:run
```

For an IDE launch, use a VM option such as:

```text
-Dteamaccesshub.api.base-url=https://access.example.com/api
```

## Verification

Run the headless client tests from the repository root:

```powershell
.\mvnw.cmd -f javafx-client\pom.xml test
```

The tests cover configuration validation, injected HTTP transport, compatible
request routing, authorization-header use, sensitive-output silence,
independent in-memory sessions, logout cleanup, and packaging configuration.

The complete repository verifier runs backend tests first and client tests
second:

```powershell
.\scripts\verify.ps1
```

## Packaged runtime image

The supported desktop deliverable is a self-contained, platform-specific
`jpackage` application image named `TeamAccessHub`. From the repository root,
run exactly:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\javafx-client\package.ps1
```

The script requires a JDK 17 or newer installation containing `jpackage`. Set
`JAVA_HOME` to that JDK or place `jpackage` on `PATH`. It performs a clean
client test run and stops before packaging if any test fails.

Successful launcher paths are:

- Windows: `javafx-client/target/distribution/TeamAccessHub/TeamAccessHub.exe`
- Linux: `javafx-client/target/distribution/TeamAccessHub/bin/TeamAccessHub`
- macOS: `javafx-client/target/distribution/TeamAccessHub.app/Contents/MacOS/TeamAccessHub`

Set `TEAM_ACCESS_HUB_API_BASE_URL` in the launching process when the packaged
client should use a non-default backend:

```powershell
$env:TEAM_ACCESS_HUB_API_BASE_URL = "https://access.example.com/api"
.\javafx-client\target\distribution\TeamAccessHub\TeamAccessHub.exe
```

Application images contain native launchers and runtimes for the operating
system and architecture on which they are built. The workflow does not
cross-compile. This task produces an unpacked, unsigned app image rather than a
signed installer; build, sign, and test separately for every target platform.

## API operations used

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth` | Login |
| `POST` | `/api/auth/register` | Registration when server policy allows it |
| `GET` | `/api/users/me` | Current profile |
| `PUT` | `/api/users/me` | Update current profile |
| `PUT` | `/api/users/me/password` | Change password |
| `GET` | `/api/users` | Search and paginate users |
| `GET` | `/api/users/{username}` | User lookup |
| `GET` | `/api/users/id/{id}` | User lookup by identifier |
| `PUT` | `/api/users/{id}` | Update a user |
| `DELETE` | `/api/users/{id}` | Delete a user |
| `PATCH` | `/api/users/{id}/role` | Change role |
| `PATCH` | `/api/users/{id}/status` | Enable or disable |
| `GET` | `/api/users/generate/{count}` | Download generated JSON |
| `POST` | `/api/users/batch` | Import restricted JSON records |
| `GET` | `/api/users/export/csv` | Download CSV |
| `GET` | `/api/stats/users` | Dashboard statistics |

Registration is denied by the server's default policy. The `dev` profile or
`DEMO_REGISTRATION_ENABLED=true` can enable it explicitly.

## Session and security behavior

- The access token and current user are held in memory only.
- Logout clears both values.
- Tokens, authorization headers, and sensitive response bodies are not printed
  to standard output or error.
- The server, not the UI, enforces member and administrator permissions.
- Persistent login is not implemented; any future implementation must use an
  operating-system credential store rather than plaintext preferences or JSON.

## Known limitations

- The UI is built programmatically, and its controllers still combine several
  presentation responsibilities.
- There are no refresh tokens, active-session list, or explicit session
  revocation workflows in v0.2.
- Offline, expired-session, loading, empty, and partial-failure states need
  further view-model extraction and component-level coverage.
- Native application images are unsigned and platform-specific.
