package com.resqmesh.network.controller;

import com.resqmesh.dispatch.service.RepositioningService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config/repositioning")
public class RepositioningConfigController {

    private final RepositioningService repositioningService;

    public RepositioningConfigController(RepositioningService repositioningService) {
        this.repositioningService = repositioningService;
    }

    @PostMapping
    public String setRepositioning(@RequestParam boolean enabled) {
        repositioningService.setEnabled(enabled);
        return "Repositioning enabled: " + enabled;
    }
}
