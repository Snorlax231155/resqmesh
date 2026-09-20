package com.resqmesh.dispatch.policy.impl;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.dto.RouteResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class EarliestDeadlineFirstPolicy extends AbstractGreedyPolicy {
    @Override
    public String name() {
        return "EarliestDeadlineFirst";
    }

    @Override
    public String rationale(Mission m, Agent a) {
        return "Assigned based on minimum deadline slack";
    }

    @Override
    protected List<Mission> sortMissions(List<Mission> pending, List<Agent> available, RoutingEngine engine) {
        Instant now = Instant.now();
        return pending.stream()
                .sorted(Comparator.comparingLong(m -> calculateSlack(m, available, engine, now)))
                .collect(Collectors.toList());
    }

    private long calculateSlack(Mission mission, List<Agent> available, RoutingEngine engine, Instant now) {
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
        
        if (lowestTime == Integer.MAX_VALUE) {
            return Long.MAX_VALUE; // Unreachable, put at end? Or beginning?
            // Usually if unreachable, we can't assign it anyway, so it doesn't matter too much. Let's put at the end.
        }

        long minutesUntilDeadline = ChronoUnit.MINUTES.between(now, mission.getDeadline());
        return minutesUntilDeadline - lowestTime;
    }
}
