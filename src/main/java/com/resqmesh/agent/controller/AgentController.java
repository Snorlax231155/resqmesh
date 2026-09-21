package com.resqmesh.agent.controller;

import com.resqmesh.agent.dto.AgentCreateRequest;
import com.resqmesh.agent.dto.AgentStatusUpdateRequest;
import com.resqmesh.agent.entity.Agent;
import com.resqmesh.agent.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<Agent> registerAgent(@Valid @RequestBody AgentCreateRequest request) {
        return new ResponseEntity<>(agentService.registerAgent(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Agent>> getAllAgents() {
        return ResponseEntity.ok(agentService.getAllAgents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgent(@PathVariable UUID id) {
        return ResponseEntity.ok(agentService.getAgent(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Agent> updateStatus(@PathVariable UUID id, @Valid @RequestBody AgentStatusUpdateRequest request) {
        return ResponseEntity.ok(agentService.updateAgentStatus(id, request));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Agent> releaseAgent(@PathVariable UUID id) {
        return ResponseEntity.ok(agentService.releaseAgent(id));
    }
}
