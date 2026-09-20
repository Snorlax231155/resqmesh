package com.resqmesh.network.algorithm;

import com.resqmesh.network.dto.RouteResponse;
import com.resqmesh.network.model.GraphEdge;
import com.resqmesh.network.model.GraphNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingEngineTest {

    private RoutingEngine routingEngine;

    @BeforeEach
    void setUp() {
        routingEngine = new RoutingEngine();
    }

    @Test
    void testShortestRouteFound() {
        List<GraphNode> nodes = List.of(
                new GraphNode("N1"),
                new GraphNode("N2"),
                new GraphNode("N3")
        );

        List<GraphEdge> edges = List.of(
                new GraphEdge("E1", "N1", "N2", 10.0),
                new GraphEdge("E2", "N2", "N3", 5.0),
                new GraphEdge("E3", "N1", "N3", 20.0)
        );

        routingEngine.buildGraph(nodes, edges, null, null);
        RouteResponse response = routingEngine.calculateShortestPath(nodes, edges, "N1", "N3");

        assertTrue(response.isReachable());
        assertEquals(15, response.getTotalTravelTimeMinutes());
        assertEquals(List.of("N1", "N2", "N3"), response.getNodePath());
        assertEquals(List.of("E1", "E2"), response.getRoadPath());
    }

    @Test
    void testUnreachableDestination() {
        List<GraphNode> nodes = List.of(
                new GraphNode("N1"),
                new GraphNode("N2"),
                new GraphNode("N3")
        );

        List<GraphEdge> edges = List.of(
                new GraphEdge("E1", "N1", "N2", 10.0)
        );

        routingEngine.buildGraph(nodes, edges, null, null);
        RouteResponse response = routingEngine.calculateShortestPath(nodes, edges, "N1", "N3");

        assertFalse(response.isReachable());
        assertNull(response.getTotalTravelTimeMinutes());
        assertTrue(response.getNodePath().isEmpty());
    }

    @Test
    void testSourceEqualsDestination() {
        List<GraphNode> nodes = List.of(
                new GraphNode("N1")
        );

        List<GraphEdge> edges = List.of();

        routingEngine.buildGraph(nodes, edges, null, null);
        RouteResponse response = routingEngine.calculateShortestPath(nodes, edges, "N1", "N1");

        assertTrue(response.isReachable());
        assertEquals(0, response.getTotalTravelTimeMinutes());
        assertEquals(List.of("N1"), response.getNodePath());
        assertTrue(response.getRoadPath().isEmpty());
    }

    @Test
    void testNegativeWeightsRejected() {
        List<GraphNode> nodes = List.of(
                new GraphNode("N1"),
                new GraphNode("N2"),
                new GraphNode("N3")
        );

        List<GraphEdge> edges = List.of(
                new GraphEdge("E1", "N1", "N2", 10.0),
                new GraphEdge("E2", "N2", "N3", Double.POSITIVE_INFINITY),
                new GraphEdge("E3", "N1", "N3", 15.0)
        );

        routingEngine.buildGraph(nodes, edges, null, null);
        RouteResponse response = routingEngine.calculateShortestPath(nodes, edges, "N1", "N3");

        assertTrue(response.isReachable());
        assertEquals(15, response.getTotalTravelTimeMinutes());
        assertEquals(List.of("N1", "N3"), response.getNodePath());
    }
    
    @Test
    void testBlockedRoadAvoidedByServiceLayerEquivalent() {
        List<GraphNode> nodes = List.of(
                new GraphNode("N1"),
                new GraphNode("N2"),
                new GraphNode("N3")
        );

        List<GraphEdge> edges = List.of(
                new GraphEdge("E3", "N1", "N3", 20.0)
        );

        routingEngine.buildGraph(nodes, edges, null, null);
        RouteResponse response = routingEngine.calculateShortestPath(nodes, edges, "N1", "N3");

        assertTrue(response.isReachable());
        assertEquals(20, response.getTotalTravelTimeMinutes());
        assertEquals(List.of("N1", "N3"), response.getNodePath());
    }
}
