package com.resqmesh.agent.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AgentCreateRequest {

    @NotBlank
    private String agentCode;

    @NotBlank
    private String name;

    @NotBlank
    private String currentNodeId;

    @NotNull
    @Min(1)
    private Integer capacity;

    public AgentCreateRequest() {}

    public AgentCreateRequest(String agentCode, String name, String currentNodeId, Integer capacity) {
        this.agentCode = agentCode;
        this.name = name;
        this.currentNodeId = currentNodeId;
        this.capacity = capacity;
    }

    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
