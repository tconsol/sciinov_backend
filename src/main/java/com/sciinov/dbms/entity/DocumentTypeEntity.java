package com.sciinov.dbms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents a document type (e.g., Program, Book, Positive Sheets).
 * Managed by SUPER_ADMIN — replaces any hardcoded DocumentType enum.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /** Human-readable name shown on UI, e.g. "Program", "Positive Sheets" */
    private String displayName;

    /** Optional description of what this document type represents */
    private String description;

    /**
     * Folder name used in GCS bucket path.
     * Auto-derived from slug with hyphens instead of underscores.
     * e.g., "positive-sheets"
     */
    private String folderName;

    /** Sort order for UI dropdowns */
    private Integer sortOrder;

    /** true = available for new uploads */
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean deleted = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdByUserId;
    private String createdByUserName;

    /** Generate slug from displayName. e.g., "Positive Sheets" to "positive_sheets" */
    public static String toSlug(String displayName) {
        if (displayName == null) return "";
        return displayName.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");
    }

    /** Generate folderName from slug. e.g., "positive_sheets" to "positive-sheets" */
    public static String toFolderName(String slug) {
        if (slug == null) return "";
        return slug.replace('_', '-');
    }
}
