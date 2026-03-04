package com.sciinov.dbms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for uploading conference documents via multipart form.
 * documentType is a dynamic slug managed by SUPER_ADMIN via /api/document-types.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceDocumentUploadRequest {
    private String conferenceId;
    private Integer year;
    /** Slug of the document type, e.g. "program", "book", "positive_sheets" */
    private String documentType;
}
