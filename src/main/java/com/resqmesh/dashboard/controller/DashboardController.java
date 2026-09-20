package com.resqmesh.dashboard.controller;

import com.resqmesh.agent.enums.AgentStatus;
import com.resqmesh.agent.repository.AgentRepository;
import com.resqmesh.dashboard.dto.DashboardSummary;
import com.resqmesh.disruption.repository.DisruptionRepository;
import com.resqmesh.mission.enums.MissionStatus;
import com.resqmesh.mission.repository.MissionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final AgentRepository agentRepository;
    private final MissionRepository missionRepository;
    private final DisruptionRepository disruptionRepository;

    public DashboardController(AgentRepository agentRepository, MissionRepository missionRepository, DisruptionRepository disruptionRepository) {
        this.agentRepository = agentRepository;
        this.missionRepository = missionRepository;
        this.disruptionRepository = disruptionRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getSummary() {
        long totalAgents = agentRepository.count();
        long availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE).size();
        
        long totalMissions = missionRepository.count();
        long pendingMissions = missionRepository.findByStatus(MissionStatus.PENDING).size();
        
        long activeDisruptions = disruptionRepository.findByActiveTrue().size();

        return ResponseEntity.ok(new DashboardSummary(totalAgents, availableAgents, totalMissions, pendingMissions, activeDisruptions));
    }
}
