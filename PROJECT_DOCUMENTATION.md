# 📚 SciInov DBMS — Complete Project Documentation

> **Last Updated:** March 4, 2026
> **Stack:** Spring Boot 3.2.3 · MongoDB · JWT Auth · Apache POI · Caffeine Cache · SSE · Google Cloud Storage
> **Base URL (production):** `https://api.sciinovdbms.com`
> **Base URL (local dev):** `http://localhost:8080`

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Database Collections](#2-database-collections)
3. [Authentication & Security](#3-authentication--security)
4. [API Reference — Auth](#4-api-reference--auth)
5. [API Reference — Users](#5-api-reference--users)
6. [API Reference — Conferences](#6-api-reference--conferences)
7. [API Reference — Dashboard Masters](#7-api-reference--dashboard-masters)
8. [API Reference — Dashboard Data (Upload + View)](#8-api-reference--dashboard-data-upload--view)
9. [API Reference — Export (Excel / PDF)](#9-api-reference--export-excel--pdf)
10. [API Reference — Analytics & Logs](#10-api-reference--analytics--logs)
11. [API Reference — Conference Documents](#11-api-reference--conference-documents)
12. [API Reference — Document Types](#12-api-reference--document-types)
13. [API Reference — File Upload (GCS)](#13-api-reference--file-upload-gcs)
14. [Real-Time SSE Log Streaming](#14-real-time-sse-log-streaming)
15. [Async Upload Flow](#15-async-upload-flow)
16. [Environment Variables Reference](#16-environment-variables-reference)
17. [CORS & Security Hardening](#17-cors--security-hardening)
18. [Performance & Caching](#18-performance--caching)
19. [Production Deployment](#19-production-deployment)
20. [Bugs Fixed (March 2026)](#20-bugs-fixed-march-2026)

---

## 1. Architecture Overview

```
┌────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot 3.2.3                              │
│                                                                        │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────────────┐  │
│  │  Controllers │──▶│   Services   │──▶│    Repositories (MongoDB) │  │
│  └──────────────┘   └──────────────┘   └──────────────────────────┘  │
│         │                  │                                          │
│  ┌──────┴──────┐    ┌──────┴────────────────┐                        │
│  │ JWT Filter  │    │  ExcelService (Async) │                        │
│  │ CORS Filter │    │  @Async("bulkTask-   │                        │
│  │ AuthEntry   │    │   Executor")          │                        │
│  └─────────────┘    └───────────────────────┘                        │
│                                                                        │
│  Cache: Caffeine (userDetails 10m, domainExtensions 5m, count 30s)   │
│  GCS:   Google Cloud Storage (conference images, documents)           │
│  SSE:   Server-Sent Events (real-time admin activity log streaming)   │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

| Concern | Solution |
|---|---|
| Fast dashboard uploads | Async processing + 10 000-row bulk insert batches |
| Duplicate detection | O(1) HashSet in-memory dedup per upload |
| CORS on all responses (including errors) | Highest-precedence `CorsFilter` bean |
| 401/403 strip CORS headers | `AuthEntryPointJwt` + `AccessDeniedHandler` write JSON directly (no `sendError()`) |
| Real-time admin dashboard | SSE emitters per admin + global super-admin channel |
| Serialisation bottleneck | Removed `synchronized` from `getNextSerialNo()` |
| Memory leak in upload tracker | `@Scheduled` cleanup evicts entries older than 2 h |

---

## 2. Database Collections

| Collection | Purpose | Key Indexes |
|---|---|---|
| `users` | Admin & Super-Admin accounts | `userId` (unique), `email` (unique, sparse), `phoneNumber` (unique, sparse) |
| `conferences` | Conference records | `deleted` |
| `dashboard_masters` | Dashboard type definitions | `deleted` |
| `dashboard_data` | Uploaded attendee name+email rows | `(conferenceId, dashboardMasterId, email)` unique · `(…, serialNo)` · `(…, createdAt)` |
| `dashboard_upload_stats` | Upload history per admin | `adminId`, `conferenceId`, `uploadedAt DESC` |
| `admin_activity_logs` | Every admin action (upload/download/view) | `adminId`, `conferenceId`, `createdAt DESC` |
| `conference_documents` | Program/Book/etc files (GCS-backed) | `(conferenceId, year, documentType)` unique |
| `conference_document_logs` | Download/access logs for documents | `conferenceId`, `userId` |
| `document_types` | Dynamic document type catalogue | `slug` (unique) |
| `password_reset_tokens` | Single-use reset tokens | `token` (unique), `userId` |
| `refresh_tokens` | JWT refresh tokens | `token` (unique), `userId` |

---

## 3. Authentication & Security

### JWT Token Lifecycle

```
POST /api/auth/signin
        │
        ▼
  Access Token  (HS256, 24 h)
  Refresh Token (random Base64URL, 7 d)
        │
 [Access expires after 24 h]
        │
        ▼
POST /api/auth/refresh-token   ──▶   New Access Token (24 h)
        │
 [Refresh expires after 7 d]
        │
        ▼
POST /api/auth/signin  (must log in again)
```

### Roles

| Role | Capabilities |
|---|---|
| `SUPER_ADMIN` | Full access: create users, conferences, dashboards, view all logs |
| `ADMIN` | Scoped to assigned conferences only: upload data, export, view own logs |

### Request Headers

```
Authorization: Bearer <access_token>
Content-Type:  application/json          (for JSON endpoints)
Content-Type:  multipart/form-data       (for file upload endpoints)
```

---

## 4. API Reference — Auth

### 4.1 Sign In
```
POST /api/auth/signin
Access: Public
```

**Request Body:**
```json
{
  "userId": "admin123",
  "password": "securePass123"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "MTJkMmQ0ZGRkZDMzNGE0YmEyZWY5...",
  "type": "Bearer",
  "id": "65f1a2b3c4d5e6f7a8b9c0d1",
  "userId": "admin123",
  "email": "admin@example.com",
  "roles": ["ROLE_ADMIN"]
}
```

**Response 401:** Wrong credentials

---

### 4.2 Refresh Access Token
```
POST /api/auth/refresh-token
Access: Public
```

**Request Body:**
```json
{ "refreshToken": "MTJkMmQ0ZGRkZDMzNGE0YmEy..." }
```

**Response 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "MTJkMmQ0ZGRkZDMzNGE0YmEy...",
  "type": "Bearer"
}
```

**Response 401:**
```json
{ "success": false, "message": "Invalid or expired refresh token" }
```

---

### 4.3 Logout
```
POST /api/auth/logout
Access: ADMIN, SUPER_ADMIN
Authorization: Bearer <token>
```

**Request Body (optional):**
```json
{ "refreshToken": "MTJkMmQ0ZGRkZDMzNGE0YmEy..." }
```

**Response 200:**
```json
{ "message": "Logged out successfully", "success": true }
```

---

### 4.4 Forgot Password
```
POST /api/auth/forgot-password
Access: Public
```

**Request Body:**
```json
{ "userId": "admin123" }
```

**Response 200:**
```json
{
  "success": true,
  "message": "Password reset email sent",
  "email": "a***@example.com"
}
```

---

### 4.5 Validate Reset Token
```
GET /api/auth/validate-reset-token?token=<token>
Access: Public
```

**Response 200:** `{ "success": true, "message": "Token is valid" }`
**Response 400:** `{ "success": false, "message": "Invalid or expired token" }`

---

### 4.6 Reset Password
```
POST /api/auth/reset-password
Access: Public
```

**Request Body:**
```json
{
  "token": "abc123resettoken",
  "newPassword": "NewSecure@123",
  "confirmPassword": "NewSecure@123"
}
```

**Response 200:** `{ "success": true, "message": "Password reset successful" }`

---

### 4.7 Forgot Username
```
POST /api/auth/forgot-username
Access: Public
```

**Request Body:**
```json
{ "email": "admin@example.com" }
```
or
```json
{ "phoneNumber": "+919876543210" }
```

**Response 200:** `{ "success": true, "message": "Username sent to email", "email": "a***@example.com" }`

---

### 4.8 Change Password
```
POST /api/auth/change-password
Access: ADMIN, SUPER_ADMIN
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "currentPassword": "OldPass123",
  "newPassword": "NewPass@456",
  "confirmPassword": "NewPass@456"
}
```

**Response 200:** `{ "success": true, "message": "Password changed successfully" }`

---

### 4.9 Get Token Expiration Info
```
GET /api/auth/token-expiration
Access: Public
```

**Response 200:**
```json
{
  "success": true,
  "tokenExpirationMs": 86400000,
  "tokenExpirationMinutes": 1440,
  "tokenExpirationHours": 24
}
```

---

## 5. API Reference — Users

### 5.1 Get All Admins
```
GET /api/users/admins
Access: SUPER_ADMIN
```

**Response 200:** Array of `User` objects (passwords excluded)

---

### 5.2 Get All Admins With Conferences
```
GET /api/users/admins/conferences/all
Access: SUPER_ADMIN
```

**Response 200:**
```json
[
  {
    "id": "65f1a2b3...",
    "firstName": "John",
    "lastName": "Doe",
    "userId": "jdoe",
    "email": "jdoe@example.com",
    "role": "ADMIN",
    "status": true,
    "conferences": [
      { "id": "conf123", "title": "IEEE 2025", "status": "ACTIVE" }
    ]
  }
]
```

---

### 5.3 Get All Super Admins
```
GET /api/users/super-admins
Access: SUPER_ADMIN
```

**Response 200:** Array of `User` objects

---

### 5.4 Get User By ID
```
GET /api/users/{id}
Access: SUPER_ADMIN
```

**Response 200:** Single `User` object
**Response 404:** User not found

---

### 5.5 Create User
```
POST /api/users
Access: SUPER_ADMIN
```

**Request Body:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "userId": "jsmith",
  "email": "jsmith@example.com",
  "phoneNumber": "+919876543210",
  "password": "Initial@123",
  "role": "ADMIN",
  "status": true
}
```

**Response 200:** Created `User` object (password hashed, not returned)

---

### 5.6 Update User
```
PUT /api/users/{id}
Access: SUPER_ADMIN
```

**Request Body:** Same as create (partial updates supported)
**Response 200:** Updated `User` object

---

### 5.7 Delete User (Soft Delete)
```
DELETE /api/users/{id}
Access: SUPER_ADMIN
```

**Response 200:** Empty body with 200 status

---

### 5.8 Update User Status
```
PATCH /api/users/{id}/status
Access: SUPER_ADMIN
```

**Request Body:**
```json
{ "status": false }
```

**Response 200:** Updated `User` object

---

### 5.9 Get Admin Conferences
```
GET /api/users/{adminId}/conferences
Access: SUPER_ADMIN
```

**Response 200:**
```json
["conf123", "conf456"]
```

---

### 5.10 Assign Conference to Admin
```
POST /api/users/{adminId}/conferences/{conferenceId}
Access: SUPER_ADMIN
```

**Response 200:** Updated `User` object with new conferenceIds

---

### 5.11 Remove Conference from Admin
```
DELETE /api/users/{adminId}/conferences/{conferenceId}
Access: SUPER_ADMIN
```

**Response 200:** Updated `User` object

---

## 6. API Reference — Conferences

### 6.1 Get All Conferences
```
GET /api/conferences
Access: SUPER_ADMIN, ADMIN
```

**Response 200:**
```json
[
  {
    "id": "conf123",
    "title": "IEEE Conference 2025",
    "imageUrl": "https://storage.googleapis.com/...",
    "imageBlobName": "conferences/conf123/image.jpg",
    "status": "ACTIVE",
    "dashboardMasterIds": ["dm1", "dm2"],
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-06-20T14:00:00",
    "deleted": false
  }
]
```

---

### 6.2 Get My Conferences (Admin Only)
```
GET /api/conferences/me
Access: ADMIN
```

**Response 200:** Array of conferences assigned to the logged-in admin

---

### 6.3 Get Conference By ID
```
GET /api/conferences/{id}
Access: SUPER_ADMIN, ADMIN
```

**Response 200:** Single `Conference` object
**Response 404:** Conference not found

---

### 6.4 Create Conference
```
POST /api/conferences
Access: SUPER_ADMIN
```

**Request Body:**
```json
{
  "title": "IEEE Conference 2026",
  "imageUrl": "https://storage.googleapis.com/...",
  "status": "ACTIVE"
}
```

**Response 200:** Created `Conference` object

---

### 6.5 Update Conference
```
PUT /api/conferences/{id}
Access: SUPER_ADMIN
```

**Request Body:** Same as create
**Response 200:** Updated `Conference` object

---

### 6.6 Update Conference Status
```
PATCH /api/conferences/{id}/status
Access: SUPER_ADMIN
```

**Request Body:**
```json
{ "status": "INACTIVE" }
```

**Response 200:** Updated `Conference` object

---

### 6.7 Delete Conference (Soft Delete)
```
DELETE /api/conferences/{id}
Access: SUPER_ADMIN
```

**Response 200:** Empty body

---

### 6.8 Get Dashboards for Conference
```
GET /api/conferences/{id}/dashboards
Access: SUPER_ADMIN, ADMIN
```

**Response 200:**
```json
{
  "conferenceId": "conf123",
  "conferenceTitle": "IEEE 2025",
  "dashboards": [
    { "id": "dm1", "name": "Main Dashboard", "status": true }
  ],
  "message": "Dashboards retrieved successfully",
  "success": true
}
```

---

### 6.9 Attach Dashboards to Conference
```
POST /api/conferences/{id}/dashboards/attach
Access: SUPER_ADMIN
```

**Request Body:**
```json
{ "dashboardMasterIds": ["dm1", "dm2", "dm3"] }
```

**Response 200:** Same structure as 6.8

---

### 6.10 Detach Dashboards from Conference
```
POST /api/conferences/{id}/dashboards/detach
Access: SUPER_ADMIN
```

**Request Body:**
```json
{ "dashboardMasterIds": ["dm2"] }
```

**Response 200:** Same structure as 6.8

---

### 6.11 Get Total Conference Count
```
GET /api/conferences/count
Access: SUPER_ADMIN, ADMIN
```

**Response 200:**
```json
{ "success": true, "totalConferences": 12 }
```

---

### 6.12 Get Dashboard Count for Conference
```
GET /api/conferences/{id}/dashboards/count
Access: SUPER_ADMIN, ADMIN
```

**Response 200:**
```json
{ "success": true, "conferenceId": "conf123", "dashboardCount": 5 }
```

---

## 7. API Reference — Dashboard Masters

Dashboard Masters are the types of dashboards (e.g., "Positive Scan", "Negative Scan", "Registration").

### 7.1 Get All Dashboard Masters
```
GET /api/dashboard-masters
Access: SUPER_ADMIN, ADMIN
```

**Response 200:**
```json
[
  { "id": "dm1", "name": "Positive Scan", "status": true, "createdAt": "...", "updatedAt": "..." }
]
```

---

### 7.2 Get Dashboard Types
```
GET /api/dashboard-masters/types
Access: SUPER_ADMIN, ADMIN
```

**Response 200:**
```json
{
  "success": true,
  "total": 3,
  "data": [ { "id": "dm1", "name": "Positive Scan", "status": true } ]
}
```

---

### 7.3 Get Dashboard Master By ID
```
GET /api/dashboard-masters/{id}
Access: SUPER_ADMIN, ADMIN
```

**Response 200:** Single `DashboardMaster` object
**Response 404:** Not found

---

### 7.4 Create Dashboard Master
```
POST /api/dashboard-masters
Access: SUPER_ADMIN
```

**Request Body:**
```json
{ "name": "Registration", "status": true }
```

**Response 200:** Created object

---

### 7.5 Update Dashboard Master
```
PUT /api/dashboard-masters/{id}
Access: SUPER_ADMIN
```

**Request Body:** Same as create
**Response 200:** Updated object

---

### 7.6 Delete Dashboard Master
```
DELETE /api/dashboard-masters/{id}
Access: SUPER_ADMIN
```

**Response 200:** Empty body

---

## 8. API Reference — Dashboard Data (Upload + View)

### 8.1 Upload Excel File ⚡
```
POST /api/dashboard-data/upload
Access: ADMIN only
Content-Type: multipart/form-data
```

> ⚡ **Returns immediately** (async processing). Data is inserted in background.
> Use the returned `uploadId` to poll progress via endpoint 8.2.

**Request (multipart/form-data):**

| Field | Type | Description |
|---|---|---|
| `file` | File | Excel file (.xlsx, .xls, .xlsm). Must have `Name` and `Email` columns |
| `conferenceId` | String | ID of the target conference |
| `dashboardMasterId` | String | ID of the target dashboard master |

**Excel File Format:**

```
Row 1 (Header): | Name      | Email              |
Row 2+  (Data): | John Doe  | john@example.com   |
```

- Column order doesn't matter — detected by header name
- `Email` column is mandatory; `Name` column is optional
- Duplicate emails (within file or already in DB) are automatically skipped
- Supports `.xlsx` (Office Open XML), `.xls` (Legacy), `.xlsm` (Macro-enabled)

**Response 200 (immediate — upload started):**
```json
{
  "status": "PROCESSING",
  "message": "Upload started! Data will be stored immediately...",
  "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "fileName": "attendees.xlsx",
  "hint": "Use uploadId to check progress. Data is being inserted to database in background."
}
```

**Response 400:** Unsupported file format
**Response 403:** Admin not assigned to this conference

---

### 8.2 Check Upload Progress
```
GET /api/dashboard-data/upload/progress/{uploadId}
Access: ADMIN
```

**Response 200 (in progress):**
```json
{
  "uploadId": "a1b2c3d4-...",
  "fileName": "attendees.xlsx",
  "status": "PROCESSING",
  "startTime": "2026-03-04T10:00:00",
  "recordsProcessed": 0,
  "recordsInserted": 0,
  "message": "Upload in progress..."
}
```

**Response 200 (completed):**
```json
{
  "uploadId": "a1b2c3d4-...",
  "fileName": "attendees.xlsx",
  "status": "COMPLETED",
  "message": "Upload completed successfully!",
  "completionTime": "2026-03-04T10:02:35",
  "result": {
    "status": "success",
    "totalRecordsInFile": 50000,
    "newRecordsAdded": 48250,
    "duplicateRecordsIgnored": 1700,
    "invalidRowsSkipped": 50,
    "processingTimeMs": 155000,
    "batchesInserted": 5
  }
}
```

**Response 200 (failed):**
```json
{
  "status": "FAILED",
  "message": "Upload failed: ...",
  "error": "..."
}
```

**Response 200 (not found):**
```json
{ "status": "NOT_FOUND", "message": "Upload ID not found or expired" }
```

---

### 8.3 Get Dashboard Data (By Serial Range, Paginated)
```
GET /api/dashboard-data
    ?conferenceId={id}
    &dashboardMasterId={id}
    &fromSerialNo={number}
    &toSerialNo={number}
    &page={0}
    &size={500}         (max 1000)
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{
  "data": [
    {
      "id": "65f1...",
      "conferenceId": "conf123",
      "dashboardMasterId": "dm1",
      "serialNo": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "status": true,
      "createdAt": "2026-01-15T10:30:00",
      "updatedAt": "2026-01-15T10:30:00",
      "deleted": false
    }
  ],
  "currentPage": 0,
  "pageSize": 500,
  "totalRecords": 48250,
  "totalPages": 97,
  "fromSerialNo": 1,
  "toSerialNo": 1000
}
```

> 💡 `totalRecords` and `totalPages` are only returned on `page=0` to avoid extra count query on every page turn.

---

### 8.4 Get Dashboard Data (Advanced Filter, Paginated)
```
GET /api/dashboard-data/filter
    ?conferenceId={id}
    &dashboardMasterId={id}
    &fromSerialNo={n}        (optional)
    &toSerialNo={n}          (optional)
    &startDate={YYYY-MM-DD}  (optional)
    &endDate={YYYY-MM-DD}    (optional)
    &emailDomain={domain}    (optional, e.g., gmail.com)
    &page={0}
    &size={500}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:** Same structure as 8.3

---

### 8.5 Get Data By Date Range
```
GET /api/dashboard-data/by-date
    ?conferenceId={id}
    &dashboardMasterId={id}
    &startDate={YYYY-MM-DD}
    &endDate={YYYY-MM-DD}
    &page={0}
    &size={500}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:** Same structure as 8.3

---

### 8.6 Get Data By Domain Extension (TLD Filter)
```
GET /api/dashboard-data/by-domain-extension
    ?conferenceId={id}
    &dashboardMasterId={id}
    &extension={tld}          (e.g., com, edu, org — or .com, .edu)
    &fromSerialNo={n}         (optional)
    &toSerialNo={n}           (optional)
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{
  "data": [...],
  "requestedRange": { "from": 1, "to": 10000 },
  "totalMatchingInRange": 2500,
  "recordsReturned": 1000,
  "maxRecordsPerRequest": 1000,
  "hasMoreRecords": true,
  "nextRangeSuggestion": {
    "fromSerialNo": 1001,
    "toSerialNo": 11000,
    "message": "Search from serial 1001 to 11000 to get next batch"
  },
  "rangeCoverage": "partial"
}
```

---

### 8.7 Get Distinct Domain Extensions
```
GET /api/dashboard-data/domain-extensions
    ?conferenceId={id}
    &dashboardMasterId={id}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{ "total": 4, "extensions": ["com", "edu", "org", "in"] }
```

> Cached for 5 minutes (Caffeine).

---

### 8.8 Get Data By Full Email Domain
```
GET /api/dashboard-data/by-email-domain
    ?conferenceId={id}
    &dashboardMasterId={id}
    &emailDomain=gmail.com
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{
  "emailDomain": "gmail.com",
  "totalRecords": 3200,
  "data": [...]
}
```

---

### 8.9 Get Filtered Record Count
```
GET /api/dashboard-data/count
    ?conferenceId={id}
    &dashboardMasterId={id}
    &fromSerialNo={n}       (optional)
    &toSerialNo={n}         (optional)
    &startDate={date}       (optional)
    &endDate={date}         (optional)
    &emailDomain={domain}   (optional)
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{
  "count": 48250,
  "conferenceId": "conf123",
  "dashboardMasterId": "dm1"
}
```

---

## 9. API Reference — Export (Excel / PDF)

All export endpoints stream the file directly. Set `Content-Disposition: attachment` for download.

### 9.1 Export to Excel (Serial Range)
```
GET /api/export/excel
    ?conferenceId={id}
    &dashboardMasterId={id}
    &fromSerialNo={n}
    &toSerialNo={n}
Access: ADMIN, SUPER_ADMIN
```

**Response:** Binary `.xlsx` file download
**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

---

### 9.2 Export to PDF (Serial Range)
```
GET /api/export/pdf
    ?conferenceId={id}
    &dashboardMasterId={id}
    &fromSerialNo={n}
    &toSerialNo={n}
Access: ADMIN, SUPER_ADMIN
```

**Response:** Binary `.pdf` file download
**Content-Type:** `application/pdf`

---

### 9.3 Advanced Excel Export (Filters)
```
GET /api/export/excel/advanced
    ?conferenceId={id}
    &dashboardMasterId={id}
    &fromSerialNo={n}       (optional)
    &toSerialNo={n}         (optional)
    &startDate={date}       (optional)
    &endDate={date}         (optional)
    &emailDomain={domain}   (optional)
Access: ADMIN, SUPER_ADMIN
```

**Response:** Binary `.xlsx` file download

---

### 9.4 Advanced PDF Export (Filters)
```
GET /api/export/pdf/advanced
    (same params as 9.3)
Access: ADMIN, SUPER_ADMIN
```

**Response:** Binary `.pdf` file download

---

### 9.5 Export Excel (POST with filter body)
```
POST /api/export/excel/filter
Access: ADMIN, SUPER_ADMIN
```

**Request Body:**
```json
{
  "conferenceId": "conf123",
  "dashboardMasterId": "dm1",
  "fromSerialNo": 1,
  "toSerialNo": 5000,
  "startDate": "2025-01-01",
  "endDate": "2025-12-31",
  "emailDomain": "gmail.com"
}
```

**Response:** Binary `.xlsx` file download

---

### 9.6 Export PDF (POST with filter body)
```
POST /api/export/pdf/filter
Access: ADMIN, SUPER_ADMIN
```

**Request Body:** Same as 9.5
**Response:** Binary `.pdf` file download

---

### 9.7 Export Excel By TLD Extension
```
GET /api/export/excel/by-extension
    ?conferenceId={id}
    &dashboardMasterId={id}
    &extension=com
    &fromSerialNo={n}   (optional)
    &toSerialNo={n}     (optional)
Access: ADMIN, SUPER_ADMIN
```

**Response:** Binary `.xlsx` file (max 1000 records per request)

---

### 9.8 Export PDF By TLD Extension
```
GET /api/export/pdf/by-extension
    ?conferenceId={id}
    &dashboardMasterId={id}
    &extension=edu
    &fromSerialNo={n}   (optional)
    &toSerialNo={n}     (optional)
Access: ADMIN, SUPER_ADMIN
```

**Response:** Binary `.pdf` file (max 1000 records per request)

---

### 9.9 Preview Filtered Data
```
GET /api/export/preview
    (same params as 9.3)
Access: ADMIN, SUPER_ADMIN
```

**Response 200:** Array of `DashboardData` objects (JSON, no file download)

---

### 9.10 Get Distinct Email Domains
```
GET /api/export/email-domains
    ?conferenceId={id}
    &dashboardMasterId={id}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
["gmail.com", "yahoo.com", "outlook.com", "hotmail.com"]
```

---

## 10. API Reference — Analytics & Logs

### 10.1 Get My Upload Stats
```
GET /api/analytics/upload-stats/me
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{
  "success": true,
  "count": 12,
  "data": [
    {
      "id": "stat123",
      "adminId": "65f1...",
      "conferenceId": "conf123",
      "dashboardMasterId": "dm1",
      "fileName": "attendees.xlsx",
      "totalRecordsInFile": 5000,
      "newRecordsAdded": 4800,
      "duplicateRecordsIgnored": 200,
      "uploadedAt": "2026-03-04T10:00:00"
    }
  ]
}
```

---

### 10.2 Get My Upload Stats For Conference
```
GET /api/analytics/upload-stats/me/conference/{conferenceId}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:** Same structure as 10.1

---

### 10.3 Get My Activity Logs
```
GET /api/analytics/logs/me
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{
  "success": true,
  "count": 25,
  "data": [
    {
      "id": "log123",
      "adminId": "65f1...",
      "adminName": "John Doe",
      "conferenceId": "conf123",
      "dashboardMasterId": "dm1",
      "actionType": "UPLOAD_EXCEL",
      "description": "Uploaded Excel: attendees.xlsx | Added: 4800 | Duplicates: 200",
      "ipAddress": "127.0.0.1",
      "createdAt": "2026-03-04T10:02:35"
    }
  ]
}
```

**Action Types:**
- `UPLOAD_EXCEL` — Dashboard data file upload
- `DOWNLOAD_EXCEL` — Excel export
- `DOWNLOAD_PDF` — PDF export
- `VIEW` — Data view
- `UPLOAD_FILE` — Conference document upload
- `DOWNLOAD_FILE` — Conference document download
- `DELETE_FILE` — Conference document delete
- `VIEW_TLD_FILTER` — TLD domain filter view
- `DOWNLOAD_EXCEL_TLD` — TLD-filtered Excel download
- `DOWNLOAD_PDF_TLD` — TLD-filtered PDF download

---

### 10.4 Get My Logs For Conference
```
GET /api/analytics/logs/me/conference/{conferenceId}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:** Same structure as 10.3

---

### 10.5 Get All Upload Stats (Super Admin)
```
GET /api/analytics/upload-stats
Access: SUPER_ADMIN
```

**Response 200:** Same structure as 10.1 but with all admins' data

---

### 10.6 Get Upload Stats By Admin (Super Admin)
```
GET /api/analytics/upload-stats/admin/{adminId}
Access: SUPER_ADMIN
```

**Response 200:** Filtered by adminId

---

### 10.7 Get Upload Stats By Conference (Super Admin)
```
GET /api/analytics/upload-stats/conference/{conferenceId}
Access: SUPER_ADMIN
```

**Response 200:** Filtered by conferenceId

---

### 10.8 Get All Activity Logs (Super Admin)
```
GET /api/analytics/logs
Access: SUPER_ADMIN
```

**Response 200:** All activity logs across all admins

---

### 10.9 Get Logs By Admin (Super Admin)
```
GET /api/analytics/logs/admin/{adminId}
Access: SUPER_ADMIN
```

**Response 200:** Logs for specific admin

---

### 10.10 Get Logs By Conference (Super Admin)
```
GET /api/analytics/logs/conference/{conferenceId}
Access: SUPER_ADMIN
```

**Response 200:** Logs for specific conference

---

### 10.11 Get SSE Connection Stats
```
GET /api/analytics/stream/connections
Access: SUPER_ADMIN
```

**Response 200:**
```json
{ "superAdminConnections": 2, "adminConnections": { "adminId1": 1 } }
```

---

## 11. API Reference — Conference Documents

### 11.1 Upload Conference Document
```
POST /api/conference-documents/upload
Access: ADMIN, SUPER_ADMIN
Content-Type: multipart/form-data
```

**Request (multipart/form-data):**

| Field | Type | Description |
|---|---|---|
| `conferenceId` | String | Conference ID |
| `year` | Integer | Year (2000 – current+10) |
| `documentType` | String | Type slug (e.g., `program`) or type ID |
| `file` | File | Document file (any format) |

**Response 200:**
```json
{
  "success": true,
  "message": "Document uploaded successfully. If a previous version existed, it has been replaced.",
  "data": {
    "id": "doc123",
    "conferenceId": "conf123",
    "conferenceName": "IEEE 2025",
    "year": 2025,
    "documentType": "program",
    "documentTypeDisplayName": "Program",
    "fileName": "program.xlsx",
    "publicUrl": "https://storage.googleapis.com/...",
    "fileSize": 204800,
    "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "uploadedAt": "2026-03-04T10:00:00",
    "uploadedByUserName": "John Doe"
  }
}
```

> If a document of the same `(conferenceId, year, documentType)` already exists, it is **replaced**.

---

### 11.2 Get All Documents
```
GET /api/conference-documents
    ?conferenceId={id}      (optional)
    &year={year}            (optional)
    &documentType={slug}    (optional)
    &page={0}
    &size={20}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{
  "success": true,
  "data": [ { ...document objects... } ],
  "totalElements": 45,
  "totalPages": 3,
  "currentPage": 0
}
```

---

### 11.3 Get Document By ID
```
GET /api/conference-documents/{id}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:** Single `ConferenceDocumentResponse` object

---

### 11.4 Download Document
```
GET /api/conference-documents/{id}/download
Access: ADMIN, SUPER_ADMIN
```

**Response:** Binary file stream download
**Headers:** `Content-Disposition: attachment; filename="program.xlsx"`

---

### 11.5 Delete Document
```
DELETE /api/conference-documents/{id}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{ "success": true, "message": "Document deleted successfully" }
```

---

### 11.6 Get Document Logs
```
GET /api/conference-documents/{id}/logs
Access: SUPER_ADMIN
```

**Response 200:** Array of `ConferenceDocumentLog` objects

---

### 11.7 Get All Document Logs For Conference
```
GET /api/conference-documents/logs/conference/{conferenceId}
Access: SUPER_ADMIN
```

**Response 200:** Array of logs

---

## 12. API Reference — Document Types

Document types define the catalogue of document categories (Program, Book, Positive Sheets, etc.). Managed dynamically by SUPER_ADMIN.

### 12.1 Get Active Document Types
```
GET /api/document-types/active
Access: ADMIN, SUPER_ADMIN
```

**Response 200:**
```json
{
  "success": true,
  "total": 3,
  "data": [
    { "id": "dt1", "name": "Program", "slug": "program", "active": true },
    { "id": "dt2", "name": "Book", "slug": "book", "active": true },
    { "id": "dt3", "name": "Positive Sheets", "slug": "positive_sheets", "active": true }
  ]
}
```

---

### 12.2 Get All Document Types (including inactive)
```
GET /api/document-types
Access: SUPER_ADMIN
```

**Response 200:** Same structure including `active: false` entries

---

### 12.3 Get Document Type By ID
```
GET /api/document-types/{id}
Access: SUPER_ADMIN
```

**Response 200:** Single `DocumentTypeResponse` object

---

### 12.4 Create Document Type
```
POST /api/document-types
Access: SUPER_ADMIN
```

**Request Body:**
```json
{ "name": "Registration", "slug": "registration", "active": true }
```

**Response 201:** Created `DocumentTypeResponse`

---

### 12.5 Update Document Type
```
PUT /api/document-types/{id}
Access: SUPER_ADMIN
```

**Request Body:** Same as create
**Response 200:** Updated object

---

### 12.6 Toggle Document Type Status
```
PATCH /api/document-types/{id}/toggle
Access: SUPER_ADMIN
```

**Response 200:**
```json
{ "success": true, "message": "Document type deactivated", "data": {...} }
```

---

### 12.7 Delete Document Type
```
DELETE /api/document-types/{id}
Access: SUPER_ADMIN
```

**Response 200:** `{ "success": true, "message": "Document type deleted" }`

---

## 13. API Reference — File Upload (GCS)

General-purpose file storage on Google Cloud Storage (conference images, etc.).

### 13.1 Upload Single File
```
POST /api/files/upload
Access: ADMIN, SUPER_ADMIN
Content-Type: multipart/form-data
```

**Request (multipart/form-data):**

| Field | Type |
|---|---|
| `file` | File |
| `conferenceId` | String |
| `dashboardMasterId` | String |

**Response 200:**
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "fileUrl": "https://storage.googleapis.com/bucket/...",
  "fileName": "image.jpg",
  "fileSize": 204800,
  "contentType": "image/jpeg"
}
```

---

### 13.2 Upload Multiple Files
```
POST /api/files/upload-multiple
Access: ADMIN, SUPER_ADMIN
Content-Type: multipart/form-data
```

**Request:** `files[]` (multiple), `conferenceId`, `dashboardMasterId`

**Response 200:**
```json
{
  "success": true,
  "totalFiles": 3,
  "uploadedFiles": [
    { "fileName": "...", "fileUrl": "...", "success": true }
  ],
  "successCount": 3,
  "failCount": 0
}
```

---

### 13.3 List Files
```
GET /api/files
    ?conferenceId={id}
    &dashboardMasterId={id}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:** Array of file objects with URLs

---

### 13.4 Download File
```
GET /api/files/download
    ?blobName={blob_path}
Access: ADMIN, SUPER_ADMIN
```

**Response:** Binary file stream

---

### 13.5 Delete File
```
DELETE /api/files
    ?blobName={blob_path}
Access: ADMIN, SUPER_ADMIN
```

**Response 200:** `{ "success": true, "message": "File deleted" }`

---

## 14. Real-Time SSE Log Streaming

> ⚠️ The browser's native `EventSource` cannot send `Authorization` headers.
> Pass the JWT token as a **query parameter**: `?token=<JWT>`

### 14.1 Super Admin — All Logs Stream
```
GET /api/analytics/stream?token=<JWT_TOKEN>
Access: SUPER_ADMIN
Produces: text/event-stream
```

**Events:**

| Event Name | Data | Description |
|---|---|---|
| `connected` | `{ "message": "Connected...", "role": "SUPER_ADMIN" }` | Initial handshake |
| `data-log` | `AdminActivityLog` JSON | Every admin upload/download/view |
| `doc-log` | `ConferenceDocumentLog` JSON | Every document upload/download/delete |

**Frontend Example:**
```javascript
const es = new EventSource(`/api/analytics/stream?token=${accessToken}`);
es.addEventListener('connected', e => console.log('Connected'));
es.addEventListener('data-log', e => {
  const log = JSON.parse(e.data);
  console.log(`${log.adminName} performed ${log.actionType}`);
});
es.addEventListener('doc-log', e => {
  const log = JSON.parse(e.data);
  console.log(`Doc event: ${log.action}`);
});
```

---

### 14.2 Admin — Own Logs Stream
```
GET /api/analytics/stream/me?token=<JWT_TOKEN>
Access: ADMIN, SUPER_ADMIN
Produces: text/event-stream
```

**Events:** Same as above but filtered to the logged-in admin's own actions.

---

## 15. Async Upload Flow

```
Frontend                     Backend                      MongoDB
   │                            │                            │
   │── POST /api/dashboard-data/upload ──▶│                  │
   │                            │                            │
   │                            │── Read file bytes          │
   │                            │── Init uploadProgress map  │
   │                            │── Start @Async("bulk-      │
   │                            │    TaskExecutor") thread   │
   │                            │                            │
   │◀── 200 { uploadId, status: PROCESSING } ──│            │
   │                            │                            │
   │  (every 2-3 seconds)       │── Load existing emails ──▶│
   │── GET /upload/progress/{id}│    (ONE query, projection) │
   │◀── { status: PROCESSING }  │                            │
   │                            │── Parse Excel rows         │
   │                            │── In-memory HashSet dedup  │
   │                            │── Bulk insert batches ────▶│
   │                            │    (10,000 rows/batch)     │
   │                            │                            │
   │                            │── saveStats() ────────────▶│
   │                            │── logUploadActivity() ─────▶│
   │                            │── pushLog (SSE) ──▶ Admins │
   │                            │                            │
   │── GET /upload/progress/{id}│                            │
   │◀── { status: COMPLETED, result: {...} }                 │
```

**Key points:**
- HTTP response returns in < 100 ms regardless of file size
- No browser timeout on large uploads
- If upload **fails**, an activity log is still created with `FAILED:` prefix
- Progress map entries cleaned up after 2 hours by `@Scheduled` task

---

## 16. Environment Variables Reference

All values are loaded from the `.env` file in the project root.

```properties
# ─── Server ───────────────────────────────────────────────
SERVER_PORT=8080
TOMCAT_MAX_THREADS=800
TOMCAT_MIN_THREADS=50
TOMCAT_ACCEPT_COUNT=500
TOMCAT_MAX_CONNECTIONS=16384

# ─── MongoDB ──────────────────────────────────────────────
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/sciinovdbms
MONGODB_MAX_POOL_SIZE=100
MONGODB_MIN_POOL_SIZE=10
MONGODB_MAX_IDLE_TIME=30000
MONGODB_MAX_LIFE_TIME=0
MONGODB_SOCKET_TIMEOUT=300000
MONGODB_SERVER_TIMEOUT=10000

# ─── JWT ──────────────────────────────────────────────────
JWT_SECRET=<generate_secure_256_bit_base64_key>
JWT_EXPIRATION_MS=86400000          # 24 hours
JWT_REFRESH_EXPIRATION_DAYS=7       # 7 days

# ─── File Upload ──────────────────────────────────────────
MAX_FILE_SIZE=200MB
MAX_REQUEST_SIZE=200MB

# ─── SMTP Mail ────────────────────────────────────────────
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@sciinovdbms.com
MAIL_PASSWORD=<app_password>
MAIL_FROM=noreply@sciinovdbms.com
MAIL_FROM_NAME=SciInov DBMS
MAIL_TIMEOUT=10000

# ─── Google Cloud Storage ─────────────────────────────────
GCS_PROJECT_ID=your-gcp-project-id
GCS_BUCKET_NAME=sciinov-prod
GCS_TYPE=service_account
GCS_PRIVATE_KEY_ID=<key_id>
GCS_PRIVATE_KEY=<private_key>
GCS_CLIENT_EMAIL=<service_account_email>
GCS_CLIENT_ID=<client_id>
GCS_AUTH_URI=https://accounts.google.com/o/oauth2/auth
GCS_TOKEN_URI=https://oauth2.googleapis.com/token
GCS_AUTH_PROVIDER_X509_CERT_URL=https://www.googleapis.com/oauth2/v1/certs
GCS_CLIENT_X509_CERT_URL=<cert_url>
GCS_UNIVERSE_DOMAIN=googleapis.com

# ─── App URLs ─────────────────────────────────────────────
APP_BASE_URL=https://api.sciinovdbms.com
APP_FRONTEND_BASE_URL=https://sciinovdbms.com
APP_PASSWORD_RESET_URL=https://sciinovdbms.com/reset-password
APP_FORGOT_PASSWORD_URL=https://sciinovdbms.com/forgot-password
APP_CORS_ALLOWED_ORIGINS=https://sciinovdbms.com,https://www.sciinovdbms.com

# ─── Swagger ──────────────────────────────────────────────
SWAGGER_UI_ENABLED=false            # Always false in production!

# ─── Logging ──────────────────────────────────────────────
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
LOG_LEVEL_CONTROLLER=INFO
LOG_LEVEL_SERVICE=INFO
LOG_LEVEL_SECURITY=WARN
LOG_LEVEL_SPRING=WARN
LOG_LEVEL_SPRING_SEC=WARN
LOG_LEVEL_MONGO=WARN
LOG_FILE_PATH=logs/application.log
LOG_MAX_SIZE=50MB
LOG_MAX_HISTORY=30

# ─── Session ──────────────────────────────────────────────
SESSION_MAX_AGE=86400
SESSION_SECURE=true

# ─── Async Task Pool ──────────────────────────────────────
TASK_CORE_SIZE=8
TASK_MAX_SIZE=16
TASK_QUEUE_SIZE=200
```

---

## 17. CORS & Security Hardening

### Why Intermittent CORS Errors Were Happening

CORS was **intermittent** (not always) because:

1. **Before the fix:** `@Async` without an executor name fell back to Spring's `SimpleAsyncTaskExecutor` (unbounded threads). Under load this exhausted server resources → random 500 errors → error responses were sometimes dispatched through Tomcat's error pipeline **which stripped CORS headers**.

2. **Before the fix:** 403 Forbidden responses used Spring Security's default `AccessDeniedHandler` which calls `sendError()` → again strips CORS headers from the response.

### Fixes Applied (March 2026)

| Fix | File | Change |
|---|---|---|
| Bind async to bounded pool | `ExcelService.java` | `@Async` → `@Async("bulkTaskExecutor")` |
| 403 keeps CORS headers | `WebSecurityConfig.java` | Added custom `AccessDeniedHandler` (writes JSON directly, no `sendError()`) |
| 401 keeps CORS headers | `AuthEntryPointJwt.java` | Already correct (writes JSON directly) |
| Preflight cache extended | `WebSecurityConfig.java` | `maxAge` 3600 → 86400 (24h) |
| Refresh token public | `WebSecurityConfig.java` | Added `/api/auth/refresh-token` to public list |
| Upload concurrency | `ExcelService.java` | Removed `synchronized` from `getNextSerialNo()` |
| Memory leak fix | `ExcelService.java` | Added `@Scheduled` cleanup for uploadProgress map |
| Scheduling enabled | `SciInovDbmsApplication.java` | Added `@EnableScheduling` |

### CORS Configuration Summary

```
Allowed Origins:   from APP_CORS_ALLOWED_ORIGINS env var
Allowed Methods:   GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD
Allowed Headers:   * (all)
Exposed Headers:   Authorization, X-Correlation-ID, X-Total-Count, X-Page-Number, Content-Disposition
Allow Credentials: true
Max Age:           86400 seconds (24 hours preflight cache)
Filter Order:      HIGHEST_PRECEDENCE (applies before security filter)
```

---

## 18. Performance & Caching

### Caffeine Cache Configuration

| Cache Name | TTL | Max Entries | Purpose |
|---|---|---|---|
| `userDetails` | 10 min | 500 | User auth objects — eliminates DB hit on every API request |
| `domainExtensions` | 5 min | 200 | TLD list per conference |
| Default | 5 min | 1000 | All other caches |

### Upload Performance

| Mechanism | Impact |
|---|---|
| Async processing | HTTP returns in <100ms for any file size |
| Batch insert (10 000 rows/batch) | ~5x faster than row-by-row insert |
| HashSet in-memory dedup | O(1) duplicate check instead of O(n) DB queries |
| Single email-load query with projection | One DB round-trip instead of n queries |
| Removed `synchronized` lock | Concurrent uploads no longer serialised |

### BCrypt Strength

Password hashing strength changed from **12 → 10** (default).
Strength 12 adds ~300 ms per login under load. Strength 10 is ~100 ms and still very secure.

### Thread Pool (AsyncConfig)

```
Core pool:    8 threads
Max pool:    16 threads
Queue:      200 tasks
Thread name: bulk-upload-*
Keep alive:  60 seconds
```

---

## 19. Production Deployment

### Docker

```bash
docker build -t sciinov-backend:latest .
docker run -d \
  --env-file .env \
  -p 8080:8080 \
  --name sciinov-backend \
  sciinov-backend:latest
```

### Docker Compose

```bash
docker-compose up -d
```

### Production Checklist

- [ ] Set `JWT_SECRET` to a randomly generated 256-bit Base64-encoded key
- [ ] Set `SWAGGER_UI_ENABLED=false`
- [ ] Set `LOG_LEVEL_ROOT=WARN`, `LOG_LEVEL_SPRING_SEC=WARN`
- [ ] Set `APP_CORS_ALLOWED_ORIGINS` to production frontend URL only
- [ ] Set `SESSION_SECURE=true`
- [ ] Ensure MongoDB is using a secured cluster with auth
- [ ] GCS credentials are in place and bucket is private (signed URLs used for downloads)
- [ ] Tomcat thread counts sized for your server CPU
- [ ] MongoDB connection pool sized for expected concurrency

---

## 20. Bugs Fixed (March 2026)

### Bug 1: Intermittent CORS Error on Upload
**Symptom:** Same upload endpoint sometimes works, sometimes returns CORS error.
**Root Cause:** `@Async` without executor name → unbounded `SimpleAsyncTaskExecutor` → resource exhaustion → 500 → error response stripped CORS headers.
**Fix:** Changed to `@Async("bulkTaskExecutor")` which uses the bounded `ThreadPoolTaskExecutor`.

### Bug 2: Admin Logs Missing After Failed Upload
**Symptom:** Data partially saves to MongoDB, but no entry appears in the admin dashboard logs.
**Root Cause:** `logUploadActivity()` was only called on **successful** completion of `processBulkUpload()`. If an exception was thrown, the `catch` block only updated the progress map — it never wrote an `AdminActivityLog`.
**Fix:** Added `logUploadActivity()` call in the `catch` block with `"FAILED: ..."` note. An activity log is now **always** created regardless of success or failure.

### Bug 3: Concurrent Upload Serialisation
**Symptom:** Slow upload throughput when multiple admins upload simultaneously.
**Root Cause:** `getNextSerialNo()` was declared `synchronized`, meaning every concurrent upload waited for it serially.
**Fix:** Removed `synchronized` keyword. The serial number is read once at the start of each upload and incremented in-memory from there — no cross-upload lock needed.

### Bug 4: uploadProgress Memory Leak
**Symptom:** In-memory `ConcurrentHashMap` grows unbounded in long-running production deployments.
**Root Cause:** Completed/failed entries were never evicted.
**Fix:** Added `@Scheduled(fixedDelay=30min)` cleanup task that removes entries older than 2 hours.

### Bug 5: 403 Responses Strip CORS Headers
**Symptom:** When an admin tries to access a restricted endpoint, browser reports CORS error instead of 403 Forbidden.
**Root Cause:** Spring Security's default `AccessDeniedHandler` calls `sendError()` which dispatches through Tomcat's error pipeline and strips CORS headers.
**Fix:** Added custom `AccessDeniedHandler` bean that writes JSON directly to the response output stream (same pattern as existing `AuthEntryPointJwt`).

### Bug 6: Preflight Requests Too Frequent
**Symptom:** Browser sends OPTIONS preflight before nearly every API call.
**Root Cause:** `maxAge` was set to 3600 seconds (1 hour).
**Fix:** Extended to 86400 seconds (24 hours). Browser caches preflight result for 24 hours.

---

## Error Response Format

All error responses follow a consistent JSON format:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/dashboard-data/upload"
}
```

| HTTP Status | Meaning |
|---|---|
| 200 | Success |
| 400 | Bad request (validation error, wrong file format, etc.) |
| 401 | Unauthorized — missing or expired JWT token |
| 403 | Forbidden — authenticated but insufficient role or conference access |
| 404 | Resource not found |
| 500 | Internal server error |

---

## Common Patterns

### Frontend Token Refresh Pattern

```javascript
async function apiCall(endpoint, options = {}) {
  const token = localStorage.getItem('accessToken');
  let response = await fetch(endpoint, {
    ...options,
    headers: { ...options.headers, 'Authorization': `Bearer ${token}` }
  });

  if (response.status === 401) {
    // Token expired — refresh it
    const refreshResp = await fetch('/api/auth/refresh-token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: localStorage.getItem('refreshToken') })
    });

    if (refreshResp.ok) {
      const data = await refreshResp.json();
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      // Retry original request
      return apiCall(endpoint, options);
    } else {
      // Refresh token also expired — force login
      window.location.href = '/login';
    }
  }

  return response;
}
```

### Excel Upload + Progress Polling Pattern

```javascript
async function uploadAndPoll(file, conferenceId, dashboardMasterId) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('conferenceId', conferenceId);
  formData.append('dashboardMasterId', dashboardMasterId);

  // 1. Start upload (returns immediately)
  const uploadResp = await apiCall('/api/dashboard-data/upload', {
    method: 'POST', body: formData
  });
  const { uploadId } = await uploadResp.json();

  // 2. Poll progress every 3 seconds
  const interval = setInterval(async () => {
    const progResp = await apiCall(`/api/dashboard-data/upload/progress/${uploadId}`);
    const progress = await progResp.json();

    console.log('Status:', progress.status);

    if (progress.status === 'COMPLETED') {
      clearInterval(interval);
      console.log('Done!', progress.result);
    } else if (progress.status === 'FAILED') {
      clearInterval(interval);
      console.error('Upload failed:', progress.error);
    }
  }, 3000);
}
```

---

*Documentation generated March 4, 2026. Reflects all code changes including the March 2026 bug fixes.*

