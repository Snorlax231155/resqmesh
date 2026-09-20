package com.resqmesh.dispatch.dto;

import java.util.UUID;

public class AssignmentResponse {

    private UUID missionId;
    private UUID agentId;
    private String routeSource;
    private String routeDestination;
    private Integer totalTravelTimeMinutes;

    public AssignmentResponse() {}

    public AssignmentResponse(UUID missionId, UUID agentId, String routeSource, String routeDestination, Integer totalTravelTimeMinutes) {
        this.missionId = missionId;
        this.agentId = agentId;
        this.routeSource = routeSource;
        this.routeDestination = routeDestination;
        this.totalTravelTimeMinutes = totalTravelTimeMinutes;
    }

    public UUID getMissionId() { return missionId; }
    public void setMissionId(UUID missionId) { this.missionId = missionId; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public String getRouteSource() { return routeSource; }
    public void setRouteSource(String routeSource) { this.routeSource = routeSource; }
    public String getRouteDestination() { return routeDestination; }
    public void setRouteDestination(String routeDestination) { this.routeDestination = routeDestination; }
    public Integer getTotalTravelTimeMinutes() { return totalTravelTimeMinutes; }
    public void setTotalTravelTimeMinutes(Integer totalTravelTimeMinutes) { this.totalTravelTimeMinutes = totalTravelTimeMinutes; }
}
