package com.resqmesh.dispatch.engine;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.dispatch.policy.Assignment;
import com.resqmesh.dispatch.policy.impl.FirstComeFirstServedPolicy;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.mission.enums.MissionPriority;
import com.resqmesh.mission.enums.MissionStatus;
import com.resqmesh.mission.enums.RiskStatus;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.dto.RouteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Replaces DispatchEngineTest after the DispatchEngine was removed and replaced
 * by the TriagePolicy interface. Tests use FirstComeFirstServedPolicy as a proxy.
 */
class DispatchEngineTest {

    private RoutingEngine routingEngine;
    private FirstComeFirstServedPolicy policy;

    @BeforeEach
    void setUp() {
        routingEngine = mock(RoutingEngine.class);
        policy = new FirstComeFirstServedPolicy();
    }

    @Test
    void testFindBestAgent_SuccessfulAssignment() {
        Mission mission = new Mission();
        mission.setId(UUID.randomUUID());
        mission.setPickupNodeId("N2");
        mission.setDestinationNodeId("N3");
        mission.setStatus(MissionStatus.PENDING);
        mission.setPriority(MissionPriority.HIGH);
        mission.setDeadline(Instant.now().plusSeconds(3600));
        mission.setCreatedAt(Instant.now());

        Agent agent1 = new Agent();
        agent1.setId(UUID.randomUUID());
        agent1.setAgentCode("A1");
        agent1.setCurrentNodeId("N1");

        Agent agent2 = new Agent();
        agent2.setId(UUID.randomUUID());
        agent2.setAgentCode("A2");
        agent2.setCurrentNodeId("N5");

        when(routingEngine.bidirectionalAStar("N1", "N2"))
                .thenReturn(new RouteResponse(true, 10, Collections.emptyList(), Collections.emptyList()));
        when(routingEngine.bidirectionalAStar("N5", "N2"))
                .thenReturn(new RouteResponse(true, 20, Collections.emptyList(), Collections.emptyList()));
        when(routingEngine.bidirectionalAStar("N2", "N3"))
                .thenReturn(new RouteResponse(true, 15, Collections.emptyList(), Collections.emptyList()));

        List<Assignment> assignments = policy.assign(List.of(mission), List.of(agent1, agent2), routingEngine);

        assertEquals(1, assignments.size());
        assertEquals(agent1, assignments.get(0).getAgent());
        assertEquals(25, assignments.get(0).getEstimatedTotalTime()); // 10 + 15
    }

    @Test
    void testFindBestAgent_NoReachableAgents() {
        Mission mission = new Mission();
        mission.setId(UUID.randomUUID());
        mission.setPickupNodeId("N2");
        mission.setDestinationNodeId("N3");
        mission.setStatus(MissionStatus.PENDING);
        mission.setPriority(MissionPriority.HIGH);
        mission.setDeadline(Instant.now().plusSeconds(3600));
        mission.setCreatedAt(Instant.now());

        Agent agent1 = new Agent();
        agent1.setId(UUID.randomUUID());
        agent1.setAgentCode("A1");
        agent1.setCurrentNodeId("N1");

        when(routingEngine.bidirectionalAStar(anyString(), anyString()))
                .thenReturn(new RouteResponse(false, null, Collections.emptyList(), Collections.emptyList()));

        List<Assignment> assignments = policy.assign(List.of(mission), List.of(agent1), routingEngine);

        assertTrue(assignments.isEmpty());
    }
}
