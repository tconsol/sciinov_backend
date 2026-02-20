package com.sciinov.dbms.dto;

import java.util.List;

/**
 * Request DTO for filtering conference documents
 */
public class ConferenceDocumentFilterRequest {
    private String conferenceId;
    private String conferenceName;
    private Integer year;
    private String documentType; // PROGRAM, BOOK, POSITIVE_SHEETS
    private Integer pageNumber;
    private Integer pageSize;
    private String sortBy; // year, uploadedAt, updatedAt
    private String sortOrder; // asc, desc

    // Getters and Setters
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

    public Integer getPageNumber() {
        return pageNumber != null ? pageNumber : 0;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize != null ? pageSize : 10;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getSortBy() {
        return sortBy != null ? sortBy : "year";
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder != null ? sortOrder : "desc";
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}

