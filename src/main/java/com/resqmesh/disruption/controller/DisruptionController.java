package com.resqmesh.disruption.controller;

import com.resqmesh.disruption.dto.DisruptionCreateRequest;
import com.resqmesh.disruption.entity.Disruption;
import com.resqmesh.disruption.service.DisruptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disruptions")
public class DisruptionController {

    private final DisruptionService disruptionService;

    public DisruptionController(DisruptionService disruptionService) {
        this.disruptionService = disruptionService;
    }

    @PostMapping
    public ResponseEntity<Disruption> reportDisruption(@Valid @RequestBody DisruptionCreateRequest request) {
        return new ResponseEntity<>(disruptionService.reportDisruption(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Disruption>> getAllActiveDisruptions() {
        return ResponseEntity.ok(disruptionService.getAllActiveDisruptions());
    }
}
