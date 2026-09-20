package com.resqmesh.network.controller;

import com.resqmesh.network.algorithm.RoutingEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/benchmark")
public class BenchmarkController {

    private final RoutingEngine routingEngine;

    public BenchmarkController(RoutingEngine routingEngine) {
        this.routingEngine = routingEngine;
    }

    @GetMapping
    public Map<String, Object> runBenchmark() {
        int nodeCount = routingEngine.getNodeCount();
        if (nodeCount < 2) {
            return Map.of("error", "Graph not loaded or too small");
        }

        int N = 100;
        Random random = new Random(42); // Deterministic

        return runBenchmarkImpl(N, random);
    }
    
    private Map<String, Object> runBenchmarkImpl(int N, Random random) {
        List<Long> dijkstraLatencies = new ArrayList<>();
        List<Long> aStarLatencies = new ArrayList<>();
        
        for (int i = 0; i < N; i++) {
            String src = routingEngine.getRandomNodeId(random);
            String dst = routingEngine.getRandomNodeId(random);
            
            if (src == null || dst == null) continue;

            long t0 = System.nanoTime();
            routingEngine.dijkstra(src, dst);
            long t1 = System.nanoTime();
            dijkstraLatencies.add((t1 - t0) / 1000L); // microseconds
            
            long t2 = System.nanoTime();
            routingEngine.bidirectionalAStar(src, dst);
            long t3 = System.nanoTime();
            aStarLatencies.add((t3 - t2) / 1000L); // microseconds
        }
        
        return Map.of(
            "graph", Map.of(
                "nodes", routingEngine.getNodeCount(),
                "edges", routingEngine.getEdgeCount()
            ),
            "dijkstra", calculateStats(dijkstraLatencies),
            "bidirectionalAStar", calculateStats(aStarLatencies)
        );
    }
    
    private Map<String, Object> calculateStats(List<Long> latencies) {
        if (latencies.isEmpty()) return Map.of();
        Collections.sort(latencies);
        
        long sum = 0;
        for (long l : latencies) sum += l;
        
        long p50 = latencies.get(latencies.size() / 2);
        long p95 = latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.get((int) (latencies.size() * 0.99));
        
        double avg = (double) sum / latencies.size();
        
        // queries per second
        double totalTimeSeconds = sum / 1_000_000.0; // sum is in us, so /1M gives seconds
        double qps = latencies.size() / totalTimeSeconds;
        
        return Map.of(
            "p50_us", p50,
            "p95_us", p95,
            "p99_us", p99,
            "avg_us", Math.round(avg),
            "qps", Math.round(qps)
        );
    }
}
