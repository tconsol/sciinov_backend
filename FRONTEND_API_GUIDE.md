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
7. [Activity Logs](#7-activity-logs)
8. [Upload Stats](#8-upload-stats)
9. [User Management](#9-user-management)

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
    "region": "Asia",
    "country": "India",
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
| `region` | String | `Asia` |
| `country` | String | `India` |
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

### 5.5 Filter by Region
```
GET /api/dashboard-data/by-region?conferenceId={id}&dashboardMasterId={id}&region=Asia
```

### 5.6 Filter by Country
```
GET /api/dashboard-data/by-country?conferenceId={id}&dashboardMasterId={id}&country=India
```

### 5.7 Get Count
```
GET /api/dashboard-data/count?conferenceId={id}&dashboardMasterId={id}&fromSerialNo=1&toSerialNo=100&emailDomain=gmail.com
```
**Response:** `100` (a number)

### 5.8 Upload Excel Data (Admin)
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
| `region` | ❌ | `Asia` |
| `country` | ❌ | `India` |
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
  "region": "Asia",
  "country": "India",
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

### 6.8 Get Distinct Regions
```
GET /api/export/regions?conferenceId={id}&dashboardMasterId={id}
```
**Response:** `["Asia", "Europe", "North America"]`

### 6.9 Get Distinct Countries
```
GET /api/export/countries?conferenceId={id}&dashboardMasterId={id}
```
**Response:** `["India", "Germany", "USA"]`

### 6.10 Get Distinct Email Domains
```
GET /api/export/email-domains?conferenceId={id}&dashboardMasterId={id}
```
**Response:** `["gmail.com", "yahoo.com", "outlook.com"]`

---

## 7. Activity Logs

> Logs are always returned **latest first** (newest at top).
> Each log includes: action type, serial range, total records, email domain used, full filter summary.

### Log Object Structure
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

**`actionType` values:**
| Value | Meaning |
|-------|---------|
| `VIEW` | Admin viewed/filtered data (did NOT download) |
| `DOWNLOAD_EXCEL` | Admin downloaded Excel file |
| `DOWNLOAD_PDF` | Admin downloaded PDF file |
| `UPLOAD_EXCEL` | Admin uploaded Excel data file |
| `UPLOAD_FILE` | Admin uploaded conference document (program/book/sheet) |
| `DOWNLOAD_FILE` | Admin downloaded a conference document |
| `DELETE_FILE` | Admin deleted a conference document |
| `CREATE` | Record created |
| `UPDATE` | Record updated |
| `DELETE` | Record deleted |

---

### 7.1 Get My Activity Logs (Admin — own logs)
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

### 7.3 Get All Activity Logs (Super Admin)
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

## 8. Upload Stats

> Stats for dashboard Excel data uploads (not conference documents).

### 8.1 My Upload Stats (Admin)
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

### 8.2 My Stats for a Conference (Admin)
```
GET /api/analytics/upload-stats/me/conference/{conferenceId}
```

### 8.3 All Upload Stats (Super Admin)
```
GET /api/analytics/upload-stats
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```

### 8.4 Stats by Admin (Super Admin)
```
GET /api/analytics/upload-stats/admin/{adminId}
```

### 8.5 Stats by Conference (Super Admin)
```
GET /api/analytics/upload-stats/conference/{conferenceId}
```

---

## 9. User Management

### 9.1 Get All Users (Super Admin)
```
GET /api/users
Authorization: Bearer <SUPER_ADMIN_TOKEN>
```

### 9.2 Get User by ID
```
GET /api/users/{userId}
```

### 9.3 Create Admin User (Super Admin)
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

### 9.4 Update User (Super Admin)
```
PUT /api/users/{userId}
```

### 9.5 Delete User (Super Admin)
```
DELETE /api/users/{userId}
```

### 9.6 Assign Conference to Admin (Super Admin)
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
| VIEW logs recorded when admin views/filters data (not just downloads) | ✅ Added |
| Download logs include: serial range, total records, email domain, filter summary | ✅ Added |
| All logs returned latest first (newest on top) | ✅ Fixed |
| `emailDomain` param in export advanced/filter/preview endpoints | ✅ Added |
| Blob name stored in DB after file upload | ✅ (see ConferenceDocument entity) |
| Delete file → removes from DB and GCS bucket | ✅ (see ConferenceDocumentController) |

