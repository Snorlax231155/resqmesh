package com.resqmesh.dispatch.policy;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.mission.entity.Mission;

import java.util.List;

public class Assignment {
    private Mission mission;
    private Agent agent;
    private int estimatedTotalTime;
    private List<String> routeEdgeIds;
    private List<String> routeNodeIds;
    private String rationale;

    public Assignment(Mission mission, Agent agent, int estimatedTotalTime, List<String> routeEdgeIds, List<String> routeNodeIds, String rationale) {
        this.mission = mission;
        this.agent = agent;
        this.estimatedTotalTime = estimatedTotalTime;
        this.routeEdgeIds = routeEdgeIds;
        this.routeNodeIds = routeNodeIds;
        this.rationale = rationale;
    }

    public Mission getMission() { return mission; }
    public Agent getAgent() { return agent; }
    public int getEstimatedTotalTime() { return estimatedTotalTime; }
    public List<String> getRouteEdgeIds() { return routeEdgeIds; }
    public List<String> getRouteNodeIds() { return routeNodeIds; }
    public String getRationale() { return rationale; }
}
