package com.sciinov.dbms.dto;

import com.sciinov.dbms.entity.DashboardMaster;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceDashboardResponse {
    private String conferenceId;
    private String conferenceTitle;
    private List<DashboardMaster> dashboards;
    private String message;
    private boolean success;
}
