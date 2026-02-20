package com.sciinov.dbms.dto;

import java.util.List;

/**
 * DTO for admin conference assignment data
 */
public class AdminConferenceResponse {
    private String adminId;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private boolean status;
    private List<String> conferenceIds;

    public AdminConferenceResponse() {}

    public AdminConferenceResponse(String adminId, String userId, String firstName, String lastName,
                                   String email, String phoneNumber, boolean status, List<String> conferenceIds) {
        this.adminId = adminId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.conferenceIds = conferenceIds;
    }

    // Getters and Setters
    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public List<String> getConferenceIds() {
        return conferenceIds;
    }

    public void setConferenceIds(List<String> conferenceIds) {
        this.conferenceIds = conferenceIds;
    }
}

