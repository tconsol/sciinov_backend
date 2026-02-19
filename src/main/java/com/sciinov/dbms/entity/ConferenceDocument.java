package com.sciinov.dbms.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.LocalDateTime;

@Document(collection = "conference_documents")
@CompoundIndex(name = "conference_year_type_idx", def = "{'conferenceId': 1, 'year': 1, 'documentType': 1}", unique = true)
public class ConferenceDocument {
    @Id
    private String id;

    @Indexed
    private String conferenceId;

    private String conferenceName;

    @Indexed
    private Integer year; // e.g., 2026, 2027, 2028

    @Indexed
    private DocumentType documentType; // PROGRAM, BOOK, POSITIVE_SHEETS

    private String fileName;

    private String filePath; // GCS path: conferences/{conference}/{year}/{documentType}/{fileName}

    private String publicUrl; // GCS public URL

    private Long fileSize; // in bytes

    private String contentType; // e.g., application/pdf, application/vnd.ms-excel

    @CreatedDate
    private LocalDateTime uploadedAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String uploadedByUserId;

    private String uploadedByUserName;

    private boolean deleted = false;

    public enum DocumentType {
        PROGRAM("Program"),
        BOOK("Book"),
        POSITIVE_SHEETS("Positive Sheets");

        private final String displayName;

        DocumentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getFolderName() {
            return this.name().toLowerCase().replace('_', '-');
        }
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

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

