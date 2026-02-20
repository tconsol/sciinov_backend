package com.sciinov.dbms.dto;

public class ForgotUsernameResponse {
    private String userId;
    private String maskedUserId;
    private String email;
    private String maskedEmail;
    private String message;
    private boolean success;

    public ForgotUsernameResponse(String userId, String email, String message, boolean success) {
        this.userId = userId;
        this.maskedUserId = maskUserId(userId);
        this.email = email;
        this.maskedEmail = maskEmail(email);
        this.message = message;
        this.success = success;
    }

    private String maskUserId(String userId) {
        if (userId == null || userId.length() <= 3) {
            return userId;
        }
        return userId.substring(0, 2) + "***" + userId.substring(userId.length() - 1);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***@" + domain;
        }
        return localPart.substring(0, 2) + "***@" + domain;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMaskedUserId() { return maskedUserId; }
    public void setMaskedUserId(String maskedUserId) { this.maskedUserId = maskedUserId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMaskedEmail() { return maskedEmail; }
    public void setMaskedEmail(String maskedEmail) { this.maskedEmail = maskedEmail; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}

