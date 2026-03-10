# API Controllers - Complete Reference Guide

**Version**: 2.0.0  
**Last Updated**: March 4, 2026  
**Status**: Production Ready

---

## 📌 Dashboard Data Controller

**Path**: `/api/dashboard-data`  
**CORS**: Enabled globally (no hardcoded origins)

### 1. Upload Excel File

```
POST /api/dashboard-data/upload
Content-Type: multipart/form-data
Authorization: Bearer <JWT_TOKEN>
```

**Request Parameters**:
- `file` (File, Required): Excel file (.xlsx, .xls, .xlsm)
- `conferenceId` (String, Required): MongoDB ID
- `dashboardMasterId` (String, Required): MongoDB ID

**Success Response (200)**:
```json
{
  "status": "success",
  "message": "File uploaded successfully",
  "fileName": "data.xlsx",
  "totalRecordsInFile": 1500,
  "newRecordsAdded": 1450,
  "duplicateRecordsIgnored": 50,
  "invalidRowsSkipped": 0,
  "processingTimeMs": 3450,
  "batchesInserted": 1
}
```

**Supported Column Names**:
- Email: `email`, `e-mail`, `email_address`, `mail`
- Name: `name`, `full_name`, `firstname`, `user_name`

### 2. Export Data

```
POST /api/dashboard-data/export
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**:
```json
{
  "conferenceId": "69a56b365232cf7e3802f000",
  "dashboardMasterId": "69a57dd45232cf7e3802f01a",
  "filters": {
    "fromSerialNo": 1,
    "toSerialNo": 1000,
    "status": true,
    "createdFromDate": "2026-01-01",
    "createdToDate": "2026-03-04"
  },
  "sortField": "serialNo",
  "sortOrder": "ASC"
}
```

**Response**: Excel file (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet)

---

## 🔐 Authentication Controller

**Path**: `/api/auth`

### 1. Sign In

```
POST /api/auth/signin
Content-Type: application/json
```

**Request**:
```json
{
  "username": "superadmin",
  "password": "SecurePassword123!"
}
```

**Response (200)**:
```json
{
  "id": "69a57ac65232cf7e3802f009",
  "username": "superadmin",
  "email": "admin@example.com",
  "firstName": "Super",
  "lastName": "Admin",
  "role": "SUPER_ADMIN",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "oI7QfqgYBeHRjZaHKdZ9HOVEI2ifRqTucQAZTYa1vIc",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

### 2. Logout

```
POST /api/auth/logout
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request**:
```json
{
  "refreshToken": "oI7QfqgYBeHRjZaHKdZ9HOVEI2ifRqTucQAZTYa1vIc"
}
```

**Response (200)**:
```json
{
  "status": "success",
  "message": "Logged out successfully"
}
```

### 3. Refresh Token

```
POST /api/auth/refresh-token
Content-Type: application/json
```

**Request**:
```json
{
  "refreshToken": "oI7QfqgYBeHRjZaHKdZ9HOVEI2ifRqTucQAZTYa1vIc"
}
```

**Response (200)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "newRefreshToken...",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

---

## 👥 User Controller

**Path**: `/api/users`

### Get Current User Profile

```
GET /api/users/me
Authorization: Bearer <JWT_TOKEN>
```

**Response (200)**:
```json
{
  "id": "69a57ac65232cf7e3802f009",
  "username": "superadmin",
  "email": "admin@example.com",
  "firstName": "Super",
  "lastName": "Admin",
  "role": "SUPER_ADMIN",
  "conferenceIds": ["69a56b365232cf7e3802f000"],
  "createdAt": "2026-01-15T10:30:00Z",
  "lastLogin": "2026-03-04T10:48:45Z"
}
```

---

## 📊 Analytics Controller

**Path**: `/api/analytics`

### Get Upload Statistics

```
GET /api/analytics/upload-stats/me/conference/{conferenceId}
Authorization: Bearer <JWT_TOKEN>
```

**Response (200)**:
```json
{
  "success": true,
  "data": [
    {
      "id": "69a5...",
      "adminId": "69a57ac65232cf7e3802f009",
      "conferenceId": "69a56b365232cf7e3802f000",
      "uploadedAt": "2026-03-04T10:48:44Z",
      "totalRecordsInFile": 500,
      "newRecordsAdded": 480,
      "duplicateRecordsIgnored": 20,
      "fileName": "data.xlsx"
    }
  ]
}
```

---

## 🏢 Conference Controller

**Path**: `/api/conferences`

### Get Conference by ID

```
GET /api/conferences/{conferenceId}
Authorization: Bearer <JWT_TOKEN>
```

**Response (200)**:
```json
{
  "id": "69a56b365232cf7e3802f000",
  "name": "Microbiome",
  "description": "International Microbiome Conference",
  "createdAt": "2026-01-01T00:00:00Z",
  "status": "ACTIVE"
}
```

---

## 📋 Dashboard Master Controller

**Path**: `/api/dashboard-masters`

### Get Dashboard Master by ID

```
GET /api/dashboard-masters/{dashboardMasterId}
Authorization: Bearer <JWT_TOKEN>
```

**Response (200)**:
```json
{
  "id": "69a57dd45232cf7e3802f01a",
  "name": "Journals & Publications",
  "description": "Track journal and publication submissions",
  "conferenceId": "69a56b365232cf7e3802f000",
  "createdAt": "2026-01-01T00:00:00Z"
}
```

---

## 🔑 Key Points

### CORS Configuration
- ✅ Handled globally in `WebSecurityConfig`
- ✅ Supports: `https://sciinovdbms.com`, `http://localhost:5173`
- ✅ No need for `@CrossOrigin` on controllers
- ✅ All HTTP methods supported: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD

### Authentication
- All endpoints except `/api/auth/*` require JWT token
- Token format: `Authorization: Bearer <TOKEN>`
- Token valid for 24 hours (configurable via JWT_EXPIRATION_MS)
- Use refresh-token endpoint before expiration

### File Upload Improvements
- ✅ Flexible column name normalization
- ✅ Supports variations: "Email", "E-mail", "email_address", etc.
- ✅ Batch processing with automatic rollback on failure
- ✅ Comprehensive error messages
- ✅ Progress tracking available

### Error Handling
- 400 Bad Request: Invalid input or file format
- 401 Unauthorized: Missing or invalid JWT token
- 403 Forbidden: Insufficient permissions (wrong role)
- 500 Internal Server Error: Database or server error

---

**For detailed documentation**: See `UPLOAD_AND_EXPORT_API_DOCUMENTATION.md`

