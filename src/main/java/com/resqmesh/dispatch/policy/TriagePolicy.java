package com.resqmesh.dispatch.policy;

import com.resqmesh.agent.entity.Agent;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.network.algorithm.RoutingEngine;

import java.util.List;

public interface TriagePolicy {
    String name();
    String rationale(Mission m, Agent a);
    List<Assignment> assign(List<Mission> pending, List<Agent> available, RoutingEngine engine);
}
