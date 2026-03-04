package com.sciinov.dbms.dto;

import com.sciinov.dbms.entity.DocumentTypeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /** Convenience constructor: map from entity */
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
}
