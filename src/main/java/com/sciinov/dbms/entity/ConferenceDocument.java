package com.sciinov.dbms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conference_documents")
@CompoundIndex(name = "conference_year_type_idx",
        def = "{'conferenceId': 1, 'year': 1, 'documentType': 1}", unique = true)
public class ConferenceDocument {

    @Id
    private String id;

    @Indexed
    private String conferenceId;

    private String conferenceName;

    @Indexed
    private Integer year;

    /**
     * Slug of the document type (references DocumentTypeEntity.slug).
     * e.g., "program", "book", "positive_sheets"
     * Dynamically managed by SUPER_ADMIN via /api/document-types.
     */
    @Indexed
    private String documentType;

    /**
     * Display name stored at upload time to avoid extra join.
     * e.g., "Program", "Book"
     */
    private String documentTypeDisplayName;

    private String fileName;

    /** GCS blob name — exact path in bucket, used for download/delete */
    private String blobName;

    /** Kept in sync with blobName */
    private String filePath;

    private String publicUrl;
    private Long fileSize;
    private String contentType;

    @CreatedDate
    private LocalDateTime uploadedAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String uploadedByUserId;
    private String uploadedByUserName;

    @Builder.Default
    private boolean deleted = false;
}
