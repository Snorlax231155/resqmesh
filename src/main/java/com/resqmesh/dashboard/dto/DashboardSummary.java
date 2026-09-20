package com.resqmesh.dashboard.dto;

public class DashboardSummary {

    private long totalAgents;
    private long availableAgents;
    private long totalMissions;
    private long pendingMissions;
    private long activeDisruptions;

    public DashboardSummary() {}

    public DashboardSummary(long totalAgents, long availableAgents, long totalMissions, long pendingMissions, long activeDisruptions) {
        this.totalAgents = totalAgents;
        this.availableAgents = availableAgents;
        this.totalMissions = totalMissions;
        this.pendingMissions = pendingMissions;
        this.activeDisruptions = activeDisruptions;
    }

    public long getTotalAgents() { return totalAgents; }
    public void setTotalAgents(long totalAgents) { this.totalAgents = totalAgents; }
    public long getAvailableAgents() { return availableAgents; }
    public void setAvailableAgents(long availableAgents) { this.availableAgents = availableAgents; }
    public long getTotalMissions() { return totalMissions; }
    public void setTotalMissions(long totalMissions) { this.totalMissions = totalMissions; }
    public long getPendingMissions() { return pendingMissions; }
    public void setPendingMissions(long pendingMissions) { this.pendingMissions = pendingMissions; }
    public long getActiveDisruptions() { return activeDisruptions; }
    public void setActiveDisruptions(long activeDisruptions) { this.activeDisruptions = activeDisruptions; }
}
