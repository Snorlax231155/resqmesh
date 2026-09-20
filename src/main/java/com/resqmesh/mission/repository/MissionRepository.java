package com.resqmesh.mission.repository;

import com.resqmesh.mission.entity.Mission;
import com.resqmesh.mission.enums.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {
    Optional<Mission> findByMissionCode(String missionCode);
    List<Mission> findByStatus(MissionStatus status);
    List<Mission> findByAssignedAgentId(UUID assignedAgentId);
    List<Mission> findByScenarioId(String scenarioId);
}
