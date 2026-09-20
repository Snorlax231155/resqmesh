package com.resqmesh.mission.entity;

import com.resqmesh.mission.enums.MissionPriority;
import com.resqmesh.mission.enums.MissionStatus;
import com.resqmesh.mission.enums.RiskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "missions")
public class Mission {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "mission_code", nullable = false, unique = true, length = 64)
    private String missionCode;

    @Column(name = "pickup_node_id", nullable = false, length = 64)
    private String pickupNodeId;

    @Column(name = "destination_node_id", nullable = false, length = 64)
    private String destinationNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private MissionPriority priority;

    @Column(name = "deadline", nullable = false)
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MissionStatus status;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_status", nullable = false, length = 32)
    private RiskStatus riskStatus;

    @Column(name = "estimated_arrival_minutes")
    private Integer estimatedArrivalMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "scenario_id", length = 64)
    private String scenarioId;

    public Mission() {}

    public Mission(UUID id, String missionCode, String pickupNodeId, String destinationNodeId, MissionPriority priority, Instant deadline, MissionStatus status, UUID assignedAgentId, RiskStatus riskStatus, Integer estimatedArrivalMinutes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.missionCode = missionCode;
        this.pickupNodeId = pickupNodeId;
        this.destinationNodeId = destinationNodeId;
        this.priority = priority;
        this.deadline = deadline;
        this.status = status;
        this.assignedAgentId = assignedAgentId;
        this.riskStatus = riskStatus;
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMissionCode() { return missionCode; }
    public void setMissionCode(String missionCode) { this.missionCode = missionCode; }
    public String getPickupNodeId() { return pickupNodeId; }
    public void setPickupNodeId(String pickupNodeId) { this.pickupNodeId = pickupNodeId; }
    public String getDestinationNodeId() { return destinationNodeId; }
    public void setDestinationNodeId(String destinationNodeId) { this.destinationNodeId = destinationNodeId; }
    public MissionPriority getPriority() { return priority; }
    public void setPriority(MissionPriority priority) { this.priority = priority; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public MissionStatus getStatus() { return status; }
    public void setStatus(MissionStatus status) { this.status = status; }
    public UUID getAssignedAgentId() { return assignedAgentId; }
    public void setAssignedAgentId(UUID assignedAgentId) { this.assignedAgentId = assignedAgentId; }
    public RiskStatus getRiskStatus() { return riskStatus; }
    public void setRiskStatus(RiskStatus riskStatus) { this.riskStatus = riskStatus; }
    public Integer getEstimatedArrivalMinutes() { return estimatedArrivalMinutes; }
    public void setEstimatedArrivalMinutes(Integer estimatedArrivalMinutes) { this.estimatedArrivalMinutes = estimatedArrivalMinutes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }

    @PrePersist
    public void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (riskStatus == null) {
            riskStatus = RiskStatus.ON_TRACK;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
