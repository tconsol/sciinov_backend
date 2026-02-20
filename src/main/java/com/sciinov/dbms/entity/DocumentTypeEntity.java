package com.sciinov.dbms.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents a document type (e.g., Program, Book, Positive Sheets, etc.)
 * Managed by SUPER_ADMIN — replaces the hardcoded DocumentType enum.
 *
 * Collection: document_types
 */
@Document(collection = "document_types")
public class DocumentTypeEntity {

    @Id
    private String id;

    /**
     * Unique slug used as the key in ConferenceDocument.
     * Auto-generated from displayName (lowercase, underscored).
     * e.g., "program", "book", "positive_sheets"
     */
    @Indexed(unique = true)
    private String slug;

    /**
     * Human-readable display name shown on UI.
     * e.g., "Program", "Book", "Positive Sheets"
     */
    private String displayName;

    /**
     * Optional description of what this document type represents.
     */
    private String description;

    /**
     * Used as the folder name in GCS bucket path.
     * Auto-derived from slug (slug with '-' instead of '_').
     * e.g., "positive-sheets"
     */
    private String folderName;

    /**
     * Display order for sorting in UI dropdowns.
     */
    private Integer sortOrder;

    /**
     * Whether this type is active (available for selection).
     */
    private boolean active = true;

    private boolean deleted = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdByUserId;
    private String createdByUserName;

    // ─── Getters & Setters ───────────────────────────────────────────

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

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getCreatedByUserName() { return createdByUserName; }
    public void setCreatedByUserName(String createdByUserName) { this.createdByUserName = createdByUserName; }

    /**
     * Helper: generate slug from displayName.
     * e.g., "Positive Sheets" → "positive_sheets"
     */
    public static String toSlug(String displayName) {
        if (displayName == null) return "";
        return displayName.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");
    }

    /**
     * Helper: generate folderName from slug.
     * e.g., "positive_sheets" → "positive-sheets"
     */
    public static String toFolderName(String slug) {
        if (slug == null) return "";
        return slug.replace('_', '-');
    }
}

