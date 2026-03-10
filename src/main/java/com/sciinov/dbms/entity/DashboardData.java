package com.sciinov.dbms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Locale;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dashboard_data")
@CompoundIndexes({
    @CompoundIndex(name = "conf_dash_email_idx",   def = "{'conferenceId': 1, 'dashboardMasterId': 1, 'email': 1}",      unique = true),
    @CompoundIndex(name = "conf_dash_serial_idx",  def = "{'conferenceId': 1, 'dashboardMasterId': 1, 'serialNo': 1}"),
    @CompoundIndex(name = "conf_dash_created_idx", def = "{'conferenceId': 1, 'dashboardMasterId': 1, 'createdAt': 1}"),
    @CompoundIndex(name = "conf_dash_deleted_idx", def = "{'conferenceId': 1, 'dashboardMasterId': 1, 'deleted': 1}")
})
public class DashboardData {

    @Id
    private String id;

    @Indexed
    private String conferenceId;

    @Indexed
    private String dashboardMasterId;

    @Indexed
    private Long serialNo;

    private String name;

    // NOTE: email setter has special normalization logic — do NOT replace with plain Lombok setter
    private String email;

    @Indexed
    private String emailExtension;  // Domain/TLD part (e.g., "gmail.com")

    private boolean status;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean deleted = false;

    /**
     * Custom email setter: normalises the value before storing.
     * Strips leading/trailing whitespace, removes internal whitespace / hidden Unicode chars,
     * and lowercases — ensures consistent duplicate detection.
     */
    public void setEmail(String email) {
        if (email == null) {
            this.email = null;
        } else {
            this.email = email.trim()
                    .replaceAll("[\\s\\u00A0\\u200B\\u200C\\u200D\\uFEFF]+", "")
                    .toLowerCase(Locale.ROOT);
        }
    }
}
