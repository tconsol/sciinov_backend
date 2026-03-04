package com.sciinov.dbms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admin_activity_logs")
public class AdminActivityLog {

    @Id
    private String id;
    private String adminId;
    private String adminName;
    private String conferenceId;
    private String dashboardMasterId;
    private ActionType actionType;
    private String description;
    private String ipAddress;

    // ── Serial range & filter info ──────────────────────────────────
    private Long fromSerialNo;
    private Long toSerialNo;
    private Long totalRecords;
    /** Email domain filter used (if any), e.g. "gmail.com" */
    private String emailDomain;
    /** Human-readable summary of applied filters */
    private String filterSummary;

    // ── TLD range filtering fields ──────────────────────────────────
    /** e.g. "com", "edu", "org" */
    private String tldExtension;
    /** Total records matching TLD in the serial range */
    private Long totalMatchingInRange;
    /** Actual records returned (max 1000 per request) */
    private Integer recordsReturned;
    /** true if more than 1000 matching records exist */
    private Boolean hasMoreRecords;
    /** "complete" or "partial" */
    private String rangeCoverage;
    /** Suggested next fromSerialNo for pagination */
    private Long suggestedNextFrom;
    /** Suggested next toSerialNo for pagination */
    private Long suggestedNextTo;
    /** Duplicate download warning or other alerts */
    private String warningMessage;
    /** Percentage of total records exported */
    private Double percentageExported;

    @CreatedDate
    private LocalDateTime createdAt;

    public enum ActionType {
        CREATE, UPDATE, DELETE, VIEW,
        DOWNLOAD_EXCEL, DOWNLOAD_PDF,
        UPLOAD_EXCEL,
        UPLOAD_FILE, DOWNLOAD_FILE, DELETE_FILE,
        /** TLD domain-range filter — view */
        VIEW_TLD_FILTER,
        /** TLD domain-range filter — Excel download */
        DOWNLOAD_EXCEL_TLD,
        /** TLD domain-range filter — PDF download */
        DOWNLOAD_PDF_TLD
    }
}
