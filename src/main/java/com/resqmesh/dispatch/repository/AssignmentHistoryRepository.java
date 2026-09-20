package com.resqmesh.dispatch.repository;

import com.resqmesh.dispatch.entity.AssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentHistoryRepository extends JpaRepository<AssignmentHistory, UUID> {
    List<AssignmentHistory> findByMissionIdOrderByCreatedAtDesc(UUID missionId);
}
