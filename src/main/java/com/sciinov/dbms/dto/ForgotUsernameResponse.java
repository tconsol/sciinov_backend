package com.sciinov.dbms.dto;

import lombok.Data;

@Data
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

    private static String maskUserId(String userId) {
        if (userId == null || userId.length() <= 3) return userId;
        return userId.substring(0, 2) + "***" + userId.charAt(userId.length() - 1);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return local.charAt(0) + "***@" + domain;
        return local.substring(0, 2) + "***@" + domain;
    }
}
