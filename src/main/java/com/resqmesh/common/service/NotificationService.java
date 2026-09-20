package com.resqmesh.common.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastMissionUpdate(Object missionPayload) {
        messagingTemplate.convertAndSend("/topic/missions", missionPayload);
    }

    public void broadcastAgentUpdate(Object agentPayload) {
        messagingTemplate.convertAndSend("/topic/agents", agentPayload);
    }

    public void broadcastDisruptionUpdate(Object disruptionPayload) {
        messagingTemplate.convertAndSend("/topic/disruptions", disruptionPayload);
    }

    public void broadcastAgentRepositioned(Object payload) {
        messagingTemplate.convertAndSend("/topic/repositioning", payload);
    }

    public void broadcastDecision(Object payload) {
        messagingTemplate.convertAndSend("/topic/decisions", payload);
    }
}
