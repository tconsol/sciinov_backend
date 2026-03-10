package com.sciinov.dbms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for bulk Excel upload operations.
 * Provides detailed statistics and rollback information for uploaded records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponse {
    private boolean success;
    private String message;
    private String fileName;
    private String conferenceId;
    private String dashboardMasterId;
    private long totalRecordsInFile;
    private long newRecordsAdded;
    private long duplicateRecordsIgnored;
    private long invalidRecordsSkipped;
    private long processingTimeMs;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
    private String status; // SUCCESS, PARTIAL_ROLLBACK, COMPLETE_ROLLBACK, FAILED
    private String rollbackReason;
    private List<String> failedRecords;
    private List<String> warnings;
    private List<String> sampleInvalidRows;
}

