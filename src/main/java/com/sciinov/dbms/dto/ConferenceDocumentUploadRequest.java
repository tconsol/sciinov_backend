package com.sciinov.dbms.dto;

import com.sciinov.dbms.entity.ConferenceDocument;
import java.time.LocalDateTime;

/**
 * Request DTO for uploading conference documents
 */
public class ConferenceDocumentUploadRequest {
    private String conferenceId;
    private Integer year; // 2026, 2027, etc.
    private ConferenceDocument.DocumentType documentType; // PROGRAM, BOOK, POSITIVE_SHEETS
    // File is sent as multipart file in request

    // Getters and Setters
    public String getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(String conferenceId) {
        this.conferenceId = conferenceId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public ConferenceDocument.DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(ConferenceDocument.DocumentType documentType) {
        this.documentType = documentType;
    }
}

