package com.resqmesh.agent.dto;

import com.resqmesh.agent.enums.AgentStatus;
import jakarta.validation.constraints.NotNull;

public class AgentStatusUpdateRequest {

    @NotNull
    private AgentStatus status;

    public AgentStatusUpdateRequest() {}

    public AgentStatusUpdateRequest(AgentStatus status) {
        this.status = status;
    }

    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = status; }
}
