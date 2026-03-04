# Authentication API Endpoints Documentation

## Base URL
```
http://localhost:8080/api/auth
```

---

## 1. LOGIN (Sign In)

### Endpoint
```
POST /api/auth/signin
```

### Description
Authenticates a user with userId and password, returns JWT access token and refresh token.

### Request Headers
```
Content-Type: application/json
```

### Request Payload
```json
{
  "userId": "superadmin",
  "password": "yourPassword123"
}
```

### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | String | ✅ Yes | The username/user ID of the user |
| password | String | ✅ Yes | The password for the user |

### Success Response (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2OTllY2QyNzIwNjRkNDYwYTZmMDQ0ZDkiLCJ1c2VySWQiOiJzdXBlcmFkbWluIiwicm9sZXMiOlsiUk9MRV9TVVBFUl9BRE1JTiJdLCJpYXQiOjE3NzAxNjAyNDUsImV4cCI6MTc3MDE2Mzg0NX0.dz8X9H5K7mZ4x2pLqW6rN8sT3vY1oJ4uB2cD5eF6gH7",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2OTllY2QyNzIwNjRkNDYwYTZmMDQ0ZDkiLCJpYXQiOjE3NzAxNjAyNDUsImV4cCI6MTc3MDc2NTA0NX0.a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0",
  "type": "Bearer",
  "id": "699ecd272064d460a6f044d9",
  "userId": "superadmin",
  "email": "admin@sciinov.com",
  "roles": [
    "ROLE_SUPER_ADMIN"
  ]
}
```

### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| token | String | JWT Access Token (valid for 1 hour) |
| refreshToken | String | Refresh Token (valid for 7 days) |
| type | String | Always "Bearer" - token type |
| id | String | MongoDB User ID |
| userId | String | Username |
| email | String | User email address |
| roles | Array | Array of user roles (ROLE_SUPER_ADMIN, ROLE_ADMIN) |

### Error Response (401 Unauthorized)
```json
{
  "timestamp": "2026-03-04T10:48:45.451+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Bad credentials",
  "path": "/api/auth/signin"
}
```

### Token Expiration
- **Access Token**: 1 hour (3600000 milliseconds)
- **Refresh Token**: 7 days

### Example cURL
```bash
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "superadmin",
    "password": "yourPassword123"
  }'
```

---

## 2. LOGOUT (Sign Out)

### Endpoint
```
POST /api/auth/logout
```

### Description
Logs out the user and revokes refresh token(s). Works with or without valid JWT token.

### Request Headers
```
Content-Type: application/json
```

### Request Payload
```json
{
  "refreshToken": "oI7QfqgYBeHRjZaHKdZ9HOVEI2ifRqTucQAZTYa1vIc"
}
```

### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| refreshToken | String | ❌ No | Optional refresh token to revoke. If not provided, all tokens for user are revoked |

### Request Body Variations

#### Option 1: Logout with Refresh Token (Recommended)
```json
{
  "refreshToken": "oI7QfqgYBeHRjZaHKdZ9HOVEI2ifRqTucQAZTYa1vIc"
}
```
Logout and revoke the specific refresh token. The refreshToken parameter is optional - if not provided, all tokens for the user are revoked (if authenticated).

#### Option 2: Logout without Refresh Token
```json
{}
```
Logout request without refresh token - still returns 200 OK.

### Success Response (200 OK)
```json
{
  "message": "Logged out successfully",
  "success": true
}
```

### Error Response (Still Returns 200 OK)
```json
{
  "message": "Logged out successfully",
  "success": true
}
```

### Behavior Notes
- ✅ Always returns **200 OK** (graceful logout)
- ✅ Works even with expired/invalid JWT token
- ✅ Clears authentication from security context
- ✅ If refreshToken in body → revokes only that token
- ✅ If no refreshToken → revokes all tokens for the user
- ✅ No exception thrown - logout is always successful

### Example cURL (Recommended)
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "oI7QfqgYBeHRjZaHKdZ9HOVEI2ifRqTucQAZTYa1vIc"
  }'
```

### Example cURL (Without Token)
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{}'
```

---

## 3. GET TOKEN EXPIRATION INFO

### Endpoint
```
GET /api/auth/token-expiration
```

### Description
Returns JWT token expiration information in milliseconds, minutes, and hours.

### Request Headers
```
Content-Type: application/json
```

### Success Response (200 OK)
```json
{
  "success": true,
  "tokenExpirationMs": 3600000,
  "tokenExpirationMinutes": 60,
  "tokenExpirationHours": 1
}
```

### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| success | Boolean | Always true |
| tokenExpirationMs | Integer | Token expiration in milliseconds (3600000 = 1 hour) |
| tokenExpirationMinutes | Integer | Token expiration in minutes (60) |
| tokenExpirationHours | Integer | Token expiration in hours (1) |

### Example cURL
```bash
curl -X GET http://localhost:8080/api/auth/token-expiration \
  -H "Content-Type: application/json"
```

---

## 4. REFRESH ACCESS TOKEN

### Endpoint
```
POST /api/auth/refresh-token
```

### Description
Refreshes the access token using a valid refresh token. Useful when access token is about to expire.

### Request Headers
```
Content-Type: application/json
```

### Request Payload
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2OTllY2QyNzIwNjRkNDYwYTZmMDQ0ZDkiLCJpYXQiOjE3NzAxNjAyNDUsImV4cCI6MTc3MDc2NTA0NX0.a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0"
}
```

### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| refreshToken | String | ✅ Yes | Valid refresh token from login response |

### Success Response (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2OTllY2QyNzIwNjRkNDYwYTZmMDQ0ZDkiLCJ1c2VySWQiOiJzdXBlcmFkbWluIiwicm9sZXMiOlsiUk9MRV9TVVBFUl9BRE1JTiJdLCJpYXQiOjE3NzAxNjAyNDUsImV4cCI6MTc3MDE2Mzg0NX0.newTokenHash",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2OTllY2QyNzIwNjRkNDYwYTZmMDQ0ZDkiLCJpYXQiOjE3NzAxNjAyNDUsImV4cCI6MTc3MDc2NTA0NX0.a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0"
}
```

### Error Response (401 Unauthorized)
```json
{
  "success": false,
  "message": "Invalid or expired refresh token"
}
```

### Example cURL
```bash
curl -X POST http://localhost:8080/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

---

## Authentication Flow Diagram

```
1. USER LOGIN
   POST /api/auth/signin
   └─→ Returns: access_token + refresh_token
   
2. USE ACCESS TOKEN
   All API calls with: Authorization: Bearer <access_token>
   
3. TOKEN EXPIRES (after 1 hour)
   POST /api/auth/refresh-token
   └─→ Returns: new access_token
   
4. USER LOGOUT
   POST /api/auth/logout
   └─→ Revokes refresh token(s)
   └─→ Always returns 200 OK
```

---

## CORS Issue Resolution

### Problem
Sometimes upload action gets CORS blocked for the same endpoint while data is stored in DB.

### Solution

Update your CORS configuration in `WebSecurityConfig.java`:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:3000",
        "http://localhost:8080"
    ));
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    ));
    configuration.setAllowedHeaders(Arrays.asList(
        "Content-Type", "Authorization", "X-Requested-With"
    ));
    configuration.setExposedHeaders(Arrays.asList(
        "Authorization", "X-Total-Count"
    ));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### Add to Logout Endpoint

Allow logout without authentication:

```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .authorizeHttpRequests(authz -> authz
        .requestMatchers("/api/auth/logout").permitAll()
        .requestMatchers("/api/auth/signin").permitAll()
        // ... other rules
    )
```

---

## Postman Collection Examples

### 1. Login in Postman
1. Create a new **POST** request
2. **URL**: `http://localhost:8080/api/auth/signin`
3. **Headers**:
   ```
   Content-Type: application/json
   ```
4. **Body** (raw JSON):
   ```json
   {
     "userId": "superadmin",
     "password": "yourPassword123"
   }
   ```
5. Click **Send**
6. Save the `token` and `refreshToken` from response

### 2. Logout in Postman
1. Create a new **POST** request
2. **URL**: `http://localhost:8080/api/auth/logout`
3. **Headers**:
   ```
   Content-Type: application/json
   ```
4. **Body** (raw JSON) - use the refreshToken from login response:
   ```json
   {
     "refreshToken": "oI7QfqgYBeHRjZaHKdZ9HOVEI2ifRqTucQAZTYa1vIc"
   }
   ```
5. Click **Send**
6. Response: 
   ```json
   {
     "message": "Logged out successfully",
     "success": true
   }
   ```

### 3. Test Protected Endpoint with Token
1. Create any **GET/POST** request to a protected endpoint
2. **Headers**:
   ```
   Authorization: Bearer <accessToken>
   Content-Type: application/json
   ```
3. Replace `<accessToken>` with the token from login response
4. Click **Send**

---

### Login
```typescript
async function login(userId: string, password: string) {
  const response = await axios.post('http://localhost:8080/api/auth/signin', {
    userId,
    password
  });
  
  localStorage.setItem('token', response.data.token);
  localStorage.setItem('refreshToken', response.data.refreshToken);
  return response.data;
}
```

### Logout
```typescript
async function logout() {
  const refreshToken = localStorage.getItem('refreshToken');
  
  try {
    await axios.post('http://localhost:8080/api/auth/logout', {
      refreshToken
    });
  } finally {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
  }
}
```

### API Request with Token
```typescript
axios.defaults.headers.common['Authorization'] = `Bearer ${localStorage.getItem('token')}`;
```

### Token Refresh Interceptor
```typescript
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      const response = await axios.post('http://localhost:8080/api/auth/refresh-token', {
        refreshToken
      });
      
      localStorage.setItem('token', response.data.token);
      axios.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
      
      return axios(error.config);
    }
    return Promise.reject(error);
  }
);
```

---

## Notes

1. **JWT Token Duration**: 1 hour (3600000 ms)
2. **Refresh Token Duration**: 7 days
3. **CORS Handling**: Some endpoints may return 401 due to CORS preflight - this is normal
4. **Logout Behavior**: Always graceful (200 OK) even with expired tokens
5. **Token Validation**: Bearer token is validated on each request except `/signin`, `/logout`, `/forgot-*` endpoints

