package com.sciinov.dbms.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "dashboard_data")
@CompoundIndexes({
    @CompoundIndex(name = "conf_dash_email_idx", def = "{'conferenceId': 1, 'dashboardMasterId': 1, 'email': 1}", unique = true),
    @CompoundIndex(name = "conf_dash_serial_idx", def = "{'conferenceId': 1, 'dashboardMasterId': 1, 'serialNo': 1}"),
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
    private String email;


    private boolean status;
    
    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private boolean deleted = false;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConferenceId() { return conferenceId; }
    public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }
    public String getDashboardMasterId() { return dashboardMasterId; }
    public void setDashboardMasterId(String dashboardMasterId) { this.dashboardMasterId = dashboardMasterId; }
    public Long getSerialNo() { return serialNo; }
    public void setSerialNo(Long serialNo) { this.serialNo = serialNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
