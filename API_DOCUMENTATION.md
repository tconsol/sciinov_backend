# 📚 REST API Documentation

**Base URL**: `http://localhost:8080/api`
**Auth Header**: `Authorization: Bearer <jwt_token>`

---

## 1️⃣ Authentication

### 🔹 Login
**POST** `/auth/signin`
*Public Access*

**Request Body:**
```json
{
  "userId": "superadmin",
  "password": "admin123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": "65e1234567890",
  "userId": "superadmin",
  "email": "superadmin@example.com",
  "roles": ["ROLE_SUPER_ADMIN"]
}
```

### 🔹 Forgot Password (Request Reset Token)
**POST** `/auth/forgot-password`
*Public Access*

**Request Body:**
```json
{
  "userId": "johndoe"
}
```

**Response (200 OK):**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "johndoe",
  "email": "john@example.com",
  "message": "Password reset token generated successfully. Token is valid for 24 hours.",
  "success": true
}
```

### 🔹 Validate Reset Token
**GET** `/auth/validate-reset-token?token={token}`
*Public Access*

**Response (200 OK):**
```json
{
  "message": "Token is valid",
  "success": true
}
```

### 🔹 Reset Password
**POST** `/auth/reset-password`
*Public Access*

**Request Body:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**Response (200 OK):**
```json
{
  "message": "Password has been reset successfully. You can now login with your new password.",
  "success": true
}
```

### 🔹 Forgot Username
**POST** `/auth/forgot-username`
*Public Access*

**Request Body (by email):**
```json
{
  "email": "john@example.com"
}
```

**OR Request Body (by phone):**
```json
{
  "phoneNumber": "9876543210"
}
```

**Response (200 OK):**
```json
{
  "userId": "johndoe",
  "maskedUserId": "jo***e",
  "email": "john@example.com",
  "maskedEmail": "jo***@example.com",
  "message": "Username found! Your User ID has been sent to your registered email/phone.",
  "success": true
}
```

### 🔹 Change Password (For Logged-in User)
**POST** `/auth/change-password`
*Access: ADMIN / SUPER_ADMIN (Authenticated)*

**Request Body:**
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**Response (200 OK):**
```json
{
  "message": "Password changed successfully",
  "success": true
}
```

---

## 2️⃣ Conference Management
*Access: SUPER_ADMIN (Write), ADMIN (Read)*

### 🔹 Get All Conferences
**GET** `/conferences`

**Response:**
```json
[
  {
    "id": "65e...",
    "title": "Tech Summit 2024",
    "imageUrl": "http://image.url/logo.png",
    "status": "ACTIVE",
    "createdAt": "2024-03-01T10:00:00",
    "updatedAt": "2024-03-01T10:00:00",
    "deleted": false
  }
]
```

### 🔹 Create Conference
**POST** `/conferences`
*Super Admin Only*

**Request Body:**
```json
{
  "title": "Medical Expo 2024",
  "imageUrl": "http://images.com/med.png",
  "status": "ACTIVE"
}
```

### 🔹 Update Conference
**PUT** `/conferences/{id}`
*Super Admin Only*

**Request Body:**
```json
{
  "title": "Medical Expo 2024 Updated",
  "imageUrl": "http://images.com/med_new.png",
  "status": "INACTIVE"
}
```

### 🔹 Delete Conference (Soft Delete)
**DELETE** `/conferences/{id}`
*Super Admin Only*

---

## 3️⃣ Dashboard Master (Types)
*Access: SUPER_ADMIN (Write), ADMIN (Read)*

### 🔹 Get All Types
**GET** `/dashboard-masters`

**Response:**
```json
[
  {
    "id": "65f...",
    "name": "University",
    "status": true
  },
  {
    "id": "65f...",
    "name": "Sponsors",
    "status": true
  }
]
```

### 🔹 Create Type
**POST** `/dashboard-masters`
*Super Admin Only*

**Request Body:**
```json
{
  "name": "Exhibitors",
  "status": true
}
```

---

## 4️⃣ User Management (Admins)
*Access: SUPER_ADMIN Only*

### 🔹 Create User (Admin/Super Admin)
**POST** `/users`

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "userId": "johndoe",
  "email": "john@example.com",
  "phoneNumber": "9876543210",
  "password": "password123",
  "role": "ADMIN",
  "status": true,
  "conferenceIds": ["65e...", "65f..."] 
}
```
*(Note: `conferenceIds` is required only for ADMIN role)*

### 🔹 Get All Admins
**GET** `/users/admins`

### 🔹 Get All Super Admins
**GET** `/users/super-admins`

### 🔹 Update User
**PUT** `/users/{id}`

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Smith",
  "email": "john.new@example.com",
  "phoneNumber": "9876543210",
  "status": true,
  "conferenceIds": ["65e..."]
}
```

### 🔹 Delete User
**DELETE** `/users/{id}`

---

## 5️⃣ Dashboard Data (Excel Operations)

### 🔹 Upload Excel
**POST** `/dashboard-data/upload`
*Access: ADMIN Only*
*Content-Type: multipart/form-data*

**Params:**
- `file`: (The Excel file)
- `conferenceId`: `65e...`
- `dashboardMasterId`: `65f...`

**Excel File Format:**
| Column A | Column B | Column C (Optional) | Column D (Optional) |
|----------|----------|---------------------|---------------------|
| Name     | Email    | Region              | Country             |
| Alice    | alice@e..| USA                 | United States       |
| Bob      | bob@e... | UK                  | United Kingdom      |

*Note: First row is header (skipped), Region and Country columns are optional*

**Response:**
```json
"File uploaded successfully!"
```

### 🔹 Fetch Data (By Serial Range)
**GET** `/dashboard-data`
*Access: ADMIN (Assigned Only) / SUPER_ADMIN*

**Query Params:**
- `conferenceId`: `65e...`
- `dashboardMasterId`: `65f...`
- `fromSerialNo`: `1`
- `toSerialNo`: `100`

### 🔹 Fetch Data with Advanced Filters
**GET** `/dashboard-data/filter`
*Access: ADMIN (Assigned Only) / SUPER_ADMIN*

**Query Params:**
- `conferenceId`: `65e...` (required)
- `dashboardMasterId`: `65f...` (required)
- `fromSerialNo`: `1` (optional)
- `toSerialNo`: `100` (optional)
- `startDate`: `2026-01-01` (optional, ISO date format)
- `endDate`: `2026-01-31` (optional, ISO date format)
- `region`: `USA` (optional, case-insensitive)
- `country`: `United States` (optional, case-insensitive)

### 🔹 Fetch Data by Date Range
**GET** `/dashboard-data/by-date`
*Access: ADMIN (Assigned Only) / SUPER_ADMIN*

**Query Params:**
- `conferenceId`: `65e...`
- `dashboardMasterId`: `65f...`
- `startDate`: `2026-01-01`
- `endDate`: `2026-01-31`

### 🔹 Fetch Data by Region
**GET** `/dashboard-data/by-region`
*Access: ADMIN (Assigned Only) / SUPER_ADMIN*

**Query Params:**
- `conferenceId`: `65e...`
- `dashboardMasterId`: `65f...`
- `region`: `USA`

### 🔹 Fetch Data by Country
**GET** `/dashboard-data/by-country`
*Access: ADMIN (Assigned Only) / SUPER_ADMIN*

**Query Params:**
- `conferenceId`: `65e...`
- `dashboardMasterId`: `65f...`
- `country`: `United States`

### 🔹 Get Filtered Data Count
**GET** `/dashboard-data/count`
*Access: ADMIN (Assigned Only) / SUPER_ADMIN*

**Query Params:** Same as `/dashboard-data/filter`

**Response:**
```json
150
```

**Response:**
```json
[
  {
    "id": "66a...",
    "conferenceId": "65e...",
    "dashboardMasterId": "65f...",
    "serialNo": 1,
    "name": "Alice Johnson",
    "email": "alice@example.com",
    "status": true,
    "createdAt": "..."
  },
  {
    "id": "66b...",
    "serialNo": 2,
    "name": "Bob Smith",
    "email": "bob@example.com",
    ...
  }
]
```

---

## 6️⃣ Data Export
*Access: ADMIN (Assigned Only) / SUPER_ADMIN*

### 🔹 Export to Excel
**GET** `/export/excel`

**Query Params:**
- `conferenceId`: `...`
- `dashboardMasterId`: `...`
- `fromSerialNo`: `1`
- `toSerialNo`: `500`

**Response:** Binary File (`.xlsx`)

### 🔹 Export to PDF
**GET** `/export/pdf`

**Query Params:** Same as above.

**Response:** Binary File (`.pdf`)

### 🔹 Advanced Export to Excel (with Date & Region Filters)
**GET** `/export/excel/advanced`

**Query Params:**
- `conferenceId`: `...` (required)
- `dashboardMasterId`: `...` (required)
- `fromSerialNo`: `1` (optional)
- `toSerialNo`: `500` (optional)
- `startDate`: `2026-01-01` (optional, ISO date format)
- `endDate`: `2026-01-31` (optional, ISO date format)
- `region`: `USA` (optional, case-insensitive)
- `country`: `United States` (optional, case-insensitive)

**Response:** Binary File (`.xlsx`)

**Example - Download January 2026 data:**
```
/export/excel/advanced?conferenceId=65e...&dashboardMasterId=65f...&startDate=2026-01-01&endDate=2026-01-31
```

**Example - Download USA region data:**
```
/export/excel/advanced?conferenceId=65e...&dashboardMasterId=65f...&region=USA
```

**Example - Combined (January 2026 + USA):**
```
/export/excel/advanced?conferenceId=65e...&dashboardMasterId=65f...&startDate=2026-01-01&endDate=2026-01-31&region=USA
```

### 🔹 Advanced Export to PDF (with Date & Region Filters)
**GET** `/export/pdf/advanced`

**Query Params:** Same as `/export/excel/advanced`

**Response:** Binary File (`.pdf`)

### 🔹 Export to Excel with Filter (POST)
**POST** `/export/excel/filter`

**Request Body:**
```json
{
  "conferenceId": "65e...",
  "dashboardMasterId": "65f...",
  "fromSerialNo": 1,
  "toSerialNo": 500,
  "startDate": "2026-01-01",
  "endDate": "2026-01-31",
  "region": "USA",
  "country": "United States"
}
```

**Response:** Binary File (`.xlsx`)

### 🔹 Export to PDF with Filter (POST)
**POST** `/export/pdf/filter`

**Request Body:** Same as `/export/excel/filter`

**Response:** Binary File (`.pdf`)

### 🔹 Preview Filtered Data
**GET** `/export/preview`
*Use this to preview data before downloading*

**Query Params:** Same as `/export/excel/advanced`

**Response:**
```json
[
  {
    "id": "66a...",
    "conferenceId": "65e...",
    "dashboardMasterId": "65f...",
    "serialNo": 1,
    "name": "Alice Johnson",
    "email": "alice@example.com",
    "region": "USA",
    "country": "United States",
    "status": true,
    "createdAt": "2026-01-15T10:30:00"
  }
]
```

### 🔹 Get Distinct Regions
**GET** `/export/regions`
*Get list of available regions for filter dropdown*

**Query Params:**
- `conferenceId`: `...`
- `dashboardMasterId`: `...`

**Response:**
```json
["USA", "UK", "India", "Germany", "Australia"]
```

### 🔹 Get Distinct Countries
**GET** `/export/countries`
*Get list of available countries for filter dropdown*

**Query Params:**
- `conferenceId`: `...`
- `dashboardMasterId`: `...`

**Response:**
```json
["United States", "United Kingdom", "India", "Germany", "Australia"]
```

---

## 7️⃣ Analytics & Logs
*Access: SUPER_ADMIN Only*

### 🔹 Get Upload Stats by Admin
**GET** `/analytics/upload-stats/admin/{adminId}`

**Response:**
```json
[
  {
    "id": "...",
    "adminId": "...",
    "conferenceId": "...",
    "uploadedAt": "2024-03-01T12:00:00",
    "totalRecordsInFile": 150,
    "newRecordsAdded": 140,
    "duplicateRecordsIgnored": 10,
    "fileName": "students_data.xlsx"
  }
]
```

### 🔹 Get All Activity Logs
**GET** `/analytics/logs`

**Response:**
```json
[
  {
    "id": "...",
    "adminId": "...",
    "actionType": "UPLOAD_EXCEL",
    "description": "Uploaded Excel: data.xlsx. Added: 50",
    "ipAddress": "127.0.0.1",
    "createdAt": "..."
  },
  {
    "id": "...",
    "actionType": "DOWNLOAD_PDF",
    "description": "Exported data",
    ...
  }
]
```

### 🔹 Get Logs by Admin
**GET** `/analytics/logs/admin/{adminId}`
