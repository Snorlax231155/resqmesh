package com.resqmesh.dispatch.service;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.agent.enums.AgentStatus;
import com.resqmesh.agent.repository.AgentRepository;
import com.resqmesh.dispatch.policy.Assignment;
import com.resqmesh.dispatch.policy.TriagePolicy;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.mission.repository.MissionRepository;
import com.resqmesh.network.algorithm.RoutingEngine;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolicyComparisonService {

    public record PolicyResult(
            String policy,
            int assigned,
            int unassigned,
            double avgResponseMinutes,
            int p95ResponseMinutes,
            int criticalMissed,
            double totalSeverityWeightedDelay,
            int worstSingleWait
    ) {}

    private final MissionRepository missionRepository;
    private final AgentRepository agentRepository;
    private final RoutingEngine routingEngine;
    private final List<TriagePolicy> policies;

    public PolicyComparisonService(MissionRepository missionRepository,
                                   AgentRepository agentRepository,
                                   RoutingEngine routingEngine,
                                   List<TriagePolicy> policies) {
        this.missionRepository = missionRepository;
        this.agentRepository = agentRepository;
        this.routingEngine = routingEngine;
        this.policies = policies;
    }

    public List<PolicyResult> compare(String scenarioId) {
        List<Mission> missions = missionRepository.findByScenarioId(scenarioId);
        List<Agent> agents = agentRepository.findByStatus(AgentStatus.AVAILABLE);

        if (missions.isEmpty()) {
            throw new IllegalArgumentException("No missions found for scenario: " + scenarioId);
        }

        List<PolicyResult> results = new ArrayList<>();
        for (TriagePolicy policy : policies) {
            results.add(evaluate(policy, missions, agents));
        }
        return results;
    }

    private PolicyResult evaluate(TriagePolicy policy, List<Mission> missions, List<Agent> agents) {
        // Dry-run: do NOT persist anything
        List<Assignment> assignments = policy.assign(missions, agents, routingEngine);

        List<Integer> etaList = assignments.stream()
                .map(Assignment::getEstimatedTotalTime)
                .sorted()
                .collect(Collectors.toList());

        int assigned = etaList.size();
        int unassigned = missions.size() - assigned;

        double avg = etaList.isEmpty() ? 0 :
                etaList.stream().mapToInt(i -> i).average().orElse(0);

        int p95 = etaList.isEmpty() ? 0 :
                etaList.get((int) Math.min(Math.ceil(etaList.size() * 0.95), etaList.size() - 1));

        int worstSingleWait = etaList.isEmpty() ? 0 :
                etaList.get(etaList.size() - 1);

        Instant now = com.resqmesh.sim.SimulationClock.now();

        // Critical missed = CRITICAL missions that have no assignment or ETA > deadline slack
        long criticalMissed = missions.stream()
                .filter(m -> m.getPriority().name().equals("CRITICAL"))
                .filter(m -> {
                    return assignments.stream()
                            .filter(a -> a.getMission().getId().equals(m.getId()))
                            .findFirst()
                            .map(a -> {
                                long slackMinutes = ChronoUnit.MINUTES.between(now, m.getDeadline());
                                return a.getEstimatedTotalTime() > slackMinutes;
                            })
                            .orElse(true); // not assigned = missed
                }).count();

        // Total severity-weighted delay = sum(severityWeight * max(0, ETA - deadline_slack))
        double severityWeightedDelay = assignments.stream().mapToDouble(a -> {
            Mission m = a.getMission();
            long slackMinutes = ChronoUnit.MINUTES.between(now, m.getDeadline());
            double delay = Math.max(0, a.getEstimatedTotalTime() - slackMinutes);
            double weight = switch (m.getPriority()) {
                case CRITICAL -> 10.0;
                case HIGH -> 5.0;
                case NORMAL -> 2.0;
                case LOW -> 1.0;
            };
            return weight * delay;
        }).sum();

        return new PolicyResult(
                policy.name(), assigned, unassigned, avg, p95,
                (int) criticalMissed, severityWeightedDelay, worstSingleWait
        );
    }
}
