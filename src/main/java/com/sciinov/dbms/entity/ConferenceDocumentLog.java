package com.sciinov.dbms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Audit log for conference document actions (upload, download, delete, view).
 * Kept separate from dashboard data logs (admin_activity_logs).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conference_document_logs")
public class ConferenceDocumentLog {

    @Id
    private String id;

    @Indexed
    private String adminId;

    private String adminName;

    @Indexed
    private String conferenceId;

    private String conferenceName;

    /** ConferenceDocument._id — null for view-list actions */
    private String documentId;

    private String fileName;

    /** Document type slug, e.g. "program", "book" */
    private String documentType;

    private Integer year;

    @Indexed
    private ActionType actionType;

    private String description;

    private String ipAddress;

    @Indexed
    private LocalDateTime createdAt;

    public enum ActionType {
        /** Admin uploaded a document */
        UPLOAD,
        /** Admin downloaded a document */
        DOWNLOAD,
        /** Admin deleted a document */
        DELETE,
        /** Admin updated document metadata */
        UPDATE,
        /** Admin viewed / listed documents */
        VIEW
    }
}
