package com.resqmesh.disruption.dto;

import com.resqmesh.disruption.enums.DisruptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DisruptionCreateRequest {

    @NotNull
    private DisruptionType type;

    @NotBlank
    private String affectedRoadId;

    @NotBlank
    private String description;

    public DisruptionCreateRequest() {}

    public DisruptionCreateRequest(DisruptionType type, String affectedRoadId, String description) {
        this.type = type;
        this.affectedRoadId = affectedRoadId;
        this.description = description;
    }

    public DisruptionType getType() { return type; }
    public void setType(DisruptionType type) { this.type = type; }
    public String getAffectedRoadId() { return affectedRoadId; }
    public void setAffectedRoadId(String affectedRoadId) { this.affectedRoadId = affectedRoadId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
