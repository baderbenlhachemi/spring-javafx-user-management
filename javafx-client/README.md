# JWT User Management - JavaFX Client

A modern, beautiful JavaFX desktop client for the JWT User Management REST API.

## 🚀 Quick Start

### Default Login Credentials

A default admin user is automatically created when the backend starts:

| Field | Value |
|-------|-------|
| **Username** | `admin` |
| **Password** | `admin` |
| **Email** | `admin@localhost.com` |
| **Role** | Administrator (full access) |

### Steps to Run

1. **Start the Spring Boot backend** (from project root):
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

2. **Start the JavaFX client** (from javafx-client folder):
   ```bash
   ..\mvnw.cmd javafx:run
   ```
   Or double-click `run.bat`

3. **Login** with username `admin` and password `admin`

## ✨ Features

### 🔐 Authentication
- Secure login with JWT token authentication
- Session management with automatic token handling
- Beautiful animated login screen with gradient effects
- Last login tracking

### 📊 Dashboard
- Welcome message with user info
- Quick action cards for navigation
- **User Statistics (Admin)** - Real-time stats with:
  - Total users count
  - Admin count with percentage
  - Regular users with percentage
  - New users today
  - Refresh button for live updates

### 👤 User Profile
- View your complete profile information
- **Edit Profile** - Update your personal information
- **Change Password** - Secure password update
- Display user role (Admin/User)
- Avatar with initials

### 👥 User Management (Admin Only)

#### All Users Page
- **Paginated User Table** with sortable columns
- **Search** - Find users by name, email, company
- **Export to CSV** - Download filtered or all users
- **Last Login Column** - Shows relative time (e.g., "2h ago")
- **Status Column** - Active/Disabled indicator
- **Role Badges** - Visual role indicators

#### User Actions
- **View/Edit User** - Click username or edit button to open modal
- **Change Role** - Promote/demote users (ADMIN ↔ USER)
- **Enable/Disable** - Toggle user account status
- **Delete User** - Remove users with confirmation

### 🔄 User Generation (Admin Only)
- Generate fake users with configurable count
- **Specify Admin Count** - Choose how many admins to generate
- Download users as JSON file
- Uses the backend's Faker library integration

### 📁 Batch Import (Admin Only)
- Upload JSON files to import users
- Drag & drop support
- Visual progress feedback
- Import results with success/failure statistics

### 🔍 User Lookup (Admin Only)
- Search for any user by username
- View complete user details
- Role-based access control

### ⚙️ Settings
- **Change Password** - Update your password securely

## 🎨 User Interface

The application features a modern dark theme with:
- Gradient accent colors (Indigo to Purple)
- Smooth animations and transitions
- Responsive sidebar navigation
- Card-based layouts
- Beautiful iconography using Ikonli
- Modal dialogs for editing
- Toast notifications for feedback

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- The backend Spring Boot application running on `localhost:9090`

## Running the Application

### Option 1: Using Maven (Recommended)

Open a terminal in the `javafx-client` folder and run:

```bash
# Windows (from javafx-client folder)
..\mvnw.cmd javafx:run

# Or from project root
cd javafx-client && ..\mvnw.cmd javafx:run
```

### Option 2: Using IntelliJ IDEA

**Step 1: Import the JavaFX Client Module**
1. Go to **File → Project Structure** (Ctrl+Alt+Shift+S)
2. Click **Modules** → **+** → **Import Module**
3. Select `javafx-client/pom.xml`
4. Click **OK** and wait for import to complete

**Step 2: Reload Maven**
1. Open the **Maven** tool window (View → Tool Windows → Maven)
2. Click the **🔄 Reload All Maven Projects** button
3. Wait for all dependencies to download

**Step 3: Run the Application**
1. In the **Maven** tool window, expand:  
   `jwt-user-management-client → Plugins → javafx`
2. Double-click on **javafx:run**

### Option 3: Run from IntelliJ Terminal

1. Open Terminal in IntelliJ (View → Tool Windows → Terminal)
2. Navigate to javafx-client: `cd javafx-client`
3. Run: `..\mvnw.cmd javafx:run`

## Project Structure

```
javafx-client/
├── src/main/java/
│   ├── module-info.java
│   └── com/badereddine/client/
│       ├── JwtUserManagementApp.java          # Main application entry
│       ├── controller/
│       │   ├── LoginController.java           # Login screen controller
│       │   └── DashboardController.java       # Main dashboard controller
│       ├── model/
│       │   ├── AuthResponse.java              # JWT auth response model
│       │   ├── User.java                      # User model
│       │   ├── Role.java                      # Role model
│       │   ├── UserStats.java                 # User statistics model
│       │   ├── UserListResponse.java          # Paginated user list
│       │   ├── PasswordChangeRequest.java     # Password change DTO
│       │   └── BatchImportResult.java         # Import result model
│       ├── service/
│       │   ├── ApiService.java                # REST API client
│       │   └── SessionManager.java            # Session/auth management
│       └── util/
│           ├── SceneManager.java              # Scene navigation
│           ├── AnimationUtils.java            # UI animations
│           └── UIComponents.java              # Reusable UI components
├── src/main/resources/
│   └── styles/
│       └── main.css                           # Application stylesheet
└── pom.xml
```

## Technologies Used

- **JavaFX 21** - Modern Java UI framework
- **ControlsFX** - Extended UI controls
- **Ikonli** - Icon packs (FontAwesome5, MaterialDesign)
- **OkHttp** - HTTP client for REST API calls
- **Gson** - JSON parsing

## Design System

### Colors
| Purpose | Color |
|---------|-------|
| Primary | `#6366F1` (Indigo) |
| Secondary | `#8B5CF6` (Purple) |
| Success | `#10B981` (Emerald) |
| Warning | `#F59E0B` (Amber) |
| Danger | `#EF4444` (Red) |
| Background | `#0F172A` (Slate 900) |
| Surface | `#1E293B` (Slate 800) |

### Typography
- Primary font: Segoe UI / SF Pro Display
- Font sizes: 11px - 36px
- Font weights: Normal, Medium, Bold

## API Endpoints Used

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth` | User authentication (updates lastLogin) |
| GET | `/api/users/me` | Get current user profile |
| PUT | `/api/users/me` | Update current user profile |
| POST | `/api/users/me/password` | Change password |
| GET | `/api/users` | List all users (paginated, searchable) |
| GET | `/api/users/{username}` | Get user by username |
| GET | `/api/users/id/{id}` | Get user by ID |
| PUT | `/api/users/{id}` | Update user (Admin) |
| DELETE | `/api/users/{id}` | Delete user (Admin) |
| PATCH | `/api/users/{id}/role` | Change user role (Admin) |
| PATCH | `/api/users/{id}/status` | Enable/disable user (Admin) |
| GET | `/api/users/generate/{count}` | Generate fake users |
| POST | `/api/users/batch` | Batch import users |
| GET | `/api/users/export/csv` | Export users to CSV |
| GET | `/api/stats/users` | Get user statistics |

## Configuration

The client connects to `http://localhost:9090/api` by default. Override the complete API base URL with either:

- Environment variable: `TEAM_ACCESS_HUB_API_BASE_URL`
- JVM property: `teamaccesshub.api.base-url`

The JVM property takes precedence when both are present. For example, in PowerShell:

```powershell
$env:TEAM_ACCESS_HUB_API_BASE_URL = "https://access.example.com/api"
..\mvnw.cmd javafx:run
```

For an IDE launch configuration, add a VM option such as
`-Dteamaccesshub.api.base-url=https://access.example.com/api`.

The value must be an absolute `http` or `https` URL with a host and no credentials, query string, or fragment. Trailing slashes are removed automatically.

## Building

To create a standalone JAR:

```bash
mvn clean package
```

## License

This project is part of the JWT User Management demonstration application.
