package com.resqmesh.dispatch.policy.impl;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.dispatch.policy.Assignment;
import com.resqmesh.dispatch.policy.TriagePolicy;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.network.algorithm.RoutingEngine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class FirstComeFirstServedPolicy extends AbstractGreedyPolicy {
    @Override
    public String name() {
        return "FirstComeFirstServed";
    }

    @Override
    public String rationale(Mission m, Agent a) {
        return "Assigned based on earliest creation time";
    }

    @Override
    protected List<Mission> sortMissions(List<Mission> pending, List<Agent> available, RoutingEngine engine) {
        return pending.stream()
                .sorted(Comparator.comparing(Mission::getCreatedAt))
                .collect(Collectors.toList());
    }
}
