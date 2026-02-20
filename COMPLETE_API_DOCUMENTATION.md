# 📚 SciInov DBMS - Complete API Documentation

**Version**: 0.0.1-SNAPSHOT  
**Base URL**: `http://localhost:8080/api`  
**Authentication**: JWT Bearer Token  
**Date**: February 17, 2026  
**Status**: 🔒 Production Ready & Secured

---

## 🔐 Security & Authentication

### **Authentication Method**
All protected endpoints require JWT Bearer Token authentication.

### **Token Format**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdXBlcmFkbWluIiwi...
```

### **Token Acquisition**
1. Call `POST /api/auth/signin` with valid credentials
2. Receive JWT token in response
3. Include token in `Authorization` header for all subsequent requests
4. Token expires in 24 hours (86400000 ms)

### **Public Endpoints (No Token Required)** 🌐
The following endpoints are publicly accessible without authentication:
- `POST /api/auth/signin` - User login
- `POST /api/auth/forgot-password` - Request password reset
- `GET /api/auth/validate-reset-token` - Validate reset token
- `POST /api/auth/reset-password` - Reset password with token
- `POST /api/auth/forgot-username` - Recover username
- `GET /swagger-ui.html` - API documentation
- `GET /api-docs/**` - OpenAPI specification

### **Protected Endpoints (Token Required)** 🔒
All other endpoints require valid JWT token:
- Conference Management APIs
- Dashboard Master APIs
- User Management APIs
- Dashboard Data APIs
- Export APIs
- File Upload APIs
- Analytics APIs
- Change Password

### **Security Features** ✅
- ✅ JWT Token-based authentication
- ✅ BCrypt password hashing (strength: 12)
- ✅ Role-based access control (SUPER_ADMIN, ADMIN)
- ✅ CORS configuration with allowed origins
- ✅ Request correlation ID tracking
- ✅ Security headers (CSP, XSS Protection, Frame Options)
- ✅ Session management: Stateless
- ✅ SQL injection prevention (MongoDB)
- ✅ Input validation on all endpoints
- ✅ Secure password reset flow with tokens
- ✅ Password reset tokens expire in 24 hours
- ✅ Single-use password reset tokens

### **CORS Configuration** 🌍
```properties
Allowed Origins: http://localhost:3000, http://localhost:5173, https://sciinovdbms.com
Allowed Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Allowed Headers: Authorization, Content-Type, X-Correlation-ID, Accept
Exposed Headers: X-Correlation-ID, Authorization
Credentials: Allowed
Max Age: 3600 seconds
```

### **Security Headers** 🛡️
```
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

---

## 📋 Table of Contents

1. [Authentication APIs](#1️⃣-authentication-apis) 🌐 **(Public)**
2. [Conference Management APIs](#2️⃣-conference-management-apis) 🔒 **(Protected)**
3. [Dashboard Master APIs](#3️⃣-dashboard-master-apis) 🔒 **(Protected)**
4. [User Management APIs](#4️⃣-user-management-apis) 🔒 **(Protected)**
5. [Dashboard Data APIs](#5️⃣-dashboard-data-apis) 🔒 **(Protected)**
6. [Export APIs](#6️⃣-export-apis) 🔒 **(Protected)**
7. [File Upload APIs](#7️⃣-file-upload-apis) 🔒 **(Protected)**
8. [Analytics APIs](#8️⃣-analytics-apis) 🔒 **(Protected)**
9. [Security Best Practices](#9️⃣-security-best-practices)
10. [Production Deployment](#🚀-production-deployment)

---

## 1️⃣ Authentication APIs 🌐

Base URL: `/api/auth`  
**Authentication Required**: ❌ No (Public endpoints)

### 1.1 User Login 🌐

**Endpoint**: `POST /signin`  
**Authentication**: ❌ **No token required** (Public)  
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

### 1.2 Forgot Password 🌐

**Endpoint**: `POST /forgot-password`  
**Authentication**: ❌ **No token required** (Public)  
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

### 1.3 Validate Reset Token 🌐

**Endpoint**: `GET /validate-reset-token`  
**Authentication**: ❌ **No token required** (Public)  
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

### 1.4 Reset Password 🌐

**Endpoint**: `POST /reset-password`  
**Authentication**: ❌ **No token required** (Public)  
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

### 1.5 Forgot Username 🌐

**Endpoint**: `POST /forgot-username`  
**Authentication**: ❌ **No token required** (Public)  
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

### 1.6 Change Password 🔒

**Endpoint**: `POST /change-password`  
**Authentication**: ✅ **JWT Token Required**  
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

## 2️⃣ Conference Management APIs 🔒

Base URL: `/api/conferences`  
**Authentication Required**: ✅ **Yes - JWT Token Required for ALL endpoints**

**Common Request Headers for ALL endpoints**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json (for POST/PUT)
```

### 2.1 Get All Conferences 🔒

**Endpoint**: `GET /`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Retrieve all conferences

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK)**:
```json
[
  {
    "id": "65e1234567890abcdef12345",
    "title": "Tech Summit 2026",
    "imageUrl": "http://example.com/tech-summit.png",
    "status": "ACTIVE",
    "dashboardMasterIds": [
      "65f1234567890abcdef11111",
      "65f1234567890abcdef22222"
    ],
    "createdAt": "2024-03-01T10:00:00",
    "updatedAt": "2024-03-01T10:00:00",
    "deleted": false
  }
]
```

---

### 2.2 Get Conference by ID 🔒

**Endpoint**: `GET /{id}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Get specific conference details with associated dashboards

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Response (200 OK)**:
```json
{
  "id": "65e1234567890abcdef12345",
  "title": "Tech Summit 2026",
  "imageUrl": "http://example.com/tech-summit.png",
  "status": "ACTIVE",
  "dashboardMasterIds": [
    "65f1234567890abcdef11111",
    "65f1234567890abcdef22222"
  ],
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-01T10:00:00",
  "deleted": false
}
```

---

### 2.3 Create Conference 🔒

**Endpoint**: `POST /`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Create new conference with optional dashboards

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "title": "Medical Conference 2026",
  "imageUrl": "http://example.com/medical.png",
  "status": "ACTIVE",
  "dashboardMasterIds": [
    "65f1234567890abcdef33333",
    "65f1234567890abcdef44444"
  ]
}
```

**Response (201 Created)**:
```json
{
  "id": "65e1234567890abcdef12347",
  "title": "Medical Conference 2026",
  "imageUrl": "http://example.com/medical.png",
  "status": "ACTIVE",
  "dashboardMasterIds": [
    "65f1234567890abcdef33333",
    "65f1234567890abcdef44444"
  ],
  "createdAt": "2026-02-17T12:00:00",
  "updatedAt": "2026-02-17T12:00:00",
  "deleted": false
}
```

---

### 2.4 Update Conference 🔒

**Endpoint**: `PUT /{id}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Update conference details and/or dashboards

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Request Body**:
```json
{
  "title": "Tech Summit 2027",
  "imageUrl": "http://example.com/tech-summit-2027.png",
  "status": "INACTIVE",
  "dashboardMasterIds": [
    "65f1234567890abcdef11111",
    "65f1234567890abcdef55555"
  ]
}
```

**Response (200 OK)**:
```json
{
  "id": "65e1234567890abcdef12345",
  "title": "Tech Summit 2027",
  "imageUrl": "http://example.com/tech-summit-2027.png",
  "status": "INACTIVE",
  "dashboardMasterIds": [
    "65f1234567890abcdef11111",
    "65f1234567890abcdef55555"
  ],
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2026-02-17T12:00:00",
  "deleted": false
}
```

---

### 2.5 Delete Conference 🔒

**Endpoint**: `DELETE /{id}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Delete (soft delete) conference

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Response (200 OK)**:
```json
{}
```

---

### 2.6 Get Conference Dashboards 🔒

**Endpoint**: `GET /{id}/dashboards`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Get all dashboards attached to a conference

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Response (200 OK)**:
```json
{
  "conferenceId": "65e1234567890abcdef12345",
  "conferenceTitle": "Tech Summit 2026",
  "dashboards": [
    {
      "id": "65f1234567890abcdef11111",
      "name": "Students",
      "status": true,
      "createdAt": "2024-01-15T08:00:00",
      "updatedAt": "2024-01-15T08:00:00",
      "deleted": false
    },
    {
      "id": "65f1234567890abcdef22222",
      "name": "University",
      "status": true,
      "createdAt": "2024-01-15T08:00:00",
      "updatedAt": "2024-01-15T08:00:00",
      "deleted": false
    }
  ],
  "message": "Dashboards retrieved successfully",
  "success": true
}
```

---

### 2.7 Attach Dashboards to Conference 🔒

**Endpoint**: `POST /{id}/dashboards/attach`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Add dashboards to a conference (without removing existing ones)

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Request Body**:
```json
{
  "dashboardMasterIds": [
    "65f1234567890abcdef11111",
    "65f1234567890abcdef22222"
  ]
}
```

**Response (200 OK)**:
```json
{
  "conferenceId": "65e1234567890abcdef12345",
  "conferenceTitle": "Tech Summit 2026",
  "dashboards": [
    {
      "id": "65f1234567890abcdef11111",
      "name": "Students",
      "status": true
    },
    {
      "id": "65f1234567890abcdef22222",
      "name": "University",
      "status": true
    }
  ],
  "message": "Dashboards attached successfully",
  "success": true
}
```

**Note**: This endpoint adds to existing dashboards and prevents duplicates.

---

### 2.8 Detach Dashboards from Conference 🔒

**Endpoint**: `POST /{id}/dashboards/detach`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Remove specific dashboards from a conference

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Request Body**:
```json
{
  "dashboardMasterIds": [
    "65f1234567890abcdef11111"
  ]
}
```

**Response (200 OK)**:
```json
{
  "conferenceId": "65e1234567890abcdef12345",
  "conferenceTitle": "Tech Summit 2026",
  "dashboards": [
    {
      "id": "65f1234567890abcdef22222",
      "name": "University",
      "status": true
    }
  ],
  "message": "Dashboards detached successfully",
  "success": true
}
```

---

### 2.9 Replace All Dashboards for Conference 🔒

**Endpoint**: `PUT /{id}/dashboards`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Replace all dashboards for a conference

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Request Body**:
```json
{
  "dashboardMasterIds": [
    "65f1234567890abcdef33333",
    "65f1234567890abcdef44444"
  ]
}
```

**Response (200 OK)**:
```json
{
  "conferenceId": "65e1234567890abcdef12345",
  "conferenceTitle": "Tech Summit 2026",
  "dashboards": [
    {
      "id": "65f1234567890abcdef33333",
      "name": "Exhibitors",
      "status": true
    },
    {
      "id": "65f1234567890abcdef44444",
      "name": "Sponsors",
      "status": true
    }
  ],
  "message": "Dashboards set successfully",
  "success": true
}
```

**Note**: This endpoint removes all existing dashboards and replaces with new list.
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

### 2.2 Get Conference by ID 🔒

**Endpoint**: `GET /{id}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Get specific conference details

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

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

### 2.3 Create Conference 🔒

**Endpoint**: `POST /`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Create new conference

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

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

### 2.4 Update Conference 🔒

**Endpoint**: `PUT /{id}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Update conference details

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

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

### 2.5 Delete Conference 🔒

**Endpoint**: `DELETE /{id}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Delete (soft delete) conference

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**Path Parameters**:
```
id: 65e1234567890abcdef12345
```

**Response (200 OK)**:
```json
{}
```

---

## 3️⃣ Dashboard Master APIs 🔒

Base URL: `/api/dashboard-masters`  
**Authentication Required**: ✅ **Yes - JWT Token Required for ALL endpoints**

### 3.1 Get All Dashboard Masters 🔒

**Endpoint**: `GET /`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Retrieve all dashboard types

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

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

### 3.2 Get Dashboard Master by ID 🔒

**Endpoint**: `GET /{id}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: ADMIN, SUPER_ADMIN  
**Description**: Get specific dashboard master

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

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

### 3.3 Create Dashboard Master 🔒

**Endpoint**: `POST /`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Create new dashboard type

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

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

### 3.4 Update Dashboard Master 🔒

**Endpoint**: `PUT /{id}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

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

### 3.5 Delete Dashboard Master 🔒

**Endpoint**: `DELETE /{id}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK)**:
```json
{}
```

---

## 4️⃣ User Management APIs 🔒

Base URL: `/api/users`  
**Authentication Required**: ✅ **Yes - JWT Token Required for ALL endpoints**

### 4.1 Get All Admins 🔒

**Endpoint**: `GET /admins`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Get all admin users

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

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

### 4.1B Get All Admins with Assigned Conferences 🔒 ⭐ (NEW)

**Endpoint**: `GET /admins/conferences/all`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**HTTP Method**: GET  
**Description**: Get all admins with their assigned conferences (cleaner response)

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**cURL Example**:
```bash
curl -X GET \
  'http://localhost:8080/api/users/admins/conferences/all' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...' \
  -H 'Content-Type: application/json'
```

**Response (200 OK)**:
```json
[
  {
    "adminId": "65g1234567890abcdef12345",
    "userId": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phoneNumber": "9876543210",
    "status": true,
    "conferenceIds": [
      "65e1234567890abcdef12345",
      "65e1234567890abcdef12346"
    ]
  },
  {
    "adminId": "65g1234567890abcdef12347",
    "userId": "janesmith",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@example.com",
    "phoneNumber": "8765432109",
    "status": true,
    "conferenceIds": [
      "65e1234567890abcdef12346"
    ]
  },
  {
    "adminId": "65g1234567890abcdef12348",
    "userId": "bobwilson",
    "firstName": "Bob",
    "lastName": "Wilson",
    "email": "bob@example.com",
    "phoneNumber": "7654321098",
    "status": false,
    "conferenceIds": []
  }
]
```

**Response Fields**:
- `adminId` - Unique MongoDB ID of the admin user
- `userId` - Username of the admin
- `firstName` - First name of the admin
- `lastName` - Last name of the admin
- `email` - Email address of the admin
- `phoneNumber` - Phone number of the admin
- `status` - Whether the admin account is active (true) or inactive (false)
- `conferenceIds` - Array of conference IDs assigned to this admin (empty array if none)

**Use Cases**:
1. **Super Admin Dashboard** - Display all admins and their conference assignments
2. **Conference Assignment Overview** - See which admins are responsible for which conferences
3. **Admin Activity Tracking** - Monitor admin coverage across all conferences

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

### 4.7 Update User Status 🔒

**Endpoint**: `PATCH /{id}/status`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Enable or disable a user account

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters**:
```
id: 65g1234567890abcdef12345
```

**Request Body**:
```json
{
  "status": false
}
```

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
  "status": false,
  "conferenceIds": ["65e1234567890abcdef12345"],
  "createdAt": "2024-02-01T10:00:00",
  "updatedAt": "2026-02-17T14:00:00",
  "deleted": false
}
```

---

### 4.8 Get Admin Conferences 🔒

**Endpoint**: `GET /{adminId}/conferences`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**Description**: Get all conferences assigned to a specific admin

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**Path Parameters**:
```
adminId: 65g1234567890abcdef12345
```

**Response (200 OK)**:
```json
[
  "65e1234567890abcdef12345",
  "65e1234567890abcdef12346",
  "65e1234567890abcdef12347"
]
```

**Error Responses**:
```json
{
  "message": "Admin user not found"
}
```
```json
{
  "message": "User is not an admin"
}
```

---

### 4.9 Assign Conference to Admin 🔒

**Endpoint**: `POST /{adminId}/conferences/{conferenceId}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**HTTP Method**: POST  
**Description**: Assign a conference to an admin

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters**:
```
adminId: 65g1234567890abcdef12345
conferenceId: 65e1234567890abcdef12345
```

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
  "conferenceIds": [
    "65e1234567890abcdef12345",
    "65e1234567890abcdef12346"
  ],
  "createdAt": "2024-02-01T10:00:00",
  "updatedAt": "2026-02-17T14:05:00",
  "deleted": false
}
```

**Error Responses**:
```json
{
  "message": "Admin user not found"
}
```
```json
{
  "message": "User is not an admin"
}
```

---

### 4.10 Remove Conference from Admin 🔒 (⭐ MOST IMPORTANT)

**Endpoint**: `DELETE /{adminId}/conferences/{conferenceId}`  
**Authentication**: ✅ **JWT Token Required**  
**Access**: SUPER_ADMIN only  
**HTTP Method**: DELETE  
**Description**: Remove an assigned conference from an admin

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**Path Parameters**:
```
adminId: 65g1234567890abcdef12345
conferenceId: 65e1234567890abcdef12345
```

**cURL Example**:
```bash
curl -X DELETE \
  'http://localhost:8080/api/users/65g1234567890abcdef12345/conferences/65e1234567890abcdef12345' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...' \
  -H 'Content-Type: application/json'
```

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
  "conferenceIds": [
    "65e1234567890abcdef12346"
  ],
  "createdAt": "2024-02-01T10:00:00",
  "updatedAt": "2026-02-17T14:10:00",
  "deleted": false
}
```

**Error Responses**:

*Admin Not Found (404)*:
```json
{
  "message": "Admin user not found"
}
```

*User is Not an Admin (400)*:
```json
{
  "message": "User is not an admin"
}
```

*Conference Not Assigned (400)*:
```json
{
  "message": "Conference not assigned to this admin"
}
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

## 9️⃣ Security Best Practices

### **Authentication & Authorization** 🔒

#### **JWT Token Management**
- Token expires in 24 hours (86400000 ms)
- Tokens are stateless and stored client-side
- Include token in every protected API request
- Token contains user ID and roles
- Tokens cannot be revoked (use short expiry)

#### **Password Security**
- BCrypt hashing with strength 12
- Minimum password requirements enforced
- Password reset tokens valid for 24 hours only
- Single-use password reset tokens
- Old passwords not reused (implement if needed)

#### **Role-Based Access Control**
```
SUPER_ADMIN:
  - Full system access
  - User management
  - Conference management
  - Dashboard master management
  - Analytics access
  - All admin capabilities

ADMIN:
  - Limited to assigned conferences
  - Data upload for assigned conferences
  - Data export for assigned conferences
  - View analytics for assigned conferences
  - Cannot manage users or conferences
```

---

### **API Security Checklist** ✅

- [x] JWT authentication on all protected endpoints
- [x] BCrypt password hashing (strength: 12)
- [x] Role-based authorization (@PreAuthorize)
- [x] CORS configured with allowed origins only
- [x] Content Security Policy (CSP) headers
- [x] XSS Protection headers
- [x] Frame Options (DENY) headers
- [x] Referrer Policy headers
- [x] Stateless session management
- [x] Input validation on all endpoints
- [x] SQL/NoSQL injection prevention
- [x] Request correlation ID tracking
- [x] Comprehensive logging
- [x] Error messages don't expose sensitive data
- [x] HTTPS enforced (in production)

---

### **Input Validation**

All endpoints validate:
- Required fields presence
- Email format validation
- Phone number format validation
- Password strength requirements
- File upload size limits (50MB)
- File type validation (Excel, PDF)
- Serial number ranges
- Date range validity
- Conference and dashboard access permissions

---

### **Rate Limiting** (Recommended for Production)

Consider implementing:
```
- Login attempts: 5 per 15 minutes per IP
- Password reset: 3 per hour per user
- File upload: 10 per hour per user
- Export requests: 20 per hour per user
- API calls: 1000 per hour per user
```

---

## 🚀 Production Deployment

### **Environment Configuration**

#### **1. Update .env for Production**
```properties
# MongoDB - Production Database
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/sciinovdbms_prod?retryWrites=true

# JWT Configuration
JWT_SECRET={generate_secure_256_bit_key}
JWT_EXPIRATION_MS=86400000

# Server Configuration
SERVER_PORT=8080

# SMTP Mail - Production
MAIL_HOST=smtp.yourprovider.com
MAIL_PORT=587
MAIL_USERNAME=noreply@sciinovdbms.com
MAIL_PASSWORD={secure_password}
MAIL_FROM=noreply@sciinovdbms.com
MAIL_FROM_NAME=SciInov DBMS

# Google Cloud Storage
GCS_PROJECT_ID=your-project-id
GCS_BUCKET_NAME=sciinov-prod
GCS_CREDENTIALS_PATH=/app/config/gcs-credentials.json

# Application URLs
APP_BASE_URL=https://api.sciinovdbms.com
APP_FRONTEND_BASE_URL=https://sciinovdbms.com
APP_PASSWORD_RESET_URL=https://sciinovdbms.com/reset-password
APP_CORS_ALLOWED_ORIGINS=https://sciinovdbms.com
```

---

### **2. Security Hardening**

#### **Generate Secure JWT Secret**
```bash
# Generate 256-bit random key
openssl rand -hex 32
```

#### **Enable HTTPS**
```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password={keystore_password}
server.ssl.key-store-type=PKCS12
```

#### **Update CORS for Production**
```properties
app.cors.allowed-origins=https://sciinovdbms.com
```

---

### **3. Database Security**

#### **MongoDB Atlas Production Setup**
- Enable IP Whitelist (restrict to application servers)
- Use strong database credentials
- Enable database encryption at rest
- Enable audit logging
- Regular backups scheduled
- Point-in-time recovery enabled

```
Connection String Format:
mongodb+srv://<user>:<password>@<cluster>.mongodb.net/<database>?retryWrites=true&w=majority
```

---

### **4. Email Configuration**

#### **Production SMTP Settings**
```properties
# Use dedicated SMTP service (SendGrid, Amazon SES, etc.)
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD={sendgrid_api_key}
```

---

### **5. Google Cloud Storage**

#### **Production GCS Setup**
1. Create production GCS bucket
2. Configure service account with minimal permissions:
   - `storage.objects.create`
   - `storage.objects.delete`
   - `storage.objects.get`
   - `storage.objects.list`
3. Store credentials securely (not in Git)
4. Use environment variable or mounted secret

```bash
# Set credentials path
export GOOGLE_APPLICATION_CREDENTIALS=/app/config/gcs-credentials.json
```

---

### **6. Logging Configuration**

#### **Production Logging**
```properties
# Log to file in production
logging.file.name=/var/log/sciinov/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# Log levels
logging.level.root=WARN
logging.level.com.sciinov.dbms=INFO
logging.level.com.sciinov.dbms.controller=INFO
logging.level.com.sciinov.dbms.service=INFO

# Pattern with correlation ID
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{correlationId}] [%-5level] [%thread] %logger{36}.%M - %msg%n
```

---

### **7. Docker Deployment**

#### **Dockerfile** (already exists)
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar
COPY .env .env
COPY fineflux-c0bba53c7d23.json /app/config/gcs-credentials.json
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### **Build and Run**
```bash
# Build application
mvn clean package -DskipTests

# Build Docker image
docker build -t sciinov-backend:latest .

# Run container
docker run -d \
  --name sciinov-backend \
  -p 8080:8080 \
  --env-file .env \
  -v /path/to/logs:/var/log/sciinov \
  sciinov-backend:latest
```

---

### **8. Kubernetes Deployment** (Optional)

#### **deployment.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sciinov-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sciinov-backend
  template:
    metadata:
      labels:
        app: sciinov-backend
    spec:
      containers:
      - name: backend
        image: sciinov-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: sciinov-secrets
              key: mongodb-uri
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: sciinov-secrets
              key: jwt-secret
```

---

### **9. Health Checks**

#### **Add Health Check Endpoint**
```java
@GetMapping("/health")
public ResponseEntity<Map<String, String>> health() {
    Map<String, String> health = new HashMap<>();
    health.put("status", "UP");
    health.put("timestamp", LocalDateTime.now().toString());
    return ResponseEntity.ok(health);
}
```

---

### **10. Monitoring & Alerts**

#### **Implement Monitoring**
- Application uptime monitoring
- API response time tracking
- Error rate monitoring
- Database connection pool monitoring
- GCS storage usage monitoring
- Email delivery rate tracking
- Failed login attempts tracking

#### **Recommended Tools**
- Prometheus + Grafana for metrics
- ELK Stack for log aggregation
- Sentry for error tracking
- Uptime Robot for availability monitoring

---

### **11. Backup Strategy**

#### **Database Backups**
- Daily automated backups
- 30-day retention period
- Test restore procedure monthly
- Store backups in separate region

#### **File Storage Backups**
- GCS versioning enabled
- Lifecycle policy for old files
- Cross-region replication

---

### **12. Performance Optimization**

#### **Database Optimization**
- Create indexes on frequently queried fields
- Connection pooling configured
- Query optimization

#### **API Optimization**
- Response compression enabled
- Caching for frequently accessed data
- Pagination for large data sets
- Async operations for file processing

---

### **13. Security Scanning**

#### **Before Production Deployment**
```bash
# Scan for vulnerabilities
mvn dependency-check:check

# Scan Docker image
docker scan sciinov-backend:latest

# Static code analysis
mvn sonar:sonar
```

---

### **14. Deployment Checklist** ✅

- [ ] Update all .env variables for production
- [ ] Generate secure JWT secret (256-bit)
- [ ] Configure production MongoDB connection
- [ ] Set up production SMTP service
- [ ] Configure GCS with proper permissions
- [ ] Enable HTTPS/SSL
- [ ] Update CORS allowed origins
- [ ] Configure logging to file
- [ ] Set up log rotation
- [ ] Enable database backups
- [ ] Configure health checks
- [ ] Set up monitoring and alerts
- [ ] Test all API endpoints
- [ ] Load testing completed
- [ ] Security scan passed
- [ ] Documentation updated
- [ ] Environment variables secured
- [ ] Credentials not in Git
- [ ] SSL certificate installed
- [ ] Domain DNS configured
- [ ] Firewall rules configured

---

## Document Information

**Last Updated**: February 17, 2026  
**API Version**: 0.0.1-SNAPSHOT  
**Status**: Production Ready ✅


