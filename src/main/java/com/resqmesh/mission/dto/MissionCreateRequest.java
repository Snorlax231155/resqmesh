package com.resqmesh.mission.dto;

import com.resqmesh.mission.enums.MissionPriority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class MissionCreateRequest {

    @NotBlank
    private String missionCode;

    @NotBlank
    private String pickupNodeId;

    @NotBlank
    private String destinationNodeId;

    @NotNull
    private MissionPriority priority;

    @NotNull
    @Future
    private Instant deadline;

    public MissionCreateRequest() {}

    public MissionCreateRequest(String missionCode, String pickupNodeId, String destinationNodeId, MissionPriority priority, Instant deadline) {
        this.missionCode = missionCode;
        this.pickupNodeId = pickupNodeId;
        this.destinationNodeId = destinationNodeId;
        this.priority = priority;
        this.deadline = deadline;
    }

    public String getMissionCode() { return missionCode; }
    public void setMissionCode(String missionCode) { this.missionCode = missionCode; }
    public String getPickupNodeId() { return pickupNodeId; }
    public void setPickupNodeId(String pickupNodeId) { this.pickupNodeId = pickupNodeId; }
    public String getDestinationNodeId() { return destinationNodeId; }
    public void setDestinationNodeId(String destinationNodeId) { this.destinationNodeId = destinationNodeId; }
    public MissionPriority getPriority() { return priority; }
    public void setPriority(MissionPriority priority) { this.priority = priority; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
}
