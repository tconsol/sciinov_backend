package com.sciinov.dbms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a DocumentType.
 */
public class DocumentTypeRequest {

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
    private String displayName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Optional sort order for UI dropdowns.
     * If not provided, it will be assigned automatically.
     */
    private Integer sortOrder;

    /**
     * Whether this type is active (available for new uploads).
     * Default: true
     */
    private Boolean active;

    // Getters and Setters
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}

