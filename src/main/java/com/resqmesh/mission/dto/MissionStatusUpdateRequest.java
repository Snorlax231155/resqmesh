package com.resqmesh.mission.dto;

import com.resqmesh.mission.enums.MissionStatus;
import jakarta.validation.constraints.NotNull;

public class MissionStatusUpdateRequest {

    @NotNull
    private MissionStatus status;

    public MissionStatusUpdateRequest() {}

    public MissionStatusUpdateRequest(MissionStatus status) {
        this.status = status;
    }

    public MissionStatus getStatus() { return status; }
    public void setStatus(MissionStatus status) { this.status = status; }
}
