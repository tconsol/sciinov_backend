package com.sciinov.dbms.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "admin_activity_logs")
public class AdminActivityLog {
    @Id
    private String id;
    private String adminId;
    private String adminName; // Added field
    private String conferenceId;
    private String dashboardMasterId;
    private ActionType actionType;
    private String description;
    private String ipAddress;
    
    @CreatedDate
    private LocalDateTime createdAt;

    public enum ActionType {
        CREATE, UPDATE, DELETE, VIEW, DOWNLOAD_EXCEL, DOWNLOAD_PDF, UPLOAD_EXCEL
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public String getAdminName() { return adminName; } // Added getter
    public void setAdminName(String adminName) { this.adminName = adminName; } // Added setter
    public String getConferenceId() { return conferenceId; }
    public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }
    public String getDashboardMasterId() { return dashboardMasterId; }
    public void setDashboardMasterId(String dashboardMasterId) { this.dashboardMasterId = dashboardMasterId; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
