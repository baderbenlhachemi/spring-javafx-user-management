# JWT User Management

A comprehensive user management application built with `Spring Boot` and `Spring Security`, featuring JWT authentication, role-based access control, and a modern JavaFX desktop client. The application generates realistic user data using the `Faker` library and provides complete CRUD operations for user management.

## ✨ Features

### Backend (Spring Boot)
- 🔐 **JWT Authentication** - Secure token-based authentication
- 👥 **Role-Based Access Control** - Admin and User roles with specific permissions
- 📊 **User Management** - Full CRUD operations for users
- 🔄 **Batch Import/Export** - Import users from JSON, export to CSV
- 📈 **User Statistics** - Dashboard stats for admins
- 🔍 **Search & Pagination** - Server-side search with sorting
- ⏱️ **Last Login Tracking** - Track when users last logged in
- 🚫 **Enable/Disable Users** - Control user access without deletion

### JavaFX Client
- 🎨 **Modern Dark Theme UI** - Beautiful, responsive interface
- 👤 **Profile Management** - View and edit your profile
- 👥 **User Administration** - Full user management for admins
- 📊 **Dashboard Statistics** - Real-time user stats
- 📁 **Generate & Import Users** - Create fake users or import from JSON
- 📥 **Export to CSV** - Download user list as spreadsheet
- 🔍 **Search & Filter** - Find users quickly
- ✏️ **Inline Editing** - Edit users via modal dialogs

## 🚀 Quick Start

### Development Administrator

The default profile does not create an administrator. Development-only initialization is opt-in through the `dev` profile, and its username, password, and email must be supplied externally.

### Running the Application

1. **Set the required backend environment variables:**

   ```powershell
   $env:DB_URL = "jdbc:postgresql://localhost:5432/team_access_hub"
   $env:DB_USERNAME = "<database-username>"
   $env:DB_PASSWORD = "<database-password>"
   $env:JWT_SECRET = "<base64-encoded-256-bit-key>"

   # Optional development-only administrator
   $env:SPRING_PROFILES_ACTIVE = "dev"
   $env:DEMO_ADMIN_USERNAME = "<development-admin-username>"
   $env:DEMO_ADMIN_PASSWORD = "<development-admin-password>"
   $env:DEMO_ADMIN_EMAIL = "<development-admin-email>"
   ```

   `JWT_EXPIRATION_MS` is optional and defaults to `86400000`. The committed `.env.example` is a reference only; Spring Boot does not automatically load `.env` files. Keep local values in your shell, IDE run configuration, or another ignored secret store.

2. **Start the Spring Boot backend:**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

3. **Open the JavaFX client** (recommended) or use Swagger UI:
   - Swagger UI: http://localhost:9090/swagger-ui/index.html

4. **Login with the development administrator values you supplied**, if the `dev` profile is active.

## 🖥️ JavaFX Client Application

A modern JavaFX desktop client is available in the `javafx-client` folder. It provides a beautiful dark-themed UI to interact with all API endpoints.

### Features
- 🔐 JWT Authentication with animated login screen
- 👤 User profile viewing and editing
- 👥 Complete user management (Admin)
- 📊 Real-time user statistics dashboard (Admin)
- 🔄 Generate fake users with customizable admin count
- 📁 Batch user import from JSON files
- 📥 Export users to CSV
- 🔍 Advanced search with sorting
- ✏️ Edit any user via modal dialog (Admin)
- 🔄 Change user roles (Admin)
- 🚫 Enable/Disable users (Admin)
- 🗑️ Delete users (Admin)
- ⏱️ Last login tracking
- 🔒 Change password

### Running the JavaFX Client

```bash
cd javafx-client
..\mvnw.cmd javafx:run
```

Or simply run `javafx-client\run.bat` on Windows.

> **Note:** Make sure the Spring Boot backend is running on `localhost:9090` before starting the client.

## 📡 API Endpoints

### Authentication

#### Login
- **Method:** POST  
- **URL:** `/api/auth`
- **Body:** `{ "username": "string", "password": "string" }`
- **Response:** JWT token and user details
- **Note:** Updates last login timestamp

#### Register (Admin only)
- **Method:** POST
- **URL:** `/api/signup`
- **Body:** User registration details
- **Secured:** Yes (Admin)

### User Profile

#### Get My Profile
- **Method:** GET  
- **URL:** `/api/users/me`
- **Secured:** Yes (User/Admin)

#### Update My Profile
- **Method:** PUT
- **URL:** `/api/users/me`
- **Secured:** Yes (User/Admin)

#### Change Password
- **Method:** POST
- **URL:** `/api/users/me/password`
- **Body:** `{ "currentPassword": "string", "newPassword": "string" }`
- **Secured:** Yes (User/Admin)

### User Management (Admin)

#### List All Users
- **Method:** GET  
- **URL:** `/api/users`
- **Parameters:** `page`, `size`, `sortBy`, `sortDir`, `search`
- **Secured:** Yes (Admin)

#### Get User by Username
- **Method:** GET  
- **URL:** `/api/users/{username}`
- **Secured:** Yes (Admin)

#### Get User by ID
- **Method:** GET
- **URL:** `/api/users/id/{id}`
- **Secured:** Yes (Admin)

#### Update User
- **Method:** PUT
- **URL:** `/api/users/{id}`
- **Secured:** Yes (Admin)

#### Delete User
- **Method:** DELETE
- **URL:** `/api/users/{id}`
- **Secured:** Yes (Admin)

#### Change User Role
- **Method:** PATCH
- **URL:** `/api/users/{id}/role?role=ROLE_USER|ROLE_ADMIN`
- **Secured:** Yes (Admin)

#### Toggle User Status
- **Method:** PATCH
- **URL:** `/api/users/{id}/status?enabled=true|false`
- **Secured:** Yes (Admin)

### User Generation & Import

#### Generate Fake Users
- **Method:** GET  
- **URL:** `/api/users/generate/{count}?adminCount=0`
- **Parameters:** 
  - `count`: Total number of users to generate (inclusive range: 1–1000)
  - `adminCount`: Number of admin users (optional, default: 0; inclusive range: 0–`count`)
- **Secured:** Yes (Admin)
- **Response:** Downloads JSON file

#### Batch Import Users
- **Method:** POST  
- **URL:** `/api/users/batch`
- **Content-Type:** multipart/form-data
- **Parameters:** `file` (JSON file)
- **Limits:** 1 MiB and 1–1000 records
- **Required record fields:** `firstName`, `lastName`, `birthDate`, `city`, `country`, `avatar`, `company`, `jobPosition`, `mobile`, `username`, `email`, and `role.name`
- **Allowed roles:** `ROLE_USER` and `ROLE_ADMIN`
- **Security:** Password/hash, ID, status, and timestamp input is ignored. Imported accounts receive a server-generated non-recoverable credential and are disabled.
- **Secured:** Yes (Admin)

#### Export Users to CSV
- **Method:** GET
- **URL:** `/api/users/export/csv`
- **Parameters:** `search` (optional)
- **Secured:** Yes (Admin)
- **Response:** Downloads CSV file

### Statistics

#### Get User Statistics
- **Method:** GET
- **URL:** `/api/stats/users`
- **Secured:** Yes (Admin)
- **Response:**
  ```json
  {
    "totalUsers": 100,
    "totalAdmins": 5,
    "totalRegularUsers": 95,
    "newUsersToday": 3
  }
  ```

## 🗄️ Data Model

### User Fields
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique identifier |
| username | String | Login username |
| email | String | Email address |
| password | String | Encrypted password |
| firstName | String | First name |
| lastName | String | Last name |
| birthDate | Date | Date of birth |
| city | String | City |
| country | String | Country |
| company | String | Company name |
| jobPosition | String | Job title |
| mobile | String | Phone number |
| avatar | String | Avatar URL |
| role | Role | User role (ADMIN/USER) |
| enabled | Boolean | Account status |
| createdAt | Timestamp | Account creation date |
| lastLogin | Timestamp | Last login timestamp |

## ⚙️ Technical Details

- **Framework:** Spring Boot 3.x
- **Java Version:** 17
- **Database:** PostgreSQL (configurable)
- **Security:** Spring Security with JWT
- **API Documentation:** Swagger/OpenAPI
- **Build Tool:** Maven
- **Client:** JavaFX 21

## 🔧 Configuration

The application runs on port `9090` by default. Database credentials and JWT signing material are not stored in source control. Startup requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a Base64-encoded JWT key of at least 256 bits in `JWT_SECRET`. Missing required values cause startup to fail with the missing environment-variable name.

`JWT_EXPIRATION_MS` is optional and defaults to 24 hours. See `.env.example` for variable names and safe placeholders; never put real values in that tracked example file.

### Swagger UI
Access the API documentation at: http://localhost:9090/swagger-ui/index.html

## 📁 Project Structure

```
SpringBoot-JWT-UserManagement/
├── src/main/java/com/badereddine/demo/
│   ├── controller/       # REST controllers
│   ├── model/            # Entity classes
│   ├── repository/       # Data repositories
│   ├── service/          # Business logic
│   ├── security/         # JWT & Spring Security
│   ├── exception/        # Custom exceptions
│   └── payload/          # Request/Response DTOs
├── javafx-client/        # JavaFX desktop client
│   ├── src/main/java/    # Client source code
│   └── pom.xml           # Client dependencies
├── pom.xml               # Backend dependencies
└── readme.md             # This file
```

## 📜 License

This project is for educational purposes.
