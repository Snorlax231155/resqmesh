package com.resqmesh.dispatch.service;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.agent.enums.AgentStatus;
import com.resqmesh.agent.repository.AgentRepository;
import com.resqmesh.dispatch.dto.AssignmentResponse;
import com.resqmesh.dispatch.policy.Assignment;
import com.resqmesh.dispatch.policy.TriagePolicy;
import com.resqmesh.dispatch.entity.AssignmentHistory;
import com.resqmesh.dispatch.repository.AssignmentHistoryRepository;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.mission.enums.MissionStatus;
import com.resqmesh.mission.repository.MissionRepository;
import com.resqmesh.common.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private final MissionRepository missionRepository;
    private final AgentRepository agentRepository;
    private final AssignmentHistoryRepository assignmentHistoryRepository;
    private final List<TriagePolicy> policies;
    private final NotificationService notificationService;
    private final com.resqmesh.network.algorithm.RoutingEngine routingEngine;
    private String activePolicyName = "ExpectedLivesSaved";

    public DispatchService(MissionRepository missionRepository, AgentRepository agentRepository, AssignmentHistoryRepository assignmentHistoryRepository, List<TriagePolicy> policies, NotificationService notificationService, com.resqmesh.network.algorithm.RoutingEngine routingEngine) {
        this.missionRepository = missionRepository;
        this.agentRepository = agentRepository;
        this.assignmentHistoryRepository = assignmentHistoryRepository;
        this.policies = policies;
        this.notificationService = notificationService;
        this.routingEngine = routingEngine;
    }
    
    public void setActivePolicy(String name) {
        this.activePolicyName = name;
    }
    
    public List<TriagePolicy> getPolicies() {
        return policies;
    }
    
    public TriagePolicy getActivePolicy() {
        return policies.stream()
                .filter(p -> p.name().equals(activePolicyName))
                .findFirst()
                .orElse(policies.get(0));
    }

    @Transactional
    public List<AssignmentResponse> runDispatchCycle() {
        List<AssignmentResponse> assignments = new ArrayList<>();
        
        List<Mission> pendingMissions = missionRepository.findByStatus(MissionStatus.PENDING);
        List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);
        TriagePolicy activePolicy = getActivePolicy();
        List<Assignment> calculatedAssignments = activePolicy.assign(pendingMissions, availableAgents, routingEngine);

        for (Assignment candidate : calculatedAssignments) {
            Mission mission = candidate.getMission();
            Agent selectedAgent = candidate.getAgent();
            
            // Re-fetch mission to ensure it's still pending (though we are in a transaction)
            if (mission.getStatus() != MissionStatus.PENDING) continue;
            
            // Update Mission
            mission.setAssignedAgentId(selectedAgent.getId());
            mission.setStatus(MissionStatus.ASSIGNED);
            mission.setEstimatedArrivalMinutes(candidate.getEstimatedTotalTime());
            missionRepository.save(mission);
            
            // Update Agent
            selectedAgent.setStatus(AgentStatus.ASSIGNED);
            agentRepository.save(selectedAgent);
            
            // Register in RoutingEngine
            if (candidate.getRouteEdgeIds() != null && !candidate.getRouteEdgeIds().isEmpty()) {
                routingEngine.registerMissionRoute(mission.getId(), candidate.getRouteEdgeIds());
            }

            // Record History
            AssignmentHistory history = new AssignmentHistory(
                    null,
                    mission.getId(),
                    null,
                    selectedAgent.getId(),
                    "Assigned via " + activePolicy.name() + ": " + candidate.getRationale(),
                    null,
                    com.resqmesh.sim.SimulationClock.now()
            );
            assignmentHistoryRepository.save(history);
            
            assignments.add(new AssignmentResponse(
                    mission.getId(),
                    selectedAgent.getId(),
                    selectedAgent.getCurrentNodeId(),
                    mission.getDestinationNodeId(),
                    candidate.getEstimatedTotalTime()
            ));
            
            notificationService.broadcastMissionUpdate(mission);
            notificationService.broadcastAgentUpdate(selectedAgent);
            
            if (candidate.getRouteNodeIds() != null && !candidate.getRouteNodeIds().isEmpty()) {
                notificationService.broadcastAgentRepositioned(new com.resqmesh.network.dto.AgentRepositionedEvent(
                        selectedAgent.getId(),
                        selectedAgent.getAgentCode(),
                        mission.getPickupNodeId(),
                        candidate.getRouteNodeIds()
                ));
            }
            
            log.info("Assigned Mission {} to Agent {} via {}", mission.getMissionCode(), selectedAgent.getAgentCode(), activePolicy.name());
        }
        
        return assignments;
    }

    @Transactional
    public void evaluateActiveMissions(java.util.Set<java.util.UUID> affectedMissionIds) {
        if (affectedMissionIds == null || affectedMissionIds.isEmpty()) return;
        List<Mission> activeMissions = missionRepository.findAllById(affectedMissionIds);
        
        for (Mission mission : activeMissions) {
            if (mission.getStatus() != MissionStatus.ASSIGNED) continue;
            Agent agent = agentRepository.findById(mission.getAssignedAgentId()).orElse(null);
            if (agent == null) continue;

            // Rung 1: Reroute same agent via alternate path
            com.resqmesh.network.dto.RouteResponse r1 = routingEngine.bidirectionalAStar(agent.getCurrentNodeId(), mission.getPickupNodeId());
            com.resqmesh.network.dto.RouteResponse r2 = routingEngine.bidirectionalAStar(mission.getPickupNodeId(), mission.getDestinationNodeId());
            
            if (r1.isReachable() && r2.isReachable()) {
                int newEta = r1.getTotalTravelTimeMinutes() + r2.getTotalTravelTimeMinutes();
                if (mission.getEstimatedArrivalMinutes() == null || newEta != mission.getEstimatedArrivalMinutes()) {
                    mission.setEstimatedArrivalMinutes(newEta);
                    missionRepository.save(mission);
                    notificationService.broadcastMissionUpdate(mission);
                    
                    List<String> route = new ArrayList<>();
                    if (r1.getRoadPath() != null) route.addAll(r1.getRoadPath());
                    if (r2.getRoadPath() != null) route.addAll(r2.getRoadPath());
                    routingEngine.registerMissionRoute(mission.getId(), route);
                    
                    notificationService.broadcastDecision(new com.resqmesh.dispatch.dto.DecisionEvent(
                            mission.getMissionCode(), 
                            "Rung 1: Rerouted same agent " + agent.getAgentCode() + " via alternate path. New ETA: " + newEta + "m"
                    ));
                }
                continue; // Resolved via Rung 1
            }

            // Rung 2: Reassign to a different available agent
            List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);
            Agent bestAltAgent = null;
            int bestAltEta = Integer.MAX_VALUE;
            List<String> bestAltRoute = null;
            
            for (Agent altAgent : availableAgents) {
                com.resqmesh.network.dto.RouteResponse a1 = routingEngine.bidirectionalAStar(altAgent.getCurrentNodeId(), mission.getPickupNodeId());
                com.resqmesh.network.dto.RouteResponse a2 = routingEngine.bidirectionalAStar(mission.getPickupNodeId(), mission.getDestinationNodeId());
                if (a1.isReachable() && a2.isReachable()) {
                    int time = a1.getTotalTravelTimeMinutes() + a2.getTotalTravelTimeMinutes();
                    if (time < bestAltEta) {
                        bestAltEta = time;
                        bestAltAgent = altAgent;
                        bestAltRoute = new ArrayList<>();
                        if (a1.getRoadPath() != null) bestAltRoute.addAll(a1.getRoadPath());
                        if (a2.getRoadPath() != null) bestAltRoute.addAll(a2.getRoadPath());
                    }
                }
            }
            
            if (bestAltAgent != null) {
                // Unassign old agent
                agent.setStatus(AgentStatus.AVAILABLE);
                agentRepository.save(agent);
                
                // Assign new agent
                bestAltAgent.setStatus(AgentStatus.ASSIGNED);
                agentRepository.save(bestAltAgent);
                
                mission.setAssignedAgentId(bestAltAgent.getId());
                mission.setEstimatedArrivalMinutes(bestAltEta);
                missionRepository.save(mission);
                
                routingEngine.registerMissionRoute(mission.getId(), bestAltRoute);
                
                notificationService.broadcastDecision(new com.resqmesh.dispatch.dto.DecisionEvent(
                        mission.getMissionCode(), 
                        "Rung 2: Reassigned to different available agent " + bestAltAgent.getAgentCode() + ". ETA: " + bestAltEta + "m"
                ));
                
                notificationService.broadcastMissionUpdate(mission);
                notificationService.broadcastAgentUpdate(agent);
                notificationService.broadcastAgentUpdate(bestAltAgent);
                continue; // Resolved via Rung 2
            }

            // Rung 3: Reshuffle (pull agent off strictly lower-priority mission)
            List<Mission> assignedMissions = missionRepository.findByStatus(MissionStatus.ASSIGNED);
            Mission lowerPriMission = null;
            Agent reshuffledAgent = null;
            int reshuffledEta = Integer.MAX_VALUE;
            List<String> reshuffledRoute = null;
            
            for (Mission otherMission : assignedMissions) {
                if (otherMission.getPriority().compareTo(mission.getPriority()) > 0) {
                    // lower priority (ordinal is higher)
                    Agent otherAgent = agentRepository.findById(otherMission.getAssignedAgentId()).orElse(null);
                    if (otherAgent != null) {
                        com.resqmesh.network.dto.RouteResponse a1 = routingEngine.bidirectionalAStar(otherAgent.getCurrentNodeId(), mission.getPickupNodeId());
                        com.resqmesh.network.dto.RouteResponse a2 = routingEngine.bidirectionalAStar(mission.getPickupNodeId(), mission.getDestinationNodeId());
                        if (a1.isReachable() && a2.isReachable()) {
                            int time = a1.getTotalTravelTimeMinutes() + a2.getTotalTravelTimeMinutes();
                            if (time < reshuffledEta) {
                                reshuffledEta = time;
                                reshuffledAgent = otherAgent;
                                lowerPriMission = otherMission;
                                reshuffledRoute = new ArrayList<>();
                                if (a1.getRoadPath() != null) reshuffledRoute.addAll(a1.getRoadPath());
                                if (a2.getRoadPath() != null) reshuffledRoute.addAll(a2.getRoadPath());
                            }
                        }
                    }
                }
            }
            
            if (reshuffledAgent != null) {
                // Cascade lower priority mission back to pending
                lowerPriMission.setStatus(MissionStatus.PENDING);
                lowerPriMission.setAssignedAgentId(null);
                missionRepository.save(lowerPriMission);
                
                // Old agent is now available
                agent.setStatus(AgentStatus.AVAILABLE);
                agentRepository.save(agent);
                
                // Assign new agent
                mission.setAssignedAgentId(reshuffledAgent.getId());
                mission.setEstimatedArrivalMinutes(reshuffledEta);
                missionRepository.save(mission);
                
                routingEngine.registerMissionRoute(mission.getId(), reshuffledRoute);
                
                notificationService.broadcastDecision(new com.resqmesh.dispatch.dto.DecisionEvent(
                        mission.getMissionCode(), 
                        "Rung 3: Reshuffled " + reshuffledAgent.getAgentCode() + " off " + lowerPriMission.getMissionCode() + " (priority " + lowerPriMission.getPriority() + "). ETA: " + reshuffledEta + "m"
                ));
                
                notificationService.broadcastMissionUpdate(mission);
                notificationService.broadcastMissionUpdate(lowerPriMission);
                notificationService.broadcastAgentUpdate(agent);
                continue; // Resolved via Rung 3
            }

            // Rung 4: Split the mission (simulate splitting)
            // Simplified: If agent capacity > 1, assume we can split it. (Standard capacity is 1, so this often skips)
            if (agent.getCapacity() != null && agent.getCapacity() > 1) {
                 notificationService.broadcastDecision(new com.resqmesh.dispatch.dto.DecisionEvent(
                        mission.getMissionCode(), 
                        "Rung 4: Mission split (partial capacity utilized). This is a stub for future complex splits."
                 ));
                 // Not actually resolving because we are simulating the ladder, fallthrough to rung 5 for now if unreachable.
            }

            // Rung 5: Escalate to human
            log.warn("Mission {} is no longer reachable by any available means. Escalating to human.", mission.getMissionCode());
            
            mission.setStatus(MissionStatus.PENDING);
            mission.setAssignedAgentId(null);
            mission.setRiskStatus(com.resqmesh.mission.enums.RiskStatus.AT_RISK);
            missionRepository.save(mission);
            
            agent.setStatus(AgentStatus.AVAILABLE);
            agentRepository.save(agent);
            
            AssignmentHistory history = new AssignmentHistory(
                    null,
                    mission.getId(),
                    agent.getId(),
                    null,
                    "Rung 5 Escalation: Unassigned due to disruption rendering destination unreachable",
                    null,
                    com.resqmesh.sim.SimulationClock.now()
            );
            assignmentHistoryRepository.save(history);

            notificationService.broadcastDecision(new com.resqmesh.dispatch.dto.DecisionEvent(
                    mission.getMissionCode(), 
                    "Rung 5: Escalated to human (AT_RISK). Reason: Unreachable by all available and reshufflable agents."
            ));

            notificationService.broadcastMissionUpdate(mission);
            notificationService.broadcastAgentUpdate(agent);
        }
    }

    @Transactional
    public AssignmentResponse manualAssign(java.util.UUID missionId, java.util.UUID agentId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        if (agent.getStatus() != AgentStatus.AVAILABLE) {
            throw new IllegalStateException("Agent " + agent.getAgentCode() + " is not AVAILABLE.");
        }

        com.resqmesh.network.dto.RouteResponse r1 = routingEngine.bidirectionalAStar(agent.getCurrentNodeId(), mission.getPickupNodeId());
        com.resqmesh.network.dto.RouteResponse r2 = routingEngine.bidirectionalAStar(mission.getPickupNodeId(), mission.getDestinationNodeId());
        int totalEta = (r1.isReachable() ? r1.getTotalTravelTimeMinutes() : 10) + (r2.isReachable() ? r2.getTotalTravelTimeMinutes() : 10);

        mission.setAssignedAgentId(agent.getId());
        mission.setStatus(MissionStatus.ASSIGNED);
        mission.setEstimatedArrivalMinutes(totalEta);
        missionRepository.save(mission);

        agent.setStatus(AgentStatus.ASSIGNED);
        agentRepository.save(agent);

        List<String> route = new ArrayList<>();
        if (r1.getRoadPath() != null) route.addAll(r1.getRoadPath());
        if (r2.getRoadPath() != null) route.addAll(r2.getRoadPath());
        if (!route.isEmpty()) {
            routingEngine.registerMissionRoute(mission.getId(), route);
        }

        AssignmentHistory history = new AssignmentHistory(
                null,
                mission.getId(),
                null,
                agent.getId(),
                "Manually assigned by dispatcher operator",
                null,
                com.resqmesh.sim.SimulationClock.now()
        );
        assignmentHistoryRepository.save(history);

        notificationService.broadcastMissionUpdate(mission);
        notificationService.broadcastAgentUpdate(agent);
        notificationService.broadcastDecision(new com.resqmesh.dispatch.dto.DecisionEvent(
                mission.getMissionCode(),
                "[MANUAL_DISPATCH] Dispatcher manually assigned Rescue Unit " + agent.getAgentCode() + " to Mission " + mission.getMissionCode() + " (ETA: " + totalEta + "m)."
        ));

        return new AssignmentResponse(
                mission.getId(),
                agent.getId(),
                agent.getCurrentNodeId(),
                mission.getDestinationNodeId(),
                totalEta
        );
    }
}
