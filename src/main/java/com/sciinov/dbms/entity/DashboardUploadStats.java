package com.sciinov.dbms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dashboard_upload_stats")
public class DashboardUploadStats {

    @Id
    private String id;
    private String adminId;
    private String conferenceId;
    private String dashboardMasterId;
    private LocalDateTime uploadedAt;
    private int totalRecordsInFile;
    private int newRecordsAdded;
    private int duplicateRecordsIgnored;
    private String fileName;
}
