# 📚 SciInov DBMS - Complete API Documentation

**Version**: 0.0.1-SNAPSHOT  
**Base URL**: `http://localhost:8080/api`  
**Authentication**: JWT Bearer Token  
**Date**: February 17, 2026

---

## 📋 Table of Contents

1. [Authentication APIs](#1-authentication-apis)
2. [Conference Management APIs](#2-conference-management-apis)
3. [Dashboard Master APIs](#3-dashboard-master-apis)
4. [User Management APIs](#4-user-management-apis)
5. [Dashboard Data APIs](#5-dashboard-data-apis)
6. [Export APIs](#6-export-apis)
7. [File Upload APIs](#7-file-upload-apis)
8. [Analytics APIs](#8-analytics-apis)

---

## 1️⃣ Authentication APIs

Base URL: `/api/auth`

### 1.1 User Login

**Endpoint**: `POST /signin`  
**Access**: Public  
**Description**: Authenticate user and receive JWT token

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "userId": "superadmin",
  "password": "admin123"
}
```

**Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdXBlcmFkbWluIiwiaWF0IjoxNjA1MzU5MjQwLCJleHAiOjE2MDU0NDU2NDB9.ABC123...",
  "type": "Bearer",
  "id": "65e1234567890abcdef12345",
  "userId": "superadmin",
  "email": "superadmin@example.com",
  "roles": ["ROLE_SUPER_ADMIN"]
}
```

**Error Response (401 Unauthorized)**:
```json
{
  "timestamp": "2026-02-17T12:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Bad credentials"
}
```

---

### 1.2 Forgot Password

**Endpoint**: `POST /forgot-password`  
**Access**: Public  
**Description**: Generate password reset token by user ID

**Request Body**:
```json
{
  "userId": "johndoe"
}
```

**Response (200 OK)**:
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "johndoe",
  "email": "john@example.com",
  "message": "Password reset link has been sent to your registered email. Token is valid for 24 hours.",
  "success": true
}
```

**Error Response (400 Bad Request)**:
```json
{
  "token": null,
  "userId": null,
  "email": null,
  "message": "User not found with the provided User ID",
  "success": false
}
```

---

### 1.3 Validate Reset Token

**Endpoint**: `GET /validate-reset-token`  
**Access**: Public  
**Description**: Validate if password reset token is still valid

**Query Parameters**:
```
token=550e8400-e29b-41d4-a716-446655440000
```

**Response (200 OK)**:
```json
{
  "message": "Token is valid",
  "success": true
}
```

**Error Response (400 Bad Request)**:
```json
{
  "message": "Token has expired. Please request a new password reset.",
  "success": false
}
```

---

### 1.4 Reset Password

**Endpoint**: `POST /reset-password`  
**Access**: Public  
**Description**: Reset password using valid token

**Request Body**:
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**Response (200 OK)**:
```json
{
  "message": "Password has been reset successfully. You can now login with your new password.",
  "success": true
}
```

**Error Response (400 Bad Request)**:
```json
{
  "message": "Passwords do not match",
  "success": false
}
```

---

### 1.5 Forgot Username

**Endpoint**: `POST /forgot-username`  
**Access**: Public  
**Description**: Recover username by email or phone number

**Request Body (Option 1 - Email)**:
```json
{
  "email": "john@example.com"
}
```

**Request Body (Option 2 - Phone)**:
```json
{
  "phoneNumber": "9876543210"
}
```

**Response (200 OK)**:
```json
{
  "userId": "johndoe",
  "email": "john@example.com",
  "message": "Your User ID has been sent to your registered email.",
  "success": true
}
```

**Error Response (400 Bad Request)**:
```json
{
  "userId": null,
  "email": null,
  "message": "No user found with the provided email or phone number",
  "success": false
}
```

---

### 1.6 Change Password (Authenticated)

**Endpoint**: `POST /change-password`  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Change password for logged-in user

**Request Headers**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**Response (200 OK)**:
```json
{
  "message": "Password changed successfully",
  "success": true
}
```

**Error Response (400 Bad Request)**:
```json
{
  "message": "Current password is incorrect",
  "success": false
}
```

---

## 2️⃣ Conference Management APIs

Base URL: `/api/conferences`

### 2.1 Get All Conferences

**Endpoint**: `GET /`  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Retrieve all conferences

**Response (200 OK)**:
```json
[
  {
    "id": "65e1234567890abcdef12345",
    "title": "Tech Summit 2024",
    "imageUrl": "http://images.example.com/tech-summit.png",
    "status": "ACTIVE",
    "createdAt": "2024-03-01T10:00:00",
    "updatedAt": "2024-03-01T10:00:00",
    "deleted": false
  },
  {
    "id": "65e1234567890abcdef12346",
    "title": "Medical Expo 2024",
    "imageUrl": "http://images.example.com/med-expo.png",
    "status": "ACTIVE",
    "createdAt": "2024-03-05T15:30:00",
    "updatedAt": "2024-03-05T15:30:00",
    "deleted": false
  }
]
```

---

### 2.2 Get Conference by ID

**Endpoint**: `GET /{id}`  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Get specific conference details

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Response (200 OK)**:
```json
{
  "id": "65e1234567890abcdef12345",
  "title": "Tech Summit 2024",
  "imageUrl": "http://images.example.com/tech-summit.png",
  "status": "ACTIVE",
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-01T10:00:00",
  "deleted": false
}
```

---

### 2.3 Create Conference

**Endpoint**: `POST /`  
**Access**: SUPER_ADMIN only  
**Description**: Create new conference

**Request Body**:
```json
{
  "title": "Medical Conference 2026",
  "imageUrl": "http://images.example.com/medical.png",
  "status": "ACTIVE"
}
```

**Response (201 Created)**:
```json
{
  "id": "65e1234567890abcdef12347",
  "title": "Medical Conference 2026",
  "imageUrl": "http://images.example.com/medical.png",
  "status": "ACTIVE",
  "createdAt": "2026-02-17T12:00:00",
  "updatedAt": "2026-02-17T12:00:00",
  "deleted": false
}
```

---

### 2.4 Update Conference

**Endpoint**: `PUT /{id}`  
**Access**: SUPER_ADMIN only  
**Description**: Update conference details

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Request Body**:
```json
{
  "title": "Tech Summit 2025",
  "imageUrl": "http://images.example.com/tech-summit-2025.png",
  "status": "INACTIVE"
}
```

**Response (200 OK)**:
```json
{
  "id": "65e1234567890abcdef12345",
  "title": "Tech Summit 2025",
  "imageUrl": "http://images.example.com/tech-summit-2025.png",
  "status": "INACTIVE",
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2026-02-17T12:00:00",
  "deleted": false
}
```

---

### 2.5 Delete Conference

**Endpoint**: `DELETE /{id}`  
**Access**: SUPER_ADMIN only  
**Description**: Delete (soft delete) conference

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Response (200 OK)**:
```json
{}
```

---

## 3️⃣ Dashboard Master APIs

Base URL: `/api/dashboard-masters`

### 3.1 Get All Dashboard Masters

**Endpoint**: `GET /`  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Retrieve all dashboard types

**Response (200 OK)**:
```json
[
  {
    "id": "65f1234567890abcdef12345",
    "name": "University",
    "status": true,
    "createdAt": "2024-01-15T08:00:00",
    "updatedAt": "2024-01-15T08:00:00",
    "deleted": false
  },
  {
    "id": "65f1234567890abcdef12346",
    "name": "Sponsors",
    "status": true,
    "createdAt": "2024-01-15T08:30:00",
    "updatedAt": "2024-01-15T08:30:00",
    "deleted": false
  }
]
```

---

### 3.2 Get Dashboard Master by ID

**Endpoint**: `GET /{id}`  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Get specific dashboard master

**Response (200 OK)**:
```json
{
  "id": "65f1234567890abcdef12345",
  "name": "University",
  "status": true,
  "createdAt": "2024-01-15T08:00:00",
  "updatedAt": "2024-01-15T08:00:00",
  "deleted": false
}
```

---

### 3.3 Create Dashboard Master

**Endpoint**: `POST /`  
**Access**: SUPER_ADMIN only  
**Description**: Create new dashboard type

**Request Body**:
```json
{
  "name": "Exhibitors",
  "status": true
}
```

**Response (201 Created)**:
```json
{
  "id": "65f1234567890abcdef12347",
  "name": "Exhibitors",
  "status": true,
  "createdAt": "2026-02-17T12:00:00",
  "updatedAt": "2026-02-17T12:00:00",
  "deleted": false
}
```

---

### 3.4 Update Dashboard Master

**Endpoint**: `PUT /{id}`  
**Access**: SUPER_ADMIN only

**Request Body**:
```json
{
  "name": "University Partners",
  "status": true
}
```

**Response (200 OK)**:
```json
{
  "id": "65f1234567890abcdef12345",
  "name": "University Partners",
  "status": true,
  "createdAt": "2024-01-15T08:00:00",
  "updatedAt": "2026-02-17T12:00:00",
  "deleted": false
}
```

---

### 3.5 Delete Dashboard Master

**Endpoint**: `DELETE /{id}`  
**Access**: SUPER_ADMIN only

**Response (200 OK)**:
```json
{}
```

---

## 4️⃣ User Management APIs

Base URL: `/api/users`

### 4.1 Get All Admins

**Endpoint**: `GET /admins`  
**Access**: SUPER_ADMIN only  
**Description**: Get all admin users

**Response (200 OK)**:
```json
[
  {
    "id": "65g1234567890abcdef12345",
    "firstName": "John",
    "lastName": "Doe",
    "userId": "johndoe",
    "email": "john@example.com",
    "phoneNumber": "9876543210",
    "role": "ADMIN",
    "status": true,
    "conferenceIds": ["65e1234567890abcdef12345"],
    "createdAt": "2024-02-01T10:00:00",
    "updatedAt": "2024-02-01T10:00:00",
    "deleted": false
  }
]
```

---

### 4.2 Get All Super Admins

**Endpoint**: `GET /super-admins`  
**Access**: SUPER_ADMIN only

**Response (200 OK)**:
```json
[
  {
    "id": "65g1234567890abcdef12346",
    "firstName": "Super",
    "lastName": "Admin",
    "userId": "superadmin",
    "email": "superadmin@example.com",
    "phoneNumber": "0000000000",
    "role": "SUPER_ADMIN",
    "status": true,
    "conferenceIds": [],
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00",
    "deleted": false
  }
]
```

---

### 4.3 Get User by ID

**Endpoint**: `GET /{id}`  
**Access**: SUPER_ADMIN only

**Response (200 OK)**:
```json
{
  "id": "65g1234567890abcdef12345",
  "firstName": "John",
  "lastName": "Doe",
  "userId": "johndoe",
  "email": "john@example.com",
  "phoneNumber": "9876543210",
  "role": "ADMIN",
  "status": true,
  "conferenceIds": ["65e1234567890abcdef12345"],
  "createdAt": "2024-02-01T10:00:00",
  "updatedAt": "2024-02-01T10:00:00",
  "deleted": false
}
```

---

### 4.4 Create User

**Endpoint**: `POST /`  
**Access**: SUPER_ADMIN only

**Request Body**:
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "userId": "janesmith",
  "email": "jane@example.com",
  "phoneNumber": "8765432109",
  "password": "SecurePassword123",
  "role": "ADMIN",
  "status": true,
  "conferenceIds": ["65e1234567890abcdef12345"]
}
```

**Response (201 Created)**:
```json
{
  "id": "65g1234567890abcdef12347",
  "firstName": "Jane",
  "lastName": "Smith",
  "userId": "janesmith",
  "email": "jane@example.com",
  "phoneNumber": "8765432109",
  "role": "ADMIN",
  "status": true,
  "conferenceIds": ["65e1234567890abcdef12345"],
  "createdAt": "2026-02-17T12:00:00",
  "updatedAt": "2026-02-17T12:00:00",
  "deleted": false
}
```

---

### 4.5 Update User

**Endpoint**: `PUT /{id}`  
**Access**: SUPER_ADMIN only

**Request Body**:
```json
{
  "firstName": "Jane",
  "lastName": "Johnson",
  "email": "jane.johnson@example.com",
  "phoneNumber": "8765432109",
  "status": true,
  "conferenceIds": ["65e1234567890abcdef12345", "65e1234567890abcdef12346"]
}
```

**Response (200 OK)**:
```json
{
  "id": "65g1234567890abcdef12347",
  "firstName": "Jane",
  "lastName": "Johnson",
  "userId": "janesmith",
  "email": "jane.johnson@example.com",
  "phoneNumber": "8765432109",
  "role": "ADMIN",
  "status": true,
  "conferenceIds": ["65e1234567890abcdef12345", "65e1234567890abcdef12346"],
  "createdAt": "2026-02-17T12:00:00",
  "updatedAt": "2026-02-17T13:00:00",
  "deleted": false
}
```

---

### 4.6 Delete User

**Endpoint**: `DELETE /{id}`  
**Access**: SUPER_ADMIN only

**Response (200 OK)**:
```json
{}
```

---

## 5️⃣ Dashboard Data APIs

Base URL: `/api/dashboard-data`

### 5.1 Upload Excel File

**Endpoint**: `POST /upload`  
**Access**: ADMIN only  
**Content-Type**: `multipart/form-data`

**Request Parameters**:
```
file: <binary file>
conferenceId: 65e1234567890abcdef12345
dashboardMasterId: 65f1234567890abcdef12345
```

**Response (200 OK)**:
```
File uploaded successfully!
```

**Error Response (400 Bad Request)**:
```json
{
  "message": "Failed to process file: Invalid file format"
}
```

---

### 5.2 Get Dashboard Data by Serial Range

**Endpoint**: `GET /`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**:
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
fromSerialNo=1
toSerialNo=100
```

**Response (200 OK)**:
```json
[
  {
    "id": "65h1234567890abcdef12345",
    "conferenceId": "65e1234567890abcdef12345",
    "dashboardMasterId": "65f1234567890abcdef12345",
    "serialNo": 1,
    "name": "Alice Johnson",
    "email": "alice@example.com",
    "region": "USA",
    "country": "United States",
    "status": true,
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-01-15T10:30:00",
    "deleted": false
  },
  {
    "id": "65h1234567890abcdef12346",
    "conferenceId": "65e1234567890abcdef12345",
    "dashboardMasterId": "65f1234567890abcdef12345",
    "serialNo": 2,
    "name": "Bob Smith",
    "email": "bob@example.com",
    "region": "UK",
    "country": "United Kingdom",
    "status": true,
    "createdAt": "2026-01-15T11:00:00",
    "updatedAt": "2026-01-15T11:00:00",
    "deleted": false
  }
]
```

---

### 5.3 Get Filtered Dashboard Data

**Endpoint**: `GET /filter`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters** (all optional except conferenceId and dashboardMasterId):
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
fromSerialNo=1
toSerialNo=100
startDate=2026-01-01
endDate=2026-01-31
region=USA
country=United States
```

**Response (200 OK)**:
```json
[
  {
    "id": "65h1234567890abcdef12345",
    "conferenceId": "65e1234567890abcdef12345",
    "dashboardMasterId": "65f1234567890abcdef12345",
    "serialNo": 1,
    "name": "Alice Johnson",
    "email": "alice@example.com",
    "region": "USA",
    "country": "United States",
    "status": true,
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-01-15T10:30:00",
    "deleted": false
  }
]
```

---

### 5.4 Get Data by Date Range

**Endpoint**: `GET /by-date`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**:
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
startDate=2026-01-01
endDate=2026-01-31
```

**Response (200 OK)**: Same as /filter

---

### 5.5 Get Data by Region

**Endpoint**: `GET /by-region`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**:
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
region=USA
```

**Response (200 OK)**: Same as /filter

---

### 5.6 Get Data by Country

**Endpoint**: `GET /by-country`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**:
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
country=United States
```

**Response (200 OK)**: Same as /filter

---

### 5.7 Get Filtered Data Count

**Endpoint**: `GET /count`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**: Same as /filter

**Response (200 OK)**:
```
150
```

---

## 6️⃣ Export APIs

Base URL: `/api/export`

### 6.1 Export to Excel (Basic)

**Endpoint**: `GET /excel`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**:
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
fromSerialNo=1
toSerialNo=500
```

**Response**: Binary Excel file (`.xlsx`)

---

### 6.2 Export to PDF (Basic)

**Endpoint**: `GET /pdf`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**: Same as /excel

**Response**: Binary PDF file (`.pdf`)

---

### 6.3 Advanced Excel Export

**Endpoint**: `GET /excel/advanced`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters** (supports date, region, country filters):
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
startDate=2026-01-01
endDate=2026-01-31
region=USA
```

**Response**: Binary Excel file (`.xlsx`)

---

### 6.4 Advanced PDF Export

**Endpoint**: `GET /pdf/advanced`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**: Same as /excel/advanced

**Response**: Binary PDF file (`.pdf`)

---

### 6.5 Excel Export with Filter (POST)

**Endpoint**: `POST /excel/filter`  
**Access**: ADMIN, SUPER_ADMIN

**Request Body**:
```json
{
  "conferenceId": "65e1234567890abcdef12345",
  "dashboardMasterId": "65f1234567890abcdef12345",
  "fromSerialNo": 1,
  "toSerialNo": 500,
  "startDate": "2026-01-01",
  "endDate": "2026-01-31",
  "region": "USA",
  "country": "United States"
}
```

**Response**: Binary Excel file (`.xlsx`)

---

### 6.6 PDF Export with Filter (POST)

**Endpoint**: `POST /pdf/filter`  
**Access**: ADMIN, SUPER_ADMIN

**Request Body**: Same as /excel/filter

**Response**: Binary PDF file (`.pdf`)

---

### 6.7 Preview Filtered Data

**Endpoint**: `GET /preview`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**: Same as /excel/advanced

**Response (200 OK)**: List of DashboardData objects (same as /filter)

---

### 6.8 Get Distinct Regions

**Endpoint**: `GET /regions`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**:
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
```

**Response (200 OK)**:
```json
["USA", "UK", "India", "Germany", "Australia"]
```

---

### 6.9 Get Distinct Countries

**Endpoint**: `GET /countries`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**:
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
```

**Response (200 OK)**:
```json
["United States", "United Kingdom", "India", "Germany", "Australia"]
```

---

## 7️⃣ File Upload APIs

Base URL: `/api/files`

### 7.1 Upload Single File to GCS

**Endpoint**: `POST /upload`  
**Access**: ADMIN, SUPER_ADMIN  
**Content-Type**: `multipart/form-data`

**Request Parameters**:
```
file: <binary file>
conferenceId: 65e1234567890abcdef12345
dashboardMasterId: 65f1234567890abcdef12345
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "fileUrl": "https://storage.googleapis.com/sciinov/conferences/tech-summit-2024/university/20260217-143022-abc123.xlsx",
  "fileName": "data.xlsx",
  "fileSize": 12345,
  "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
}
```

---

### 7.2 Upload Multiple Files to GCS

**Endpoint**: `POST /upload-multiple`  
**Access**: ADMIN, SUPER_ADMIN  
**Content-Type**: `multipart/form-data`

**Request Parameters**:
```
files: <binary files>
conferenceId: 65e1234567890abcdef12345
dashboardMasterId: 65f1234567890abcdef12345
```

**Response (200 OK)**:
```json
{
  "success": true,
  "totalFiles": 3,
  "successCount": 3,
  "failCount": 0,
  "files": [
    {
      "fileName": "file1.xlsx",
      "fileUrl": "https://storage.googleapis.com/sciinov/conferences/tech-summit-2024/university/20260217-143022-abc123.xlsx",
      "status": "success"
    },
    {
      "fileName": "file2.xlsx",
      "fileUrl": "https://storage.googleapis.com/sciinov/conferences/tech-summit-2024/university/20260217-143030-def456.xlsx",
      "status": "success"
    }
  ]
}
```

---

### 7.3 List Files in GCS Folder

**Endpoint**: `GET /list`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**:
```
conferenceId=65e1234567890abcdef12345
dashboardMasterId=65f1234567890abcdef12345
```

**Response (200 OK)**:
```json
{
  "success": true,
  "folderPath": "conferences/tech-summit-2024/university/",
  "fileCount": 5,
  "files": [
    "conferences/tech-summit-2024/university/20260217-143022-abc123.xlsx",
    "conferences/tech-summit-2024/university/20260217-150000-def456.pdf",
    "conferences/tech-summit-2024/university/20260217-160000-ghi789.docx"
  ]
}
```

---

### 7.4 Delete File from GCS

**Endpoint**: `DELETE /delete`  
**Access**: SUPER_ADMIN only

**Query Parameters**:
```
blobName=conferences/tech-summit-2024/university/file.xlsx
```

**Response (200 OK)**:
```json
{
  "message": "File deleted successfully",
  "success": true
}
```

---

### 7.5 Get Signed URL for File

**Endpoint**: `GET /signed-url`  
**Access**: ADMIN, SUPER_ADMIN

**Query Parameters**:
```
blobName=conferences/tech-summit-2024/university/file.xlsx
```

**Response (200 OK)**:
```json
{
  "success": true,
  "signedUrl": "https://storage.googleapis.com/sciinov/conferences/tech-summit-2024/university/file.xlsx?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=...",
  "validForMinutes": 15
}
```

---

### 7.6 Check GCS Configuration Status

**Endpoint**: `GET /status`  
**Access**: SUPER_ADMIN only

**Response (200 OK)**:
```json
{
  "configured": true,
  "bucketName": "sciinov"
}
```

---

## 8️⃣ Analytics APIs

Base URL: `/api/analytics`

### 8.1 Get Upload Stats by Admin

**Endpoint**: `GET /upload-stats/admin/{adminId}`  
**Access**: SUPER_ADMIN only

**Path Parameters**:
```
adminId: 65g1234567890abcdef12345
```

**Response (200 OK)**:
```json
[
  {
    "id": "65i1234567890abcdef12345",
    "adminId": "65g1234567890abcdef12345",
    "conferenceId": "65e1234567890abcdef12345",
    "dashboardMasterId": "65f1234567890abcdef12345",
    "uploadedAt": "2026-01-15T10:30:00",
    "totalRecordsInFile": 150,
    "newRecordsAdded": 140,
    "duplicateRecordsIgnored": 10,
    "fileName": "students_data.xlsx",
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-01-15T10:30:00",
    "deleted": false
  }
]
```

---

### 8.2 Get Upload Stats by Conference

**Endpoint**: `GET /upload-stats/conference/{conferenceId}`  
**Access**: SUPER_ADMIN only

**Path Parameters**:
```
conferenceId: 65e1234567890abcdef12345
```

**Response (200 OK)**: List of DashboardUploadStats objects

---

### 8.3 Get All Activity Logs

**Endpoint**: `GET /logs`  
**Access**: SUPER_ADMIN only

**Response (200 OK)**:
```json
[
  {
    "id": "65j1234567890abcdef12345",
    "adminId": "65g1234567890abcdef12345",
    "adminName": "John Doe",
    "actionType": "UPLOAD_EXCEL",
    "description": "Uploaded Excel: students_data.xlsx. Added: 140, Duplicates: 10",
    "ipAddress": "192.168.1.100",
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-01-15T10:30:00",
    "deleted": false
  },
  {
    "id": "65j1234567890abcdef12346",
    "adminId": "65g1234567890abcdef12345",
    "adminName": "John Doe",
    "actionType": "DOWNLOAD_EXCEL",
    "description": "Exported data to Excel. Serial range: 1-100",
    "ipAddress": "192.168.1.100",
    "createdAt": "2026-01-15T11:00:00",
    "updatedAt": "2026-01-15T11:00:00",
    "deleted": false
  }
]
```

---

### 8.4 Get Logs by Admin

**Endpoint**: `GET /logs/admin/{adminId}`  
**Access**: SUPER_ADMIN only

**Path Parameters**:
```
adminId: 65g1234567890abcdef12345
```

**Response (200 OK)**: List of AdminActivityLog objects

---

## Error Responses

### Common Error Status Codes

| Status Code | Description | Example |
|-------------|-------------|---------|
| 200 | OK | Successful GET/POST request |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid input or missing required fields |
| 401 | Unauthorized | Invalid credentials or missing JWT token |
| 403 | Forbidden | Insufficient permissions (role-based) |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Server error |

### Error Response Format

```json
{
  "timestamp": "2026-02-17T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error description",
  "path": "/api/endpoint"
}
```

---

## Authentication Header

All authenticated endpoints require JWT token in header:

```
Authorization: Bearer {jwt_token}
```

Example:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdXBlcmFkbWluIiwiaWF0IjoxNjA1MzU5MjQwLCJleHAiOjE2MDU0NDU2NDB9.ABC123...
```

---

## Rate Limiting

No specific rate limiting implemented. All endpoints support standard HTTP requests.

---

## CORS Configuration

CORS is enabled with:
- **Origins**: `*` (All origins)
- **Max Age**: 3600 seconds (1 hour)

---

## Base Response Wrapper

### Success Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {}
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error message",
  "error": "Error details"
}
```

---

## Example Usage

### 1. Login
```bash
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "superadmin",
    "password": "admin123"
  }'
```

### 2. Get Conferences
```bash
curl -X GET http://localhost:8080/api/conferences \
  -H "Authorization: Bearer {token}"
```

### 3. Export Data with Filters
```bash
curl -X GET "http://localhost:8080/api/export/excel/advanced?conferenceId=xxx&dashboardMasterId=yyy&startDate=2026-01-01&endDate=2026-01-31&region=USA" \
  -H "Authorization: Bearer {token}" \
  -o exported_data.xlsx
```

### 4. Upload File
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer {token}" \
  -F "file=@data.xlsx" \
  -F "conferenceId=xxx" \
  -F "dashboardMasterId=yyy"
```

---

## Document Information

**Last Updated**: February 17, 2026  
**API Version**: 0.0.1-SNAPSHOT  
**Status**: Production Ready ✅


