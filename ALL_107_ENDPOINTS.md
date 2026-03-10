# SciInov DBMS — Complete 107 API Endpoints Reference

**Total Endpoints:** 107
**Base URL:** `https://sciinovdbms-64307221061.asia-south1.run.app`
**Authentication:** Bearer JWT Token (unless marked `PUBLIC`)
**Content-Type:** `application/json`

---

## Table of Contents

1. [Authentication (8 endpoints)](#1-authentication-8-endpoints)
2. [Users (11 endpoints)](#2-users-11-endpoints)
3. [Conferences (13 endpoints)](#3-conferences-13-endpoints)
4. [Dashboard Masters (6 endpoints)](#4-dashboard-masters-6-endpoints)
5. [Dashboard Data (9 endpoints)](#5-dashboard-data-9-endpoints)
6. [Exports (10 endpoints)](#6-exports-10-endpoints)
7. [Conference Documents (15 endpoints)](#7-conference-documents-15-endpoints)
8. [Document Types (7 endpoints)](#8-document-types-7-endpoints)
9. [File Upload (7 endpoints)](#9-file-upload-7-endpoints)
10. [Analytics (18 endpoints)](#10-analytics-18-endpoints)
11. [Conference Counts (6 endpoints)](#11-conference-counts-6-endpoints)

---

## 1. Authentication (8 endpoints)

### 1.1 Sign In (PUBLIC)
**Method:** `POST`
**URL:** `/api/auth/signin`
**Auth Required:** No

**Request:**
```json
{
  "userId": "superadmin",
  "password": "YourPassword123"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
  "type": "Bearer",
  "id": "660a1b2c3d4e5f6a7b8c9d0e",
  "userId": "superadmin",
  "email": "admin@example.com",
  "roles": ["ROLE_SUPER_ADMIN"]
}
```

---

### 1.2 Refresh Token (PUBLIC)
**Method:** `POST`
**URL:** `/api/auth/refresh-token`
**Auth Required:** No

**Request:**
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
  "type": "Bearer"
}
```

---

### 1.3 Logout (PUBLIC)
**Method:** `POST`
**URL:** `/api/auth/logout`
**Auth Required:** No (works with or without JWT)

**Request (Optional):**
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}
```

**Response (200):**
```json
{
  "message": "Logged out successfully",
  "success": true
}
```

---

### 1.4 Get Token Expiration
**Method:** `GET`
**URL:** `/api/auth/token-expiration`
**Auth Required:** Yes

**Response (200):**
```json
{
  "success": true,
  "tokenExpirationMs": 86400000,
  "tokenExpirationMinutes": 1440,
  "tokenExpirationHours": 24
}
```

---

### 1.5 Forgot Password (PUBLIC)
**Method:** `POST`
**URL:** `/api/auth/forgot-password`
**Auth Required:** No

**Request:**
```json
{
  "userId": "admin_user"
}
```

**Response (200):**
```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "admin_user",
  "email": "admin@example.com",
  "message": "Password reset link has been sent to your registered email. Token is valid for 24 hours.",
  "success": true
}
```

---

### 1.6 Validate Reset Token (PUBLIC)
**Method:** `GET`
**URL:** `/api/auth/validate-reset-token?token={token}`
**Auth Required:** No

**Response (200):**
```json
{
  "message": "Token is valid",
  "success": true
}
```

---

### 1.7 Reset Password (PUBLIC)
**Method:** `POST`
**URL:** `/api/auth/reset-password`
**Auth Required:** No

**Request:**
```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "newPassword": "NewSecure@123",
  "confirmPassword": "NewSecure@123"
}
```

**Response (200):**
```json
{
  "message": "Password has been reset successfully. You can now login with your new password.",
  "success": true
}
```

---

### 1.8 Forgot Username (PUBLIC)
**Method:** `POST`
**URL:** `/api/auth/forgot-username`
**Auth Required:** No

**Request:**
```json
{
  "email": "user@example.com",
  "phoneNumber": "9876543210"
}
```

**Response (200):**
```json
{
  "userId": "admin_user",
  "maskedUserId": "ad***r",
  "email": "user@example.com",
  "maskedEmail": "us***@example.com",
  "message": "Your User ID has been sent to your registered email.",
  "success": true
}
```

---

### 1.9 Change Password
**Method:** `POST`
**URL:** `/api/auth/change-password`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Request:**
```json
{
  "currentPassword": "OldPass@123",
  "newPassword": "NewPass@456",
  "confirmPassword": "NewPass@456"
}
```

**Response (200):**
```json
{
  "message": "Password changed successfully",
  "success": true
}
```

---

## 2. Users (11 endpoints)

### 2.1 Get All Admins
**Method:** `GET`
**URL:** `/api/users/admins`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "660a...",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "9876543210",
    "email": "john@example.com",
    "userId": "johndoe",
    "role": "ADMIN",
    "status": true,
    "conferenceIds": ["conf1", "conf2"],
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-03-01T14:20:00",
    "deleted": false
  }
]
```

---

### 2.2 Get All Super Admins
**Method:** `GET`
**URL:** `/api/users/super-admins`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "660a...",
    "firstName": "Super",
    "lastName": "Admin",
    "userId": "superadmin",
    "email": "superadmin@example.com",
    "role": "SUPER_ADMIN",
    "status": true
  }
]
```

---

### 2.3 Get All Admins with Conferences
**Method:** `GET`
**URL:** `/api/users/admins/conferences/all`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "adminId": "660a...",
    "userId": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phoneNumber": "9876543210",
    "status": true,
    "conferenceIds": ["conf1", "conf2"]
  }
]
```

---

### 2.4 Get User by ID
**Method:** `GET`
**URL:** `/api/users/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "id": "660a...",
  "firstName": "John",
  "lastName": "Doe",
  "userId": "johndoe",
  "email": "john@example.com",
  "role": "ADMIN",
  "status": true,
  "conferenceIds": ["conf1"]
}
```

---

### 2.5 Create User
**Method:** `POST`
**URL:** `/api/users`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "userId": "janesmith",
  "email": "jane@example.com",
  "phoneNumber": "9876543211",
  "password": "SecurePass@123",
  "role": "ADMIN",
  "status": true,
  "conferenceIds": ["conf1"]
}
```

**Response (200):**
```json
{
  "id": "660b...",
  "firstName": "Jane",
  "lastName": "Smith",
  "userId": "janesmith",
  "email": "jane@example.com",
  "role": "ADMIN",
  "status": true,
  "createdAt": "2026-03-10T10:00:00",
  "updatedAt": "2026-03-10T10:00:00"
}
```

---

### 2.6 Update User
**Method:** `PUT`
**URL:** `/api/users/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane.smith@example.com",
  "phoneNumber": "9876543211",
  "status": true,
  "conferenceIds": ["conf1", "conf2"]
}
```

**Response (200):**
```json
{
  "id": "660b...",
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane.smith@example.com",
  "updatedAt": "2026-03-10T12:00:00"
}
```

---

### 2.7 Delete User
**Method:** `DELETE`
**URL:** `/api/users/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Empty body

---

### 2.8 Update User Status
**Method:** `PATCH`
**URL:** `/api/users/{id}/status`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "status": false
}
```

**Response (200):**
```json
{
  "id": "660b...",
  "userId": "janesmith",
  "status": false,
  "updatedAt": "2026-03-10T12:00:00"
}
```

---

### 2.9 Get Admin's Conferences
**Method:** `GET`
**URL:** `/api/users/{adminId}/conferences`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
["conf1", "conf2"]
```

---

### 2.10 Assign Conference to Admin
**Method:** `POST`
**URL:** `/api/users/{adminId}/conferences/{conferenceId}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "id": "660b...",
  "userId": "janesmith",
  "conferenceIds": ["conf1", "conf2"]
}
```

---

### 2.11 Remove Conference from Admin
**Method:** `DELETE`
**URL:** `/api/users/{adminId}/conferences/{conferenceId}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "id": "660b...",
  "userId": "janesmith",
  "conferenceIds": ["conf1"]
}
```

---

## 3. Conferences (13 endpoints)

### 3.1 Get All Conferences
**Method:** `GET`
**URL:** `/api/conferences`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "660a...",
    "title": "Tech Summit 2026",
    "imageUrl": "https://storage.googleapis.com/...",
    "imageBlobName": "conferences/tech-summit/image.png",
    "status": "ACTIVE",
    "dashboardMasterIds": ["dm1", "dm2"],
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-03-01T00:00:00",
    "deleted": false
  }
]
```

---

### 3.2 Get My Conferences (Admin)
**Method:** `GET`
**URL:** `/api/conferences/me`
**Auth Required:** Yes (ADMIN)

**Response (200):**
```json
[
  {
    "id": "660a...",
    "title": "Tech Summit 2026",
    "status": "ACTIVE"
  }
]
```

---

### 3.3 Get Conference by ID
**Method:** `GET`
**URL:** `/api/conferences/{id}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "id": "660a...",
  "title": "Tech Summit 2026",
  "imageUrl": "https://storage.googleapis.com/...",
  "status": "ACTIVE",
  "dashboardMasterIds": ["dm1", "dm2"]
}
```

---

### 3.4 Create Conference
**Method:** `POST`
**URL:** `/api/conferences`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "title": "AI Conference 2026",
  "status": "ACTIVE"
}
```

**Response (200):**
```json
{
  "id": "660c...",
  "title": "AI Conference 2026",
  "status": "ACTIVE",
  "createdAt": "2026-03-10T10:00:00"
}
```

---

### 3.5 Update Conference
**Method:** `PUT`
**URL:** `/api/conferences/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "title": "AI Conference 2026 (Updated)",
  "status": "ACTIVE",
  "dashboardMasterIds": ["dm1", "dm2"]
}
```

**Response (200):**
```json
{
  "id": "660c...",
  "title": "AI Conference 2026 (Updated)",
  "updatedAt": "2026-03-10T11:00:00"
}
```

---

### 3.6 Update Conference Status
**Method:** `PATCH`
**URL:** `/api/conferences/{id}/status`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "status": "INACTIVE"
}
```

**Response (200):**
```json
{
  "id": "660c...",
  "title": "AI Conference 2026",
  "status": "INACTIVE",
  "updatedAt": "2026-03-10T11:00:00"
}
```

---

### 3.7 Delete Conference
**Method:** `DELETE`
**URL:** `/api/conferences/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Empty body

---

### 3.8 Get Conference Dashboards
**Method:** `GET`
**URL:** `/api/conferences/{id}/dashboards`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "conferenceId": "660a...",
  "conferenceTitle": "Tech Summit 2026",
  "dashboards": [
    {
      "id": "dm1",
      "name": "Registration Dashboard",
      "status": true,
      "createdAt": "2026-01-01T00:00:00"
    }
  ],
  "message": "Dashboards retrieved successfully",
  "success": true
}
```

---

### 3.9 Attach Dashboards
**Method:** `POST`
**URL:** `/api/conferences/{id}/dashboards/attach`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "dashboardMasterIds": ["dm1", "dm2"]
}
```

**Response (200):**
```json
{
  "conferenceId": "660a...",
  "conferenceTitle": "Tech Summit 2026",
  "dashboards": [...],
  "message": "Dashboards attached successfully",
  "success": true
}
```

---

### 3.10 Detach Dashboards
**Method:** `POST`
**URL:** `/api/conferences/{id}/dashboards/detach`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "dashboardMasterIds": ["dm1"]
}
```

**Response (200):**
```json
{
  "conferenceId": "660a...",
  "conferenceTitle": "Tech Summit 2026",
  "dashboards": [...],
  "message": "Dashboards detached successfully",
  "success": true
}
```

---

### 3.11 Set/Replace Dashboards
**Method:** `PUT`
**URL:** `/api/conferences/{id}/dashboards`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "dashboardMasterIds": ["dm1", "dm2", "dm3"]
}
```

**Response (200):**
```json
{
  "conferenceId": "660a...",
  "conferenceTitle": "Tech Summit 2026",
  "dashboards": [...],
  "message": "Dashboards set successfully",
  "success": true
}
```

---

### 3.12 Upload Conference Image
**Method:** `POST`
**URL:** `/api/conferences/{id}/upload-image`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)
**Content-Type:** `multipart/form-data`

**Form Data:**
- `image` (File): Image file (JPEG, PNG, GIF, WebP)

**Response (200):**
```json
{
  "success": true,
  "message": "Conference image uploaded successfully",
  "conferenceId": "660a...",
  "conferenceName": "Tech Summit 2026",
  "imageUrl": "https://storage.googleapis.com/...",
  "imageBlobName": "conferences/tech-summit/image.png"
}
```

---

## 4. Dashboard Masters (6 endpoints)

### 4.1 Get Dashboard Types
**Method:** `GET`
**URL:** `/api/dashboard-masters/types`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "total": 3,
  "data": [
    {
      "id": "dm1",
      "name": "Registration Dashboard",
      "status": true,
      "createdAt": "2026-01-01T00:00:00"
    }
  ]
}
```

---

### 4.2 Get All Dashboard Masters
**Method:** `GET`
**URL:** `/api/dashboard-masters`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "dm1",
    "name": "Registration Dashboard",
    "status": true
  }
]
```

---

### 4.3 Get Dashboard Master by ID
**Method:** `GET`
**URL:** `/api/dashboard-masters/{id}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "id": "dm1",
  "name": "Registration Dashboard",
  "status": true
}
```

---

### 4.4 Create Dashboard Master
**Method:** `POST`
**URL:** `/api/dashboard-masters`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "name": "Attendee Dashboard",
  "status": true
}
```

**Response (200):**
```json
{
  "id": "dm2",
  "name": "Attendee Dashboard",
  "status": true
}
```

---

### 4.5 Update Dashboard Master
**Method:** `PUT`
**URL:** `/api/dashboard-masters/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "name": "Attendee Dashboard (Updated)",
  "status": true
}
```

**Response (200):**
```json
{
  "id": "dm2",
  "name": "Attendee Dashboard (Updated)",
  "status": true,
  "updatedAt": "2026-03-10T12:00:00"
}
```

---

### 4.6 Delete Dashboard Master
**Method:** `DELETE`
**URL:** `/api/dashboard-masters/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Empty body

---

## 5. Dashboard Data (9 endpoints)

### 5.1 Upload Excel
**Method:** `POST`
**URL:** `/api/dashboard-data/upload`
**Auth Required:** Yes (ADMIN)
**Content-Type:** `multipart/form-data`

**Form Data:**
- `file` (File): Excel file (.xlsx, .xls, .xlsm) - Required
- `conferenceId` (String): Conference ID - Required
- `dashboardMasterId` (String): Dashboard Master ID - Required

**Response (200):**
```json
{
  "status": "success",
  "message": "File uploaded successfully",
  "fileName": "registrations.xlsx",
  "totalRecordsInFile": 50000,
  "newRecordsAdded": 48500,
  "duplicateRecordsIgnored": 1200,
  "invalidRowsSkipped": 300,
  "processingTimeMs": 12345,
  "batchesInserted": 10
}
```

---

### 5.2 Get Dashboard Data (Paginated)
**Method:** `GET`
**URL:** `/api/dashboard-data?conferenceId={id}&dashboardMasterId={id}&fromSerialNo={from}&toSerialNo={to}&page=0&size=500`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "data": [
    {
      "id": "660d...",
      "conferenceId": "conf1",
      "dashboardMasterId": "dm1",
      "serialNo": 1,
      "name": "John Doe",
      "email": "john@gmail.com",
      "emailExtension": "gmail.com",
      "status": true,
      "createdAt": "2026-03-01T10:00:00"
    }
  ],
  "currentPage": 0,
  "pageSize": 500,
  "totalRecords": 50000,
  "totalPages": 100,
  "fromSerialNo": 1,
  "toSerialNo": 1000
}
```

---

### 5.3 Filter Dashboard Data
**Method:** `GET`
**URL:** `/api/dashboard-data/filter?conferenceId={id}&dashboardMasterId={id}&fromSerialNo={from}&toSerialNo={to}&startDate=2026-01-01&endDate=2026-03-10&emailDomain=gmail.com&page=0&size=500`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "data": [...],
  "currentPage": 0,
  "pageSize": 500,
  "totalRecords": 12345,
  "totalPages": 25
}
```

---

### 5.4 Get Data by Date Range
**Method:** `GET`
**URL:** `/api/dashboard-data/by-date?conferenceId={id}&dashboardMasterId={id}&startDate=2026-01-01&endDate=2026-03-10&page=0&size=500`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "data": [...],
  "currentPage": 0,
  "pageSize": 500,
  "totalRecords": 5000,
  "totalPages": 10
}
```

---

### 5.5 Get Data by Domain Extension
**Method:** `GET`
**URL:** `/api/dashboard-data/by-domain-extension?conferenceId={id}&dashboardMasterId={id}&extension=com&fromSerialNo=1&toSerialNo=1000`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "data": [...],
  "totalMatchingInRange": 5000,
  "recordsReturned": 1000,
  "maxRecordsPerRequest": 1000,
  "hasMoreRecords": true,
  "requestedRange": "1 - 1000",
  "rangeCoverage": "partial",
  "nextRangeSuggestion": {
    "fromSerialNo": 1001,
    "toSerialNo": 2000,
    "message": "To get the next batch, request from serial 1001 to 2000"
  }
}
```

---

### 5.6 Get Domain Extensions List
**Method:** `GET`
**URL:** `/api/dashboard-data/domain-extensions?conferenceId={id}&dashboardMasterId={id}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "total": 5,
  "extensions": ["com", "edu", "in", "net", "org"]
}
```

---

### 5.7 Get Data by Email Domain
**Method:** `GET`
**URL:** `/api/dashboard-data/by-email-domain?conferenceId={id}&dashboardMasterId={id}&emailDomain=gmail.com`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "emailDomain": "gmail.com",
  "totalRecords": 12345,
  "data": [...]
}
```

---

### 5.8 Get Filtered Data Count
**Method:** `GET`
**URL:** `/api/dashboard-data/count?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=1000&startDate=2026-01-01&endDate=2026-03-10&emailDomain=gmail.com`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "count": 50000,
  "conferenceId": "conf1",
  "dashboardMasterId": "dm1"
}
```

---

## 6. Exports (10 endpoints)

### 6.1 Export Excel (Basic)
**Method:** `GET`
**URL:** `/api/export/excel?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=1000`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response:** Binary Excel file (.xlsx)

---

### 6.2 Export PDF (Basic)
**Method:** `GET`
**URL:** `/api/export/pdf?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=1000`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response:** Binary PDF file

---

### 6.3 Export Excel (Advanced Query Params)
**Method:** `GET`
**URL:** `/api/export/excel/advanced?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=1000&startDate=2026-01-01&endDate=2026-03-10&emailDomain=gmail.com`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response:** Binary Excel file (.xlsx)

---

### 6.4 Export PDF (Advanced Query Params)
**Method:** `GET`
**URL:** `/api/export/pdf/advanced?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=1000&startDate=2026-01-01&endDate=2026-03-10&emailDomain=gmail.com`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response:** Binary PDF file

---

### 6.5 Export Excel (Body Filter)
**Method:** `POST`
**URL:** `/api/export/excel/filter`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Request:**
```json
{
  "conferenceId": "conf1",
  "dashboardMasterId": "dm1",
  "fromSerialNo": 1,
  "toSerialNo": 5000,
  "startDate": "2026-01-01",
  "endDate": "2026-03-10",
  "emailDomain": "gmail.com"
}
```

**Response:** Binary Excel file (.xlsx)

---

### 6.6 Export PDF (Body Filter)
**Method:** `POST`
**URL:** `/api/export/pdf/filter`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Request:**
```json
{
  "conferenceId": "conf1",
  "dashboardMasterId": "dm1",
  "fromSerialNo": 1,
  "toSerialNo": 5000,
  "startDate": "2026-01-01",
  "endDate": "2026-03-10",
  "emailDomain": "gmail.com"
}
```

**Response:** Binary PDF file

---

### 6.7 Export Excel by Extension
**Method:** `GET`
**URL:** `/api/export/excel/by-extension?conferenceId={id}&dashboardMasterId={id}&extension=com&fromSerialNo=1&toSerialNo=1000`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response:** Binary Excel file (.xlsx) with max 1000 records

---

### 6.8 Export PDF by Extension
**Method:** `GET`
**URL:** `/api/export/pdf/by-extension?conferenceId={id}&dashboardMasterId={id}&extension=edu&fromSerialNo=1&toSerialNo=1000`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response:** Binary PDF file with max 1000 records

---

### 6.9 Preview Export Data
**Method:** `GET`
**URL:** `/api/export/preview?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=1000&startDate=2026-01-01&endDate=2026-03-10&emailDomain=gmail.com`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "660d...",
    "conferenceId": "conf1",
    "dashboardMasterId": "dm1",
    "serialNo": 1,
    "name": "John Doe",
    "email": "john@gmail.com",
    "status": true
  }
]
```

---

### 6.10 Get Email Domains
**Method:** `GET`
**URL:** `/api/export/email-domains?conferenceId={id}&dashboardMasterId={id}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
["gmail.com", "yahoo.com", "outlook.com"]
```

---

## 7. Conference Documents (15 endpoints)

### 7.1 Upload Document
**Method:** `POST`
**URL:** `/api/conference-documents/upload`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)
**Content-Type:** `multipart/form-data`

**Form Data:**
- `conferenceId` (String): Conference ID - Required
- `year` (Integer): Year (e.g., 2026) - Required
- `documentType` (String): Document type slug (e.g., "program", "book") - Required
- `file` (File): Document file - Required

**Response (200):**
```json
{
  "success": true,
  "message": "Document uploaded successfully.",
  "data": {
    "id": "660e...",
    "conferenceId": "conf1",
    "conferenceName": "Tech Summit 2026",
    "year": 2026,
    "documentType": "program",
    "documentTypeDisplayName": "Program",
    "fileName": "program-2026.pdf",
    "blobName": "conferences/tech-summit/2026/program/program-2026.pdf",
    "publicUrl": "https://storage.googleapis.com/...",
    "fileSize": 1048576,
    "contentType": "application/pdf",
    "uploadedAt": "2026-03-10T10:00:00",
    "uploadedByUserId": "660a...",
    "uploadedByUserName": "John Doe"
  }
}
```

---

### 7.2 Get Conference Documents
**Method:** `GET`
**URL:** `/api/conference-documents?conferenceId={id}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "660e...",
    "conferenceId": "conf1",
    "conferenceName": "Tech Summit 2026",
    "year": 2026,
    "documentType": "program",
    "documentTypeDisplayName": "Program",
    "fileName": "program-2026.pdf",
    "publicUrl": "https://storage.googleapis.com/..."
  }
]
```

---

### 7.3 Get Documents by Year
**Method:** `GET`
**URL:** `/api/conference-documents/year?conferenceId={id}&year=2026`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "660e...",
    "conferenceId": "conf1",
    "year": 2026,
    "documentType": "program",
    "fileName": "program-2026.pdf"
  }
]
```

---

### 7.4 Get Documents by Type
**Method:** `GET`
**URL:** `/api/conference-documents/type?conferenceId={id}&documentType=program`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "660e...",
    "conferenceId": "conf1",
    "documentType": "program",
    "fileName": "program-2026.pdf"
  }
]
```

---

### 7.5 Search Documents
**Method:** `POST`
**URL:** `/api/conference-documents/search`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Request:**
```json
{
  "conferenceId": "conf1",
  "year": 2026,
  "documentType": "program",
  "pageNumber": 0,
  "pageSize": 20
}
```

**Response (200):**
```json
[
  {
    "id": "660e...",
    "conferenceId": "conf1",
    "year": 2026,
    "documentType": "program",
    "fileName": "program-2026.pdf"
  }
]
```

---

### 7.6 Get All Documents (Super Admin)
**Method:** `GET`
**URL:** `/api/conference-documents/admin/all?conferenceId={id}&year=2026&documentType=program&pageNumber=0&pageSize=20`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "660e...",
    "conferenceId": "conf1",
    "year": 2026,
    "documentType": "program",
    "fileName": "program-2026.pdf"
  }
]
```

---

### 7.7 Get Document Count (Super Admin)
**Method:** `GET`
**URL:** `/api/conference-documents/admin/count`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "totalDocumentCount": 150
}
```

---

### 7.8 Update Document (Super Admin)
**Method:** `PUT`
**URL:** `/api/conference-documents/admin/{documentId}`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "year": 2026,
  "documentType": "program"
}
```

**Response (200):**
```json
{
  "id": "660e...",
  "year": 2026,
  "documentType": "program",
  "updatedAt": "2026-03-10T11:00:00"
}
```

---

### 7.9 Delete Document (Super Admin)
**Method:** `DELETE`
**URL:** `/api/conference-documents/admin/{documentId}?deleteType=soft`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Empty body

---

### 7.10 Search All Documents (Super Admin)
**Method:** `POST`
**URL:** `/api/conference-documents/admin/search`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "conferenceId": "conf1",
  "year": 2026,
  "documentType": "program"
}
```

**Response (200):**
```json
[
  {
    "id": "660e...",
    "conferenceId": "conf1",
    "year": 2026,
    "documentType": "program"
  }
]
```

---

### 7.11 Get Available Years
**Method:** `GET`
**URL:** `/api/conference-documents/available-years?conferenceId={id}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "conferenceId": "conf1",
  "availableYears": [2024, 2025, 2026],
  "count": 3
}
```

---

### 7.12 Admin Documents Dashboard
**Method:** `GET`
**URL:** `/api/conference-documents/admin/dashboard`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "totalDocuments": 150,
  "conferenceStats": [
    {
      "conferenceId": "conf1",
      "conferenceName": "Tech Summit 2026",
      "documentCount": 50
    }
  ]
}
```

---

### 7.13 Get Document Statistics
**Method:** `GET`
**URL:** `/api/conference-documents/statistics?conferenceId={id}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "conferenceId": "conf1",
  "statistics": {
    "totalDocuments": 30,
    "byType": {
      "program": 10,
      "book": 15,
      "positive_sheets": 5
    },
    "byYear": {
      "2024": 5,
      "2025": 10,
      "2026": 15
    }
  }
}
```

---

### 7.14 Get Global Statistics (Super Admin)
**Method:** `GET`
**URL:** `/api/conference-documents/statistics/global`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "globalStatistics": {
    "totalDocuments": 500,
    "totalConferences": 10,
    "totalYears": 3
  }
}
```

---

### 7.15 Get Document by Type (All Conferences - Super Admin)
**Method:** `GET`
**URL:** `/api/conference-documents/year-all?year=2026`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
[
  {
    "id": "660e...",
    "conferenceId": "conf1",
    "year": 2026,
    "documentType": "program"
  }
]
```

---

## 8. Document Types (7 endpoints)

### 8.1 Get Active Types
**Method:** `GET`
**URL:** `/api/document-types/active`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "total": 3,
  "data": [
    {
      "id": "660f...",
      "slug": "program",
      "displayName": "Program",
      "description": "Conference program document",
      "folderName": "program",
      "sortOrder": 1,
      "active": true,
      "createdAt": "2026-01-01T00:00:00",
      "createdByUserId": "660a...",
      "createdByUserName": "Super Admin"
    }
  ]
}
```

---

### 8.2 Get All Types (Super Admin)
**Method:** `GET`
**URL:** `/api/document-types`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "total": 5,
  "data": [...]
}
```

---

### 8.3 Get Type by ID (Super Admin)
**Method:** `GET`
**URL:** `/api/document-types/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "id": "660f...",
  "slug": "program",
  "displayName": "Program",
  "description": "Conference program document",
  "active": true
}
```

---

### 8.4 Create Type (Super Admin)
**Method:** `POST`
**URL:** `/api/document-types`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "displayName": "Positive Sheets",
  "description": "Attendance positive sheets",
  "sortOrder": 3,
  "active": true
}
```

**Response (200):**
```json
{
  "id": "6610...",
  "slug": "positive-sheets",
  "displayName": "Positive Sheets",
  "description": "Attendance positive sheets",
  "sortOrder": 3,
  "active": true,
  "createdAt": "2026-03-10T10:00:00"
}
```

---

### 8.5 Update Type (Super Admin)
**Method:** `PUT`
**URL:** `/api/document-types/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Request:**
```json
{
  "displayName": "Positive Sheets (Updated)",
  "description": "Attendance positive sheets",
  "sortOrder": 3,
  "active": true
}
```

**Response (200):**
```json
{
  "id": "6610...",
  "displayName": "Positive Sheets (Updated)",
  "updatedAt": "2026-03-10T11:00:00"
}
```

---

### 8.6 Toggle Active (Super Admin)
**Method:** `PATCH`
**URL:** `/api/document-types/{id}/toggle-active`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "id": "6610...",
  "displayName": "Positive Sheets",
  "active": false,
  "updatedAt": "2026-03-10T11:00:00"
}
```

---

### 8.7 Delete Type (Super Admin)
**Method:** `DELETE`
**URL:** `/api/document-types/{id}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Empty body

---

## 9. File Upload (7 endpoints)

### 9.1 Upload File
**Method:** `POST`
**URL:** `/api/files/upload`
**Auth Required:** Yes (ADMIN)
**Content-Type:** `multipart/form-data`

**Form Data:**
- `file` (File): File to upload - Required
- `conferenceId` (String): Conference ID - Required
- `dashboardMasterId` (String): Dashboard Master ID - Required

**Response (200):**
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "fileUrl": "https://storage.googleapis.com/...",
  "fileName": "document.pdf",
  "fileSize": 1048576,
  "contentType": "application/pdf"
}
```

---

### 9.2 Upload Multiple Files
**Method:** `POST`
**URL:** `/api/files/upload-multiple`
**Auth Required:** Yes (ADMIN)
**Content-Type:** `multipart/form-data`

**Form Data:**
- `files` (File[]): Multiple files - Required
- `conferenceId` (String): Conference ID - Required
- `dashboardMasterId` (String): Dashboard Master ID - Required

**Response (200):**
```json
{
  "success": true,
  "message": "Files uploaded successfully",
  "uploadedCount": 3,
  "files": [
    {
      "fileName": "document1.pdf",
      "fileUrl": "https://storage.googleapis.com/...",
      "fileSize": 1048576
    }
  ]
}
```

---

### 9.3 List Files
**Method:** `GET`
**URL:** `/api/files/list?conferenceId={id}&dashboardMasterId={id}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "files": [
    {
      "blobName": "conferences/tech-summit/2026/document.pdf",
      "fileName": "document.pdf",
      "fileSize": 1048576,
      "uploadedAt": "2026-03-10T10:00:00"
    }
  ]
}
```

---

### 9.4 Delete File
**Method:** `DELETE`
**URL:** `/api/files/delete?blobName={blobName}`
**Auth Required:** Yes (ADMIN)

**Response (200):**
```json
{
  "success": true,
  "message": "File deleted successfully"
}
```

---

### 9.5 Get Signed URL
**Method:** `GET`
**URL:** `/api/files/signed-url?blobName={blobName}`
**Auth Required:** Yes (ADMIN)

**Response (200):**
```json
{
  "success": true,
  "signedUrl": "https://storage.googleapis.com/...?X-Goog-Signature=..."
}
```

---

### 9.6 Download File
**Method:** `GET`
**URL:** `/api/files/download/{documentId}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response:** Binary file download

---

### 9.7 Get File Info
**Method:** `GET`
**URL:** `/api/files/info/{documentId}`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "documentId": "660e...",
  "fileName": "document.pdf",
  "fileSize": 1048576,
  "contentType": "application/pdf",
  "uploadedAt": "2026-03-10T10:00:00",
  "uploadedByUserName": "John Doe"
}
```

---

## 10. Analytics (18 endpoints)

### 10.1 Stream All Logs (SSE - Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/stream?token={JWT}`
**Auth Required:** Yes (SUPER_ADMIN)
**Protocol:** Server-Sent Events (SSE)

**Response:** Event Stream
```
event: data-log
data: {"adminId":"...","actionType":"UPLOAD","timestamp":"2026-03-10T10:00:00"}

event: doc-log
data: {"adminId":"...","actionType":"UPLOAD","timestamp":"2026-03-10T10:00:00"}

event: connected
data: {"message":"Connected to analytics stream"}
```

---

### 10.2 Stream My Logs (SSE)
**Method:** `GET`
**URL:** `/api/analytics/stream/me?token={JWT}`
**Auth Required:** Yes (ADMIN)
**Protocol:** Server-Sent Events (SSE)

**Response:** Event Stream (filtered for current user)

---

### 10.3 Connection Stats (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/stream/connections`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "activeConnections": 5,
  "totalConnections": 150
}
```

---

### 10.4 My Upload Stats
**Method:** `GET`
**URL:** `/api/analytics/upload-stats/me`
**Auth Required:** Yes (ADMIN)

**Response (200):**
```json
{
  "success": true,
  "count": 5,
  "data": [
    {
      "id": "660g...",
      "adminId": "admin1",
      "conferenceId": "conf1",
      "dashboardMasterId": "dm1",
      "uploadedAt": "2026-03-10T10:00:00",
      "totalRecordsInFile": 50000,
      "newRecordsAdded": 48500,
      "duplicateRecordsIgnored": 1500,
      "fileName": "data.xlsx"
    }
  ]
}
```

---

### 10.5 My Upload Stats by Conference
**Method:** `GET`
**URL:** `/api/analytics/upload-stats/me/conference/{conferenceId}`
**Auth Required:** Yes (ADMIN)

**Response (200):** Same format as 10.4

---

### 10.6 All Upload Stats (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/upload-stats`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Array of upload stats for all users

---

### 10.7 Upload Stats by Admin (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/upload-stats/admin/{adminId}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Upload stats for specified admin

---

### 10.8 Upload Stats by Conference (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/upload-stats/conference/{conferenceId}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Upload stats for conference

---

### 10.9 My Activity Logs
**Method:** `GET`
**URL:** `/api/analytics/logs/me`
**Auth Required:** Yes (ADMIN)

**Response (200):**
```json
[
  {
    "id": "660h...",
    "adminId": "admin1",
    "actionType": "UPLOAD",
    "conferenceId": "conf1",
    "dashboardMasterId": "dm1",
    "timestamp": "2026-03-10T10:00:00",
    "details": "Uploaded 50000 records"
  }
]
```

---

### 10.10 My Logs by Conference
**Method:** `GET`
**URL:** `/api/analytics/logs/me/conference/{conferenceId}`
**Auth Required:** Yes (ADMIN)

**Response (200):** Activity logs for specified conference

---

### 10.11 All Activity Logs (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/logs`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** All activity logs

---

### 10.12 Logs by Admin (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/logs/admin/{adminId}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Activity logs for specified admin

---

### 10.13 Logs by Conference (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/logs/conference/{conferenceId}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Activity logs for conference

---

### 10.14 My Document Logs
**Method:** `GET`
**URL:** `/api/analytics/doc-logs/me`
**Auth Required:** Yes (ADMIN)

**Response (200):**
```json
[
  {
    "id": "660i...",
    "adminId": "admin1",
    "actionType": "UPLOAD",
    "documentId": "doc1",
    "timestamp": "2026-03-10T10:00:00",
    "fileName": "program-2026.pdf"
  }
]
```

---

### 10.15 My Document Logs by Conference
**Method:** `GET`
**URL:** `/api/analytics/doc-logs/me/conference/{conferenceId}`
**Auth Required:** Yes (ADMIN)

**Response (200):** Document logs for conference

---

### 10.16 All Document Logs (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/doc-logs`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** All document logs

---

### 10.17 Document Logs by Admin (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/doc-logs/admin/{adminId}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Document logs for admin

---

### 10.18 Document Logs by Conference (Super Admin)
**Method:** `GET`
**URL:** `/api/analytics/doc-logs/conference/{conferenceId}`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):** Document logs for conference

---

## 11. Conference Counts (6 endpoints)

### 11.1 Total Conference Count
**Method:** `GET`
**URL:** `/api/conferences/count`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "totalConferences": 12
}
```

---

### 11.2 Dashboard Count for Conference
**Method:** `GET`
**URL:** `/api/conferences/{id}/dashboards/count`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "conferenceId": "conf1",
  "dashboardCount": 5
}
```

---

### 11.3 Document Count for Conference
**Method:** `GET`
**URL:** `/api/conferences/{id}/documents/count`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "conferenceId": "conf1",
  "totalDocuments": 45,
  "documentTypes": {
    "program": 15,
    "book": 20,
    "positive_sheets": 10
  }
}
```

---

### 11.4 Dashboard Data Count for Conference
**Method:** `GET`
**URL:** `/api/conferences/{id}/dashboard-data/count`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "conferenceId": "conf1",
  "totalDataRecords": 100000,
  "dataByDashboard": {
    "Registration Dashboard": 50000,
    "Attendee Dashboard": 50000
  }
}
```

---

### 11.5 Get Conference Count Statistics
**Method:** `GET`
**URL:** `/api/conferences/stats`
**Auth Required:** Yes (SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "totalConferences": 12,
  "activeConferences": 10,
  "inactiveConferences": 2,
  "conferencesByYear": {
    "2024": 3,
    "2025": 5,
    "2026": 4
  }
}
```

---

### 11.6 Get Dashboard Data Count Statistics
**Method:** `GET`
**URL:** `/api/conferences/{id}/dashboard-data/stats`
**Auth Required:** Yes (ADMIN or SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "conferenceId": "conf1",
  "totalRecords": 100000,
  "recordsByStatus": {
    "active": 95000,
    "inactive": 5000
  },
  "recordsByDomain": {
    "gmail.com": 30000,
    "yahoo.com": 20000,
    "outlook.com": 15000,
    "other": 35000
  }
}
```

---

## Authentication Flow

```
1. POST /api/auth/signin → Get JWT + refresh token
2. Use JWT in: Authorization: Bearer <token>
3. When JWT expires (401) → POST /api/auth/refresh-token
4. On logout → POST /api/auth/logout (revokes all tokens for that device)
```

## Role-Based Access Control

| Role | Access Level |
|------|---|
| SUPER_ADMIN | Full access to all endpoints |
| ADMIN | Access only to assigned conferences and own data |
| PUBLIC | No authentication - only signin, forgot password, validate token, reset password, forgot username |

## Pagination

- All list endpoints support pagination: `?page=0&size=500`
- Max page size: 1000 records
- Default page size: 500 records

## Data Formats

- **Dates:** ISO 8601 format (`YYYY-MM-DDTHH:mm:ss`)
- **Query Dates:** `YYYY-MM-DD` format
- **Files:** `multipart/form-data` for uploads
- **Response:** Always includes `success` boolean and optional `message`

## Error Responses

### 401 Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required"
}
```

### 403 Forbidden
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied: You are not assigned to this conference."
}
```

### 400 Bad Request
```json
{
  "success": false,
  "message": "Error message"
}
```

### 500 Internal Server Error
```json
{
  "status": "error",
  "message": "Internal server error",
  "action": "Please try again later"
}
```

---

## Notes

- **Base URL** is provided by deployment (GCP Cloud Run, AWS, etc.)
- All endpoints return JSON unless file download is requested
- File downloads return binary content with appropriate `Content-Type` headers
- SSE streams return chunked `text/event-stream` responses
- All timestamps are in UTC
- Email addresses are normalized (lowercase, trimmed)
- Serial numbers are unique per conference/dashboard combination
- Password fields are never exposed in API responses

---

**Total: 107 Endpoints ✅**

