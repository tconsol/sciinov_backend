package com.sciinov.dbms.dto;

import java.util.List;

public class ConferenceDashboardRequest {
    private String conferenceId;
    private List<String> dashboardMasterIds;

    public ConferenceDashboardRequest() {}

    public ConferenceDashboardRequest(String conferenceId, List<String> dashboardMasterIds) {
        this.conferenceId = conferenceId;
        this.dashboardMasterIds = dashboardMasterIds;
    }

    public String getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(String conferenceId) {
        this.conferenceId = conferenceId;
    }

    public List<String> getDashboardMasterIds() {
        return dashboardMasterIds;
    }

    public void setDashboardMasterIds(List<String> dashboardMasterIds) {
        this.dashboardMasterIds = dashboardMasterIds;
    }
}

