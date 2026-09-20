package com.resqmesh.network.controller;

import com.resqmesh.network.dto.RouteRequest;
import com.resqmesh.network.dto.RouteResponse;
import com.resqmesh.network.entity.RoadEdge;
import com.resqmesh.network.entity.RoadNode;
import com.resqmesh.network.service.NetworkService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/network")
public class NetworkController {

    private final NetworkService networkService;

    public NetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<RoadNode>> getAllNodes() {
        return ResponseEntity.ok(networkService.getAllNodes());
    }

    @GetMapping("/roads")
    public ResponseEntity<List<RoadEdge>> getAllRoads() {
        return ResponseEntity.ok(networkService.getAllRoads());
    }

    @PostMapping("/routes")
    public ResponseEntity<RouteResponse> calculateRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(networkService.calculateRoute(request));
    }
}
