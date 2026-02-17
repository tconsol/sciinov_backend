package com.sciinov.dbms.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "dashboard_upload_stats")
public class DashboardUploadStats {
    @Id
    private String id;
    private String adminId;
    private String conferenceId;
    private String dashboardMasterId;
    private LocalDateTime uploadedAt;
    private int totalRecordsInFile;
    private int newRecordsAdded;
    private int duplicateRecordsIgnored;
    private String fileName;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public String getConferenceId() { return conferenceId; }
    public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }
    public String getDashboardMasterId() { return dashboardMasterId; }
    public void setDashboardMasterId(String dashboardMasterId) { this.dashboardMasterId = dashboardMasterId; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public int getTotalRecordsInFile() { return totalRecordsInFile; }
    public void setTotalRecordsInFile(int totalRecordsInFile) { this.totalRecordsInFile = totalRecordsInFile; }
    public int getNewRecordsAdded() { return newRecordsAdded; }
    public void setNewRecordsAdded(int newRecordsAdded) { this.newRecordsAdded = newRecordsAdded; }
    public int getDuplicateRecordsIgnored() { return duplicateRecordsIgnored; }
    public void setDuplicateRecordsIgnored(int duplicateRecordsIgnored) { this.duplicateRecordsIgnored = duplicateRecordsIgnored; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
