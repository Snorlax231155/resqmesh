package com.resqmesh.network.dto;

import java.util.List;

public class RouteResponse {

    private boolean reachable;
    private Integer totalTravelTimeMinutes;
    private List<String> nodePath;
    private List<String> roadPath;

    public RouteResponse() {}

    public RouteResponse(boolean reachable, Integer totalTravelTimeMinutes, List<String> nodePath, List<String> roadPath) {
        this.reachable = reachable;
        this.totalTravelTimeMinutes = totalTravelTimeMinutes;
        this.nodePath = nodePath;
        this.roadPath = roadPath;
    }

    public boolean isReachable() { return reachable; }
    public void setReachable(boolean reachable) { this.reachable = reachable; }
    public Integer getTotalTravelTimeMinutes() { return totalTravelTimeMinutes; }
    public void setTotalTravelTimeMinutes(Integer totalTravelTimeMinutes) { this.totalTravelTimeMinutes = totalTravelTimeMinutes; }
    public List<String> getNodePath() { return nodePath; }
    public void setNodePath(List<String> nodePath) { this.nodePath = nodePath; }
    public List<String> getRoadPath() { return roadPath; }
    public void setRoadPath(List<String> roadPath) { this.roadPath = roadPath; }
}
