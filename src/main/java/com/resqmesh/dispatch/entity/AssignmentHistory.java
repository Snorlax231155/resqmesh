package com.resqmesh.dispatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignment_histories")
public class AssignmentHistory {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "previous_agent_id")
    private UUID previousAgentId;

    @Column(name = "new_agent_id")
    private UUID newAgentId;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @Column(name = "disruption_id")
    private UUID disruptionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AssignmentHistory() {}

    public AssignmentHistory(UUID id, UUID missionId, UUID previousAgentId, UUID newAgentId, String reason, UUID disruptionId, Instant createdAt) {
        this.id = id;
        this.missionId = missionId;
        this.previousAgentId = previousAgentId;
        this.newAgentId = newAgentId;
        this.reason = reason;
        this.disruptionId = disruptionId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMissionId() { return missionId; }
    public void setMissionId(UUID missionId) { this.missionId = missionId; }
    public UUID getPreviousAgentId() { return previousAgentId; }
    public void setPreviousAgentId(UUID previousAgentId) { this.previousAgentId = previousAgentId; }
    public UUID getNewAgentId() { return newAgentId; }
    public void setNewAgentId(UUID newAgentId) { this.newAgentId = newAgentId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public UUID getDisruptionId() { return disruptionId; }
    public void setDisruptionId(UUID disruptionId) { this.disruptionId = disruptionId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = com.resqmesh.sim.SimulationClock.now();
        }
    }
}
