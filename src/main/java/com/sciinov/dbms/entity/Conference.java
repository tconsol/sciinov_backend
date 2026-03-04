package com.sciinov.dbms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conferences")
public class Conference {

    @Id
    private String id;
    private String title;
    private String imageUrl;

    /** GCS blob name for the conference image */
    private String imageBlobName;

    /** ACTIVE or INACTIVE */
    private Status status;

    /** Dashboard Master IDs associated with this conference */
    @Builder.Default
    private List<String> dashboardMasterIds = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean deleted = false;

    public enum Status {
        ACTIVE, INACTIVE
    }
}
