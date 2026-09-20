package com.resqmesh.dispatch.service;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.agent.enums.AgentStatus;
import com.resqmesh.agent.repository.AgentRepository;
import com.resqmesh.dispatch.entity.AssignmentHistory;
import com.resqmesh.dispatch.policy.TriagePolicy;
import com.resqmesh.dispatch.policy.impl.ExpectedLivesSavedPolicy;
import com.resqmesh.dispatch.policy.impl.FirstComeFirstServedPolicy;
import com.resqmesh.dispatch.repository.AssignmentHistoryRepository;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.mission.enums.MissionPriority;
import com.resqmesh.mission.enums.MissionStatus;
import com.resqmesh.mission.enums.RiskStatus;
import com.resqmesh.mission.repository.MissionRepository;
import com.resqmesh.common.service.NotificationService;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.dto.RouteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DispatchServiceTest {

    private MissionRepository missionRepository;
    private AgentRepository agentRepository;
    private AssignmentHistoryRepository assignmentHistoryRepository;
    private NotificationService notificationService;
    private RoutingEngine routingEngine;
    private DispatchService dispatchService;

    @BeforeEach
    void setUp() {
        missionRepository = mock(MissionRepository.class);
        agentRepository = mock(AgentRepository.class);
        assignmentHistoryRepository = mock(AssignmentHistoryRepository.class);
        notificationService = mock(NotificationService.class);
        routingEngine = mock(RoutingEngine.class);

        List<TriagePolicy> policies = List.of(
                new FirstComeFirstServedPolicy(),
                new ExpectedLivesSavedPolicy()
        );
        dispatchService = new DispatchService(
                missionRepository, agentRepository, assignmentHistoryRepository,
                policies, notificationService, routingEngine
        );
    }

    @Test
    void testEvaluateActiveMissions_Rung1_Reroute() {
        Mission mission = makeMission(MissionStatus.ASSIGNED);
        Agent agent = makeAgent(AgentStatus.ASSIGNED);
        mission.setAssignedAgentId(agent.getId());
        mission.setPickupNodeId("N2");
        mission.setDestinationNodeId("N3");
        mission.setEstimatedArrivalMinutes(20);
        agent.setCurrentNodeId("N1");

        when(missionRepository.findAllById(any())).thenReturn(List.of(mission));
        when(agentRepository.findById(agent.getId())).thenReturn(Optional.of(agent));

        // Still reachable — new ETA is 30
        when(routingEngine.bidirectionalAStar("N1", "N2"))
                .thenReturn(new RouteResponse(true, 15, List.of(), List.of()));
        when(routingEngine.bidirectionalAStar("N2", "N3"))
                .thenReturn(new RouteResponse(true, 15, List.of(), List.of()));

        dispatchService.evaluateActiveMissions(Collections.singleton(mission.getId()));

        // Rung 1: ETA updated, mission stays ASSIGNED
        assertEquals(MissionStatus.ASSIGNED, mission.getStatus());
        assertEquals(30, mission.getEstimatedArrivalMinutes());
        verify(missionRepository).save(mission);
    }

    @Test
    void testEvaluateActiveMissions_Rung5_Escalation() {
        Mission mission = makeMission(MissionStatus.ASSIGNED);
        Agent agent = makeAgent(AgentStatus.ASSIGNED);
        mission.setAssignedAgentId(agent.getId());
        mission.setPickupNodeId("N2");
        mission.setDestinationNodeId("N3");
        mission.setRiskStatus(RiskStatus.ON_TRACK);
        agent.setCurrentNodeId("N1");
        agent.setCapacity(1);

        when(missionRepository.findAllById(any())).thenReturn(List.of(mission));
        when(agentRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        // Rung 2 check: no available agents
        when(agentRepository.findByStatus(AgentStatus.AVAILABLE)).thenReturn(List.of());
        // Rung 3 check: no assigned missions
        when(missionRepository.findByStatus(MissionStatus.ASSIGNED)).thenReturn(List.of());
        // Route is unreachable for everyone
        when(routingEngine.bidirectionalAStar(anyString(), anyString()))
                .thenReturn(new RouteResponse(false, null, List.of(), List.of()));

        dispatchService.evaluateActiveMissions(Collections.singleton(mission.getId()));

        // Rung 5: AT_RISK, status back to PENDING
        assertEquals(MissionStatus.PENDING, mission.getStatus());
        assertEquals(RiskStatus.AT_RISK, mission.getRiskStatus());
        assertEquals(AgentStatus.AVAILABLE, agent.getStatus());
        verify(missionRepository).save(mission);
        verify(agentRepository).save(agent);
        verify(assignmentHistoryRepository).save(any(AssignmentHistory.class));
    }

    private Mission makeMission(MissionStatus status) {
        Mission m = new Mission();
        m.setId(UUID.randomUUID());
        m.setMissionCode("M-" + UUID.randomUUID().toString().substring(0, 6));
        m.setStatus(status);
        m.setPriority(MissionPriority.HIGH);
        m.setDeadline(com.resqmesh.sim.SimulationClock.now().plusSeconds(3600));
        m.setCreatedAt(com.resqmesh.sim.SimulationClock.now());
        m.setRiskStatus(RiskStatus.ON_TRACK);
        return m;
    }

    private Agent makeAgent(AgentStatus status) {
        Agent a = new Agent();
        a.setId(UUID.randomUUID());
        a.setAgentCode("A-" + UUID.randomUUID().toString().substring(0, 6));
        a.setStatus(status);
        a.setCapacity(1);
        return a;
    }
}
