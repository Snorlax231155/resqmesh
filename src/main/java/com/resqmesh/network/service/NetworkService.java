package com.resqmesh.network.service;

import com.resqmesh.common.exception.NodeNotFoundException;
import com.resqmesh.common.exception.RouteNotFoundException;
import com.resqmesh.network.algorithm.RoutingEngine;
import com.resqmesh.network.dto.RouteRequest;
import com.resqmesh.network.dto.RouteResponse;
import com.resqmesh.network.entity.RoadEdge;
import com.resqmesh.network.entity.RoadNode;
import com.resqmesh.network.model.GraphEdge;
import com.resqmesh.network.model.GraphNode;
import com.resqmesh.network.repository.RoadEdgeRepository;
import com.resqmesh.network.repository.RoadNodeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;

@Service
public class NetworkService {

    private final RoadNodeRepository roadNodeRepository;
    private final RoadEdgeRepository roadEdgeRepository;
    private final RoutingEngine routingEngine;

    public NetworkService(RoadNodeRepository roadNodeRepository, RoadEdgeRepository roadEdgeRepository, RoutingEngine routingEngine) {
        this.roadNodeRepository = roadNodeRepository;
        this.roadEdgeRepository = roadEdgeRepository;
        this.routingEngine = routingEngine;
    }

    public List<RoadNode> getAllNodes() {
        return roadNodeRepository.findAll();
    }

    public List<RoadEdge> getAllRoads() {
        return roadEdgeRepository.findAll();
    }

    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void initializeGraph() {
        List<RoadNode> entitiesNodes = roadNodeRepository.findAll();
        List<RoadEdge> entitiesEdges = roadEdgeRepository.findAll(); // Load all edges, including blocked
        
        List<GraphNode> nodes = entitiesNodes.stream()
                .map(n -> new GraphNode(n.getId()))
                .collect(Collectors.toList());

        Map<String, Double> latitudes = new HashMap<>();
        Map<String, Double> longitudes = new HashMap<>();
        for (RoadNode n : entitiesNodes) {
            latitudes.put(n.getId(), n.getLatitude().doubleValue());
            longitudes.put(n.getId(), n.getLongitude().doubleValue());
        }

        List<GraphEdge> edges = new ArrayList<>();
        for (RoadEdge entityEdge : entitiesEdges) {
            // Forward edge
            edges.add(new GraphEdge(
                    entityEdge.getId(),
                    entityEdge.getSourceNodeId(),
                    entityEdge.getDestinationNodeId(),
                    entityEdge.getBlocked() ? Double.POSITIVE_INFINITY : (double) entityEdge.getTravelTimeMinutes()
            ));
            
            // Reverse edge if bidirectional
            if (Boolean.TRUE.equals(entityEdge.getBidirectional())) {
                edges.add(new GraphEdge(
                        entityEdge.getId() + "_rev",
                        entityEdge.getDestinationNodeId(),
                        entityEdge.getSourceNodeId(),
                        entityEdge.getBlocked() ? Double.POSITIVE_INFINITY : (double) entityEdge.getTravelTimeMinutes()
                ));
            }
        }

        routingEngine.buildGraph(nodes, edges, latitudes, longitudes);
    }

    public RouteResponse calculateRoute(RouteRequest request) {
        // Just delegate to RoutingEngine directly, zero DB queries!
        RouteResponse response = routingEngine.bidirectionalAStar(
                request.getSourceNodeId(), 
                request.getDestinationNodeId()
        );

        if (!response.isReachable()) {
            throw new RouteNotFoundException("No feasible route found from " + request.getSourceNodeId() + " to " + request.getDestinationNodeId());
        }

        return response;
    }
}
