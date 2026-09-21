package com.resqmesh.dispatch.policy.impl;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.dispatch.optimization.HungarianSolver;
import com.resqmesh.dispatch.policy.Assignment;
import com.resqmesh.dispatch.policy.TriagePolicy;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.dto.RouteResponse;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ExpectedLivesSavedPolicy implements TriagePolicy {

    private final HungarianSolver solver = new HungarianSolver();

    @Override
    public String name() {
        return "ExpectedLivesSaved";
    }

    @Override
    public String rationale(Mission m, Agent a) {
        return "Optimal global assignment maximizing expected survival";
    }

    @Override
    public List<Assignment> assign(List<Mission> pending, List<Agent> available, RoutingEngine engine) {
        List<Assignment> assignments = new ArrayList<>();
        if (pending.isEmpty() || available.isEmpty()) return assignments;

        int n = pending.size();
        int m = available.size();
        double[][] costMatrix = new double[n][m];
        
        // Caches
        List<List<String>> bestRoutes = new ArrayList<>();
        int[][] ETAs = new int[n][m];

        for (int i = 0; i < n; i++) {
            Mission mission = pending.get(i);
            double severityWeight = getSeverityWeight(mission);
            bestRoutes.add(new ArrayList<>());
            
            for (int j = 0; j < m; j++) {
                Agent agent = available.get(j);
                
                RouteResponse r1 = engine.bidirectionalAStar(agent.getCurrentNodeId(), mission.getPickupNodeId());
                RouteResponse r2 = engine.bidirectionalAStar(mission.getPickupNodeId(), mission.getDestinationNodeId());
                
                bestRoutes.get(i).add(null); // placeholder
                
                if (r1.isReachable() && r2.isReachable()) {
                    int time = r1.getTotalTravelTimeMinutes() + r2.getTotalTravelTimeMinutes();
                    ETAs[i][j] = time;
                    
                    double expectedLives = severityWeight * Math.exp(-0.05 * time);
                    costMatrix[i][j] = 1000.0 - expectedLives; // Minimization
                } else {
                    costMatrix[i][j] = 1000.0; // Unreachable
                    ETAs[i][j] = Integer.MAX_VALUE;
                }
            }
        }

        int[] result = solver.solve(costMatrix);
        
        for (int i = 0; i < n; i++) {
            int agentIndex = result[i];
            if (agentIndex < m && costMatrix[i][agentIndex] < 1000.0) {
                Mission mission = pending.get(i);
                Agent agent = available.get(agentIndex);
                int time = ETAs[i][agentIndex];
                
                // Recompute route for the chosen one
                RouteResponse r1 = engine.bidirectionalAStar(agent.getCurrentNodeId(), mission.getPickupNodeId());
                RouteResponse r2 = engine.bidirectionalAStar(mission.getPickupNodeId(), mission.getDestinationNodeId());
                List<String> route = new ArrayList<>();
                List<String> routeNodeIds = new ArrayList<>();
                if (r1.getNodePath() != null) routeNodeIds.addAll(r1.getNodePath());
                if (r2.getNodePath() != null) routeNodeIds.addAll(r2.getNodePath());
                
                assignments.add(new Assignment(mission, agent, time, route, routeNodeIds, rationale(mission, agent)));
            }
        }

        return assignments;
    }
    
    private double getSeverityWeight(Mission mission) {
        switch (mission.getPriority()) {
            case CRITICAL: return 10.0;
            case HIGH: return 5.0;
            case NORMAL: return 2.0;
            case LOW: return 1.0;
            default: return 1.0;
        }
    }
}
