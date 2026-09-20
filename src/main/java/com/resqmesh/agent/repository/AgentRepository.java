package com.resqmesh.agent.repository;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.agent.enums.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {
    Optional<Agent> findByAgentCode(String agentCode);
    List<Agent> findByStatus(AgentStatus status);
}
