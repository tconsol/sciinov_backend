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
