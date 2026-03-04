package com.sciinov.dbms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating or updating a DocumentType.
 */
@Data
public class DocumentTypeRequest {

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
    private String displayName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /** Optional sort order for UI dropdowns. Auto-assigned if not provided. */
    private Integer sortOrder;

    /** Whether this type is active. Defaults to true if not provided. */
    private Boolean active;
}
