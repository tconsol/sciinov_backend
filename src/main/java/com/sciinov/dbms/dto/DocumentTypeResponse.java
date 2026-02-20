package com.sciinov.dbms.dto;

import com.sciinov.dbms.entity.DocumentTypeEntity;
import java.time.LocalDateTime;

/**
 * Response DTO for DocumentType.
 */
public class DocumentTypeResponse {

    private String id;
    private String slug;
    private String displayName;
    private String description;
    private String folderName;
    private Integer sortOrder;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByUserId;
    private String createdByUserName;

    public DocumentTypeResponse() {}

    public DocumentTypeResponse(DocumentTypeEntity entity) {
        this.id = entity.getId();
        this.slug = entity.getSlug();
        this.displayName = entity.getDisplayName();
        this.description = entity.getDescription();
        this.folderName = entity.getFolderName();
        this.sortOrder = entity.getSortOrder();
        this.active = entity.isActive();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.createdByUserId = entity.getCreatedByUserId();
        this.createdByUserName = entity.getCreatedByUserName();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getCreatedByUserName() { return createdByUserName; }
    public void setCreatedByUserName(String createdByUserName) { this.createdByUserName = createdByUserName; }
}

