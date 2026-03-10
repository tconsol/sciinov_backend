# SciInov DBMS — Complete API Reference

> **Base URL:** `https://sciinovdbms-64307221061.asia-south1.run.app`
> **Auth:** Bearer JWT Token (unless marked `PUBLIC`)
> **Content-Type:** `application/json` (unless file upload)

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Users](#2-users)
3. [Conferences](#3-conferences)
4. [Dashboard Masters](#4-dashboard-masters)
5. [Dashboard Data](#5-dashboard-data)
6. [Export (Excel / PDF)](#6-export-excel--pdf)
7. [Conference Documents](#7-conference-documents)
8. [Document Types](#8-document-types)
9. [File Upload (GCS)](#9-file-upload-gcs)
10. [Analytics & Logs](#10-analytics--logs)
11. [Conference Counts](#11-conference-counts)
12. [Error Responses](#12-error-responses)

---

## 1. Authentication

### 1.1 Sign In — `POST /api/auth/signin` (PUBLIC)

Authenticates a user and returns JWT + refresh token.

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

**Usage:** Store `token` for API calls in `Authorization: Bearer <token>`. Store `refreshToken` to refresh expired tokens.

---

### 1.2 Refresh Token — `POST /api/auth/refresh-token` (PUBLIC)

Get a new access token using a valid refresh token.

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

**Usage:** Call when JWT expires (401 response). Replace old token with the new one.

---

### 1.3 Logout — `POST /api/auth/logout` (PUBLIC — works with or without JWT)

Revokes refresh tokens and clears session.

**Request (optional body):**
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

**Usage:** Call on user logout. If `refreshToken` provided, only that token is revoked. If omitted and user is authenticated, all tokens for the user are revoked. Always returns 200.

---

### 1.4 Get Token Expiration — `GET /api/auth/token-expiration`

**Auth Required:** Yes (any role)

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

### 1.5 Forgot Password — `POST /api/auth/forgot-password` (PUBLIC)

Sends a password reset link to the user's registered email.

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

### 1.6 Validate Reset Token — `GET /api/auth/validate-reset-token?token={token}` (PUBLIC)

**Response (200):**
```json
{
  "message": "Token is valid",
  "success": true
}
```

---

### 1.7 Reset Password — `POST /api/auth/reset-password` (PUBLIC)

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

### 1.8 Forgot Username — `POST /api/auth/forgot-username` (PUBLIC)

Sends the username to the registered email.

**Request:**
```json
{
  "email": "user@example.com",
  "phoneNumber": "9876543210"
}
```
> Provide either `email` OR `phoneNumber` (or both).

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

### 1.9 Change Password — `POST /api/auth/change-password`

**Auth Required:** ADMIN or SUPER_ADMIN

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

## 2. Users

> All endpoints require **SUPER_ADMIN** role unless noted.

### 2.1 Get All Admins — `GET /api/users/admins`

**Response (200):** Array of User objects (password field excluded).
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

### 2.2 Get All Super Admins — `GET /api/users/super-admins`

Same response format as 2.1.

### 2.3 Get All Admins with Conferences — `GET /api/users/admins/conferences/all`

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

### 2.4 Get User by ID — `GET /api/users/{id}`

**Response (200):** Single User object.

### 2.5 Create User — `POST /api/users`

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

**Response (200):** Created User object.

### 2.6 Update User — `PUT /api/users/{id}`

Same request body as create. Only provided fields are updated. Password is optional.

### 2.7 Delete User — `DELETE /api/users/{id}`

Soft-deletes the user. Returns 200 with empty body.

### 2.8 Update User Status — `PATCH /api/users/{id}/status`

**Request:**
```json
{
  "status": false
}
```

**Response (200):** Updated User object.

### 2.9 Get Admin's Conferences — `GET /api/users/{adminId}/conferences`

**Response (200):**
```json
["conf1", "conf2"]
```

### 2.10 Assign Conference to Admin — `POST /api/users/{adminId}/conferences/{conferenceId}`

**Response (200):** Updated User object.

### 2.11 Remove Conference from Admin — `DELETE /api/users/{adminId}/conferences/{conferenceId}`

**Response (200):** Updated User object.

---

## 3. Conferences

### 3.1 Get All Conferences — `GET /api/conferences`

**Auth:** SUPER_ADMIN or ADMIN

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

### 3.2 Get My Conferences (Admin) — `GET /api/conferences/me`

**Auth:** ADMIN only. Returns only conferences assigned to the logged-in admin.

### 3.3 Get Conference by ID — `GET /api/conferences/{id}`

**Auth:** SUPER_ADMIN or ADMIN

### 3.4 Create Conference — `POST /api/conferences`

**Auth:** SUPER_ADMIN

**Request:**
```json
{
  "title": "AI Conference 2026",
  "status": "ACTIVE"
}
```

### 3.5 Update Conference — `PUT /api/conferences/{id}`

**Auth:** SUPER_ADMIN

**Request:**
```json
{
  "title": "AI Conference 2026 (Updated)",
  "status": "ACTIVE",
  "dashboardMasterIds": ["dm1", "dm2"]
}
```

### 3.6 Update Conference Status — `PATCH /api/conferences/{id}/status`

**Auth:** SUPER_ADMIN

**Request:**
```json
{
  "status": "INACTIVE"
}
```

### 3.7 Delete Conference — `DELETE /api/conferences/{id}`

**Auth:** SUPER_ADMIN. Soft-deletes.

### 3.8 Get Conference Dashboards — `GET /api/conferences/{id}/dashboards`

**Auth:** SUPER_ADMIN or ADMIN

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
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00",
      "deleted": false
    }
  ],
  "message": "Dashboards retrieved successfully",
  "success": true
}
```

### 3.9 Attach Dashboards — `POST /api/conferences/{id}/dashboards/attach`

**Auth:** SUPER_ADMIN

**Request:**
```json
{
  "dashboardMasterIds": ["dm1", "dm2"]
}
```

### 3.10 Detach Dashboards — `POST /api/conferences/{id}/dashboards/detach`

Same request format as attach.

### 3.11 Set/Replace Dashboards — `PUT /api/conferences/{id}/dashboards`

Replaces all dashboards. Same request format.

### 3.12 Upload Conference Image — `POST /api/conferences/{id}/upload-image`

**Auth:** SUPER_ADMIN or ADMIN | **Content-Type:** `multipart/form-data`

| Field   | Type | Required |
|---------|------|----------|
| `image` | File | Yes      |

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

## 4. Dashboard Masters

### 4.1 Get Dashboard Types — `GET /api/dashboard-masters/types`

**Auth:** ADMIN or SUPER_ADMIN

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
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00",
      "deleted": false
    }
  ]
}
```

### 4.2 Get All Dashboard Masters — `GET /api/dashboard-masters`

### 4.3 Get by ID — `GET /api/dashboard-masters/{id}`

### 4.4 Create — `POST /api/dashboard-masters` (SUPER_ADMIN)

**Request:**
```json
{
  "name": "Attendee Dashboard",
  "status": true
}
```

### 4.5 Update — `PUT /api/dashboard-masters/{id}` (SUPER_ADMIN)

### 4.6 Delete — `DELETE /api/dashboard-masters/{id}` (SUPER_ADMIN)

---

## 5. Dashboard Data

### 5.1 Upload Excel — `POST /api/dashboard-data/upload`

**Auth:** ADMIN | **Content-Type:** `multipart/form-data`

| Field                | Type   | Required |
|----------------------|--------|----------|
| `file`               | File   | Yes      |
| `conferenceId`       | String | Yes      |
| `dashboardMasterId`  | String | Yes      |

Supported formats: `.xlsx`, `.xls`, `.xlsm`

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

**Usage:** Upload any document (PDF, Excel, Word, etc.) for a conference year and document type. Multiple files for the **same conference/year/type are fully supported** — each upload is stored independently without replacing any previous file.

---

### 5.2 Get Dashboard Data — `GET /api/dashboard-data`

**Auth:** ADMIN or SUPER_ADMIN

| Param              | Type   | Required | Default |
|--------------------|--------|----------|---------|
| `conferenceId`     | String | Yes      |         |
| `dashboardMasterId`| String | Yes      |         |
| `fromSerialNo`     | Long   | Yes      |         |
| `toSerialNo`       | Long   | Yes      |         |
| `page`             | int    | No       | 0       |
| `size`             | int    | No       | 500     |

**Response (200):**
```json
{
  "data": [
    {
      "id": "660a...",
      "conferenceId": "conf1",
      "dashboardMasterId": "dm1",
      "serialNo": 1,
      "name": "John Doe",
      "email": "john@gmail.com",
      "emailExtension": "gmail.com",
      "status": true,
      "createdAt": "2026-03-01T10:00:00",
      "updatedAt": "2026-03-01T10:00:00",
      "deleted": false
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

### 5.3 Filter Dashboard Data — `GET /api/dashboard-data/filter`

**Auth:** ADMIN or SUPER_ADMIN

| Param              | Type       | Required |
|--------------------|------------|----------|
| `conferenceId`     | String     | Yes      |
| `dashboardMasterId`| String     | Yes      |
| `fromSerialNo`     | Long       | No       |
| `toSerialNo`       | Long       | No       |
| `startDate`        | YYYY-MM-DD | No       |
| `endDate`          | YYYY-MM-DD | No       |
| `emailDomain`      | String     | No       |
| `page`             | int        | No (0)   |
| `size`             | int        | No (500) |

Same response format as 5.2.

---

### 5.4 Get Data by Date Range — `GET /api/dashboard-data/by-date`

| Param              | Type       | Required |
|--------------------|------------|----------|
| `conferenceId`     | String     | Yes      |
| `dashboardMasterId`| String     | Yes      |
| `startDate`        | YYYY-MM-DD | Yes      |
| `endDate`          | YYYY-MM-DD | Yes      |
| `page`             | int        | No (0)   |
| `size`             | int        | No (500) |

---

### 5.5 Get Data by Domain Extension — `GET /api/dashboard-data/by-domain-extension`

Returns records where email ends with the given TLD (e.g., `com`, `edu`, `org`). Max 1000 records per request.

| Param              | Type   | Required |
|--------------------|--------|----------|
| `conferenceId`     | String | Yes      |
| `dashboardMasterId`| String | Yes      |
| `extension`        | String | Yes      |
| `fromSerialNo`     | Long   | No       |
| `toSerialNo`       | Long   | No       |

**Response (200):**
```json
{
  "data": [ ... ],
  "totalMatchingInRange": 5000,
  "recordsReturned": 1000,
  "maxRecordsPerRequest": 1000,
  "hasMoreRecords": true,
  "requestedRange": "1 - 10000",
  "rangeCoverage": "partial",
  "nextRangeSuggestion": {
    "fromSerialNo": 1001,
    "toSerialNo": 2000,
    "message": "To get the next batch, request from serial 1001 to 2000"
  }
}
```

---

### 5.6 Get Domain Extensions List — `GET /api/dashboard-data/domain-extensions`

Returns distinct TLD extensions (e.g., `com`, `edu`, `org`) for a conference.

| Param              | Type   | Required |
|--------------------|--------|----------|
| `conferenceId`     | String | Yes      |
| `dashboardMasterId`| String | Yes      |

**Response (200):**
```json
{
  "total": 5,
  "extensions": ["com", "edu", "in", "net", "org"]
}
```

---

### 5.7 Get Data by Email Domain — `GET /api/dashboard-data/by-email-domain`

Returns ALL records matching a full email domain (e.g., `gmail.com`). No limit.

| Param              | Type   | Required |
|--------------------|--------|----------|
| `conferenceId`     | String | Yes      |
| `dashboardMasterId`| String | Yes      |
| `emailDomain`      | String | Yes      |

**Response (200):**
```json
{
  "emailDomain": "gmail.com",
  "totalRecords": 12345,
  "data": [ ... ]
}
```

---

### 5.8 Get Filtered Data Count — `GET /api/dashboard-data/count`

Efficient count query (no data loaded).

| Param              | Type       | Required |
|--------------------|------------|----------|
| `conferenceId`     | String     | Yes      |
| `dashboardMasterId`| String     | Yes      |
| `fromSerialNo`     | Long       | No       |
| `toSerialNo`       | Long       | No       |
| `startDate`        | YYYY-MM-DD | No       |
| `endDate`          | YYYY-MM-DD | No       |
| `emailDomain`      | String     | No       |

**Response (200):**
```json
{
  "count": 50000,
  "conferenceId": "conf1",
  "dashboardMasterId": "dm1"
}
```

---

## 6. Export (Excel / PDF)

### 6.1 Export Excel (Basic) — `GET /api/export/excel`

| Param              | Type   | Required |
|--------------------|--------|----------|
| `conferenceId`     | String | Yes      |
| `dashboardMasterId`| String | Yes      |
| `fromSerialNo`     | Long   | Yes      |
| `toSerialNo`       | Long   | Yes      |

**Response:** Binary `.xlsx` file download.

### 6.2 Export PDF (Basic) — `GET /api/export/pdf`

Same params as 6.1. Returns binary `.pdf` file.

### 6.3 Export Excel (Advanced) — `GET /api/export/excel/advanced`

| Param              | Type       | Required |
|--------------------|------------|----------|
| `conferenceId`     | String     | Yes      |
| `dashboardMasterId`| String     | Yes      |
| `fromSerialNo`     | Long       | No       |
| `toSerialNo`       | Long       | No       |
| `startDate`        | YYYY-MM-DD | No       |
| `endDate`          | YYYY-MM-DD | No       |
| `emailDomain`      | String     | No       |

### 6.4 Export PDF (Advanced) — `GET /api/export/pdf/advanced`

Same params as 6.3.

### 6.5 Export Excel with Body Filter — `POST /api/export/excel/filter`

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

### 6.6 Export PDF with Body Filter — `POST /api/export/pdf/filter`

Same request body as 6.5.

### 6.7 Export Excel by Extension — `GET /api/export/excel/by-extension`

| Param              | Type   | Required |
|--------------------|--------|----------|
| `conferenceId`     | String | Yes      |
| `dashboardMasterId`| String | Yes      |
| `extension`        | String | Yes (e.g., `com`) |
| `fromSerialNo`     | Long   | No       |
| `toSerialNo`       | Long   | No       |

### 6.8 Export PDF by Extension — `GET /api/export/pdf/by-extension`

Same params as 6.7.

### 6.9 Preview Export Data — `GET /api/export/preview`

Same params as 6.3. Returns JSON data instead of file download.

### 6.10 Get Email Domains — `GET /api/export/email-domains`

| Param              | Type   | Required |
|--------------------|--------|----------|
| `conferenceId`     | String | Yes      |
| `dashboardMasterId`| String | Yes      |

**Response (200):**
```json
["gmail.com", "yahoo.com", "outlook.com"]
```

---

## 7. Conference Documents

### 7.1 Upload Document — `POST /api/conference-documents/upload`

**Auth:** ADMIN or SUPER_ADMIN | **Content-Type:** `multipart/form-data`

| Field           | Type    | Required | Description                          |
|-----------------|---------|----------|--------------------------------------|
| `conferenceId`  | String  | Yes      | Conference ID                        |
| `year`          | Integer | Yes      | e.g., 2026                           |
| `documentType`  | String  | Yes      | Slug or ID (e.g., `program`, `book`) |
| `file`          | File    | Yes      | Any document file                    |

**Response (200):**
```json
{
  "success": true,
  "message": "Document uploaded successfully.",
  "data": {
    "id": "660a...",
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

### 7.2 Get Conference Documents — `GET /api/conference-documents?conferenceId={id}`

### 7.3 Get Documents by Year — `GET /api/conference-documents/year?conferenceId={id}&year={year}`

### 7.4 Get Documents by Year (All Conferences) — `GET /api/conference-documents/year-all?year={year}` (SUPER_ADMIN)

### 7.5 Get Documents by Type — `GET /api/conference-documents/type?conferenceId={id}&documentType={slug}`

### 7.6 Search Documents — `POST /api/conference-documents/search`

Query params: `conferenceId`, `conferenceName`, `year`, `documentType`, `pageNumber`, `pageSize`

### 7.7 Get All Documents (Super Admin) — `GET /api/conference-documents/admin/all`

Query params: `conferenceId`, `conferenceName`, `year`, `documentType`, `pageNumber`, `pageSize`

### 7.8 Get Document Count — `GET /api/conference-documents/admin/count` (SUPER_ADMIN)

**Response (200):**
```json
{
  "success": true,
  "totalDocumentCount": 150
}
```

### 7.9 Update Document — `PUT /api/conference-documents/admin/{documentId}` (SUPER_ADMIN)

### 7.10 Delete Document — `DELETE /api/conference-documents/admin/{documentId}?deleteType=soft` (SUPER_ADMIN)

`deleteType` can be `soft` (default) or `hard`.

### 7.11 Search All Documents (Super Admin) — `POST /api/conference-documents/admin/search` (SUPER_ADMIN)

**Request:**
```json
{
  "conferenceId": "conf1",
  "year": 2026,
  "documentType": "program"
}
```

### 7.12 Get Available Years — `GET /api/conference-documents/available-years?conferenceId={id}`

**Response (200):**
```json
{
  "success": true,
  "conferenceId": "conf1",
  "availableYears": [2024, 2025, 2026],
  "count": 3
}
```

### 7.13 Admin Documents Dashboard — `GET /api/conference-documents/admin/dashboard`

Returns comprehensive document data for all assigned conferences.

### 7.14 Get Statistics — `GET /api/conference-documents/statistics?conferenceId={id}`

### 7.15 Get Global Statistics — `GET /api/conference-documents/statistics/global` (SUPER_ADMIN)

---

## 8. Document Types

### 8.1 Get Active Types — `GET /api/document-types/active`

**Auth:** ADMIN or SUPER_ADMIN

**Response (200):**
```json
{
  "success": true,
  "total": 3,
  "data": [
    {
      "id": "660a...",
      "slug": "program",
      "displayName": "Program",
      "description": "Conference program document",
      "folderName": "program",
      "sortOrder": 1,
      "active": true,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00",
      "createdByUserId": "660a...",
      "createdByUserName": "Super Admin"
    }
  ]
}
```

### 8.2 Get All Types — `GET /api/document-types` (SUPER_ADMIN)

### 8.3 Get by ID — `GET /api/document-types/{id}` (SUPER_ADMIN)

### 8.4 Create Type — `POST /api/document-types` (SUPER_ADMIN)

**Request:**
```json
{
  "displayName": "Positive Sheets",
  "description": "Attendance positive sheets",
  "sortOrder": 3,
  "active": true
}
```

### 8.5 Update Type — `PUT /api/document-types/{id}` (SUPER_ADMIN)

Same request format. Note: `slug` is not updatable.

### 8.6 Toggle Active — `PATCH /api/document-types/{id}/toggle-active` (SUPER_ADMIN)

### 8.7 Delete Type — `DELETE /api/document-types/{id}` (SUPER_ADMIN)

Soft-deletes. Existing documents using this type are unaffected.

---

## 9. File Upload (GCS)

### 9.1 Upload File — `POST /api/files/upload`

**Content-Type:** `multipart/form-data`

| Field              | Type   | Required |
|--------------------|--------|----------|
| `file`             | File   | Yes      |
| `conferenceId`     | String | Yes      |
| `dashboardMasterId`| String | Yes      |

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

### 9.2 Upload Multiple Files — `POST /api/files/upload-multiple`

| Field              | Type     | Required |
|--------------------|----------|----------|
| `files`            | File[]   | Yes      |
| `conferenceId`     | String   | Yes      |
| `dashboardMasterId`| String   | Yes      |

### 9.3 List Files — `GET /api/files/list?conferenceId={id}&dashboardMasterId={id}`

### 9.4 Delete File — `DELETE /api/files/delete?blobName={blobName}`

### 9.5 Get Signed URL — `GET /api/files/signed-url?blobName={blobName}`

---

## 10. Analytics & Logs

### Real-time SSE Streams

#### 10.1 Stream All Logs (Super Admin) — `GET /api/analytics/stream`

**Auth:** SUPER_ADMIN | **Response:** Server-Sent Events stream

**Usage:**
```javascript
const es = new EventSource('/api/analytics/stream?token=<JWT>');
es.addEventListener('data-log', e => { const log = JSON.parse(e.data); });
es.addEventListener('doc-log', e => { const log = JSON.parse(e.data); });
es.addEventListener('connected', e => console.log('connected'));
```

#### 10.2 Stream My Logs — `GET /api/analytics/stream/me`

#### 10.3 Connection Stats — `GET /api/analytics/stream/connections` (SUPER_ADMIN)

### Upload Stats

#### 10.4 My Upload Stats — `GET /api/analytics/upload-stats/me`

**Response (200):**
```json
{
  "success": true,
  "count": 5,
  "data": [
    {
      "id": "660a...",
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

#### 10.5 My Upload Stats by Conference — `GET /api/analytics/upload-stats/me/conference/{conferenceId}`

#### 10.6 All Upload Stats — `GET /api/analytics/upload-stats` (SUPER_ADMIN)

#### 10.7 Upload Stats by Admin — `GET /api/analytics/upload-stats/admin/{adminId}` (SUPER_ADMIN)

#### 10.8 Upload Stats by Conference — `GET /api/analytics/upload-stats/conference/{conferenceId}` (SUPER_ADMIN)

### Activity Logs

#### 10.9 My Activity Logs — `GET /api/analytics/logs/me`

#### 10.10 My Logs by Conference — `GET /api/analytics/logs/me/conference/{conferenceId}`

#### 10.11 All Activity Logs — `GET /api/analytics/logs` (SUPER_ADMIN)

#### 10.12 Logs by Admin — `GET /api/analytics/logs/admin/{adminId}` (SUPER_ADMIN)

#### 10.13 Logs by Conference — `GET /api/analytics/logs/conference/{conferenceId}` (SUPER_ADMIN)

### Document Logs

#### 10.14 My Document Logs — `GET /api/analytics/doc-logs/me`

#### 10.15 My Document Logs by Conference — `GET /api/analytics/doc-logs/me/conference/{conferenceId}`

#### 10.16 All Document Logs — `GET /api/analytics/doc-logs` (SUPER_ADMIN)

#### 10.17 Document Logs by Admin — `GET /api/analytics/doc-logs/admin/{adminId}` (SUPER_ADMIN)

#### 10.18 Document Logs by Conference — `GET /api/analytics/doc-logs/conference/{conferenceId}` (SUPER_ADMIN)

---

## 11. Conference Counts

### 11.1 Total Conference Count — `GET /api/conferences/count`

**Response (200):**
```json
{
  "success": true,
  "totalConferences": 12
}
```

### 11.2 Dashboard Count for Conference — `GET /api/conferences/{id}/dashboards/count`

**Response (200):**
```json
{
  "success": true,
  "conferenceId": "conf1",
  "dashboardCount": 5
}
```

### 11.3 Document Count for Conference — `GET /api/conferences/{id}/documents/count`

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

### 11.4 Dashboard Data Count for Conference — `GET /api/conferences/{id}/dashboard-data/count`

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

## 12. Error Responses

### 401 Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/users/admins"
}
```

### 403 Forbidden
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied: You are not assigned to this conference.",
  "path": "/api/dashboard-data"
}
```

### 400 Bad Request
```json
{
  "success": false,
  "message": "Error: UserId is already taken!"
}
```

### 500 Internal Server Error
```json
{
  "status": "upload_failed",
  "message": "Upload failed during database insert: ...",
  "action": "No data was saved. Please re-upload the file."
}
```

---

## Authentication Flow

```
1. POST /api/auth/signin → get JWT token + refresh token
2. Use JWT in: Authorization: Bearer <token>
3. When JWT expires (401) → POST /api/auth/refresh-token with refresh token
4. On logout → POST /api/auth/logout (revokes all tokens)
```

## Role-Based Access

| Role          | Access                                           |
|---------------|--------------------------------------------------|
| SUPER_ADMIN   | Full access to all endpoints                     |
| ADMIN         | Access only to assigned conferences and own data |

## Notes

- All dates in ISO 8601 format (`YYYY-MM-DDTHH:mm:ss`)
- Date query params use `YYYY-MM-DD` format
- File uploads use `multipart/form-data`
- Max file size: 100MB
- Pagination: `page` (0-indexed), `size` (max 1000)
- Soft-delete pattern: records have `deleted: true` but are not removed from DB
- Email deduplication is case-insensitive
- SSE streams require `?token=<JWT>` query param (browsers can't set headers for EventSource)

