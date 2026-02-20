package com.sciinov.dbms.dto;

import java.time.LocalDate;

public class ExportFilterRequest {
    private String conferenceId;
    private String dashboardMasterId;

    // Serial number range filter
    private Long fromSerialNo;
    private Long toSerialNo;

    // Date range filter (for upload date)
    private LocalDate startDate;
    private LocalDate endDate;


    // Email domain filter (e.g., "gmail.com")
    private String emailDomain;

    // Getters and Setters
    public String getConferenceId() { return conferenceId; }
    public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }
    public String getDashboardMasterId() { return dashboardMasterId; }
    public void setDashboardMasterId(String dashboardMasterId) { this.dashboardMasterId = dashboardMasterId; }
    public Long getFromSerialNo() { return fromSerialNo; }
    public void setFromSerialNo(Long fromSerialNo) { this.fromSerialNo = fromSerialNo; }
    public Long getToSerialNo() { return toSerialNo; }
    public void setToSerialNo(Long toSerialNo) { this.toSerialNo = toSerialNo; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getEmailDomain() { return emailDomain; }
    public void setEmailDomain(String emailDomain) { this.emailDomain = emailDomain; }
}

