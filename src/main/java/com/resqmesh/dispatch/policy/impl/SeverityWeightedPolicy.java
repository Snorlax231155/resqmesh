package com.resqmesh.dispatch.policy.impl;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.dto.RouteResponse;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class SeverityWeightedPolicy extends AbstractGreedyPolicy {
    @Override
    public String name() {
        return "SeverityWeighted";
    }

    @Override
    public String rationale(Mission m, Agent a) {
        return "Assigned based on severity, tie-broken by ETA";
    }

    @Override
    protected List<Mission> sortMissions(List<Mission> pending, List<Agent> available, RoutingEngine engine) {
        return pending.stream()
                .sorted(Comparator.comparing(Mission::getPriority)
                        .thenComparingInt(m -> minEta(m, available, engine)))
                .collect(Collectors.toList());
    }

    private int minEta(Mission mission, List<Agent> available, RoutingEngine engine) {
        int lowestTime = Integer.MAX_VALUE;
        for (Agent agent : available) {
            RouteResponse r1 = engine.bidirectionalAStar(agent.getCurrentNodeId(), mission.getPickupNodeId());
            if (!r1.isReachable()) continue;
            RouteResponse r2 = engine.bidirectionalAStar(mission.getPickupNodeId(), mission.getDestinationNodeId());
            if (!r2.isReachable()) continue;
            
            int time = r1.getTotalTravelTimeMinutes() + r2.getTotalTravelTimeMinutes();
            if (time < lowestTime) {
                lowestTime = time;
            }
        }
        return lowestTime;
    }
}
