package com.resqmesh.network.dto;

import jakarta.validation.constraints.NotBlank;

public class RouteRequest {

    @NotBlank
    private String sourceNodeId;

    @NotBlank
    private String destinationNodeId;

    public RouteRequest() {}

    public RouteRequest(String sourceNodeId, String destinationNodeId) {
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
    }

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public String getDestinationNodeId() { return destinationNodeId; }
    public void setDestinationNodeId(String destinationNodeId) { this.destinationNodeId = destinationNodeId; }
}
