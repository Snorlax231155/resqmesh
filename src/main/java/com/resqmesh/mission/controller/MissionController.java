package com.resqmesh.mission.controller;

import com.resqmesh.mission.dto.MissionCreateRequest;
import com.resqmesh.mission.dto.MissionStatusUpdateRequest;
import com.resqmesh.mission.entity.Mission;
import com.resqmesh.mission.service.MissionService;
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
@RequestMapping("/api/v1/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @PostMapping
    public ResponseEntity<Mission> createMission(@Valid @RequestBody MissionCreateRequest request) {
        return new ResponseEntity<>(missionService.createMission(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Mission>> getAllMissions() {
        return ResponseEntity.ok(missionService.getAllMissions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mission> getMission(@PathVariable UUID id) {
        return ResponseEntity.ok(missionService.getMission(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Mission> updateStatus(@PathVariable UUID id, @Valid @RequestBody MissionStatusUpdateRequest request) {
        return ResponseEntity.ok(missionService.updateMissionStatus(id, request));
    }
}
