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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;
    private String firstName;
    private String lastName;

    @Indexed(unique = true, sparse = true)
    private String phoneNumber;

    @Indexed(unique = true, sparse = true)
    private String email;

    @Indexed(unique = true)
    private String userId;

    private String password;

    /** SUPER_ADMIN or ADMIN */
    private Role role;

    /** true = active, false = deactivated */
    private boolean status;

    /** Assigned conference IDs — ADMIN only */
    private List<String> conferenceIds;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean deleted = false;

    public enum Role {
        SUPER_ADMIN, ADMIN
    }
}
