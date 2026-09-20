package com.resqmesh.agent.service;

import com.resqmesh.agent.dto.AgentCreateRequest;
import com.resqmesh.agent.dto.AgentStatusUpdateRequest;
import com.resqmesh.agent.entity.Agent;
import com.resqmesh.agent.enums.AgentStatus;
import com.resqmesh.agent.repository.AgentRepository;
import com.resqmesh.common.exception.NodeNotFoundException;
import com.resqmesh.network.repository.RoadNodeRepository;
import com.resqmesh.common.service.NotificationService;
import com.resqmesh.network.service.CoverageService;
import com.resqmesh.dispatch.service.RepositioningService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final RoadNodeRepository roadNodeRepository;
    private final NotificationService notificationService;
    private final CoverageService coverageService;
    private final RepositioningService repositioningService;

    public AgentService(AgentRepository agentRepository, RoadNodeRepository roadNodeRepository, NotificationService notificationService, CoverageService coverageService, RepositioningService repositioningService) {
        this.agentRepository = agentRepository;
        this.roadNodeRepository = roadNodeRepository;
        this.notificationService = notificationService;
        this.coverageService = coverageService;
        this.repositioningService = repositioningService;
    }

    public Agent registerAgent(AgentCreateRequest request) {
        if (!roadNodeRepository.existsById(request.getCurrentNodeId())) {
            throw new NodeNotFoundException("Node not found: " + request.getCurrentNodeId());
        }

        Agent agent = new Agent();
        agent.setAgentCode(request.getAgentCode());
        agent.setName(request.getName());
        agent.setCurrentNodeId(request.getCurrentNodeId());
        agent.setCapacity(request.getCapacity());
        agent.setStatus(AgentStatus.AVAILABLE);

        agent = agentRepository.save(agent);
        notificationService.broadcastAgentUpdate(agent);
        
        coverageService.computeCoverage();
        repositioningService.runRepositioning();
        
        return agent;
    }

    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    public Agent getAgent(UUID id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
    }

    public Agent updateAgentStatus(UUID id, AgentStatusUpdateRequest request) {
        Agent agent = getAgent(id);
        agent.setStatus(request.getStatus());
        agent = agentRepository.save(agent);
        notificationService.broadcastAgentUpdate(agent);
        
        coverageService.computeCoverage();
        repositioningService.runRepositioning();
        
        return agent;
    }
}
