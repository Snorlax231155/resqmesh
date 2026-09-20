package com.resqmesh.dispatch.controller;

import com.resqmesh.dispatch.service.PolicyComparisonService;
import com.resqmesh.dispatch.service.DispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyComparisonService comparisonService;
    private final DispatchService dispatchService;

    public PolicyController(PolicyComparisonService comparisonService, DispatchService dispatchService) {
        this.comparisonService = comparisonService;
        this.dispatchService = dispatchService;
    }

    /**
     * Compare all triage policies against a named scenario.
     * GET /api/v1/policies/compare?scenario=test1
     */
    @GetMapping("/compare")
    public ResponseEntity<List<PolicyComparisonService.PolicyResult>> compare(
            @RequestParam String scenario) {
        return ResponseEntity.ok(comparisonService.compare(scenario));
    }

    /**
     * Get the names of all registered triage policies.
     * GET /api/v1/policies
     */
    @GetMapping
    public ResponseEntity<List<String>> listPolicies() {
        return ResponseEntity.ok(
                dispatchService.getPolicies().stream()
                        .map(p -> p.name())
                        .toList()
        );
    }

    /**
     * Set the active triage policy.
     * POST /api/v1/policies/active
     * Body: { "name": "SeverityWeighted" }
     */
    @PostMapping("/active")
    public ResponseEntity<Map<String, String>> setActivePolicy(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        dispatchService.setActivePolicy(name);
        return ResponseEntity.ok(Map.of("active", name));
    }

    /**
     * Get the currently active triage policy.
     * GET /api/v1/policies/active
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, String>> getActivePolicy() {
        return ResponseEntity.ok(Map.of("active", dispatchService.getActivePolicy().name()));
    }
}
