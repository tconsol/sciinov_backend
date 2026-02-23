package com.sciinov.dbms.dto;

import com.sciinov.dbms.entity.Conference;

public class UpdateConferenceStatusRequest {
    private Conference.Status status;

    public UpdateConferenceStatusRequest() {}

    public UpdateConferenceStatusRequest(Conference.Status status) {
        this.status = status;
    }

    // Getters and Setters
    public Conference.Status getStatus() {
        return status;
    }

    public void setStatus(Conference.Status status) {
        this.status = status;
    }
}

