package com.resqmesh.mission.service;

import com.resqmesh.common.exception.NodeNotFoundException;
import com.resqmesh.mission.dto.MissionCreateRequest;
import com.resqmesh.mission.dto.MissionStatusUpdateRequest;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.mission.enums.MissionStatus;
import com.resqmesh.mission.enums.RiskStatus;
import com.resqmesh.mission.repository.MissionRepository;
import com.resqmesh.network.repository.RoadNodeRepository;
import com.resqmesh.common.service.NotificationService;
import com.resqmesh.agent.repository.AgentRepository;
import com.resqmesh.agent.enums.AgentStatus;
import com.resqmesh.dispatch.dto.DecisionEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MissionService {

    private final MissionRepository missionRepository;
    private final RoadNodeRepository roadNodeRepository;
    private final NotificationService notificationService;
    private final AgentRepository agentRepository;

    public MissionService(MissionRepository missionRepository, RoadNodeRepository roadNodeRepository, NotificationService notificationService, AgentRepository agentRepository) {
        this.missionRepository = missionRepository;
        this.roadNodeRepository = roadNodeRepository;
        this.notificationService = notificationService;
        this.agentRepository = agentRepository;
    }

    public Mission createMission(MissionCreateRequest request) {
        if (!roadNodeRepository.existsById(request.getPickupNodeId())) {
            throw new NodeNotFoundException("Pickup node not found: " + request.getPickupNodeId());
        }
        if (!roadNodeRepository.existsById(request.getDestinationNodeId())) {
            throw new NodeNotFoundException("Destination node not found: " + request.getDestinationNodeId());
        }

        Mission mission = new Mission();
        mission.setMissionCode(request.getMissionCode());
        mission.setPickupNodeId(request.getPickupNodeId());
        mission.setDestinationNodeId(request.getDestinationNodeId());
        mission.setPriority(request.getPriority());
        mission.setDeadline(request.getDeadline());
        mission.setStatus(MissionStatus.PENDING);
        mission.setRiskStatus(RiskStatus.ON_TRACK);

        mission = missionRepository.save(mission);
        notificationService.broadcastMissionUpdate(mission);
        return mission;
    }

    public List<Mission> getAllMissions() {
        return missionRepository.findAll();
    }

    public Mission getMission(UUID id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + id));
    }

    public Mission updateMissionStatus(UUID id, MissionStatusUpdateRequest request) {
        Mission mission = getMission(id);
        mission.setStatus(request.getStatus());
        if (request.getStatus() == MissionStatus.COMPLETED) {
            releaseAssignedUnit(mission);
        }
        mission = missionRepository.save(mission);
        notificationService.broadcastMissionUpdate(mission);
        return mission;
    }

    public Mission completeMission(UUID id) {
        Mission mission = getMission(id);
        mission.setStatus(MissionStatus.COMPLETED);
        releaseAssignedUnit(mission);
        mission = missionRepository.save(mission);
        notificationService.broadcastMissionUpdate(mission);
        notificationService.broadcastDecision(new DecisionEvent(
            mission.getMissionCode(),
            "[MISSION_COMPLETED] Mission marked COMPLETED. Assigned rescue unit released to AVAILABLE status."
        ));
        return mission;
    }

    private void releaseAssignedUnit(Mission mission) {
        if (mission.getAssignedAgentId() != null) {
            agentRepository.findById(mission.getAssignedAgentId()).ifPresent(agent -> {
                agent.setStatus(AgentStatus.AVAILABLE);
                if (mission.getDestinationNodeId() != null) {
                    agent.setCurrentNodeId(mission.getDestinationNodeId());
                } else if (mission.getPickupNodeId() != null) {
                    agent.setCurrentNodeId(mission.getPickupNodeId());
                }
                agentRepository.save(agent);
                notificationService.broadcastAgentUpdate(agent);
            });
        }
    }
}
