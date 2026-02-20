package com.sciinov.dbms.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Separate audit log for conference document actions (upload, download, delete, view).
 * Kept in its own collection — independent of dashboard data logs (admin_activity_logs).
 */
@Document(collection = "conference_document_logs")
public class ConferenceDocumentLog {

    @Id
    private String id;

    @Indexed
    private String adminId;

    private String adminName;

    @Indexed
    private String conferenceId;

    private String conferenceName;

    private String documentId;      // ConferenceDocument._id (null for view-list actions)
    private String fileName;
    private String documentType;    // PROGRAM / BOOK / POSITIVE_SHEETS
    private Integer year;

    @Indexed
    private ActionType actionType;

    private String description;

    private String ipAddress;

    @Indexed
    private LocalDateTime createdAt;

    public enum ActionType {
        UPLOAD,     // admin uploaded a document
        DOWNLOAD,   // admin downloaded a document
        DELETE,     // admin deleted a document
        VIEW        // admin viewed / listed documents
    }

    // ── Getters & Setters ────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getConferenceId() { return conferenceId; }
    public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }

    public String getConferenceName() { return conferenceName; }
    public void setConferenceName(String conferenceName) { this.conferenceName = conferenceName; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

