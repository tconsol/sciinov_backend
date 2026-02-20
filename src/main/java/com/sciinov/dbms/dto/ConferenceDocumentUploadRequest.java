package com.sciinov.dbms.dto;

/**
 * Request DTO for uploading conference documents.
 * documentType is now a dynamic slug (e.g., "program", "book", "positive_sheets")
 * managed by SUPER_ADMIN via /api/document-types
 */
public class ConferenceDocumentUploadRequest {
    private String conferenceId;
    private Integer year;
    /** Slug of the document type, e.g. "program", "book", "positive_sheets" */
    private String documentType;

    public String getConferenceId() { return conferenceId; }
    public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
}
