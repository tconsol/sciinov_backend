# SciInov Backend — Frontend API Guide

> Base URL: `http://localhost:8080`  
> All protected endpoints require the header: `Authorization: Bearer <JWT_TOKEN>`

---

## Table of Contents
1. [Authentication](#1-authentication)
2. [Conference Management (Admin / Super Admin)](#2-conference-management)
3. [Conference Documents — File Upload / Download / Delete](#3-conference-documents)
4. [Conference Document Stats](#4-conference-document-stats)
5. [Dashboard Data — View & Filter](#5-dashboard-data-view--filter)
6. [Export — Excel & PDF Download](#6-export--excel--pdf-download)
7. [Dashboard Data Activity Logs](#7-dashboard-data-activity-logs)
8. [Conference Document Logs (Separate)](#8-conference-document-logs-separate)
9. [Upload Stats](#9-upload-stats)
10. [Conference Image Upload](#10-conference-image-upload)
11. [User Management](#11-user-management)

---

## 1. Authentication

### 1.1 Login
```
POST /api/auth/login
```
**Body:**
```json
{
  "username": "admin@example.com",
  "password": "yourpassword"
}
```
**Response:**
```json
{
  "token": "eyJhbGc...",
  "type": "Bearer",
  "id": "userId",
  "username": "admin@example.com",
  "email": "admin@example.com",
  "role": "ADMIN"
}
```

### 1.2 Forgot Password
```
POST /api/auth/forgot-password
```
**Body:**
```json
{ "email": "admin@example.com" }
```
**Response:**
```json
{ "message": "Password reset email sent" }
```

### 1.3 Reset Password
```
POST /api/auth/reset-password
```
**Body:**
```json
{
  "token": "resetToken",
  "newPassword": "newPass123"
}
```

### 1.4 Change Password
```
POST /api/auth/change-password
```
**Body:**
```json
{
  "oldPassword": "oldPass",
  "newPassword": "newPass"
}
```

### 1.5 Forgot Username
```
POST /api/auth/forgot-username
```
**Body:**
```json
{ "email": "admin@example.com" }
```

---

## 2. Conference Management

> Roles: `ADMIN` (own conferences only), `SUPER_ADMIN` (all)

### 2.1 Get My Conferences (Admin)
```
GET /api/conferences/my
```
**Response:**
```json
{
  "success": true,
  "count": 2,
  "data": [
    {
      "id": "conf123",
      "name": "ICSE 2026",
      "year": 2026,
      "location": "Hyderabad",
      "description": "International Conference",
      "createdAt": "2026-01-10T10:00:00"
    }
  ]
}
```

### 2.2 Get All Conferences (Super Admin)
```
GET /api/conferences
```
**Response:** Same structure as above.

### 2.3 Get Conference by ID
```
GET /api/conferences/{conferenceId}
```

### 2.4 Create Conference (Super Admin)
```
POST /api/conferences
```
**Body:**
```json
{
  "name": "ICSE 2026",
  "year": 2026,
  "location": "Hyderabad",
  "description": "International Conference on Software Engineering"
}
```

### 2.5 Update Conference (Super Admin)
```
PUT /api/conferences/{conferenceId}
```
**Body:** Same as create.

### 2.6 Delete Conference (Super Admin)
```
DELETE /api/conferences/{conferenceId}
```

---

## 3. Conference Documents

> Admin can upload, download, and delete files (program, book, positive_sheet) associated with a conference and year.
> Files are stored in Google Cloud Storage; the blob name is stored in DB.

### 3.1 Upload Conference Document
```
POST /api/conference-documents/upload
Content-Type: multipart/form-data
```
**Form Data:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conferenceId` | String | ✅ | Conference ID |
| `year` | Integer | ✅ | e.g., `2026` |
| `fileType` | String | ✅ | One of: `program`, `book`, `positive_sheet` |
| `file` | File | ✅ | The file to upload |

**Response:**
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "id": "doc123",
    "conferenceId": "conf123",
    "year": 2026,
    "fileType": "program",
    "fileName": "program.pdf",
    "blobName": "conferences/conf123/2026/program/program.pdf",
    "fileSize": 204800,
    "uploadedAt": "2026-02-20T10:30:00",
    "uploadedBy": "adminId"
  }
}
```

### 3.2 Download Conference Document
```
GET /api/conference-documents/download/{documentId}
```
**Response:** Binary file stream (PDF, XLSX, etc.)  
The browser will prompt download automatically.

### 3.3 Delete Conference Document
```
DELETE /api/conference-documents/{documentId}
```
> ⚠️ This deletes the record from DB **and** the file from GCS bucket.

**Response:**
```json
{
  "success": true,
  "message": "File deleted successfully"
}
```

### 3.4 Get Documents by Conference
```
GET /api/conference-documents?conferenceId={conferenceId}
```
**Response:**
```json
{
  "success": true,
  "count": 3,
  "data": [
    {
      "id": "doc123",
      "conferenceId": "conf123",
      "year": 2026,
      "fileType": "program",
      "fileName": "program.pdf",
      "blobName": "conferences/conf123/2026/program/program.pdf",
      "fileSize": 204800,
      "uploadedAt": "2026-02-20T10:30:00",
      "uploadedBy": "adminId"
    }
  ]
}
```

### 3.5 Get Documents by Conference and Year
```
GET /api/conference-documents?conferenceId={conferenceId}&year={year}
```

### 3.6 Get Documents by Conference, Year and FileType
```
GET /api/conference-documents?conferenceId={conferenceId}&year={year}&fileType={fileType}
```
**`fileType` values:** `program`, `book`, `positive_sheet`

### 3.7 Filter Documents (POST)
```
POST /api/conference-documents/filter
```
**Body:**
```json
{
  "conferenceId": "conf123",
  "year": 2026,
  "fileType": "program"
}
```

---

## 4. Conference Document Stats

> Stats per admin for their conference document uploads.

### 4.1 Get My Conference Document Upload Stats (Admin)
```
GET /api/analytics/upload-stats/me
Authorization: Bearer <ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "count": 5,
  "data": [
    {
      "id": "stat123",
      "adminId": "admin1",
      "adminName": "John Doe",
      "conferenceId": "conf123",
      "conferenceName": "ICSE 2026",
      "fileName": "program.pdf",
      "fileType": "program",
      "fileSize": 204800,
      "uploadedAt": "2026-02-20T10:30:00"
    }
  ]
}
```

### 4.2 Get My Stats for Specific Conference (Admin)
```
GET /api/analytics/upload-stats/me/conference/{conferenceId}
Authorization: Bearer <ADMIN_TOKEN>
```
**Response:** Same as above filtered to that conference.

### 4.3 Get All Upload Stats (Super Admin)
```
GET /api/analytics/upload-stats
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```

### 4.4 Get Stats by Admin (Super Admin)
```
GET /api/analytics/upload-stats/admin/{adminId}
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```

### 4.5 Get Stats by Conference (Super Admin)
```
GET /api/analytics/upload-stats/conference/{conferenceId}
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```

---

## 5. Dashboard Data — View & Filter

> Every time an admin views or filters data, a **VIEW log** is recorded with the serial range, total records, filters applied (including email domain).

### 5.1 Get Data by Serial Range
```
GET /api/dashboard-data?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=100
```
> Returns records 1 through 100 **inclusive**.

**Response:** Array of `DashboardData` objects sorted by `serialNo` ASC.
```json
[
  {
    "id": "data123",
    "conferenceId": "conf123",
    "dashboardMasterId": "dm123",
    "serialNo": 1,
    "name": "Alice",
    "email": "alice@gmail.com",
    "status": true,
    "createdAt": "2026-01-15T09:00:00"
  }
]
```

### 5.2 Filter Data (All Filters Combined)
```
GET /api/dashboard-data/filter
```
**Query Params (all optional except conferenceId & dashboardMasterId):**
| Param | Type | Example |
|-------|------|---------|
| `conferenceId` | String | `conf123` |
| `dashboardMasterId` | String | `dm123` |
| `fromSerialNo` | Long | `1` |
| `toSerialNo` | Long | `100` |
| `startDate` | Date (ISO) | `2026-01-01` |
| `endDate` | Date (ISO) | `2026-01-31` |
| `emailDomain` | String | `gmail.com` |

**Response:** Array of `DashboardData` objects.

### 5.3 Filter by Email Domain
```
GET /api/dashboard-data/by-email-domain?conferenceId={id}&dashboardMasterId={id}&emailDomain=gmail.com
```
> Returns only records where email ends with `@gmail.com`.

### 5.4 Filter by Date Range
```
GET /api/dashboard-data/by-date?conferenceId={id}&dashboardMasterId={id}&startDate=2026-01-01&endDate=2026-01-31
```

### 5.5 Get Count
```
GET /api/dashboard-data/count?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=100&emailDomain=gmail.com
```
**Response:** `100` (a number)

### 5.6 Upload Excel Data (Admin)
```
POST /api/dashboard-data/upload
Content-Type: multipart/form-data
```
**Form Data:**
| Field | Value |
|-------|-------|
| `file` | Excel file (.xlsx) |
| `conferenceId` | `conf123` |
| `dashboardMasterId` | `dm123` |

**Response:**
```json
"File uploaded successfully!"
```

---

## 6. Export — Excel & PDF Download

> Download actions are logged with serial range, total record count, email domain (if filtered), and all applied filters.

### 6.1 Download Excel (Serial Range)
```
GET /api/export/excel?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=100
```
**Response:** Binary `.xlsx` file download.

### 6.2 Download PDF (Serial Range)
```
GET /api/export/pdf?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=100
```
**Response:** Binary `.pdf` file download.

### 6.3 Download Excel (Advanced Filters)
```
GET /api/export/excel/advanced
```
**Query Params:**
| Param | Required | Example |
|-------|----------|---------|
| `conferenceId` | ✅ | `conf123` |
| `dashboardMasterId` | ✅ | `dm123` |
| `fromSerialNo` | ❌ | `1` |
| `toSerialNo` | ❌ | `100` |
| `startDate` | ❌ | `2026-01-01` |
| `endDate` | ❌ | `2026-01-31` |
| `emailDomain` | ❌ | `gmail.com` |

**Response:** Binary `.xlsx` file download.

### 6.4 Download PDF (Advanced Filters)
```
GET /api/export/pdf/advanced
```
Same params as Excel advanced above. **Response:** Binary `.pdf` file.

### 6.5 Download Excel (POST with Body)
```
POST /api/export/excel/filter
```
**Body:**
```json
{
  "conferenceId": "conf123",
  "dashboardMasterId": "dm123",
  "fromSerialNo": 1,
  "toSerialNo": 100,
  "startDate": "2026-01-01",
  "endDate": "2026-01-31",
  "emailDomain": "gmail.com"
}
```

### 6.6 Download PDF (POST with Body)
```
POST /api/export/pdf/filter
```
Same body as above.

### 6.7 Preview Filtered Data (Before Export)
```
GET /api/export/preview?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=100&emailDomain=gmail.com
```
**Response:** Array of `DashboardData` objects (same as filter endpoint).

### 6.8 Get Distinct Email Domains
```
GET /api/export/email-domains?conferenceId={id}&dashboardMasterId={id}
```
**Response:** `["gmail.com", "yahoo.com", "outlook.com"]`

---

## 7. Dashboard Data Activity Logs

> These logs are **only** for dashboard data operations (view, filter, download Excel/PDF).  
> Conference document actions (upload/download/delete files) are logged **separately** — see [Section 8](#8-conference-document-logs-separate).  
> Logs are always returned **latest first** (newest at top).

### Dashboard Data Log Object
```json
{
  "id": "log123",
  "adminId": "admin1",
  "adminName": "John Doe",
  "conferenceId": "conf123",
  "dashboardMasterId": "dm123",
  "actionType": "DOWNLOAD_EXCEL",
  "description": "Downloaded Excel | Serial No 1 to 100 | Total records: 100 | Email Domain: gmail.com",
  "fromSerialNo": 1,
  "toSerialNo": 100,
  "totalRecords": 100,
  "emailDomain": "gmail.com",
  "filterSummary": "Serial No: 1 to 100; Email Domain: gmail.com;",
  "ipAddress": "127.0.0.1",
  "createdAt": "2026-02-20T10:30:00"
}
```

**`actionType` values (dashboard data only):**
| Value | Meaning |
|-------|---------|
| `VIEW` | Admin viewed/filtered dashboard data (did NOT download) |
| `DOWNLOAD_EXCEL` | Admin downloaded Excel file of dashboard data |
| `DOWNLOAD_PDF` | Admin downloaded PDF file of dashboard data |
| `UPLOAD_EXCEL` | Admin uploaded an Excel data file to dashboard |

---

### 7.1 Get My Dashboard Data Logs (Admin — own logs)
```
GET /api/analytics/logs/me
Authorization: Bearer <ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "count": 12,
  "data": [ /* array of log objects, latest first */ ]
}
```

### 7.2 Get My Logs for Specific Conference (Admin)
```
GET /api/analytics/logs/me/conference/{conferenceId}
Authorization: Bearer <ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "conferenceId": "conf123",
  "count": 5,
  "data": [ /* array of log objects, latest first */ ]
}
```

### 7.3 Get All Dashboard Data Logs (Super Admin)
```
GET /api/analytics/logs
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "count": 150,
  "data": [ /* all logs, latest first */ ]
}
```

### 7.4 Get Logs by Admin (Super Admin)
```
GET /api/analytics/logs/admin/{adminId}
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```

### 7.5 Get Logs by Conference (Super Admin)
```
GET /api/analytics/logs/conference/{conferenceId}
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```

---

## 8. Conference Document Logs (Separate)

> These logs are **completely separate** from dashboard data logs.  
> Every **upload**, **download**, **delete**, and **view of a specific document** is recorded here.  
> **VIEW logs are created only when clicking to view a specific document** (via `GET /api/conference-documents/{id}`), NOT when listing documents.  
> Logs always returned **latest first** (newest at top).  
> **Admin** can only see their own logs. **Super Admin** can see all.

### Conference Document Log Object
```json
{
  "id": "doclog123",
  "adminId": "admin1",
  "adminName": "John Doe",
  "conferenceId": "conf123",
  "conferenceName": "ICSE 2026",
  "documentId": "doc456",
  "fileName": "program.pdf",
  "documentType": "PROGRAM",
  "year": 2026,
  "actionType": "UPLOAD",
  "description": "Uploaded Program 'program.pdf' (Year: 2026) for ICSE 2026",
  "ipAddress": "192.168.1.1",
  "createdAt": "2026-02-20T10:30:00"
}
```

**`actionType` values:**
| Value | Meaning |
|-------|---------|
| `UPLOAD` | Admin uploaded a conference document file |
| `DOWNLOAD` | Admin downloaded a conference document file |
| `DELETE` | Admin deleted a conference document file |
| `VIEW` | Admin viewed/listed conference documents |

---

### 8.1 Get My Conference Document Logs (Admin — own logs)
```
GET /api/analytics/doc-logs/me
Authorization: Bearer <ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "count": 8,
  "data": [
    {
      "id": "doclog123",
      "adminId": "admin1",
      "adminName": "John Doe",
      "conferenceId": "conf123",
      "conferenceName": "ICSE 2026",
      "documentId": "doc456",
      "fileName": "program.pdf",
      "documentType": "PROGRAM",
      "year": 2026,
      "actionType": "UPLOAD",
      "description": "Uploaded Program 'program.pdf' (Year: 2026) for ICSE 2026",
      "ipAddress": "192.168.1.1",
      "createdAt": "2026-02-20T10:30:00"
    }
  ]
}
```

### 8.2 Get My Document Logs for Specific Conference (Admin)
```
GET /api/analytics/doc-logs/me/conference/{conferenceId}
Authorization: Bearer <ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "conferenceId": "conf123",
  "count": 3,
  "data": [ /* array of doc-log objects, latest first */ ]
}
```

### 8.3 Get All Conference Document Logs (Super Admin)
```
GET /api/analytics/doc-logs
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "count": 50,
  "data": [ /* all doc-log objects across all admins, latest first */ ]
}
```

### 8.4 Get Document Logs by Admin (Super Admin)
```
GET /api/analytics/doc-logs/admin/{adminId}
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "adminId": "admin1",
  "count": 12,
  "data": [ /* doc-log objects for that admin, latest first */ ]
}
```

### 8.5 Get Document Logs by Conference (Super Admin)
```
GET /api/analytics/doc-logs/conference/{conferenceId}
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "conferenceId": "conf123",
  "count": 20,
  "data": [ /* doc-log objects for that conference, latest first */ ]
}
```

---

## 9. Upload Stats

> Stats for dashboard Excel data uploads (not conference documents).

### 9.1 My Upload Stats (Admin)
```
GET /api/analytics/upload-stats/me
Authorization: Bearer <ADMIN_TOKEN>
```
**Response:**
```json
{
  "success": true,
  "count": 3,
  "data": [
    {
      "id": "stat1",
      "adminId": "admin1",
      "adminName": "John Doe",
      "conferenceId": "conf123",
      "dashboardMasterId": "dm123",
      "fileName": "data.xlsx",
      "totalRows": 500,
      "newRows": 480,
      "duplicateRows": 20,
      "uploadedAt": "2026-02-18T14:00:00"
    }
  ]
}
```

### 9.2 My Stats for a Conference (Admin)
```
GET /api/analytics/upload-stats/me/conference/{conferenceId}
```

### 9.3 All Upload Stats (Super Admin)
```
GET /api/analytics/upload-stats
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```

### 9.4 Stats by Admin (Super Admin)
```
GET /api/analytics/upload-stats/admin/{adminId}
```

### 9.5 Stats by Conference (Super Admin)
```
GET /api/analytics/upload-stats/conference/{conferenceId}
```

---

## 10. Conference Image Upload

> Upload a conference image. If the conference already has an image, the old one is automatically deleted from the bucket and replaced with the new one.  
> **Only one image per conference** — new uploads overwrite the previous image.  
> Supported formats: JPEG, PNG, GIF, WebP, etc.

### 10.1 Upload Conference Image

```
POST /api/conferences/{conferenceId}/upload-image
Content-Type: multipart/form-data
Authorization: Bearer <ADMIN_OR_SUPER_ADMIN_TOKEN>
```

**Path Parameters:**
| Param | Type | Example |
|-------|------|---------|
| `conferenceId` | String | `conf123` |

**Form Data:**
| Field | Type | Required | Example |
|-------|------|----------|---------|
| `image` | File | ✅ | image.png |

**Response (Success):**
```json
{
  "success": true,
  "message": "Conference image uploaded successfully",
  "conferenceId": "conf123",
  "conferenceName": "ICSE 2026",
  "imageUrl": "https://storage.googleapis.com/sciinovfiles/conferences/icse-2026/image_1708424268.png?X-Goog-Algorithm=...",
  "imageBlobName": "conferences/icse-2026/image_1708424268.png"
}
```

**Response (Error - Not Image):**
```json
{
  "success": false,
  "message": "File must be an image (JPEG, PNG, GIF, WebP, etc.)"
}
```

**Response (Error - Conference Not Found):**
```json
{
  "success": false,
  "message": "Conference not found: conf123"
}
```

---

## 11. User Management

### 11.1 Get All Users (Super Admin)
```
GET /api/users
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```

### 11.2 Get User by ID
```
GET /api/users/{userId}
```

### 11.3 Create Admin User (Super Admin)
```
POST /api/users
```
**Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "username": "johndoe",
  "password": "pass123",
  "role": "ADMIN",
  "conferenceIds": ["conf123", "conf456"]
}
```

### 11.4 Update User (Super Admin)
```
PUT /api/users/{userId}
```

### 11.5 Delete User (Super Admin)
```
DELETE /api/users/{userId}
```

### 11.6 Assign Conference to Admin (Super Admin)
```
PUT /api/users/{userId}/assign-conference/{conferenceId}
```

---

## Common Error Responses

### 401 Unauthorized
```json
{ "message": "Unauthorized" }
```

### 403 Forbidden
```json
{ "message": "Access Denied: You are not assigned to this conference." }
```

### 400 Bad Request
```json
{ "message": "Error description" }
```

### 404 Not Found
```json
{ "message": "Resource not found" }
```

---

## Changes Summary (What Was Updated)

| Feature | Status |
|---------|--------|
| `emailDomain` filter on all data view/export endpoints | ✅ Added |
| `GET /api/export/email-domains` — get distinct email domains | ✅ Added |
| `GET /api/dashboard-data/by-email-domain` — filter by domain | ✅ Added |
| Region concept removed from all entities, DTOs, queries, exports | ✅ Removed |
| **Country concept removed from DashboardData entity** | ✅ Removed |
| VIEW logs recorded when admin views/filters dashboard data | ✅ Added |
| Download logs include: serial range, total records, email domain, filter summary | ✅ Added |
| All logs returned latest first (newest on top) | ✅ Fixed |
| **Conference document logs now in separate collection `conference_document_logs`** | ✅ Added |
| `GET /api/analytics/doc-logs/me` — admin own document logs | ✅ Added |
| `GET /api/analytics/doc-logs/me/conference/{id}` — admin own doc logs per conference | ✅ Added |
| `GET /api/analytics/doc-logs` — super admin all document logs | ✅ Added |
| `GET /api/analytics/doc-logs/admin/{adminId}` — super admin views specific admin's doc logs | ✅ Added |
| `GET /api/analytics/doc-logs/conference/{id}` — super admin views conference doc logs | ✅ Added |
| Upload/Download/Delete/View of conference documents logged to `conference_document_logs` | ✅ Added |
| Dashboard data logs (`admin_activity_logs`) no longer mixed with document logs | ✅ Fixed |
| Blob name stored in DB after file upload | ✅ (see ConferenceDocument entity) |
| Delete file → removes from DB and GCS bucket | ✅ (see ConferenceDocumentController) |
| DashboardData now stores only: `serialNo`, `name`, `email`, `status`, `createdAt`, `updatedAt` | ✅ Simplified |
| **Conference image upload endpoint** | ✅ Added |
| **Auto-delete previous image when uploading new one** | ✅ Added |
| **Store image URL and blob name in Conference entity** | ✅ Added |

---

## Dashboard Data Entity Structure

**DashboardData fields:**
- `serialNo` (Long): Unique serial number
- `name` (String): Person/entity name
- `email` (String): Email address
- `status` (Boolean): Active/inactive status
- `createdAt` (LocalDateTime): Record creation timestamp

**Excel upload format** (via `/api/dashboard-data/upload`):
- Column 0: **Name** (required)
- Column 1: **Email** (required)
- No other columns are read or stored

---

## Log Collections Summary

| Collection | What is logged | Who can access |
|------------|---------------|----------------|
| `admin_activity_logs` | Dashboard data: VIEW, DOWNLOAD_EXCEL, DOWNLOAD_PDF, UPLOAD_EXCEL | Admin (own) via `/api/analytics/logs/me`; Super Admin (all) via `/api/analytics/logs` |
| `conference_document_logs` | Conference files: UPLOAD, DOWNLOAD, DELETE, VIEW | Admin (own) via `/api/analytics/doc-logs/me`; Super Admin (all) via `/api/analytics/doc-logs` |

