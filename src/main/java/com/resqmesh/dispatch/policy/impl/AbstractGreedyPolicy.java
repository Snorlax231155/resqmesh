package com.resqmesh.dispatch.policy.impl;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.dispatch.policy.Assignment;
import com.resqmesh.dispatch.policy.TriagePolicy;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.dto.RouteResponse;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGreedyPolicy implements TriagePolicy {
    
    protected abstract List<Mission> sortMissions(List<Mission> pending, List<Agent> available, RoutingEngine engine);

    @Override
    public List<Assignment> assign(List<Mission> pending, List<Agent> available, RoutingEngine engine) {
        List<Assignment> assignments = new ArrayList<>();
        List<Agent> pool = new ArrayList<>(available);

        List<Mission> sorted = sortMissions(pending, pool, engine);

        for (Mission mission : sorted) {
            if (pool.isEmpty()) break;

            Agent bestAgent = null;
            int lowestTime = Integer.MAX_VALUE;
            List<String> bestRoute = null;

            for (Agent agent : pool) {
                RouteResponse r1 = engine.bidirectionalAStar(agent.getCurrentNodeId(), mission.getPickupNodeId());
                if (!r1.isReachable()) continue;
                RouteResponse r2 = engine.bidirectionalAStar(mission.getPickupNodeId(), mission.getDestinationNodeId());
                if (!r2.isReachable()) continue;

                int time = r1.getTotalTravelTimeMinutes() + r2.getTotalTravelTimeMinutes();
                if (time < lowestTime) {
                    lowestTime = time;
                    bestAgent = agent;
                    bestRoute = new ArrayList<>();
                    if (r1.getRoadPath() != null) bestRoute.addAll(r1.getRoadPath());
                    if (r2.getRoadPath() != null) bestRoute.addAll(r2.getRoadPath());
                }
            }

            if (bestAgent != null) {
                assignments.add(new Assignment(mission, bestAgent, lowestTime, bestRoute, rationale(mission, bestAgent)));
                pool.remove(bestAgent);
            }
        }
        return assignments;
    }
}
