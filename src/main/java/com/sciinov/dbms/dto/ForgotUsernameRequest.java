package com.sciinov.dbms.dto;

import jakarta.validation.constraints.NotBlank;

public class ForgotUsernameRequest {
    private String email;
    private String phoneNumber;

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}

