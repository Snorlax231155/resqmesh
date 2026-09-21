package com.resqmesh.dispatch.controller;

import com.resqmesh.dispatch.dto.AssignmentResponse;
import com.resqmesh.dispatch.service.DispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping("/run")
    public ResponseEntity<List<AssignmentResponse>> runDispatchCycle() {
        return ResponseEntity.ok(dispatchService.runDispatchCycle());
    }

    @PostMapping("/assign")
    public ResponseEntity<AssignmentResponse> manualAssign(@org.springframework.web.bind.annotation.RequestParam java.util.UUID missionId, @org.springframework.web.bind.annotation.RequestParam java.util.UUID agentId) {
        return ResponseEntity.ok(dispatchService.manualAssign(missionId, agentId));
    }
}
