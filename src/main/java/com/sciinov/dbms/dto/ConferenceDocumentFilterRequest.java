package com.sciinov.dbms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter request for listing/searching conference documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceDocumentFilterRequest {
    private String conferenceId;
    private String conferenceName;
    private Integer year;
    /** Document type slug, e.g. "program", "book", "positive_sheets" */
    private String documentType;
    @Builder.Default
    private Integer pageNumber = 0;
    @Builder.Default
    private Integer pageSize = 10;
    /** Sort field: year | uploadedAt | updatedAt */
    @Builder.Default
    private String sortBy = "uploadedAt";
    /** asc or desc */
    @Builder.Default
    private String sortOrder = "desc";
}
