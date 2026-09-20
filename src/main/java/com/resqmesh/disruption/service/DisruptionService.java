package com.resqmesh.disruption.service;

import com.resqmesh.dispatch.service.DispatchService;
import com.resqmesh.disruption.dto.DisruptionCreateRequest;
import com.resqmesh.disruption.entity.Disruption;
import com.resqmesh.disruption.repository.DisruptionRepository;
import com.resqmesh.network.entity.RoadEdge;
import com.resqmesh.network.repository.RoadEdgeRepository;
import com.resqmesh.common.service.NotificationService;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.service.CoverageService;
import com.resqmesh.dispatch.service.RepositioningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DisruptionService {

    private final DisruptionRepository disruptionRepository;
    private final RoadEdgeRepository roadEdgeRepository;
    private final DispatchService dispatchService;
    private final NotificationService notificationService;
    private final RoutingEngine routingEngine;
    private final CoverageService coverageService;
    private final RepositioningService repositioningService;

    public DisruptionService(DisruptionRepository disruptionRepository, RoadEdgeRepository roadEdgeRepository, DispatchService dispatchService, NotificationService notificationService, RoutingEngine routingEngine, CoverageService coverageService, RepositioningService repositioningService) {
        this.disruptionRepository = disruptionRepository;
        this.roadEdgeRepository = roadEdgeRepository;
        this.dispatchService = dispatchService;
        this.notificationService = notificationService;
        this.routingEngine = routingEngine;
        this.coverageService = coverageService;
        this.repositioningService = repositioningService;
    }

    @Transactional
    public Disruption reportDisruption(DisruptionCreateRequest request) {
        Disruption disruption = new Disruption();
        disruption.setType(request.getType());
        disruption.setAffectedRoadId(request.getAffectedRoadId());
        disruption.setDescription(request.getDescription());
        disruption.setActive(true);

        disruption = disruptionRepository.save(disruption);

        RoadEdge roadEdge = roadEdgeRepository.findById(request.getAffectedRoadId())
                .orElseThrow(() -> new IllegalArgumentException("Road edge not found: " + request.getAffectedRoadId()));
        
        roadEdge.setBlocked(true);
        roadEdgeRepository.save(roadEdge);

        // Update in-memory graph
        routingEngine.closeEdge(roadEdge.getId());
        if (Boolean.TRUE.equals(roadEdge.getBidirectional())) {
            routingEngine.closeEdge(roadEdge.getId() + "_rev");
        }

        java.util.Set<java.util.UUID> affectedMissions = new java.util.HashSet<>(routingEngine.getMissionsOnEdge(roadEdge.getId()));
        if (Boolean.TRUE.equals(roadEdge.getBidirectional())) {
            affectedMissions.addAll(routingEngine.getMissionsOnEdge(roadEdge.getId() + "_rev"));
        }

        dispatchService.evaluateActiveMissions(affectedMissions);

        dispatchService.runDispatchCycle();

        notificationService.broadcastDisruptionUpdate(disruption);
        
        coverageService.computeCoverage();
        repositioningService.runRepositioning();

        return disruption;
    }

    public List<Disruption> getAllActiveDisruptions() {
        return disruptionRepository.findByActiveTrue();
    }
}
