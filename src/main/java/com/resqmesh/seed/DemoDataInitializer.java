package com.resqmesh.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resqmesh.network.entity.RoadEdge;
import com.resqmesh.network.entity.RoadNode;
import com.resqmesh.network.repository.RoadEdgeRepository;
import com.resqmesh.network.repository.RoadNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final RoadNodeRepository roadNodeRepository;
    private final RoadEdgeRepository roadEdgeRepository;
    private final ObjectMapper objectMapper;

    public DemoDataInitializer(RoadNodeRepository roadNodeRepository, RoadEdgeRepository roadEdgeRepository) {
        this.roadNodeRepository = roadNodeRepository;
        this.roadEdgeRepository = roadEdgeRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roadNodeRepository.count() == 0) {
            log.info("Seeding initial network data from JSON...");
            seedNetwork();
            log.info("Network data seeded.");
        }
    }

    private void seedNetwork() {
        try (InputStream is = new ClassPathResource("manhattan_graph.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            
            List<RoadNode> nodes = new ArrayList<>();
            for (JsonNode n : root.get("nodes")) {
                nodes.add(new RoadNode(
                        n.get("id").asText(),
                        n.get("name").asText(),
                        new BigDecimal(n.get("latitude").asText()),
                        new BigDecimal(n.get("longitude").asText())
                ));
            }
            log.info("Saving {} nodes...", nodes.size());
            roadNodeRepository.saveAll(nodes);

            List<RoadEdge> edges = new ArrayList<>();
            for (JsonNode e : root.get("edges")) {
                edges.add(new RoadEdge(
                        e.get("id").asText(),
                        e.get("sourceNodeId").asText(),
                        e.get("destinationNodeId").asText(),
                        e.get("travelTimeMinutes").asInt(),
                        new BigDecimal(e.get("distanceKm").asText()),
                        e.get("blocked").asBoolean(),
                        e.get("bidirectional").asBoolean()
                ));
            }
            log.info("Saving {} edges...", edges.size());
            roadEdgeRepository.saveAll(edges);
            
        } catch (Exception e) {
            log.error("Failed to load manhattan graph. Ensure manhattan_graph.json exists.", e);
        }
    }
}
