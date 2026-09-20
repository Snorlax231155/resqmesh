package com.resqmesh.sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resqmesh.agent.dto.AgentCreateRequest;
import com.resqmesh.agent.enums.AgentStatus;
import com.resqmesh.agent.service.AgentService;
import com.resqmesh.mission.dto.MissionCreateRequest;
import com.resqmesh.mission.service.MissionService;
import com.resqmesh.disruption.dto.DisruptionCreateRequest;
import com.resqmesh.disruption.service.DisruptionService;
import com.resqmesh.agent.repository.AgentRepository;
import com.resqmesh.mission.repository.MissionRepository;
import com.resqmesh.disruption.repository.DisruptionRepository;
import com.resqmesh.dispatch.repository.AssignmentHistoryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ScenarioManager {
    private static final Logger log = LoggerFactory.getLogger(ScenarioManager.class);
    private final ObjectMapper objectMapper;
    private final SimulationClock clock;
    
    private final AgentService agentService;
    private final MissionService missionService;
    private final DisruptionService disruptionService;
    
    private final AgentRepository agentRepository;
    private final MissionRepository missionRepository;
    private final DisruptionRepository disruptionRepository;
    private final AssignmentHistoryRepository assignmentHistoryRepository;

    private Thread eventThread;
    private volatile boolean running = true;
    private PriorityQueue<ScenarioEvent> eventQueue = new PriorityQueue<>(Comparator.comparingLong(ScenarioEvent::getTOffsetSeconds));
    private long scenarioStartTimeMillis = 0;

    public ScenarioManager(ObjectMapper objectMapper, SimulationClock clock, AgentService agentService, MissionService missionService, DisruptionService disruptionService, AgentRepository agentRepository, MissionRepository missionRepository, DisruptionRepository disruptionRepository, AssignmentHistoryRepository assignmentHistoryRepository) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.agentService = agentService;
        this.missionService = missionService;
        this.disruptionService = disruptionService;
        this.agentRepository = agentRepository;
        this.missionRepository = missionRepository;
        this.disruptionRepository = disruptionRepository;
        this.assignmentHistoryRepository = assignmentHistoryRepository;
    }

    @PostConstruct
    public void init() {
        eventThread = new Thread(this::eventLoop);
        eventThread.setName("ScenarioEventThread");
        eventThread.start();
    }

    @PreDestroy
    public void cleanup() {
        running = false;
        eventThread.interrupt();
    }

    @Transactional
    public void loadScenario(String scenarioName) throws IOException {
        clock.pause();
        clock.reset();
        eventQueue.clear();

        assignmentHistoryRepository.deleteAll();
        missionRepository.deleteAll();
        disruptionRepository.deleteAll();
        agentRepository.deleteAll();

        ClassPathResource resource = new ClassPathResource("scenarios/" + scenarioName + ".json");
        Scenario scenario = objectMapper.readValue(resource.getInputStream(), Scenario.class);

        if (scenario.getAgents() != null) {
            for (var agentNode : scenario.getAgents()) {
                AgentCreateRequest req = objectMapper.treeToValue(agentNode, AgentCreateRequest.class);
                agentService.registerAgent(req);
            }
        }

        if (scenario.getEvents() != null) {
            eventQueue.addAll(scenario.getEvents());
        }

        scenarioStartTimeMillis = clock.getCurrentTime().toEpochMilli();
        log.info("Loaded scenario: {}", scenarioName);
    }

    private void eventLoop() {
        while (running) {
            try {
                Thread.sleep(100);
                if (eventQueue.isEmpty()) continue;

                long currentElapsedSec = (clock.getCurrentTime().toEpochMilli() - scenarioStartTimeMillis) / 1000;

                while (!eventQueue.isEmpty() && eventQueue.peek().getTOffsetSeconds() <= currentElapsedSec) {
                    ScenarioEvent event = eventQueue.poll();
                    processEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing scenario event", e);
            }
        }
    }

    private void processEvent(ScenarioEvent event) throws Exception {
        log.info("Processing event: {}", event.getType());
        switch (event.getType()) {
            case "AGENT_ONLINE":
                String agentCode = event.getPayload().get("agentCode").asText();
                agentRepository.findByAgentCode(agentCode).ifPresent(agent -> {
                    agentService.updateAgentStatus(agent.getId(), new com.resqmesh.agent.dto.AgentStatusUpdateRequest(AgentStatus.AVAILABLE));
                });
                break;
            case "MISSION_CREATED":
                MissionCreateRequest mReq = objectMapper.treeToValue(event.getPayload(), MissionCreateRequest.class);
                missionService.createMission(mReq);
                break;
            case "EDGE_CLOSED":
                DisruptionCreateRequest dReq = objectMapper.treeToValue(event.getPayload(), DisruptionCreateRequest.class);
                disruptionService.reportDisruption(dReq);
                break;
            case "EDGE_REOPENED":
                if (event.getPayload().has("disruptionId")) {
                    disruptionService.resolveDisruption(java.util.UUID.fromString(event.getPayload().get("disruptionId").asText()));
                }
                break;
            case "MISSION_COMPLETED":
                String mCode = event.getPayload().get("missionCode").asText();
                missionRepository.findByMissionCode(mCode).ifPresent(mission -> {
                    missionService.updateMissionStatus(mission.getId(), new com.resqmesh.mission.dto.MissionStatusUpdateRequest(com.resqmesh.mission.enums.MissionStatus.COMPLETED));
                });
                break;
        }
    }
}
