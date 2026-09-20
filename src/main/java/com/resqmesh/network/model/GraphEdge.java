package com.resqmesh.network.model;

import java.util.Objects;

public class GraphEdge {
    private String id;
    private String sourceNodeId;
    private String destinationNodeId;
    private Double travelTimeMinutes;

    public GraphEdge() {}

    public GraphEdge(String id, String sourceNodeId, String destinationNodeId, Double travelTimeMinutes) {
        this.id = id;
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
        this.travelTimeMinutes = travelTimeMinutes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public String getDestinationNodeId() { return destinationNodeId; }
    public void setDestinationNodeId(String destinationNodeId) { this.destinationNodeId = destinationNodeId; }
    public Double getTravelTimeMinutes() { return travelTimeMinutes; }
    public void setTravelTimeMinutes(Double travelTimeMinutes) { this.travelTimeMinutes = travelTimeMinutes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(id, graphEdge.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
