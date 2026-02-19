package com.sciinov.dbms.dto;

import com.sciinov.dbms.entity.ConferenceDocument;
import java.time.LocalDateTime;

/**
 * Response DTO for conference documents
 */
public class ConferenceDocumentResponse {
    private String id;
    private String conferenceId;
    private String conferenceName;
    private Integer year;
    private String documentType;
    private String documentTypeDisplayName;
    private String fileName;
    private String filePath;
    private String publicUrl;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private String uploadedByUserId;
    private String uploadedByUserName;

    // Constructor from Entity
    public ConferenceDocumentResponse(ConferenceDocument doc) {
        this.id = doc.getId();
        this.conferenceId = doc.getConferenceId();
        this.conferenceName = doc.getConferenceName();
        this.year = doc.getYear();
        this.documentType = doc.getDocumentType().name();
        this.documentTypeDisplayName = doc.getDocumentType().getDisplayName();
        this.fileName = doc.getFileName();
        this.filePath = doc.getFilePath();
        this.publicUrl = doc.getPublicUrl();
        this.fileSize = doc.getFileSize();
        this.contentType = doc.getContentType();
        this.uploadedAt = doc.getUploadedAt();
        this.updatedAt = doc.getUpdatedAt();
        this.uploadedByUserId = doc.getUploadedByUserId();
        this.uploadedByUserName = doc.getUploadedByUserName();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(String conferenceId) {
        this.conferenceId = conferenceId;
    }

    public String getConferenceName() {
        return conferenceName;
    }

    public void setConferenceName(String conferenceName) {
        this.conferenceName = conferenceName;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentTypeDisplayName() {
        return documentTypeDisplayName;
    }

    public void setDocumentTypeDisplayName(String documentTypeDisplayName) {
        this.documentTypeDisplayName = documentTypeDisplayName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUploadedByUserId() {
        return uploadedByUserId;
    }

    public void setUploadedByUserId(String uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
    }

    public String getUploadedByUserName() {
        return uploadedByUserName;
    }

    public void setUploadedByUserName(String uploadedByUserName) {
        this.uploadedByUserName = uploadedByUserName;
    }
}

