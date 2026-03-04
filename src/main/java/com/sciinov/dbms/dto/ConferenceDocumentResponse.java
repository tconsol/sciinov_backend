package com.sciinov.dbms.dto;

import com.sciinov.dbms.entity.ConferenceDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceDocumentResponse {
    private String id;
    private String conferenceId;
    private String conferenceName;
    private Integer year;
    /** Slug, e.g. "program" */
    private String documentType;
    /** Human-readable name, e.g. "Program" */
    private String documentTypeDisplayName;
    private String fileName;
    private String blobName;
    private String filePath;
    private String publicUrl;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private String uploadedByUserId;
    private String uploadedByUserName;

    /** Convenience constructor: map from entity */
    public ConferenceDocumentResponse(ConferenceDocument doc) {
        this.id = doc.getId();
        this.conferenceId = doc.getConferenceId();
        this.conferenceName = doc.getConferenceName();
        this.year = doc.getYear();
        this.documentType = doc.getDocumentType();
        this.documentTypeDisplayName = doc.getDocumentTypeDisplayName();
        this.fileName = doc.getFileName();
        this.blobName = doc.getBlobName();
        this.filePath = doc.getFilePath();
        this.publicUrl = doc.getPublicUrl();
        this.fileSize = doc.getFileSize();
        this.contentType = doc.getContentType();
        this.uploadedAt = doc.getUploadedAt();
        this.updatedAt = doc.getUpdatedAt();
        this.uploadedByUserId = doc.getUploadedByUserId();
        this.uploadedByUserName = doc.getUploadedByUserName();
    }
}
