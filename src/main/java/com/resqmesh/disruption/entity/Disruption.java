package com.resqmesh.disruption.entity;

import com.resqmesh.disruption.enums.DisruptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disruptions")
public class Disruption {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private DisruptionType type;

    @Column(name = "affected_road_id", length = 64)
    private String affectedRoadId;

    @Column(name = "affected_agent_id")
    private UUID affectedAgentId;

    @Column(name = "description", nullable = false, length = 512)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Disruption() {}

    public Disruption(UUID id, DisruptionType type, String affectedRoadId, UUID affectedAgentId, String description, Boolean active, Instant createdAt, Instant resolvedAt) {
        this.id = id;
        this.type = type;
        this.affectedRoadId = affectedRoadId;
        this.affectedAgentId = affectedAgentId;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public DisruptionType getType() { return type; }
    public void setType(DisruptionType type) { this.type = type; }
    public String getAffectedRoadId() { return affectedRoadId; }
    public void setAffectedRoadId(String affectedRoadId) { this.affectedRoadId = affectedRoadId; }
    public UUID getAffectedAgentId() { return affectedAgentId; }
    public void setAffectedAgentId(UUID affectedAgentId) { this.affectedAgentId = affectedAgentId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    @PrePersist
    public void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = com.resqmesh.sim.SimulationClock.now();
        }
        if (active == null) {
            active = true;
        }
    }
}
