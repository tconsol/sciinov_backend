package com.sciinov.dbms.dto;

public class PasswordResetTokenResponse {
    private String token;
    private String userId;
    private String email;
    private String message;
    private boolean success;

    public PasswordResetTokenResponse(String token, String userId, String email, String message, boolean success) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.message = message;
        this.success = success;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}

