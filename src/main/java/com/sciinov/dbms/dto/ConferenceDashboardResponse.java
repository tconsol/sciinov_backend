package com.sciinov.dbms.dto;

import com.sciinov.dbms.entity.DashboardMaster;

import java.util.List;

public class ConferenceDashboardResponse {
    private String conferenceId;
    private String conferenceTitle;
    private List<DashboardMaster> dashboards;
    private String message;
    private boolean success;

    public ConferenceDashboardResponse() {}

    public ConferenceDashboardResponse(String conferenceId, String conferenceTitle,
                                      List<DashboardMaster> dashboards, String message, boolean success) {
        this.conferenceId = conferenceId;
        this.conferenceTitle = conferenceTitle;
        this.dashboards = dashboards;
        this.message = message;
        this.success = success;
    }

    public String getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(String conferenceId) {
        this.conferenceId = conferenceId;
    }

    public String getConferenceTitle() {
        return conferenceTitle;
    }

    public void setConferenceTitle(String conferenceTitle) {
        this.conferenceTitle = conferenceTitle;
    }

    public List<DashboardMaster> getDashboards() {
        return dashboards;
    }

    public void setDashboards(List<DashboardMaster> dashboards) {
        this.dashboards = dashboards;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}

