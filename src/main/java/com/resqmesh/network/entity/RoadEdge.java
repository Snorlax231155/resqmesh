package com.resqmesh.network.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "road_edges")
public class RoadEdge {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "source_node_id", nullable = false, length = 64)
    private String sourceNodeId;

    @Column(name = "destination_node_id", nullable = false, length = 64)
    private String destinationNodeId;

    @Column(name = "travel_time_minutes", nullable = false)
    private Integer travelTimeMinutes;

    @Column(name = "distance_km", precision = 8, scale = 3)
    private BigDecimal distanceKm;

    @Column(name = "blocked", nullable = false)
    private Boolean blocked;

    @Column(name = "bidirectional", nullable = false)
    private Boolean bidirectional;

    public RoadEdge() {}

    public RoadEdge(String id, String sourceNodeId, String destinationNodeId, Integer travelTimeMinutes, BigDecimal distanceKm, Boolean blocked, Boolean bidirectional) {
        this.id = id;
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
        this.travelTimeMinutes = travelTimeMinutes;
        this.distanceKm = distanceKm;
        this.blocked = blocked;
        this.bidirectional = bidirectional;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public String getDestinationNodeId() { return destinationNodeId; }
    public void setDestinationNodeId(String destinationNodeId) { this.destinationNodeId = destinationNodeId; }
    public Integer getTravelTimeMinutes() { return travelTimeMinutes; }
    public void setTravelTimeMinutes(Integer travelTimeMinutes) { this.travelTimeMinutes = travelTimeMinutes; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public Boolean getBlocked() { return blocked; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }
    public Boolean getBidirectional() { return bidirectional; }
    public void setBidirectional(Boolean bidirectional) { this.bidirectional = bidirectional; }
}
