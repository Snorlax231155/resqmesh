package com.resqmesh.network.dto;

import java.util.List;
import java.util.UUID;

public class AgentRepositionedEvent {
    private UUID agentId;
    private String agentCode;
    private String targetNodeId;
    private List<String> routeNodeIds;

    public AgentRepositionedEvent(UUID agentId, String agentCode, String targetNodeId, List<String> routeNodeIds) {
        this.agentId = agentId;
        this.agentCode = agentCode;
        this.targetNodeId = targetNodeId;
        this.routeNodeIds = routeNodeIds;
    }

    public UUID getAgentId() { return agentId; }
    public String getAgentCode() { return agentCode; }
    public String getTargetNodeId() { return targetNodeId; }
    public List<String> getRouteNodeIds() { return routeNodeIds; }
}
