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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MissionService {

    private final MissionRepository missionRepository;
    private final RoadNodeRepository roadNodeRepository;
    private final NotificationService notificationService;

    public MissionService(MissionRepository missionRepository, RoadNodeRepository roadNodeRepository, NotificationService notificationService) {
        this.missionRepository = missionRepository;
        this.roadNodeRepository = roadNodeRepository;
        this.notificationService = notificationService;
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
        mission = missionRepository.save(mission);
        notificationService.broadcastMissionUpdate(mission);
        return mission;
    }
}
