# SciInov DBMS — Complete API Reference

> **Base URL:** `https://sciinov-643072216061.asia-south1.run.app`  
> **Local Dev:** `http://localhost:8080`  
> **All protected routes require:** `Authorization: Bearer <JWT_TOKEN>`

---

## 📋 Table of Contents

1. [Authentication Flow](#1-authentication)
2. [User Management (SUPER_ADMIN)](#2-user-management)
3. [Conference Management](#3-conference-management)
4. [Dashboard Master Management](#4-dashboard-master)
5. [Dashboard Data (Excel Upload & View)](#5-dashboard-data)
6. [Export (Excel / PDF Download)](#6-export)
7. [Document Types (SUPER_ADMIN)](#7-document-types)
8. [Conference Documents (Upload / Download / Delete)](#8-conference-documents)
9. [Analytics & Logs](#9-analytics--logs)
10. [Role Summary & Access Matrix](#10-role-summary)

---

## 1. Authentication

> No token required for these endpoints.

---

### 1.1 Login
```
POST /api/auth/signin
```
**Body:**
```json
{
  "userId": "admin01",
  "password": "yourpassword"
}
```
**Response `200`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "id": "65f1a2b3c4d5e6f7a8b9c0d1",
  "username": "admin01",
  "email": "admin01@example.com",
  "roles": ["ROLE_ADMIN"]
}
```
**Flow:** Store `token` in localStorage. Send it as `Authorization: Bearer <token>` in every subsequent request. Roles will be either `["ROLE_ADMIN"]` or `["ROLE_SUPER_ADMIN"]`.

---

### 1.2 Forgot Password
```
POST /api/auth/forgot-password
```
**Body:**
```json
{ "userId": "admin01" }
```
**Response `200`:**
```json
{
  "success": true,
  "message": "Password reset link sent to your email",
  "email": "ad***@example.com"
}
```

---

### 1.3 Validate Reset Token
```
GET /api/auth/validate-reset-token?token=<TOKEN>
```
**Response `200`:**
```json
{ "success": true, "message": "Token is valid" }
```
**Response `400`:**
```json
{ "success": false, "message": "Token is expired or invalid" }
```

---

### 1.4 Reset Password
```
POST /api/auth/reset-password
```
**Body:**
```json
{
  "token": "abc123resettoken",
  "newPassword": "NewPassword@123"
}
```
**Response `200`:**
```json
{ "success": true, "message": "Password reset successfully" }
```

---

### 1.5 Forgot Username
```
POST /api/auth/forgot-username
```
**Body:**
```json
{ "email": "admin01@example.com" }
```
**Response `200`:**
```json
{
  "success": true,
  "message": "Username sent to your email",
  "email": "ad***@example.com"
}
```

---

### 1.6 Change Password (Logged-in User)
```
POST /api/auth/change-password
```
🔒 **Requires:** `ADMIN` or `SUPER_ADMIN`

**Body:**
```json
{
  "currentPassword": "OldPassword@123",
  "newPassword": "NewPassword@456"
}
```
**Response `200`:**
```json
{ "success": true, "message": "Password changed successfully" }
```

---

## 2. User Management

🔒 **All endpoints require:** `SUPER_ADMIN`

---

### 2.1 Get All Admins
```
GET /api/users/admins
```
**Response `200`:**
```json
[
  {
    "id": "65f1a2b3c4d5e6f7a8b9c0d1",
    "firstName": "John",
    "lastName": "Doe",
    "userId": "admin01",
    "email": "john@example.com",
    "phoneNumber": "9876543210",
    "role": "ADMIN",
    "status": true,
    "conferenceIds": ["conf001", "conf002"],
    "createdAt": "2026-01-15T10:00:00",
    "updatedAt": "2026-02-01T12:00:00",
    "deleted": false
  }
]
```

---

### 2.2 Get All Admins With Assigned Conferences
```
GET /api/users/admins/conferences/all
```
**Response `200`:**
```json
[
  {
    "id": "65f1a2b3c4d5e6f7a8b9c0d1",
    "firstName": "John",
    "lastName": "Doe",
    "userId": "admin01",
    "email": "john@example.com",
    "role": "ADMIN",
    "status": true,
    "conferences": [
      { "id": "conf001", "title": "ICSE 2026", "status": "ACTIVE" }
    ]
  }
]
```

---

### 2.3 Get User by ID
```
GET /api/users/{id}
```
**Response `200`:** Same as single user object above.  
**Response `404`:** Not found.

---

### 2.4 Create Admin User
```
POST /api/users
```
**Body:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "userId": "admin02",
  "email": "jane@example.com",
  "phoneNumber": "9876543211",
  "password": "TempPass@123",
  "role": "ADMIN",
  "status": true,
  "conferenceIds": []
}
```
**Response `200`:** Created user object (same structure as get).

> ⚠️ `role` must be `"ADMIN"` — only one SUPER_ADMIN exists (seeded at startup).

---

### 2.5 Update Admin User
```
PUT /api/users/{id}
```
**Body:** Same as create (all fields optional except those being updated).

**Response `200`:** Updated user object.

---

### 2.6 Delete Admin User
```
DELETE /api/users/{id}
```
**Response `200`:** Empty body.

---

### 2.7 Update User Status (Enable/Disable)
```
PATCH /api/users/{id}/status
```
**Body:**
```json
{ "status": false }
```
**Response `200`:** Updated user object.

---

### 2.8 Get Admin's Assigned Conference IDs
```
GET /api/users/{adminId}/conferences
```
**Response `200`:**
```json
["conf001", "conf002"]
```

---

### 2.9 Assign Conference to Admin
```
POST /api/users/{adminId}/conferences/{conferenceId}
```
**No body required.**  
**Response `200`:** Updated user object with new conferenceIds list.

---

### 2.10 Remove Conference from Admin
```
DELETE /api/users/{adminId}/conferences/{conferenceId}
```
**Response `200`:** Updated user object.

---

## 3. Conference Management

---

### 3.1 Get All Conferences
```
GET /api/conferences
```
🔒 `SUPER_ADMIN` or `ADMIN`

**Response `200`:**
```json
[
  {
    "id": "conf001",
    "title": "ICSE 2026",
    "imageUrl": "https://storage.googleapis.com/sciinovfiles/conferences/icse-2026/cover.jpg",
    "imageBlobName": "conferences/icse-2026/cover.jpg",
    "status": "ACTIVE",
    "dashboardMasterIds": ["dash001", "dash002"],
    "createdAt": "2026-01-01T10:00:00",
    "updatedAt": "2026-02-10T09:30:00",
    "deleted": false
  }
]
```

---

### 3.2 Get My Conferences (Admin)
```
GET /api/conferences/me
```
🔒 `ADMIN`

Returns only conferences assigned to the logged-in admin.  
**Response `200`:** Array of conference objects (same as above).

---

### 3.3 Get Conference by ID
```
GET /api/conferences/{id}
```
🔒 `SUPER_ADMIN` or `ADMIN`

**Response `200`:** Single conference object.  
**Response `404`:** Not found.

---

### 3.4 Create Conference
```
POST /api/conferences
```
🔒 `SUPER_ADMIN`

**Body:**
```json
{
  "title": "ICSE 2026",
  "status": "ACTIVE"
}
```
**Response `200`:** Created conference object.

---

### 3.5 Update Conference
```
PUT /api/conferences/{id}
```
🔒 `SUPER_ADMIN`

**Body:**
```json
{
  "title": "ICSE 2026 Updated",
  "status": "INACTIVE"
}
```
**Response `200`:** Updated conference object.

---

### 3.6 Update Conference Status Only
```
PATCH /api/conferences/{id}/status
```
🔒 `SUPER_ADMIN`

**Description:** Update only the status of a conference. All conference data is returned in the response.

**Body:**
```json
{
  "status": "ACTIVE"
}
```
**Valid Status Values:**
- `ACTIVE`
- `INACTIVE`

**Response `200`:**
```json
{
  "id": "conf001",
  "title": "ICSE 2026",
  "imageUrl": "https://storage.googleapis.com/sciinovfiles/conferences/icse-2026/cover.jpg",
  "imageBlobName": "conferences/icse-2026/cover.jpg",
  "status": "ACTIVE",
  "dashboardMasterIds": ["dash001", "dash002"],
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-02-23T14:30:00",
  "deleted": false
}
```
**Response `404`:**
```json
{
  "timestamp": "2026-02-23T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Conference not found"
}
```

**Example Request:**
```bash
curl -X PATCH 'http://localhost:8080/api/conferences/conf001/status' \
  -H 'Authorization: Bearer <JWT_TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "INACTIVE"
  }'
```

---

### 3.7 Delete Conference
```
DELETE /api/conferences/{id}
```
🔒 `SUPER_ADMIN`

**Response `200`:** Empty body.

---

### 3.8 Upload / Replace Conference Image
```
POST /api/conferences/{id}/upload-image
```
🔒 `SUPER_ADMIN` or `ADMIN`

**Body:** `multipart/form-data`
| Field | Type | Required |
|-------|------|----------|
| `image` | file | ✅ |

> ✅ If a previous image exists, it is **auto-deleted** from GCS and replaced.  
> ✅ Accepts: JPEG, PNG, WebP, GIF.

**Response `200`:**
```json
{
  "success": true,
  "message": "Conference image uploaded successfully",
  "conferenceId": "conf001",
  "conferenceName": "ICSE 2026",
  "imageUrl": "https://storage.googleapis.com/sciinovfiles/conferences/icse-2026/cover.jpg",
  "imageBlobName": "conferences/icse-2026/cover.jpg"
}
```

---

### 3.9 Get Conference's Dashboards
```
GET /api/conferences/{id}/dashboards
```
🔒 `SUPER_ADMIN` or `ADMIN`

**Response `200`:**
```json
{
  "conferenceId": "conf001",
  "conferenceName": "ICSE 2026",
  "dashboards": [
    { "id": "dash001", "name": "Registrations", "status": true }
  ],
  "message": "Dashboards retrieved successfully",
  "success": true
}
```

---

### 3.10 Attach Dashboards to Conference
```
POST /api/conferences/{id}/dashboards/attach
```
🔒 `SUPER_ADMIN`

**Body:**
```json
{ "dashboardMasterIds": ["dash001", "dash002"] }
```
**Response `200`:** Same as get dashboards response.

---
### 3.11 Detach Dashboards from Conference
```
POST /api/conferences/{id}/dashboards/detach
```
🔒 `SUPER_ADMIN`

**Body:**
```json
{ "dashboardMasterIds": ["dash002"] }
```
**Response `200`:** Updated dashboards response.

---

### 3.12 Set / Replace All Dashboards
```
PUT /api/conferences/{id}/dashboards
```
🔒 `SUPER_ADMIN`

**Body:**
```json
{ "dashboardMasterIds": ["dash001"] }
```
Replaces ALL attached dashboards with the provided list.  
**Response `200`:** Updated dashboards response.

---

## 4. Dashboard Master

🔒 `SUPER_ADMIN` (manage) | `ADMIN` or `SUPER_ADMIN` (read)

---

### 4.1 Get All Dashboard Masters
```
GET /api/dashboard-masters
```
**Response `200`:**
```json
[
  {
    "id": "dash001",
    "name": "Registrations",
    "status": true,
    "createdAt": "2026-01-10T08:00:00",
    "updatedAt": "2026-01-15T10:00:00",
    "deleted": false
  }
]
```

---

### 4.2 Get Dashboard Master by ID
```
GET /api/dashboard-masters/{id}
```

---

### 4.3 Create Dashboard Master
```
POST /api/dashboard-masters
```
🔒 `SUPER_ADMIN`

**Body:**
```json
{ "name": "Registrations", "status": true }
```
**Response `200`:** Created dashboard master object.

---

### 4.4 Update Dashboard Master
```
PUT /api/dashboard-masters/{id}
```
🔒 `SUPER_ADMIN`

**Body:**
```json
{ "name": "Updated Name", "status": true }
```

---

### 4.5 Delete Dashboard Master
```
DELETE /api/dashboard-masters/{id}
```
🔒 `SUPER_ADMIN`

---

## 5. Dashboard Data

🔒 `ADMIN` (upload) | `ADMIN` or `SUPER_ADMIN` (read)

---

### 5.1 Upload Excel File (Bulk, Production-Optimised)
```
POST /api/dashboard-data/upload
```
🔒 `ADMIN`

**Body:** `multipart/form-data`
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `file` | `.xlsx` file | ✅ | Column A = Name, Column B = Email |
| `conferenceId` | string | ✅ | Must be assigned to this admin |
| `dashboardMasterId` | string | ✅ | |

> ✅ **Bulk optimised** — handles 1 lakh rows in seconds using:
> - Single email-set query for deduplication
> - Batch insert (500 records/batch)
> - Pre-allocated serial numbers

**Response `200`:**
```json
{
  "status": "success",
  "message": "File uploaded successfully",
  "fileName": "registrations.xlsx",
  "totalRecordsInFile": 100000,
  "newRecordsAdded": 97543,
  "duplicateRecordsIgnored": 2457,
  "processingTimeMs": 8432
}
```

---

### 5.2 View Data by Serial Number Range (Paginated)
```
GET /api/dashboard-data?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=100&page=0&size=500
```

| Param | Required | Default | Notes |
|-------|----------|---------|-------|
| `conferenceId` | ✅ | — | |
| `dashboardMasterId` | ✅ | — | |
| `fromSerialNo` | ✅ | — | Inclusive |
| `toSerialNo` | ✅ | — | Inclusive |
| `page` | ❌ | `0` | |
| `size` | ❌ | `500` | Max `1000` |

**Response `200`:**
```json
{
  "data": [
    {
      "id": "...",
      "serialNo": 1,
      "name": "Alice Johnson",
      "email": "alice@gmail.com",
      "conferenceId": "conf001",
      "dashboardMasterId": "dash001",
      "status": true,
      "createdAt": "2026-02-01T10:00:00"
    }
  ],
  "currentPage": 0,
  "pageSize": 500,
  "totalRecords": 97543,
  "totalPages": 196,
  "fromSerialNo": 1,
  "toSerialNo": 100
}
```
> ℹ️ A VIEW log is automatically created when data is fetched.

---

### 5.3 Advanced Filter (Serial + Date + Email Domain, Paginated)
```
GET /api/dashboard-data/filter
```
| Param | Required | Notes |
|-------|----------|-------|
| `conferenceId` | ✅ | |
| `dashboardMasterId` | ✅ | |
| `fromSerialNo` | ❌ | |
| `toSerialNo` | ❌ | |
| `startDate` | ❌ | `YYYY-MM-DD` |
| `endDate` | ❌ | `YYYY-MM-DD` |
| `emailDomain` | ❌ | e.g. `gmail.com` |
| `page` | ❌ | Default `0` |
| `size` | ❌ | Default `500`, Max `1000` |

**Response `200`:** Same paginated structure as 5.2.

---

### 5.4 Filter by Date Range
```
GET /api/dashboard-data/by-date?conferenceId=&dashboardMasterId=&startDate=2026-01-01&endDate=2026-01-31&page=0&size=500
```
**Response `200`:** Same paginated structure.

---

### 5.5 Filter by Email Domain
```
GET /api/dashboard-data/by-email-domain?conferenceId=&dashboardMasterId=&emailDomain=gmail.com&page=0&size=500
```
**Response `200`:** Same paginated structure.

---

### 5.6 Count Records (No data fetch — fast)
```
GET /api/dashboard-data/count?conferenceId=&dashboardMasterId=&fromSerialNo=1&toSerialNo=500
```
All filter params are optional.

**Response `200`:**
```json
{
  "count": 97543,
  "conferenceId": "conf001",
  "dashboardMasterId": "dash001"
}
```

---

## 6. Export

🔒 `ADMIN` or `SUPER_ADMIN`

> All export endpoints return a **file download** (binary). Handle with `blob` in frontend.

---

### 6.1 Export Excel (Serial Range)
```
GET /api/export/excel?conferenceId=&dashboardMasterId=&fromSerialNo=1&toSerialNo=100
```
**Response:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
File name: `dashboard-data.xlsx`

---

### 6.2 Export PDF (Serial Range)
```
GET /api/export/pdf?conferenceId=&dashboardMasterId=&fromSerialNo=1&toSerialNo=100
```
**Response:** `application/pdf`

---

### 6.3 Advanced Excel Export (All Filters)
```
GET /api/export/excel/advanced?conferenceId=&dashboardMasterId=&fromSerialNo=1&toSerialNo=1000&startDate=2026-01-01&endDate=2026-01-31&emailDomain=gmail.com
```
**Response:** Excel file download.

---

### 6.4 Advanced PDF Export (All Filters)
```
GET /api/export/pdf/advanced?conferenceId=&dashboardMasterId=&fromSerialNo=1&toSerialNo=1000&startDate=2026-01-01&endDate=2026-01-31&emailDomain=gmail.com
```
**Response:** PDF file download.

---

### 6.5 Export Excel via POST Body
```
POST /api/export/excel/filter
```
**Body:**
```json
{
  "conferenceId": "conf001",
  "dashboardMasterId": "dash001",
  "fromSerialNo": 1,
  "toSerialNo": 1000,
  "startDate": "2026-01-01",
  "endDate": "2026-01-31",
  "emailDomain": "gmail.com"
}
```
**Response:** Excel file download.

---

### 6.6 Export PDF via POST Body
```
POST /api/export/pdf/filter
```
**Body:** Same as 6.5.  
**Response:** PDF file download.

---

### 6.7 Preview Data Before Export
```
GET /api/export/preview?conferenceId=&dashboardMasterId=&fromSerialNo=1&toSerialNo=50&emailDomain=gmail.com
```
**Response `200`:** Array of `DashboardData` objects (no pagination — preview only).

---

### 6.8 Get Distinct Email Domains (for filter dropdown)
```
GET /api/export/email-domains?conferenceId=conf001&dashboardMasterId=dash001
```
**Response `200`:**
```json
["gmail.com", "yahoo.com", "hotmail.com", "iitm.ac.in"]
```

---

## 7. Document Types

> SUPER_ADMIN manages dynamic document types (replaces hardcoded Program/Book/Positive Sheets enum).

---

### 7.1 Get Active Types (Used by Admin during upload)
```
GET /api/document-types/active
```
🔒 `ADMIN` or `SUPER_ADMIN`

**Response `200`:**
```json
{
  "success": true,
  "total": 3,
  "data": [
    {
      "id": "dt001",
      "slug": "program",
      "displayName": "Program",
      "description": "Conference program / schedule document",
      "folderName": "program",
      "sortOrder": 1,
      "active": true,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00"
    },
    {
      "id": "dt002",
      "slug": "book",
      "displayName": "Book",
      "description": "Conference book / proceedings document",
      "folderName": "book",
      "sortOrder": 2,
      "active": true
    },
    {
      "id": "dt003",
      "slug": "positive_sheets",
      "displayName": "Positive Sheets",
      "description": "Positive sheets / attendance confirmation sheets",
      "folderName": "positive-sheets",
      "sortOrder": 3,
      "active": true
    }
  ]
}
```

---

### 7.2 Get All Types Including Inactive (SUPER_ADMIN)
```
GET /api/document-types
```
🔒 `SUPER_ADMIN`

**Response `200`:** Same structure, includes inactive types.

---

### 7.3 Get Type by ID
```
GET /api/document-types/{id}
```
🔒 `SUPER_ADMIN`

**Response `200`:**
```json
{ "success": true, "data": { /* DocumentTypeResponse */ } }
```

---

### 7.4 Create New Document Type
```
POST /api/document-types
```
🔒 `SUPER_ADMIN`

**Body:**
```json
{
  "displayName": "Abstract Book",
  "description": "Abstract submissions compiled into one book",
  "sortOrder": 4,
  "active": true
}
```
**Response `201`:**
```json
{
  "success": true,
  "message": "Document type created successfully",
  "data": {
    "id": "dt004",
    "slug": "abstract_book",
    "displayName": "Abstract Book",
    "description": "Abstract submissions compiled into one book",
    "folderName": "abstract-book",
    "sortOrder": 4,
    "active": true,
    "createdAt": "2026-02-20T10:00:00",
    "updatedAt": "2026-02-20T10:00:00"
  }
}
```
> ℹ️ `slug` and `folderName` are auto-generated from `displayName`. You cannot set them manually.

**Error `400`:** `{ "success": false, "message": "A document type with name 'Abstract Book' already exists." }`

---

### 7.5 Update Document Type
```
PUT /api/document-types/{id}
```
🔒 `SUPER_ADMIN`

**Body:**
```json
{
  "displayName": "Abstract Book (Updated)",
  "description": "New description",
  "sortOrder": 5,
  "active": true
}
```
> ⚠️ `slug` is **NOT updatable** — it's the key used in existing documents.

**Response `200`:**
```json
{
  "success": true,
  "message": "Document type updated successfully",
  "data": { /* updated DocumentTypeResponse */ }
}
```

---

### 7.6 Toggle Active / Inactive
```
PATCH /api/document-types/{id}/toggle-active
```
🔒 `SUPER_ADMIN`

**No body.**

**Response `200`:**
```json
{
  "success": true,
  "message": "Document type deactivated successfully",
  "data": { /* updated DocumentTypeResponse with active: false */ }
}
```

---

### 7.7 Delete Document Type (Soft Delete)
```
DELETE /api/document-types/{id}
```
🔒 `SUPER_ADMIN`

**Response `200`:**
```json
{
  "success": true,
  "message": "Document type deleted successfully. Existing documents are unaffected."
}
```

---

## 8. Conference Documents

🔒 `ADMIN` or `SUPER_ADMIN` (upload/download/delete) | Authenticated users (view)

---

### 8.1 Upload Document
```
POST /api/conference-documents/upload
```
🔒 `ADMIN` or `SUPER_ADMIN`

**Body:** `multipart/form-data`
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `conferenceId` | string | ✅ | |
| `year` | integer | ✅ | e.g. `2026` |
| `documentType` | string | ✅ | Use `slug` from `/api/document-types/active` e.g. `"program"` |
| `file` | file | ✅ | PDF, DOCX, XLSX, PPTX, ZIP etc. |

> ✅ If a document already exists for same `conferenceId + year + documentType`, it is **auto-deleted** from GCS and replaced.

**Response `200`:**
```json
{
  "success": true,
  "message": "Document uploaded successfully. If a previous version existed, it has been replaced.",
  "data": {
    "id": "doc001",
    "conferenceId": "conf001",
    "conferenceName": "ICSE 2026",
    "year": 2026,
    "documentType": "program",
    "documentTypeDisplayName": "Program",
    "fileName": "icse-2026-program.pdf",
    "blobName": "conferences/icse-2026/2026/program/icse-2026-program.pdf",
    "filePath": "conferences/icse-2026/2026/program/icse-2026-program.pdf",
    "publicUrl": "https://storage.googleapis.com/...",
    "fileSize": 2048576,
    "contentType": "application/pdf",
    "uploadedAt": "2026-02-20T10:00:00",
    "updatedAt": "2026-02-20T10:00:00",
    "uploadedByUserId": "user001",
    "uploadedByUserName": "John Doe"
  }
}
```

---

### 8.2 Get All Documents for a Conference
```
GET /api/conference-documents?conferenceId=conf001
```
🔒 Authenticated

**Response `200`:**
```json
{
  "success": true,
  "totalDocuments": 6,
  "data": [ /* array of ConferenceDocumentResponse */ ]
}
```

---

### 8.3 Get Documents by Year
```
GET /api/conference-documents/year?conferenceId=conf001&year=2026
```
**Response `200`:**
```json
{
  "success": true,
  "conference": "conf001",
  "year": 2026,
  "totalDocuments": 3,
  "data": [ /* array */ ]
}
```

---

### 8.4 Get Documents by Year — All Conferences (SUPER_ADMIN)
```
GET /api/conference-documents/year-all?year=2026
```
🔒 `SUPER_ADMIN`

**Response `200`:**
```json
{
  "success": true,
  "year": 2026,
  "totalDocuments": 15,
  "conferencesCount": 5,
  "dataByConference": {
    "ICSE 2026": [ /* docs */ ],
    "ICCA 2026": [ /* docs */ ]
  }
}
```

---

### 8.5 Get Documents by Type
```
GET /api/conference-documents/type?conferenceId=conf001&documentType=program
```
**Response `200`:**
```json
{
  "success": true,
  "conferenceId": "conf001",
  "documentType": "program",
  "totalDocuments": 3,
  "data": [ /* array */ ]
}
```

---

### 8.6 Search Documents with Filters (Paginated)
```
POST /api/conference-documents/search?conferenceId=conf001&year=2026&documentType=program&pageNumber=0&pageSize=10
```
All params are optional query params.

**Response `200`:**
```json
{
  "success": true,
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 6,
  "totalPages": 1,
  "data": [ /* array of ConferenceDocumentResponse */ ]
}
```

---

### 8.7 Get Document by ID (also logs VIEW)
```
GET /api/conference-documents/{id}
```
🔒 Authenticated

> ✅ Automatically creates a VIEW log entry in `conference_document_logs`.

**Response `200`:**
```json
{
  "success": true,
  "data": { /* ConferenceDocumentResponse */ }
}
```

---

### 8.8 Download Document
```
GET /api/conference-documents/{id}/download
```
🔒 `ADMIN` or `SUPER_ADMIN`

**Response:** Binary file stream  
Headers:
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="icse-2026-program.pdf"
Content-Length: 2048576
```

> ✅ Logs a DOWNLOAD entry in `conference_document_logs`.

---

### 8.9 Download Document (Alias)
```
GET /api/conference-documents/download/{id}
```
Same as 8.8.

---

### 8.10 Delete Document
```
DELETE /api/conference-documents/{id}
```
🔒 `ADMIN` or `SUPER_ADMIN`

> ✅ Hard-deletes from **both GCS bucket AND database**.  
> ✅ Logs a DELETE entry.

**Response `200`:**
```json
{
  "success": true,
  "message": "Document deleted successfully from bucket and database."
}
```

---

### 8.11 Get Available Years for a Conference
```
GET /api/conference-documents/available-years?conferenceId=conf001
```
**Response `200`:**
```json
{
  "success": true,
  "conferenceId": "conf001",
  "availableYears": [2026, 2025, 2024],
  "count": 3
}
```

---

### 8.12 Admin Documents Dashboard
```
GET /api/conference-documents/admin/dashboard
```
🔒 `ADMIN` or `SUPER_ADMIN`

Returns document stats for **all of the admin's assigned conferences**.

**Response `200`:**
```json
{
  "success": true,
  "adminId": "user001",
  "adminName": "John Doe",
  "data": {
    "globalStats": {
      "totalConferences": 2,
      "totalDocuments": 9,
      "totalFileSizeBytes": 10485760,
      "years": [2026, 2025],
      "documentsByType": {
        "Program": 2,
        "Book": 2,
        "Positive Sheets": 2
      },
      "lastUploadedAt": "2026-02-20T10:00:00"
    },
    "conferencesData": [
      {
        "conferenceId": "conf001",
        "conferenceName": "ICSE 2026",
        "conferenceStatus": "ACTIVE",
        "totalDocuments": 3,
        "documentsByYear": { "2026": 3 },
        "documentsByType": { "Program": 1, "Book": 1, "Positive Sheets": 1 },
        "totalFileSizeBytes": 5242880,
        "availableYears": [2026],
        "recentUploads": [
          {
            "id": "doc001",
            "fileName": "icse-program.pdf",
            "documentType": "Program",
            "documentTypeSlug": "program",
            "year": 2026,
            "fileSize": 2048576,
            "uploadedAt": "2026-02-20T10:00:00"
          }
        ]
      }
    ]
  }
}
```

---

### 8.13 Document Statistics for a Conference
```
GET /api/conference-documents/statistics?conferenceId=conf001
```
🔒 Authenticated

**Response `200`:**
```json
{
  "success": true,
  "conferenceId": "conf001",
  "statistics": {
    "totalDocuments": 6,
    "years": [2026, 2025],
    "documentsByYear": { "2026": 3, "2025": 3 },
    "documentsByType": { "Program": 2, "Book": 2, "Positive Sheets": 2 },
    "totalFileSizeBytes": 10485760,
    "lastUploadedAt": "2026-02-20T10:00:00"
  }
}
```

---

### 8.14 Global Document Statistics (SUPER_ADMIN)
```
GET /api/conference-documents/statistics/global
```
🔒 `SUPER_ADMIN`

**Response `200`:**
```json
{
  "success": true,
  "statistics": {
    "totalDocuments": 45,
    "totalConferences": 8,
    "years": [2026, 2025, 2024],
    "totalFileSizeBytes": 52428800,
    "documentsByConference": {
      "ICSE 2026": 6,
      "ICCA 2026": 3
    },
    "documentsByYear": { "2026": 24, "2025": 21 },
    "documentsByType": { "Program": 15, "Book": 15, "Positive Sheets": 15 },
    "lastUploadedAt": "2026-02-20T10:00:00"
  }
}
```

---

## 9. Analytics & Logs

> Two separate log systems:
> - **Data Logs** (`admin_activity_logs`) — tracks Excel upload, view, download of **dashboard data**
> - **Document Logs** (`conference_document_logs`) — tracks upload, download, delete, view of **conference documents**

---

### 🔴 Real-Time Log Streaming (SSE — No Page Refresh Needed)

> Uses **Server-Sent Events (SSE)**. The Super Admin's log page receives new log entries **instantly** the moment an admin performs any action — upload, download, view, delete — without any page refresh.

---

#### SSE: Super Admin — Stream ALL Logs (data-log + doc-log)
```
GET /api/analytics/stream?token=<JWT_TOKEN>
```
🔒 `SUPER_ADMIN`

> ℹ️ The browser's native `EventSource` cannot send custom headers, so the JWT token is passed as a **query parameter** `?token=<JWT>`.

**Frontend Usage:**
```js
const token = localStorage.getItem('token');
const es = new EventSource(`/api/analytics/stream?token=${token}`);

// Fires when connection is established
es.addEventListener('connected', (e) => {
  const data = JSON.parse(e.data);
  console.log('Connected:', data.message); // "Connected to real-time log stream"
});

// Fires whenever ANY admin uploads Excel, views data, downloads, etc.
es.addEventListener('data-log', (e) => {
  const log = JSON.parse(e.data);
  // Prepend to your logs list (latest first)
  setDataLogs(prev => [log, ...prev]);
});

// Fires whenever ANY admin uploads/downloads/deletes/views a conference document
es.addEventListener('doc-log', (e) => {
  const log = JSON.parse(e.data);
  setDocLogs(prev => [log, ...prev]);
});

// Clean up on component unmount
// es.close();
```

**Event payload for `data-log`:**
```json
{
  "id": "log123",
  "adminId": "user001",
  "adminName": "John Doe",
  "conferenceId": "conf001",
  "dashboardMasterId": "dash001",
  "actionType": "UPLOAD_EXCEL",
  "description": "Uploaded Excel: file.xlsx. Added: 97543, Duplicates: 2457",
  "fromSerialNo": null,
  "toSerialNo": null,
  "totalRecords": 97543,
  "emailDomain": null,
  "filterSummary": "No additional filters",
  "ipAddress": "103.21.45.67",
  "createdAt": "2026-02-20T21:30:00"
}
```

**Event payload for `doc-log`:**
```json
{
  "id": "dlog456",
  "adminId": "user001",
  "adminName": "John Doe",
  "conferenceId": "conf001",
  "conferenceName": "ICSE 2026",
  "documentId": "doc001",
  "fileName": "icse-program.pdf",
  "documentType": "program",
  "year": 2026,
  "actionType": "DOWNLOAD",
  "description": "Downloaded Program 'icse-program.pdf' (Year: 2026) from ICSE 2026",
  "ipAddress": "103.21.45.67",
  "createdAt": "2026-02-20T21:31:00"
}
```

---

#### SSE: Admin — Stream Own Logs Only
```
GET /api/analytics/stream/me?token=<JWT_TOKEN>
```
🔒 `ADMIN` or `SUPER_ADMIN`

Receives only the logged-in admin's own events. Same event names: `connected`, `data-log`, `doc-log`.

**Frontend Usage (Admin dashboard):**
```js
const token = localStorage.getItem('token');
const es = new EventSource(`/api/analytics/stream/me?token=${token}`);

es.addEventListener('data-log', (e) => {
  const log = JSON.parse(e.data);
  setMyLogs(prev => [log, ...prev]);
});

es.addEventListener('doc-log', (e) => {
  const log = JSON.parse(e.data);
  setMyDocLogs(prev => [log, ...prev]);
});
```

---

#### GET: Active SSE Connection Count (Debug/Health)
```
GET /api/analytics/stream/connections
```
🔒 `SUPER_ADMIN`

**Response `200`:**
```json
{
  "superAdminConnections": 2,
  "adminConnections": 5,
  "adminsConnected": 3
}
```

---

### ⚡ What Triggers a Real-Time Push?

| Admin Action | Event Name | Who Gets It |
|-------------|------------|-------------|
| Upload Excel file | `data-log` (actionType: `UPLOAD_EXCEL`) | SUPER_ADMIN + that Admin |
| View dashboard data | `data-log` (actionType: `VIEW`) | SUPER_ADMIN + that Admin |
| Download Excel | `data-log` (actionType: `DOWNLOAD_EXCEL`) | SUPER_ADMIN + that Admin |
| Download PDF | `data-log` (actionType: `DOWNLOAD_PDF`) | SUPER_ADMIN + that Admin |
| Upload conference document | `doc-log` (actionType: `UPLOAD`) | SUPER_ADMIN + that Admin |
| Download conference document | `doc-log` (actionType: `DOWNLOAD`) | SUPER_ADMIN + that Admin |
| Delete conference document | `doc-log` (actionType: `DELETE`) | SUPER_ADMIN + that Admin |
| View conference document | `doc-log` (actionType: `VIEW`) | SUPER_ADMIN + that Admin |

---

Each log entry structure:
```json
{
  "id": "log001",
  "adminId": "user001",
  "adminName": "John Doe",
  "conferenceId": "conf001",
  "dashboardMasterId": "dash001",
  "actionType": "UPLOAD_EXCEL",
  "description": "Uploaded Excel: registrations.xlsx. Added: 97543, Duplicates: 2457",
  "ipAddress": "103.21.45.67",
  "fromSerialNo": 1,
  "toSerialNo": 100,
  "totalRecords": 100,
  "emailDomain": "gmail.com",
  "filterSummary": "Serial: 1-100 | Domain: gmail.com",
  "createdAt": "2026-02-20T10:00:00"
}
```
`actionType` values: `UPLOAD_EXCEL`, `VIEW`, `DOWNLOAD_EXCEL`, `DOWNLOAD_PDF`, `UPLOAD_FILE`, `DOWNLOAD_FILE`, `DELETE_FILE`

---

#### 9A.1 My Upload Stats (Admin)
```
GET /api/analytics/upload-stats/me
```
🔒 `ADMIN` or `SUPER_ADMIN`

**Response `200`:**
```json
{
  "success": true,
  "count": 5,
  "data": [
    {
      "id": "stat001",
      "adminId": "user001",
      "conferenceId": "conf001",
      "dashboardMasterId": "dash001",
      "fileName": "registrations.xlsx",
      "totalRecordsInFile": 100000,
      "newRecordsAdded": 97543,
      "duplicateRecordsIgnored": 2457,
      "uploadedAt": "2026-02-20T10:00:00"
    }
  ]
}
```

---

#### 9A.2 My Upload Stats — Scoped to Conference
```
GET /api/analytics/upload-stats/me/conference/{conferenceId}
```
🔒 `ADMIN` or `SUPER_ADMIN`

---

#### 9A.3 My Activity Logs (Admin)
```
GET /api/analytics/logs/me
```
🔒 `ADMIN` or `SUPER_ADMIN`

**Response `200`:**
```json
{
  "success": true,
  "count": 12,
  "data": [ /* array of AdminActivityLog — latest first */ ]
}
```

---

#### 9A.4 My Logs — Scoped to Conference
```
GET /api/analytics/logs/me/conference/{conferenceId}
```

---

#### 9A.5 All Upload Stats (SUPER_ADMIN)
```
GET /api/analytics/upload-stats
```
🔒 `SUPER_ADMIN`

---

#### 9A.6 Upload Stats by Admin (SUPER_ADMIN)
```
GET /api/analytics/upload-stats/admin/{adminId}
```

---

#### 9A.7 Upload Stats by Conference (SUPER_ADMIN)
```
GET /api/analytics/upload-stats/conference/{conferenceId}
```

---

#### 9A.8 All Activity Logs (SUPER_ADMIN)
```
GET /api/analytics/logs
```
🔒 `SUPER_ADMIN`

**Response:** `{ "success": true, "count": N, "data": [ /* latest first */ ] }`

---

#### 9A.9 Logs by Admin (SUPER_ADMIN)
```
GET /api/analytics/logs/admin/{adminId}
```

---

#### 9A.10 Logs by Conference (SUPER_ADMIN)
```
GET /api/analytics/logs/conference/{conferenceId}
```

---

### 9B. Document Logs (ConferenceDocumentLog)

Each document log entry structure:
```json
{
  "id": "dlog001",
  "adminId": "user001",
  "adminName": "John Doe",
  "ipAddress": "103.21.45.67",
  "actionType": "DOWNLOAD",
  "conferenceId": "conf001",
  "conferenceName": "ICSE 2026",
  "documentId": "doc001",
  "fileName": "icse-2026-program.pdf",
  "documentType": "program",
  "year": 2026,
  "description": "Downloaded Program 'icse-2026-program.pdf' (Year: 2026) from ICSE 2026",
  "createdAt": "2026-02-20T10:30:00"
}
```
`actionType` values: `UPLOAD`, `DOWNLOAD`, `DELETE`, `VIEW`

---

#### 9B.1 My Document Logs (Admin)
```
GET /api/analytics/doc-logs/me
```
🔒 `ADMIN` or `SUPER_ADMIN`

**Response `200`:**
```json
{
  "success": true,
  "count": 8,
  "data": [ /* array of ConferenceDocumentLog — latest first */ ]
}
```

---

#### 9B.2 My Document Logs — Scoped to Conference
```
GET /api/analytics/doc-logs/me/conference/{conferenceId}
```

---

#### 9B.3 All Document Logs (SUPER_ADMIN)
```
GET /api/analytics/doc-logs
```
🔒 `SUPER_ADMIN`

---

#### 9B.4 Document Logs by Admin (SUPER_ADMIN)
```
GET /api/analytics/doc-logs/admin/{adminId}
```

---

#### 9B.5 Document Logs by Conference (SUPER_ADMIN)
```
GET /api/analytics/doc-logs/conference/{conferenceId}
```

---

## 10. Role Summary

| Feature | SUPER_ADMIN | ADMIN |
|---------|-------------|-------|
| Login | ✅ | ✅ |
| Create/Edit/Delete Users | ✅ | ❌ |
| Assign Conferences to Admin | ✅ | ❌ |
| Create/Edit/Delete Conferences | ✅ | ❌ |
| View All Conferences | ✅ | ✅ (own only via `/me`) |
| Upload Conference Image | ✅ | ✅ (own conferences) |
| Manage Dashboard Masters | ✅ | ❌ |
| Create/Manage Document Types | ✅ | ❌ |
| View Active Document Types | ✅ | ✅ |
| Upload Dashboard Data (Excel) | ❌ | ✅ (own conferences) |
| View Dashboard Data | ✅ | ✅ (own conferences) |
| Export Excel/PDF | ✅ | ✅ (own conferences) |
| Upload Conference Documents | ✅ | ✅ (own conferences) |
| Download Conference Documents | ✅ | ✅ (own conferences) |
| Delete Conference Documents | ✅ | ✅ (own conferences) |
| View All Data Logs | ✅ | ❌ |
| View Own Data Logs | ✅ | ✅ |
| View All Document Logs | ✅ | ❌ |
| View Own Document Logs | ✅ | ✅ |

---

## 11. Frontend Integration Flow

### Login Flow
```
1. POST /api/auth/signin → get token + role
2. Store token in localStorage
3. If role = ROLE_ADMIN  → redirect to Admin Dashboard
4. If role = ROLE_SUPER_ADMIN → redirect to Super Admin Dashboard
```

### Admin Dashboard Flow
```
1. GET /api/conferences/me → list assigned conferences
2. Select a conference → GET /api/conferences/{id}/dashboards → get dashboards
3. Select a dashboard → GET /api/dashboard-data/count → show total records
4. View data → GET /api/dashboard-data?fromSerialNo=1&toSerialNo=100&page=0
5. Export → GET /api/export/excel?fromSerialNo=1&toSerialNo=100
6. Upload Excel → POST /api/dashboard-data/upload (multipart)
```

### Conference Documents Flow (Admin)
```
1. GET /api/document-types/active → populate document type dropdown
2. POST /api/conference-documents/upload → upload file (conferenceId, year, documentType slug, file)
3. GET /api/conference-documents?conferenceId=... → list uploaded files
4. GET /api/conference-documents/{id} → view file details (also logs VIEW)
5. GET /api/conference-documents/{id}/download → download file
6. DELETE /api/conference-documents/{id} → delete from DB + GCS
```

### Super Admin Dashboard Flow
```
1. GET /api/conferences → all conferences
2. GET /api/users/admins → all admins
3. POST /api/users/{adminId}/conferences/{conferenceId} → assign
4. GET /api/analytics/logs → all data activity logs
5. GET /api/analytics/doc-logs → all document logs
6. GET /api/document-types → manage types
7. POST /api/document-types → create new type
```

---

## 12. Common Error Responses

| HTTP | Meaning | Example |
|------|---------|---------|
| `400` | Bad Request / Validation | `{ "success": false, "message": "File is empty" }` |
| `401` | Unauthorized (no/bad token) | Spring Security default |
| `403` | Forbidden (wrong role) | `{ "success": false, "message": "Access Denied" }` |
| `404` | Not Found | `{ "success": false, "message": "Document not found" }` |
| `500` | Internal Server Error | `{ "success": false, "message": "Unexpected error: ..." }` |

---

## 13. Request Headers

All protected endpoints require:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json        (for JSON body)
Content-Type: multipart/form-data     (for file uploads)
```

---

## 14. Important Notes for Frontend

1. **Pagination** — All view/filter endpoints return paginated responses. Default `size=500`, max `size=1000`. Use `page` param to get next pages.

2. **Document Type Slugs** — Always fetch `/api/document-types/active` before showing the upload form. Use the `slug` field as the `documentType` value when uploading.

3. **File Downloads** — Export and document download endpoints return binary streams. Use `fetch` with `blob()` in JS:
   ```js
   const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
   const blob = await res.blob();
   const link = document.createElement('a');
   link.href = URL.createObjectURL(blob);
   link.download = 'filename.xlsx';
   link.click();
   ```

4. **Excel Upload** — Column A = Name, Column B = Email. First row is header (skipped automatically).

5. **Serial Numbers** — Are assigned sequentially starting from 1 per `conferenceId + dashboardMasterId`. Use `fromSerialNo=1&toSerialNo=<count>` to get all data.

6. **Logs are latest-first** — All log responses are ordered by `createdAt DESC`.

7. **Conference Image** — Upload via `POST /api/conferences/{id}/upload-image` with `image` field. Old image auto-deleted.

