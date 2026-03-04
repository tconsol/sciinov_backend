# JWT Refresh Token Implementation Guide

## Overview
A refresh token mechanism has been successfully implemented for your SciInov DBMS application. This allows users to obtain new access tokens without re-entering their credentials when their current token expires.

## How It Works

### Token Lifespan
- **Access Token (JWT)**: Valid for **24 hours** (86,400,000 milliseconds)
- **Refresh Token**: Valid for **7 days** (configurable via `JWT_REFRESH_EXPIRATION_DAYS`)

### Token Flow
```
1. User logs in with username/password
   ↓
2. System generates:
   - Access Token (24 hours validity)
   - Refresh Token (7 days validity)
   ↓
3. Both tokens are returned to client
   ↓
4. Access Token is used for API requests
   ↓
5. When Access Token expires:
   - Client sends Refresh Token
   - Server validates Refresh Token
   - New Access Token is issued
   ↓
6. When Refresh Token expires:
   - User must log in again
```

## New API Endpoints

### 1. Login (Updated)
**Endpoint**: `POST /api/auth/signin`

**Request**:
```json
{
  "userId": "admin123",
  "password": "password123"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": "user_id_123",
  "userId": "admin123",
  "email": "admin@example.com",
  "roles": ["ROLE_SUPER_ADMIN"]
}
```

### 2. Refresh Token (New)
**Endpoint**: `POST /api/auth/refresh-token`

**Description**: Obtain a new access token using a valid refresh token

**Request**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

**Error Response** (401 Unauthorized):
```json
{
  "success": false,
  "message": "Invalid or expired refresh token"
}
```

### 3. Logout (Updated)
**Endpoint**: `POST /api/auth/logout`

**Description**: Logout user and revoke refresh token(s)

**Request** (with optional refresh token):
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Or empty body to revoke all tokens for the user**:
```json
{}
```

**Response** (200 OK):
```json
{
  "message": "Logged out successfully",
  "success": true
}
```

## Frontend Implementation Example

### JavaScript/TypeScript Example

```javascript
// 1. Login
async function login(userId, password) {
  const response = await fetch('/api/auth/signin', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, password })
  });

  const data = await response.json();

  // Store tokens in localStorage or sessionStorage
  localStorage.setItem('accessToken', data.token);
  localStorage.setItem('refreshToken', data.refreshToken);

  return data;
}

// 2. Make API request with automatic token refresh
async function apiCall(endpoint, options = {}) {
  let token = localStorage.getItem('accessToken');

  let response = await fetch(endpoint, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${token}`
    }
  });

  // If 401, try to refresh token
  if (response.status === 401) {
    const refreshToken = localStorage.getItem('refreshToken');

    const refreshResponse = await fetch('/api/auth/refresh-token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    if (refreshResponse.ok) {
      const refreshData = await refreshResponse.json();

      // Update tokens
      localStorage.setItem('accessToken', refreshData.accessToken);
      localStorage.setItem('refreshToken', refreshData.refreshToken);

      // Retry original request with new token
      return apiCall(endpoint, options);
    } else {
      // Refresh failed, redirect to login
      logout();
      window.location.href = '/login';
    }
  }

  return response;
}

// 3. Logout
async function logout() {
  const refreshToken = localStorage.getItem('refreshToken');

  await fetch('/api/auth/logout', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    },
    body: JSON.stringify({ refreshToken })
  });

  // Clear tokens
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}
```

## Database Schema

### refresh_tokens Collection
```
{
  "_id": ObjectId,
  "token": String (unique),
  "userId": String (indexed),
  "expiryDate": LocalDateTime,
  "createdAt": LocalDateTime,
  "revoked": Boolean (default: false)
}
```

## Configuration

### Environment Variables
Add these to your `.env` file:

```properties
# JWT Access Token expiration (24 hours in milliseconds)
JWT_EXPIRATION_MS=86400000

# JWT Secret key (generate a secure 256-bit key)
JWT_SECRET=your_secure_256_bit_key_here

# Refresh Token expiration (days)
JWT_REFRESH_EXPIRATION_DAYS=7
```

### application.properties
Configuration is automatically loaded:
```properties
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS}
app.jwt.refresh-expiration-days=${JWT_REFRESH_EXPIRATION_DAYS:7}
```

## Security Features

1. **Secure Token Generation**: Uses `SecureRandom` and Base64URL encoding
2. **Token Revocation**: Old refresh tokens are automatically revoked when new ones are generated
3. **Expiration Validation**: Both tokens are validated for expiration
4. **Single-User Token**: Only one refresh token is kept active per user
5. **Logout Revocation**: Refresh tokens are revoked on logout
6. **Status Validation**: User status and deletion flags are checked during refresh

## Implementation Details

### New Classes Created

1. **RefreshToken Entity** (`com.sciinov.dbms.entity.RefreshToken`)
   - Stores refresh token data in MongoDB
   - Includes expiration and revocation status

2. **RefreshTokenRepository** (`com.sciinov.dbms.repository.RefreshTokenRepository`)
   - Provides database access for refresh tokens
   - Methods: `findByToken()`, `findByUserId()`, `deleteByUserId()`, `deleteByToken()`

3. **RefreshTokenService** (`com.sciinov.dbms.service.RefreshTokenService`)
   - Business logic for token generation and validation
   - Key methods:
     - `generateRefreshToken(userId)`: Creates new refresh token
     - `validateRefreshToken(token)`: Validates token
     - `getUserIdFromRefreshToken(token)`: Extracts user ID
     - `revokeRefreshToken(token)`: Marks token as revoked
     - `revokeAllTokensForUser(userId)`: Revokes all tokens for a user

4. **DTOs**
   - `RefreshTokenRequest`: Request body with refresh token
   - `TokenRefreshResponse`: Response with new access and refresh tokens

### Modified Classes

1. **JwtResponse DTO**
   - Added `refreshToken` field
   - Updated constructors

2. **AuthController**
   - Updated `signin()` to generate and return refresh token
   - Added `refreshToken()` endpoint
   - Enhanced `logout()` to revoke tokens

3. **JwtUtils**
   - Added refresh token expiration configuration

4. **application.properties**
   - Added refresh token configuration properties

## Testing the Feature

### 1. Login and Get Tokens
```bash
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"userId":"admin123","password":"password123"}'
```

### 2. Refresh Access Token
```bash
curl -X POST http://localhost:8080/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token_here>"}'
```

### 3. Logout and Revoke Tokens
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token_here>"}'
```

## Troubleshooting

### "Invalid or expired refresh token"
- Refresh token has expired (7 days validity exceeded)
- Refresh token has been revoked
- User has been deleted or deactivated
- Solution: User must log in again

### "User not found or inactive"
- User account has been deleted or deactivated
- Solution: Reactivate user or log in with different account

### Token Not Refreshing
- Check if refresh token is being sent in correct request body
- Verify token hasn't expired
- Check MongoDB connection and refresh_tokens collection
- Review application logs for errors

## Best Practices for Clients

1. **Store tokens securely**: Use secure storage (not vulnerable to XSS)
2. **Refresh proactively**: Refresh token before expiration
3. **Handle expiration gracefully**: Catch 401 responses and refresh
4. **Clear tokens on logout**: Remove all tokens from storage
5. **Short-lived access tokens**: Keep access token expiration short
6. **Longer refresh tokens**: Allow reasonable duration for refresh tokens
7. **HTTPS only**: Always use HTTPS in production

## Future Enhancements

1. **Sliding window expiration**: Reset refresh token expiration on each use
2. **Token rotation**: Issue new refresh token with each refresh
3. **Refresh token families**: Track token genealogy for security
4. **Redis caching**: Cache valid tokens for faster validation
5. **Rate limiting**: Limit refresh attempts per user
6. **Device tracking**: Support multiple devices with different tokens

## Summary

| Feature | Access Token | Refresh Token |
|---------|-------------|---------------|
| **Validity** | 24 hours | 7 days |
| **Purpose** | API requests | Get new access token |
| **Revocation** | Not possible | Yes, on logout |
| **Multiple per user** | No | No (one active) |
| **Encryption** | HS256 JWT | Random Base64URL |
| **Storage** | Client-side | Client-side + DB |
| **Rotation** | On each login | On refresh or new login |

Your application now supports seamless token refresh without requiring users to re-enter credentials!

