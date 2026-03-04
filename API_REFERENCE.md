# SciInov DBMS — Complete API Reference
> **Stack:** Spring Boot 3.4.3 · MongoDB · JWT (jjwt 0.12.6) · Lombok 1.18.38 · Java 17
> **Base URL:** `http://localhost:8080` (dev) | `https://api.sciinovdbms.com` (prod)
> **Generated:** March 4, 2026

---

## Global Conventions

### Authentication Header (all protected endpoints)
```
Authorization: Bearer <access_token>
```

### Common Error Response
```json
{ "status": 401, "error": "Unauthorized", "message": "...", "path": "/api/..." }
{ "status": 403, "error": "Forbidden",    "message": "...", "path": "/api/..." }
```

### Enums Used Across APIs

| Enum | Values | Used In |
|------|--------|---------|
| `User.Role` | `SUPER_ADMIN`, `ADMIN` | User create/update |
| `Conference.Status` | `ACTIVE`, `INACTIVE` | Conference create/update/status |
| `AdminActivityLog.ActionType` | `CREATE`, `UPDATE`, `DELETE`, `VIEW`, `DOWNLOAD_EXCEL`, `DOWNLOAD_PDF`, `UPLOAD_EXCEL`, `UPLOAD_FILE`, `DOWNLOAD_FILE`, `DELETE_FILE`, `VIEW_TLD_FILTER`, `DOWNLOAD_EXCEL_TLD`, `DOWNLOAD_PDF_TLD` | Activity logs (read-only, server-set) |
| `ConferenceDocumentLog.ActionType` | `UPLOAD`, `DOWNLOAD`, `DELETE`, `VIEW` | Document logs (read-only, server-set) |

---

## 1. AuthController
**Base path:** `/api/auth`
**Controller file:** `AuthController.java`

All endpoints in this controller are **public** (no JWT required) unless stated.

---

### POST `/api/auth/signin`
**Description:** Authenticate a user and receive JWT access + refresh tokens.
**Access:** Public

**Request Body** (`application/json`):
```json
{
  "userId":   "admin123",    // REQUIRED — unique login username
  "password": "Pass@1234"    // REQUIRED — plain-text password (min 1 char)
}
```

**Response 200:**
```json
{
  "token":        "eyJhbGciOiJIUzI1NiJ9...",   // JWT access token (valid 24 h)
  "refreshToken": "dGhpcyBpcyBh...",            // Refresh token (valid 7 days)
  "type":         "Bearer",
  "id":           "65f1a2b3c4d5e6f7a8b9c0d1",   // MongoDB _id of the user
  "userId":       "admin123",
  "email":        "admin@example.com",
  "roles":        ["ROLE_ADMIN"]                 // or ["ROLE_SUPER_ADMIN"]
}
```

**Response 401:** `{ "message": "Invalid credentials" }`
**Response 403:** `{ "message": "Account is deactivated. Please contact administrator." }`

---

### POST `/api/auth/refresh-token`
**Description:** Exchange a valid refresh token for a new access token.
**Access:** Public

**Request Body:**
```json
{
  "refreshToken": "dGhpcyBpcyBh..."    // REQUIRED
}
```

**Response 200:**
```json
{
  "accessToken":  "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "dGhpcyBpcyBh...",
  "type":         "Bearer"
}
```

**Response 401:** `{ "success": false, "message": "Invalid or expired refresh token" }`

---

### POST `/api/auth/logout`
**Description:** Revoke the current refresh token. The access token naturally expires in 24 h.
**Access:** `ADMIN`, `SUPER_ADMIN` *(Bearer token required)*

**Request Body** *(optional)*:
```json
{
  "refreshToken": "dGhpcyBpcyBh..."    // OPTIONAL — if omitted, all tokens for user are revoked
}
```

**Response 200:**
```json
{ "message": "Logged out successfully", "success": true }
```

---

### POST `/api/auth/forgot-password`
**Description:** Send a password reset email to the user's registered email address.
**Access:** Public

**Request Body:**
```json
{
  "userId": "admin123"    // REQUIRED
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Password reset email sent",
  "email":   "a***@example.com"    // masked for security
}
```

**Response 404:** `{ "success": false, "message": "User not found" }`

---

### GET `/api/auth/validate-reset-token?token={token}`
**Description:** Check if a password-reset token is still valid (not expired, not used).
**Access:** Public

**Query Parameters:**

| Param | Required | Description |
|-------|----------|-------------|
| `token` | ✅ | The reset token from the email link |

**Response 200:** `{ "success": true, "message": "Token is valid" }`
**Response 400:** `{ "success": false, "message": "Invalid or expired token" }`

---

### POST `/api/auth/reset-password`
**Description:** Set a new password using a valid reset token.
**Access:** Public

**Request Body:**
```json
{
  "token":           "abc123-reset-token",   // REQUIRED — from email link
  "newPassword":     "NewPass@456",          // REQUIRED — min 6 characters
  "confirmPassword": "NewPass@456"           // REQUIRED — must match newPassword
}
```

**Response 200:** `{ "success": true, "message": "Password reset successful" }`
**Response 400:** `{ "success": false, "message": "Passwords do not match" }`
**Response 400:** `{ "success": false, "message": "Invalid or expired token" }`

---

### POST `/api/auth/forgot-username`
**Description:** Send the user's username to their registered email or phone.
**Access:** Public

**Request Body** *(provide at least one)*:
```json
{
  "email":       "admin@example.com",    // OPTIONAL — lookup by email
  "phoneNumber": "+919876543210"         // OPTIONAL — lookup by phone
}
```
> ⚠️ At least one of `email` or `phoneNumber` must be provided.

**Response 200:**
```json
{
  "userId":       "admin123",
  "maskedUserId": "ad***3",
  "email":        "admin@example.com",
  "maskedEmail":  "ad***@example.com",
  "message":      "Username sent to your email",
  "success":      true
}
```

---

### POST `/api/auth/change-password`
**Description:** Change password for the currently logged-in user.
**Access:** `ADMIN`, `SUPER_ADMIN` *(Bearer token required)*

**Request Body:**
```json
{
  "currentPassword": "OldPass123",     // REQUIRED
  "newPassword":     "NewPass@456",    // REQUIRED — min 6 characters
  "confirmPassword": "NewPass@456"     // REQUIRED — must match newPassword
}
```

**Response 200:** `{ "success": true, "message": "Password changed successfully" }`
**Response 400:** `{ "success": false, "message": "Current password is incorrect" }`
**Response 400:** `{ "success": false, "message": "Passwords do not match" }`

---

### GET `/api/auth/token-expiration`
**Description:** Returns JWT token expiration configuration.
**Access:** Public

**Response 200:**
```json
{
  "success":                  true,
  "tokenExpirationMs":        86400000,
  "tokenExpirationMinutes":   1440,
  "tokenExpirationHours":     24
}
```

---

## 2. UserController
**Base path:** `/api/users`
**Controller file:** `UserController.java`
**All endpoints:** `SUPER_ADMIN` only

---

### GET `/api/users/admins`
**Description:** Get all ADMIN role users (non-deleted).

**Response 200** — Array of `User` objects:
```json
[
  {
    "id":            "65f1a2b3c4d5e6f7a8b9c0d1",
    "firstName":     "John",
    "lastName":      "Doe",
    "phoneNumber":   "+919876543210",
    "email":         "jdoe@example.com",
    "userId":        "jdoe",
    "role":          "ADMIN",           // Enum: SUPER_ADMIN | ADMIN
    "status":        true,
    "conferenceIds": ["conf123", "conf456"],
    "createdAt":     "2026-01-15T10:30:00",
    "updatedAt":     "2026-02-20T08:00:00",
    "deleted":       false
  }
]
```
> Note: `password` field is **never** returned in any response.

---

### GET `/api/users/admins/conferences/all`
**Description:** Get all ADMIN users with their full assigned conference details.

**Response 200** — Array of `AdminConferenceResponse`:
```json
[
  {
    "adminId":       "65f1a2b3c4d5e6f7a8b9c0d1",
    "userId":        "jdoe",
    "firstName":     "John",
    "lastName":      "Doe",
    "email":         "jdoe@example.com",
    "phoneNumber":   "+919876543210",
    "status":        true,
    "conferenceIds": ["conf123", "conf456"]
  }
]
```

---

### GET `/api/users/super-admins`
**Description:** Get all SUPER_ADMIN users.

**Response 200:** Array of `User` objects (same schema as above).

---

### GET `/api/users/{id}`
**Description:** Get a single user by their MongoDB `_id`.

**Path Parameters:** `id` — MongoDB document ID

**Response 200:** Single `User` object
**Response 404:** Empty body

---

### POST `/api/users`
**Description:** Create a new ADMIN or SUPER_ADMIN user.

**Request Body:**
```json
{
  "firstName":   "Jane",                // REQUIRED
  "lastName":    "Smith",               // REQUIRED
  "userId":      "jsmith",              // REQUIRED — must be unique
  "email":       "jsmith@example.com",  // OPTIONAL — must be unique if provided
  "phoneNumber": "+919876543210",       // OPTIONAL — must be unique if provided
  "password":    "Initial@123",         // REQUIRED — min 6 characters
  "role":        "ADMIN",               // REQUIRED — Enum: SUPER_ADMIN | ADMIN
  "status":      true                   // OPTIONAL — default true
}
```

**Response 200:** Created `User` object (password excluded)
**Response 400:** `{ "message": "User ID already exists" }`

---

### PUT `/api/users/{id}`
**Description:** Update an existing user's details.

**Path Parameters:** `id` — MongoDB document ID

**Request Body** *(same schema as create — all fields optional for update)*:
```json
{
  "firstName":   "Jane",                // OPTIONAL
  "lastName":    "Smith",               // OPTIONAL
  "email":       "new@example.com",     // OPTIONAL
  "phoneNumber": "+919999999999",       // OPTIONAL
  "role":        "ADMIN",               // OPTIONAL — Enum: SUPER_ADMIN | ADMIN
  "status":      true                   // OPTIONAL
}
```

**Response 200:** Updated `User` object

---

### DELETE `/api/users/{id}`
**Description:** Soft-delete a user (sets `deleted=true`).

**Response 200:** Empty 200 response

---

### PATCH `/api/users/{id}/status`
**Description:** Activate or deactivate a user account.

**Request Body:**
```json
{
  "status": false    // REQUIRED — boolean: true=active, false=deactivated
}
```

**Response 200:** Updated `User` object

---

### GET `/api/users/{adminId}/conferences`
**Description:** Get list of conference IDs assigned to an admin.

**Response 200:**
```json
["conf123", "conf456", "conf789"]
```

---

### POST `/api/users/{adminId}/conferences/{conferenceId}`
**Description:** Assign a conference to an admin user.

**Path Parameters:** `adminId`, `conferenceId`
**Response 200:** Updated `User` object with new `conferenceIds`

---

### DELETE `/api/users/{adminId}/conferences/{conferenceId}`
**Description:** Remove a conference from an admin user's assignment.

**Response 200:** Updated `User` object

---

## 3. ConferenceController + ConferenceCountController
**Base path:** `/api/conferences`
**Controller files:** `ConferenceController.java`, `ConferenceCountController.java`

---

### GET `/api/conferences`
**Access:** `SUPER_ADMIN`, `ADMIN`
**Description:** Get all non-deleted conferences.

**Response 200** — Array of `Conference`:
```json
[
  {
    "id":                "conf123",
    "title":             "IEEE Conference 2026",
    "imageUrl":          "https://storage.googleapis.com/bucket/...",
    "imageBlobName":     "conferences/conf123/cover.jpg",
    "status":            "ACTIVE",           // Enum: ACTIVE | INACTIVE
    "dashboardMasterIds":["dm1", "dm2"],
    "createdAt":         "2026-01-10T09:00:00",
    "updatedAt":         "2026-02-01T12:00:00",
    "deleted":           false
  }
]
```

---

### GET `/api/conferences/me`
**Access:** `ADMIN`
**Description:** Get only the conferences assigned to the currently logged-in admin.

**Response 200:** Array of `Conference` objects (same schema).

---

### GET `/api/conferences/{id}`
**Access:** `SUPER_ADMIN`, `ADMIN`
**Description:** Get a single conference by ID.

**Response 200:** Single `Conference` object
**Response 404:** Not found

---

### POST `/api/conferences`
**Access:** `SUPER_ADMIN`
**Description:** Create a new conference.

**Request Body:**
```json
{
  "title":     "IEEE Conference 2026",             // REQUIRED
  "imageUrl":  "https://storage.googleapis.com/...", // OPTIONAL
  "status":    "ACTIVE"                            // OPTIONAL — Enum: ACTIVE | INACTIVE (default ACTIVE)
}
```

**Response 200:** Created `Conference` object

---

### PUT `/api/conferences/{id}`
**Access:** `SUPER_ADMIN`
**Description:** Update a conference.

**Request Body:**
```json
{
  "title":    "Updated Conference Title",   // OPTIONAL
  "imageUrl": "https://...",               // OPTIONAL
  "status":   "INACTIVE"                  // OPTIONAL — Enum: ACTIVE | INACTIVE
}
```

**Response 200:** Updated `Conference` object

---

### PATCH `/api/conferences/{id}/status`
**Access:** `SUPER_ADMIN`
**Description:** Toggle a conference between ACTIVE and INACTIVE.

**Request Body:**
```json
{
  "status": "INACTIVE"    // REQUIRED — Enum: ACTIVE | INACTIVE
}
```

**Response 200:** Updated `Conference` object

---

### DELETE `/api/conferences/{id}`
**Access:** `SUPER_ADMIN`
**Description:** Soft-delete a conference.

**Response 200:** Empty body

---

### GET `/api/conferences/{id}/dashboards`
**Access:** `SUPER_ADMIN`, `ADMIN`
**Description:** Get all dashboard masters linked to this conference.

**Response 200:**
```json
{
  "conferenceId":    "conf123",
  "conferenceTitle": "IEEE 2026",
  "dashboards": [
    { "id": "dm1", "name": "Positive Scan", "status": true }
  ],
  "message": "Dashboards retrieved successfully",
  "success": true
}
```

---

### POST `/api/conferences/{id}/dashboards/attach`
**Access:** `SUPER_ADMIN`
**Description:** Link one or more dashboard masters to a conference.

**Request Body:**
```json
{
  "dashboardMasterIds": ["dm1", "dm2", "dm3"]    // REQUIRED — array of IDs
}
```

**Response 200:** Same as GET dashboards response above.

---

### POST `/api/conferences/{id}/dashboards/detach`
**Access:** `SUPER_ADMIN`
**Description:** Unlink dashboard masters from a conference.

**Request Body:**
```json
{
  "dashboardMasterIds": ["dm2"]    // REQUIRED
}
```

**Response 200:** Updated dashboard list response.

---

### GET `/api/conferences/count`
**Access:** `SUPER_ADMIN`, `ADMIN`
**Description:** Get total count of non-deleted conferences.

**Response 200:**
```json
{ "success": true, "totalConferences": 12 }
```

---

### GET `/api/conferences/{id}/dashboards/count`
**Access:** `SUPER_ADMIN`, `ADMIN`
**Description:** Get count of dashboards linked to a specific conference.

**Response 200:**
```json
{ "success": true, "conferenceId": "conf123", "dashboardCount": 5 }
```

---

## 4. DashboardMasterController
**Base path:** `/api/dashboard-masters`
**Controller file:** `DashboardMasterController.java`

---

### GET `/api/dashboard-masters/types`
**Access:** `SUPER_ADMIN`, `ADMIN`
**Description:** Get all dashboard masters as a typed list.

**Response 200:**
```json
{
  "success": true,
  "total":   3,
  "data": [
    { "id": "dm1", "name": "Positive Scan", "status": true, "createdAt": "...", "updatedAt": "...", "deleted": false }
  ]
}
```

---

### GET `/api/dashboard-masters`
**Access:** `SUPER_ADMIN`, `ADMIN`
**Description:** Get all dashboard masters (raw array).

**Response 200:** Array of `DashboardMaster` objects.

---

### GET `/api/dashboard-masters/{id}`
**Access:** `SUPER_ADMIN`, `ADMIN`
**Description:** Get single dashboard master by ID.

**Response 200:** `DashboardMaster` object
**Response 404:** Empty body

---

### POST `/api/dashboard-masters`
**Access:** `SUPER_ADMIN`
**Description:** Create a new dashboard master type.

**Request Body:**
```json
{
  "name":   "Registration",    // REQUIRED
  "status": true               // OPTIONAL — default true
}
```

**Response 200:** Created `DashboardMaster` object.

---

### PUT `/api/dashboard-masters/{id}`
**Access:** `SUPER_ADMIN`
**Description:** Update a dashboard master.

**Request Body:**
```json
{
  "name":   "Updated Name",    // OPTIONAL
  "status": false              // OPTIONAL
}
```

**Response 200:** Updated `DashboardMaster` object.

---

### DELETE `/api/dashboard-masters/{id}`
**Access:** `SUPER_ADMIN`
**Description:** Soft-delete a dashboard master.

**Response 200:** Empty body.

---

## 5. DashboardDataController
**Base path:** `/api/dashboard-data`
**Controller file:** `DashboardDataController.java`

---

### POST `/api/dashboard-data/upload` ⚡ Async
**Access:** `ADMIN` only
**Description:** Upload an Excel file to bulk-insert attendee records.
**Content-Type:** `multipart/form-data`

> ⚡ Returns immediately (< 100 ms). Processing happens in background via `@Async("bulkTaskExecutor")`.
> Use `uploadId` to poll progress.

**Request Parts:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | `MultipartFile` | ✅ | Excel file (`.xlsx`, `.xls`, `.xlsm`) |
| `conferenceId` | `String` | ✅ | Target conference ID |
| `dashboardMasterId` | `String` | ✅ | Target dashboard master ID |

**Excel File Format Required:**
```
Row 1 (Header): | Name      | Email              |  ← header names are case-insensitive
Row 2+  (Data): | John Doe  | john@example.com   |
```
- `Email` column is **mandatory**; `Name` is optional
- Duplicate emails (within file or already in DB) are auto-skipped
- Email is normalised: trimmed, lowercased, hidden Unicode chars removed

**Response 200** *(immediate, processing started)*:
```json
{
  "status":    "PROCESSING",
  "message":   "Upload started! Data will be stored immediately...",
  "uploadId":  "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "fileName":  "attendees.xlsx",
  "hint":      "Use uploadId to check progress."
}
```

**Response 400:** Unsupported file format
**Response 403:** Admin not assigned to this conference

---

### GET `/api/dashboard-data/upload/progress/{uploadId}`
**Access:** `ADMIN`
**Description:** Poll async upload progress.

**Path Parameters:** `uploadId` — from the upload response

**Response 200** *(in progress)*:
```json
{
  "uploadId":         "a1b2c3d4-...",
  "fileName":         "attendees.xlsx",
  "status":           "PROCESSING",
  "startTime":        "2026-03-04T10:00:00",
  "recordsProcessed": 0,
  "message":          "Upload in progress..."
}
```

**Response 200** *(completed)*:
```json
{
  "uploadId":       "a1b2c3d4-...",
  "status":         "COMPLETED",
  "completionTime": "2026-03-04T10:02:35",
  "message":        "Upload completed successfully!",
  "result": {
    "status":                   "success",
    "totalRecordsInFile":       50000,
    "newRecordsAdded":          48250,
    "duplicateRecordsIgnored":  1700,
    "invalidRowsSkipped":       50,
    "processingTimeMs":         155000,
    "batchesInserted":          5
  }
}
```

**Response 200** *(failed)*:
```json
{
  "status":  "FAILED",
  "message": "Upload failed: <reason>",
  "error":   "<exception message>"
}
```

**Response 200** *(not found/expired)*:
```json
{ "status": "NOT_FOUND", "message": "Upload ID not found or expired" }
```

---

### GET `/api/dashboard-data`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get paginated dashboard data filtered by serial number range.

**Query Parameters:**

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `conferenceId` | String | ✅ | — | Conference ID |
| `dashboardMasterId` | String | ✅ | — | Dashboard master ID |
| `fromSerialNo` | Long | ✅ | — | Start of serial range |
| `toSerialNo` | Long | ✅ | — | End of serial range |
| `page` | int | ❌ | `0` | Page number (0-based) |
| `size` | int | ❌ | `500` | Page size (max capped at 1000) |

**Response 200:**
```json
{
  "data": [
    {
      "id":                "65f1...",
      "conferenceId":      "conf123",
      "dashboardMasterId": "dm1",
      "serialNo":          1,
      "name":              "John Doe",
      "email":             "john@example.com",
      "status":            true,
      "createdAt":         "2026-01-15T10:30:00",
      "updatedAt":         "2026-01-15T10:30:00",
      "deleted":           false
    }
  ],
  "currentPage":  0,
  "pageSize":     500,
  "totalRecords": 48250,     // only present on page=0
  "totalPages":   97,        // only present on page=0
  "fromSerialNo": 1,
  "toSerialNo":   1000
}
```
> `totalRecords` and `totalPages` are only included on **page=0** to avoid extra count queries on subsequent pages. Cache them on the frontend.

---

### GET `/api/dashboard-data/filter`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Advanced filtered view with optional serial range, date range, and email domain.

**Query Parameters:**

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `conferenceId` | String | ✅ | — | Conference ID |
| `dashboardMasterId` | String | ✅ | — | Dashboard master ID |
| `fromSerialNo` | Long | ❌ | — | Serial range start |
| `toSerialNo` | Long | ❌ | — | Serial range end |
| `startDate` | LocalDate (`YYYY-MM-DD`) | ❌ | — | Upload date range start |
| `endDate` | LocalDate (`YYYY-MM-DD`) | ❌ | — | Upload date range end |
| `emailDomain` | String | ❌ | — | Full domain e.g. `gmail.com` |
| `page` | int | ❌ | `0` | Page number |
| `size` | int | ❌ | `500` | Page size (max 1000) |

**Response 200:** Same structure as `/api/dashboard-data`.

---

### GET `/api/dashboard-data/by-date`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get data uploaded within a date range.

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `conferenceId` | String | ✅ | |
| `dashboardMasterId` | String | ✅ | |
| `startDate` | `YYYY-MM-DD` | ✅ | Upload date range start |
| `endDate` | `YYYY-MM-DD` | ✅ | Upload date range end |
| `page` | int | ❌ | default `0` |
| `size` | int | ❌ | default `500`, max `1000` |

**Response 200:** Same paginated structure.

---

### GET `/api/dashboard-data/by-domain-extension`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get data filtered by email TLD (Top-Level Domain) extension with optional serial range. Max 1000 records per request.

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `conferenceId` | String | ✅ | |
| `dashboardMasterId` | String | ✅ | |
| `extension` | String | ✅ | TLD e.g. `com`, `edu`, `org` (leading `.` auto-stripped) |
| `fromSerialNo` | Long | ❌ | Serial range start for pagination |
| `toSerialNo` | Long | ❌ | Serial range end for pagination |

**Response 200:**
```json
{
  "data": [ { ...DashboardData objects... } ],
  "requestedRange": { "from": 1, "to": 10000 },
  "totalMatchingInRange": 2500,
  "recordsReturned":      1000,
  "maxRecordsPerRequest": 1000,
  "hasMoreRecords":       true,
  "nextRangeSuggestion": {
    "fromSerialNo": 1001,
    "toSerialNo":   11000,
    "message":      "Search from serial 1001 to 11000 to get next batch"
  },
  "rangeCoverage": "partial"    // "complete" or "partial"
}
```

---

### GET `/api/dashboard-data/domain-extensions`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get all distinct TLD extensions present in the dataset (cached 5 min).

**Query Parameters:**

| Param | Type | Required |
|-------|------|----------|
| `conferenceId` | String | ✅ |
| `dashboardMasterId` | String | ✅ |

**Response 200:**
```json
{
  "total":      4,
  "extensions": ["com", "edu", "org", "in"]
}
```

---

### GET `/api/dashboard-data/by-email-domain`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get ALL records for an exact email domain (no pagination limit).

**Query Parameters:**

| Param | Type | Required | Example |
|-------|------|----------|---------|
| `conferenceId` | String | ✅ | |
| `dashboardMasterId` | String | ✅ | |
| `emailDomain` | String | ✅ | `gmail.com` |

**Response 200:**
```json
{
  "emailDomain":  "gmail.com",
  "totalRecords": 3200,
  "data":         [ { ...DashboardData... } ]
}
```

---

### GET `/api/dashboard-data/count`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Efficient count query (no data loaded).

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `conferenceId` | String | ✅ | |
| `dashboardMasterId` | String | ✅ | |
| `fromSerialNo` | Long | ❌ | |
| `toSerialNo` | Long | ❌ | |
| `startDate` | `YYYY-MM-DD` | ❌ | |
| `endDate` | `YYYY-MM-DD` | ❌ | |
| `emailDomain` | String | ❌ | |

**Response 200:**
```json
{
  "count":            48250,
  "conferenceId":     "conf123",
  "dashboardMasterId":"dm1"
}
```

---

## 6. ExportController
**Base path:** `/api/export`
**Controller file:** `ExportController.java`
**Access:** `ADMIN`, `SUPER_ADMIN`

> All export endpoints stream the file binary directly to the browser.
> `ADMIN` can only export data from their assigned conferences.

---

### GET `/api/export/excel`
**Description:** Export serial range to Excel file.

| Param | Type | Required |
|-------|------|----------|
| `conferenceId` | String | ✅ |
| `dashboardMasterId` | String | ✅ |
| `fromSerialNo` | Long | ✅ |
| `toSerialNo` | Long | ✅ |

**Response:** Binary `.xlsx` download
**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

---

### GET `/api/export/pdf`
**Description:** Export serial range to PDF file.

| Param | Type | Required |
|-------|------|----------|
| `conferenceId` | String | ✅ |
| `dashboardMasterId` | String | ✅ |
| `fromSerialNo` | Long | ✅ |
| `toSerialNo` | Long | ✅ |

**Response:** Binary `.pdf` download

---

### GET `/api/export/excel/advanced`
**Description:** Export with full filter options to Excel.

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `conferenceId` | String | ✅ | |
| `dashboardMasterId` | String | ✅ | |
| `fromSerialNo` | Long | ❌ | |
| `toSerialNo` | Long | ❌ | |
| `startDate` | `YYYY-MM-DD` | ❌ | |
| `endDate` | `YYYY-MM-DD` | ❌ | |
| `emailDomain` | String | ❌ | e.g. `gmail.com` |

**Response:** Binary `.xlsx` download

---

### GET `/api/export/pdf/advanced`
**Description:** Export with full filter options to PDF.

*Same parameters as `/api/export/excel/advanced`*

**Response:** Binary `.pdf` download

---

### POST `/api/export/excel/filter`
**Description:** Export filtered data to Excel using a JSON request body.

**Request Body:**
```json
{
  "conferenceId":     "conf123",        // REQUIRED
  "dashboardMasterId":"dm1",            // REQUIRED
  "fromSerialNo":     1,                // OPTIONAL
  "toSerialNo":       5000,             // OPTIONAL
  "startDate":        "2025-01-01",     // OPTIONAL — YYYY-MM-DD
  "endDate":          "2025-12-31",     // OPTIONAL — YYYY-MM-DD
  "emailDomain":      "gmail.com"       // OPTIONAL
}
```

**Response:** Binary `.xlsx` download

---

### POST `/api/export/pdf/filter`
**Description:** Export filtered data to PDF using a JSON request body.

**Request Body:** Same as `/api/export/excel/filter`

**Response:** Binary `.pdf` download

---

### GET `/api/export/excel/by-extension`
**Description:** Export TLD-filtered data to Excel. Max 1000 records per request.

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `conferenceId` | String | ✅ | |
| `dashboardMasterId` | String | ✅ | |
| `extension` | String | ✅ | TLD e.g. `com`, `edu` |
| `fromSerialNo` | Long | ❌ | Serial range for pagination |
| `toSerialNo` | Long | ❌ | Serial range for pagination |

**Response:** Binary `.xlsx` download (max 1000 records)

---

### GET `/api/export/pdf/by-extension`
**Description:** Export TLD-filtered data to PDF. Max 1000 records per request.

*Same parameters as `/api/export/excel/by-extension`*

**Response:** Binary `.pdf` download

---

### GET `/api/export/preview`
**Description:** Preview filtered records as JSON (no file download).

*Same parameters as `/api/export/excel/advanced`*

**Response 200:** Array of `DashboardData` objects.

---

### GET `/api/export/email-domains`
**Description:** Get list of distinct full email domains (e.g. `gmail.com`, `yahoo.com`).

| Param | Type | Required |
|-------|------|----------|
| `conferenceId` | String | ✅ |
| `dashboardMasterId` | String | ✅ |

**Response 200:**
```json
["gmail.com", "yahoo.com", "outlook.com", "hotmail.com"]
```

---

## 7. AnalyticsController
**Base path:** `/api/analytics`
**Controller file:** `AnalyticsController.java`

---

### GET `/api/analytics/stream` *(SSE)*
**Access:** `SUPER_ADMIN`
**Description:** Subscribe to real-time log stream for ALL admins.

> ⚠️ Browser `EventSource` cannot send `Authorization` headers — pass JWT as query parameter.

```
GET /api/analytics/stream?token=<JWT_ACCESS_TOKEN>
```

**Produces:** `text/event-stream`

**Event Types:**

| Event | Payload | Trigger |
|-------|---------|---------|
| `connected` | `{"message":"Connected...","role":"SUPER_ADMIN"}` | On subscribe |
| `data-log` | Full `AdminActivityLog` JSON | Every upload/download/view action |
| `doc-log` | Full `ConferenceDocumentLog` JSON | Every document upload/download/delete |

**Frontend Example:**
```javascript
const token = localStorage.getItem('accessToken');
const es = new EventSource(`/api/analytics/stream?token=${token}`);
es.addEventListener('data-log', e => {
  const log = JSON.parse(e.data);
  // log.adminName, log.actionType, log.description, log.createdAt ...
});
es.addEventListener('doc-log', e => { ... });
es.onerror = () => es.close();
```

---

### GET `/api/analytics/stream/me` *(SSE)*
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Subscribe to real-time log stream for own actions only.

```
GET /api/analytics/stream/me?token=<JWT_ACCESS_TOKEN>
```

**Events:** Same as above but filtered to logged-in admin.

---

### GET `/api/analytics/stream/connections`
**Access:** `SUPER_ADMIN`
**Description:** Get current SSE connection count.

**Response 200:**
```json
{
  "superAdminConnections": 2,
  "adminConnections": { "adminId1": 1, "adminId2": 2 }
}
```

---

### GET `/api/analytics/upload-stats/me`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get upload statistics for the currently logged-in user.

**Response 200:**
```json
{
  "success": true,
  "count":   12,
  "data": [
    {
      "id":                       "stat123",
      "adminId":                  "65f1...",
      "conferenceId":             "conf123",
      "dashboardMasterId":        "dm1",
      "fileName":                 "attendees.xlsx",
      "totalRecordsInFile":       5000,
      "newRecordsAdded":          4800,
      "duplicateRecordsIgnored":  200,
      "uploadedAt":               "2026-03-04T10:00:00"
    }
  ]
}
```

---

### GET `/api/analytics/upload-stats/me/conference/{conferenceId}`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get own upload stats filtered to a specific conference.

**Response 200:** Same structure as above.

---

### GET `/api/analytics/logs/me`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get activity logs for the currently logged-in user.

**Response 200:**
```json
{
  "success": true,
  "count":   25,
  "data": [
    {
      "id":               "log123",
      "adminId":          "65f1...",
      "adminName":        "John Doe",
      "conferenceId":     "conf123",
      "dashboardMasterId":"dm1",
      "actionType":       "UPLOAD_EXCEL",   // See ActionType enum at top
      "description":      "Uploaded Excel: attendees.xlsx | Added: 4800 | Duplicates: 200",
      "ipAddress":        "127.0.0.1",
      "fromSerialNo":     null,
      "toSerialNo":       null,
      "totalRecords":     null,
      "emailDomain":      null,
      "filterSummary":    null,
      "tldExtension":     null,
      "createdAt":        "2026-03-04T10:02:35"
    }
  ]
}
```

---

### GET `/api/analytics/logs/me/conference/{conferenceId}`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get own logs filtered to a specific conference.

**Response 200:** Same structure as above.

---

### GET `/api/analytics/upload-stats` *(SUPER_ADMIN)*
**Access:** `SUPER_ADMIN`
**Description:** Get all upload stats across all admins.

**Response 200:** Same structure as `/upload-stats/me`.

---

### GET `/api/analytics/upload-stats/admin/{adminId}` *(SUPER_ADMIN)*
**Access:** `SUPER_ADMIN`
**Description:** Get upload stats for a specific admin.

**Response 200:** Filtered by `adminId`.

---

### GET `/api/analytics/upload-stats/conference/{conferenceId}` *(SUPER_ADMIN)*
**Access:** `SUPER_ADMIN`
**Description:** Get upload stats for all admins on a specific conference.

**Response 200:** Filtered by `conferenceId`.

---

### GET `/api/analytics/logs` *(SUPER_ADMIN)*
**Access:** `SUPER_ADMIN`
**Description:** Get all activity logs across all admins.

**Response 200:** Same structure as `/logs/me`.

---

### GET `/api/analytics/logs/admin/{adminId}` *(SUPER_ADMIN)*
**Access:** `SUPER_ADMIN`
**Description:** Get all logs for a specific admin.

---

### GET `/api/analytics/logs/conference/{conferenceId}` *(SUPER_ADMIN)*
**Access:** `SUPER_ADMIN`
**Description:** Get all logs for a specific conference.

---

## 8. ConferenceDocumentController
**Base path:** `/api/conference-documents`
**Controller file:** `ConferenceDocumentController.java`

---

### POST `/api/conference-documents/upload`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Upload a document for a specific conference + year + document type.
**Content-Type:** `multipart/form-data`

> If a document already exists for the same `(conferenceId, year, documentType)` it is **replaced** (old file deleted from GCS).

**Request Parts:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | `MultipartFile` | ✅ | Any file type |
| `conferenceId` | String | ✅ | Conference ID |
| `year` | Integer | ✅ | Year (e.g. `2026`) |
| `documentType` | String | ✅ | Type slug e.g. `program`, `book`, `positive_sheets` |

**Response 200:**
```json
{
  "success": true,
  "message": "Document uploaded successfully. If a previous version existed, it has been replaced.",
  "data": {
    "id":                      "doc123",
    "conferenceId":            "conf123",
    "conferenceName":          "IEEE 2026",
    "year":                    2026,
    "documentType":            "program",           // slug
    "documentTypeDisplayName": "Program",           // human-readable
    "fileName":                "program_2026.xlsx",
    "blobName":                "documents/conf123/2026/program/program_2026.xlsx",
    "filePath":                "documents/conf123/2026/program/program_2026.xlsx",
    "publicUrl":               "https://storage.googleapis.com/...",
    "fileSize":                204800,
    "contentType":             "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "uploadedAt":              "2026-03-04T10:00:00",
    "updatedAt":               "2026-03-04T10:00:00",
    "uploadedByUserId":        "65f1...",
    "uploadedByUserName":      "John Doe"
  }
}
```

---

### GET `/api/conference-documents`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get all documents for a specific conference.

| Param | Type | Required |
|-------|------|----------|
| `conferenceId` | String | ✅ |

**Response 200:**
```json
{
  "success":        true,
  "totalDocuments": 6,
  "data":           [ { ...ConferenceDocumentResponse... } ]
}
```

---

### GET `/api/conference-documents/year`
**Access:** `SUPER_ADMIN`
**Description:** Get all documents for a specific year (all conferences).

| Param | Type | Required |
|-------|------|----------|
| `year` | Integer | ✅ |

**Response 200:** `{ "success": true, "data": [...] }`

---

### GET `/api/conference-documents/type`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get all documents of a specific type slug for a conference.

| Param | Type | Required | Example |
|-------|------|----------|---------|
| `conferenceId` | String | ✅ | |
| `documentType` | String | ✅ | `program` |

**Response 200:** `{ "success": true, "data": [...] }`

---

### GET `/api/conference-documents/{id}`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get a single document by its ID.

**Response 200:** `{ "success": true, "data": { ...ConferenceDocumentResponse... } }`
**Response 404:** `{ "success": false, "message": "Document not found" }`

---

### GET `/api/conference-documents/{id}/download`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Download the file binary from GCS.

**Response:** Binary file stream
**Headers:** `Content-Disposition: attachment; filename="program_2026.xlsx"`

---

### DELETE `/api/conference-documents/{id}`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Delete document from GCS and database.

**Response 200:** `{ "success": true, "message": "Document deleted successfully" }`
**Response 404:** `{ "success": false, "message": "Document not found" }`

---

### POST `/api/conference-documents/search`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Search documents with optional filters (paginated).

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `conferenceId` | String | ❌ | Filter by conference |
| `conferenceName` | String | ❌ | Filter by conference name |
| `year` | Integer | ❌ | Filter by year |
| `documentType` | String | ❌ | Filter by type slug |
| `pageNumber` | int | ❌ | default `0` |
| `pageSize` | int | ❌ | default `10` |

**Response 200:**
```json
{
  "success":       true,
  "pageNumber":    0,
  "pageSize":      10,
  "totalElements": 6,
  "totalPages":    1,
  "data":          [ { ...ConferenceDocumentResponse... } ]
}
```

---

### GET `/api/conference-documents/available-years`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get all years that have documents for a conference.

| Param | Type | Required |
|-------|------|----------|
| `conferenceId` | String | ✅ |

**Response 200:**
```json
{ "success": true, "conferenceId": "conf123", "availableYears": [2024, 2025, 2026] }
```

---

### GET `/api/conference-documents/{id}/logs`
**Access:** `SUPER_ADMIN`
**Description:** Get access logs for a specific document.

**Response 200:** Array of `ConferenceDocumentLog`:
```json
[
  {
    "id":             "cdl123",
    "adminId":        "65f1...",
    "adminName":      "John Doe",
    "conferenceId":   "conf123",
    "conferenceName": "IEEE 2026",
    "documentId":     "doc123",
    "fileName":       "program_2026.xlsx",
    "documentType":   "program",
    "year":           2026,
    "actionType":     "DOWNLOAD",    // Enum: UPLOAD | DOWNLOAD | DELETE | VIEW
    "description":    "Downloaded program_2026.xlsx",
    "ipAddress":      "127.0.0.1",
    "createdAt":      "2026-03-04T11:00:00"
  }
]
```

---

### GET `/api/conference-documents/logs/conference/{conferenceId}`
**Access:** `SUPER_ADMIN`
**Description:** Get all document logs for a conference.

**Response 200:** Array of `ConferenceDocumentLog` objects.

---

## 9. DocumentTypeController
**Base path:** `/api/document-types`
**Controller file:** `DocumentTypeController.java`

---

### GET `/api/document-types/active`
**Access:** `ADMIN`, `SUPER_ADMIN`
**Description:** Get all active document types (for upload dropdown).

**Response 200:**
```json
{
  "success": true,
  "total":   3,
  "data": [
    {
      "id":               "dt1",
      "slug":             "program",
      "displayName":      "Program",
      "description":      "Conference program document",
      "folderName":       "program",
      "sortOrder":        1,
      "active":           true,
      "createdAt":        "2026-01-01T00:00:00",
      "updatedAt":        "2026-01-01T00:00:00",
      "createdByUserId":  "65f1...",
      "createdByUserName":"Super Admin"
    }
  ]
}
```

---

### GET `/api/document-types`
**Access:** `SUPER_ADMIN`
**Description:** Get all document types including inactive ones.

**Response 200:** Same structure (includes entries with `"active": false`).

---

### GET `/api/document-types/{id}`
**Access:** `SUPER_ADMIN`
**Description:** Get a single document type by ID.

**Response 200:** `{ "success": true, "data": { ...DocumentTypeResponse... } }`

---

### POST `/api/document-types`
**Access:** `SUPER_ADMIN`
**Description:** Create a new document type. The `slug` and `folderName` are auto-generated from `displayName`.

**Request Body:**
```json
{
  "displayName": "Positive Sheets",    // REQUIRED — 1-100 chars — slug auto-generated as "positive_sheets"
  "description": "Positive scan data", // OPTIONAL — max 500 chars
  "sortOrder":   3,                    // OPTIONAL — UI dropdown order; auto-assigned if omitted
  "active":      true                  // OPTIONAL — default true
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Document type 'Positive Sheets' created with slug 'positive_sheets'",
  "data":    { ...DocumentTypeResponse... }
}
```

---

### PUT `/api/document-types/{id}`
**Access:** `SUPER_ADMIN`
**Description:** Update an existing document type. Slug is recalculated if `displayName` changes.

**Request Body:**
```json
{
  "displayName": "Updated Name",    // OPTIONAL
  "description": "New description", // OPTIONAL
  "sortOrder":   5,                 // OPTIONAL
  "active":      true               // OPTIONAL
}
```

**Response 200:** `{ "success": true, "message": "...", "data": { ...DocumentTypeResponse... } }`

---

### PATCH `/api/document-types/{id}/toggle`
**Access:** `SUPER_ADMIN`
**Description:** Toggle active/inactive status.

**Response 200:**
```json
{
  "success": true,
  "message": "Document type 'Program' has been deactivated",
  "data":    { ...DocumentTypeResponse... }
}
```

---

### DELETE `/api/document-types/{id}`
**Access:** `SUPER_ADMIN`
**Description:** Soft-delete a document type.

**Response 200:** `{ "success": true, "message": "Document type deleted successfully" }`

---

## 10. FileUploadController
**Base path:** `/api/files`
**Controller file:** `FileUploadController.java`
**Access:** `ADMIN`, `SUPER_ADMIN`

Used for general-purpose file storage (e.g., conference images) on GCS.

---

### POST `/api/files/upload`
**Content-Type:** `multipart/form-data`
**Description:** Upload a single file to GCS.

| Field | Type | Required |
|-------|------|----------|
| `file` | `MultipartFile` | ✅ |
| `conferenceId` | String | ✅ |
| `dashboardMasterId` | String | ✅ |

**Response 200:**
```json
{
  "success":     true,
  "message":     "File uploaded successfully",
  "fileUrl":     "https://storage.googleapis.com/...",
  "fileName":    "image.jpg",
  "fileSize":    204800,
  "contentType": "image/jpeg"
}
```

---

### POST `/api/files/upload-multiple`
**Content-Type:** `multipart/form-data`
**Description:** Upload multiple files at once.

| Field | Type | Required |
|-------|------|----------|
| `files` | `MultipartFile[]` | ✅ (array) |
| `conferenceId` | String | ✅ |
| `dashboardMasterId` | String | ✅ |

**Response 200:**
```json
{
  "success":      true,
  "totalFiles":   3,
  "successCount": 3,
  "failCount":    0,
  "uploadedFiles": [
    { "fileName": "img1.jpg", "fileUrl": "https://...", "success": true },
    { "fileName": "img2.jpg", "fileUrl": "https://...", "success": true }
  ]
}
```

---

### GET `/api/files`
**Description:** List all files for a conference/dashboard.

| Param | Type | Required |
|-------|------|----------|
| `conferenceId` | String | ✅ |
| `dashboardMasterId` | String | ✅ |

**Response 200:** Array of file objects with URLs.

---

### GET `/api/files/download`
**Description:** Download a file from GCS by blob name.

| Param | Type | Required |
|-------|------|----------|
| `blobName` | String | ✅ | GCS blob path |

**Response:** Binary file stream.

---

### DELETE `/api/files`
**Description:** Delete a file from GCS.

| Param | Type | Required |
|-------|------|----------|
| `blobName` | String | ✅ |

**Response 200:** `{ "success": true, "message": "File deleted successfully" }`

---

## Quick Reference — All Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/auth/signin` | Public | Login |
| POST | `/api/auth/refresh-token` | Public | Refresh access token |
| POST | `/api/auth/logout` | Any | Logout |
| POST | `/api/auth/forgot-password` | Public | Send reset email |
| GET | `/api/auth/validate-reset-token` | Public | Validate reset token |
| POST | `/api/auth/reset-password` | Public | Reset password |
| POST | `/api/auth/forgot-username` | Public | Send username reminder |
| POST | `/api/auth/change-password` | Any | Change own password |
| GET | `/api/auth/token-expiration` | Public | Token TTL info |
| GET | `/api/users/admins` | SA | List all admins |
| GET | `/api/users/admins/conferences/all` | SA | Admins with conferences |
| GET | `/api/users/super-admins` | SA | List super admins |
| GET | `/api/users/{id}` | SA | Get user by ID |
| POST | `/api/users` | SA | Create user |
| PUT | `/api/users/{id}` | SA | Update user |
| DELETE | `/api/users/{id}` | SA | Delete user |
| PATCH | `/api/users/{id}/status` | SA | Toggle user status |
| GET | `/api/users/{adminId}/conferences` | SA | Admin's conferences |
| POST | `/api/users/{adminId}/conferences/{cId}` | SA | Assign conference |
| DELETE | `/api/users/{adminId}/conferences/{cId}` | SA | Remove conference |
| GET | `/api/conferences` | SA, A | List conferences |
| GET | `/api/conferences/me` | A | My conferences |
| GET | `/api/conferences/{id}` | SA, A | Get conference |
| POST | `/api/conferences` | SA | Create conference |
| PUT | `/api/conferences/{id}` | SA | Update conference |
| PATCH | `/api/conferences/{id}/status` | SA | Toggle status |
| DELETE | `/api/conferences/{id}` | SA | Delete conference |
| GET | `/api/conferences/{id}/dashboards` | SA, A | Conference dashboards |
| POST | `/api/conferences/{id}/dashboards/attach` | SA | Attach dashboards |
| POST | `/api/conferences/{id}/dashboards/detach` | SA | Detach dashboards |
| GET | `/api/conferences/count` | SA, A | Conference count |
| GET | `/api/conferences/{id}/dashboards/count` | SA, A | Dashboard count |
| GET | `/api/dashboard-masters` | SA, A | List masters |
| GET | `/api/dashboard-masters/types` | SA, A | Typed list |
| GET | `/api/dashboard-masters/{id}` | SA, A | Get by ID |
| POST | `/api/dashboard-masters` | SA | Create |
| PUT | `/api/dashboard-masters/{id}` | SA | Update |
| DELETE | `/api/dashboard-masters/{id}` | SA | Delete |
| POST | `/api/dashboard-data/upload` | **A only** | Upload Excel (async) |
| GET | `/api/dashboard-data/upload/progress/{id}` | A | Poll progress |
| GET | `/api/dashboard-data` | SA, A | Get by serial range |
| GET | `/api/dashboard-data/filter` | SA, A | Advanced filter |
| GET | `/api/dashboard-data/by-date` | SA, A | By date range |
| GET | `/api/dashboard-data/by-domain-extension` | SA, A | By TLD extension |
| GET | `/api/dashboard-data/domain-extensions` | SA, A | Distinct TLDs |
| GET | `/api/dashboard-data/by-email-domain` | SA, A | By exact domain |
| GET | `/api/dashboard-data/count` | SA, A | Count records |
| GET | `/api/export/excel` | SA, A | Export Excel (range) |
| GET | `/api/export/pdf` | SA, A | Export PDF (range) |
| GET | `/api/export/excel/advanced` | SA, A | Export Excel (filters) |
| GET | `/api/export/pdf/advanced` | SA, A | Export PDF (filters) |
| POST | `/api/export/excel/filter` | SA, A | Export Excel (JSON body) |
| POST | `/api/export/pdf/filter` | SA, A | Export PDF (JSON body) |
| GET | `/api/export/excel/by-extension` | SA, A | Export Excel (TLD) |
| GET | `/api/export/pdf/by-extension` | SA, A | Export PDF (TLD) |
| GET | `/api/export/preview` | SA, A | Preview JSON |
| GET | `/api/export/email-domains` | SA, A | Distinct domains |
| GET | `/api/analytics/stream` | SA | SSE all logs |
| GET | `/api/analytics/stream/me` | SA, A | SSE own logs |
| GET | `/api/analytics/stream/connections` | SA | SSE connection count |
| GET | `/api/analytics/upload-stats/me` | SA, A | My upload stats |
| GET | `/api/analytics/upload-stats/me/conference/{cId}` | SA, A | My stats by conference |
| GET | `/api/analytics/upload-stats` | SA | All upload stats |
| GET | `/api/analytics/upload-stats/admin/{adminId}` | SA | Stats by admin |
| GET | `/api/analytics/upload-stats/conference/{cId}` | SA | Stats by conference |
| GET | `/api/analytics/logs/me` | SA, A | My activity logs |
| GET | `/api/analytics/logs/me/conference/{cId}` | SA, A | My logs by conference |
| GET | `/api/analytics/logs` | SA | All activity logs |
| GET | `/api/analytics/logs/admin/{adminId}` | SA | Logs by admin |
| GET | `/api/analytics/logs/conference/{cId}` | SA | Logs by conference |
| POST | `/api/conference-documents/upload` | SA, A | Upload document |
| GET | `/api/conference-documents` | SA, A | List documents |
| GET | `/api/conference-documents/year` | SA | By year |
| GET | `/api/conference-documents/type` | SA, A | By type slug |
| GET | `/api/conference-documents/{id}` | SA, A | Get by ID |
| GET | `/api/conference-documents/{id}/download` | SA, A | Download file |
| DELETE | `/api/conference-documents/{id}` | SA, A | Delete document |
| POST | `/api/conference-documents/search` | SA, A | Search with filters |
| GET | `/api/conference-documents/available-years` | SA, A | Available years |
| GET | `/api/conference-documents/{id}/logs` | SA | Document logs |
| GET | `/api/conference-documents/logs/conference/{cId}` | SA | All logs by conference |
| GET | `/api/document-types/active` | SA, A | Active types |
| GET | `/api/document-types` | SA | All types |
| GET | `/api/document-types/{id}` | SA | Get by ID |
| POST | `/api/document-types` | SA | Create type |
| PUT | `/api/document-types/{id}` | SA | Update type |
| PATCH | `/api/document-types/{id}/toggle` | SA | Toggle active |
| DELETE | `/api/document-types/{id}` | SA | Delete type |
| POST | `/api/files/upload` | SA, A | Upload single file |
| POST | `/api/files/upload-multiple` | SA, A | Upload multiple files |
| GET | `/api/files` | SA, A | List files |
| GET | `/api/files/download` | SA, A | Download file |
| DELETE | `/api/files` | SA, A | Delete file |

> **SA** = `SUPER_ADMIN` &nbsp;|&nbsp; **A** = `ADMIN`

