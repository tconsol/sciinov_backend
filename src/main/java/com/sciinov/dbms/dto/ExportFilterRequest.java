package com.sciinov.dbms.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExportFilterRequest {
    private String conferenceId;
    private String dashboardMasterId;
    private Long fromSerialNo;
    private Long toSerialNo;
    private LocalDate startDate;
    private LocalDate endDate;
    /** Full email domain filter, e.g. "gmail.com" or "*.com" for TLD */
    private String emailDomain;
}
