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
    private String adminName;
    private String conferenceId;
    private String dashboardMasterId;
    private ActionType actionType;
    private String description;
    private String ipAddress;

    // Detailed filter/range info for download & view logs
    private Long fromSerialNo;
    private Long toSerialNo;
    private Long totalRecords;
    private String emailDomain;    // email domain filter used (if any)
    private String filterSummary;  // human-readable summary of applied filters

    @CreatedDate
    private LocalDateTime createdAt;

    public enum ActionType {
        CREATE, UPDATE, DELETE, VIEW, DOWNLOAD_EXCEL, DOWNLOAD_PDF, UPLOAD_EXCEL,
        UPLOAD_FILE, DOWNLOAD_FILE, DELETE_FILE
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
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
    public Long getFromSerialNo() { return fromSerialNo; }
    public void setFromSerialNo(Long fromSerialNo) { this.fromSerialNo = fromSerialNo; }
    public Long getToSerialNo() { return toSerialNo; }
    public void setToSerialNo(Long toSerialNo) { this.toSerialNo = toSerialNo; }
    public Long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Long totalRecords) { this.totalRecords = totalRecords; }
    public String getEmailDomain() { return emailDomain; }
    public void setEmailDomain(String emailDomain) { this.emailDomain = emailDomain; }
    public String getFilterSummary() { return filterSummary; }
    public void setFilterSummary(String filterSummary) { this.filterSummary = filterSummary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
