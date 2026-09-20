package com.resqmesh.dispatch.service;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.agent.enums.AgentStatus;
import com.resqmesh.agent.repository.AgentRepository;
import com.resqmesh.common.service.NotificationService;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.dto.AgentRepositionedEvent;
import com.resqmesh.network.dto.RouteRequest;
import com.resqmesh.network.dto.RouteResponse;
import com.resqmesh.network.entity.RoadNode;
import com.resqmesh.network.service.CoverageService;
import com.resqmesh.network.service.NetworkService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RepositioningService {

    private final AgentRepository agentRepository;
    private final RoutingEngine routingEngine;
    private final NetworkService networkService;
    private final CoverageService coverageService;
    private final NotificationService notificationService;

    private boolean enabled = false;

    public RepositioningService(AgentRepository agentRepository, RoutingEngine routingEngine, NetworkService networkService, CoverageService coverageService, NotificationService notificationService) {
        this.agentRepository = agentRepository;
        this.routingEngine = routingEngine;
        this.networkService = networkService;
        this.coverageService = coverageService;
        this.notificationService = notificationService;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            runRepositioning();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void runRepositioning() {
        if (!enabled) return;

        List<Agent> idleAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);
        if (idleAgents.isEmpty()) return;

        // Current worst covered band nodes
        List<String> allSourceNodes = agentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AgentStatus.AVAILABLE || a.getStatus() == AgentStatus.REPOSITIONING)
                .map(Agent::getCurrentNodeId)
                .collect(Collectors.toList());

        Map<String, Double> initialCoverage = routingEngine.multiSourceDijkstra(allSourceNodes);
        
        for (Agent agent : idleAgents) {
            // Re-evaluate worst covered nodes as the iteration goes on
            List<String> worstNodes = new ArrayList<>();
            for (RoadNode node : networkService.getAllNodes()) {
                Double t = initialCoverage.get(node.getId());
                if (t == null || t > 15.0) {
                    worstNodes.add(node.getId());
                }
            }

            if (worstNodes.isEmpty()) break; // Nowhere to reposition to

            // Take a small sample of worst covered nodes to evaluate
            Collections.shuffle(worstNodes);
            List<String> candidateNodes = worstNodes.stream().limit(5).collect(Collectors.toList());

            String bestNode = null;
            int bestReduction = 0;
            int currentWorstSize = worstNodes.size();

            for (String candidate : candidateNodes) {
                // Simulate agent at candidate
                List<String> simulatedSources = new ArrayList<>(allSourceNodes);
                simulatedSources.remove(agent.getCurrentNodeId());
                simulatedSources.add(candidate);

                Map<String, Double> simulatedCoverage = routingEngine.multiSourceDijkstra(simulatedSources);
                int simulatedWorstSize = 0;
                for (RoadNode n : networkService.getAllNodes()) {
                    Double t = simulatedCoverage.get(n.getId());
                    if (t == null || t > 15.0) {
                        simulatedWorstSize++;
                    }
                }

                int reduction = currentWorstSize - simulatedWorstSize;
                if (reduction > bestReduction) {
                    bestReduction = reduction;
                    bestNode = candidate;
                }
            }

            if (bestNode != null && bestReduction > 0) {
                // We found a move that reduces worst-covered size
                RouteResponse route = networkService.calculateRoute(new RouteRequest(agent.getCurrentNodeId(), bestNode));
                if (route.isReachable()) {
                    // Capture old position before mutation
                    String oldNodeId = agent.getCurrentNodeId();

                    // Execute reposition — agent moves to bestNode
                    agent.setCurrentNodeId(bestNode);
                    // Revert to AVAILABLE once repositioned (reposition is instantaneous in simulation)
                    agent.setStatus(AgentStatus.AVAILABLE);
                    agentRepository.save(agent);
                    
                    notificationService.broadcastAgentUpdate(agent);
                    notificationService.broadcastAgentRepositioned(new AgentRepositionedEvent(
                            agent.getId(),
                            agent.getAgentCode(),
                            bestNode,
                            route.getNodePath()
                    ));
                    
                    // Update allSourceNodes: remove the OLD node, add the new one
                    allSourceNodes.remove(oldNodeId);
                    allSourceNodes.add(bestNode);
                    
                    // Update initialCoverage for the next agent
                    initialCoverage = routingEngine.multiSourceDijkstra(allSourceNodes);
                }
            }
        }
        
        // Final coverage compute to update map overlays
        coverageService.computeCoverage();
    }
}
