package com.sciinov.dbms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating conference documents metadata.
 * Allows SUPER_ADMIN to update year and documentType for existing records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceDocumentUpdateRequest {
    /** Optional: Update the year of the document */
    private Integer year;

    /** Optional: Update the document type (slug or ID) */
    private String documentType;

    /** Optional: Update the file name */
    private String fileName;

    /** Optional: Add notes about the update */
    private String notes;
}

