package com.resqmesh.network.service;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.agent.enums.AgentStatus;
import com.resqmesh.agent.repository.AgentRepository;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.dto.CoverageResponse;
import com.resqmesh.network.entity.RoadNode;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CoverageService {

    private final RoutingEngine routingEngine;
    private final AgentRepository agentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NetworkService networkService;

    public CoverageService(RoutingEngine routingEngine, AgentRepository agentRepository, SimpMessagingTemplate messagingTemplate, NetworkService networkService) {
        this.routingEngine = routingEngine;
        this.agentRepository = agentRepository;
        this.messagingTemplate = messagingTemplate;
        this.networkService = networkService;
    }

    public void computeCoverage() {
        List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);
        List<String> sourceNodeIds = availableAgents.stream()
                .map(Agent::getCurrentNodeId)
                .collect(Collectors.toList());

        Map<String, Double> distances = routingEngine.multiSourceDijkstra(sourceNodeIds);
        List<RoadNode> allNodes = networkService.getAllNodes();

        List<String> band0to4 = new ArrayList<>();
        List<String> band4to8 = new ArrayList<>();
        List<String> band8to15 = new ArrayList<>();
        List<String> band15Plus = new ArrayList<>();
        List<String> unreachable = new ArrayList<>();

        for (RoadNode node : allNodes) {
            String id = node.getId();
            Double time = distances.get(id);
            if (time == null || time == Double.POSITIVE_INFINITY) {
                unreachable.add(id);
            } else if (time <= 4.0) {
                band0to4.add(id);
            } else if (time <= 8.0) {
                band4to8.add(id);
            } else if (time <= 15.0) {
                band8to15.add(id);
            } else {
                band15Plus.add(id);
            }
        }

        int worstBandSize = unreachable.size();
        if (worstBandSize == 0) {
            worstBandSize = band15Plus.size();
        }

        // Calculate a node-count-weighted coverage score.
        // Assuming: 0-4m (100%), 4-8m (75%), 8-15m (50%), 15+m (25%), unreachable (0%)
        int totalNodes = allNodes.size();
        double score = 0;
        if (totalNodes > 0) {
            double weightedSum = (band0to4.size() * 1.0) + (band4to8.size() * 0.75) + (band8to15.size() * 0.5) + (band15Plus.size() * 0.25);
            score = (weightedSum / totalNodes) * 100.0;
        }

        CoverageResponse response = new CoverageResponse(
                band0to4, band4to8, band8to15, band15Plus, unreachable, worstBandSize, score
        );

        messagingTemplate.convertAndSend("/topic/coverage", response);
    }
}
