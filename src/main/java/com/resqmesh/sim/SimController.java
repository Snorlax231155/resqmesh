package com.resqmesh.sim;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/sim")
public class SimController {

    private final ScenarioManager scenarioManager;
    private final SimulationClock clock;

    public SimController(ScenarioManager scenarioManager, SimulationClock clock) {
        this.scenarioManager = scenarioManager;
        this.clock = clock;
    }

    @PostMapping("/load")
    public ResponseEntity<Void> loadScenario(@RequestParam String scenario) throws IOException {
        scenarioManager.loadScenario(scenario);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/play")
    public ResponseEntity<Void> play() {
        clock.play();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pause")
    public ResponseEntity<Void> pause() {
        clock.pause();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/speed")
    public ResponseEntity<Void> setSpeed(@RequestParam double x) {
        clock.setSpeed(x);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        clock.reset();
        return ResponseEntity.ok().build();
    }
}
