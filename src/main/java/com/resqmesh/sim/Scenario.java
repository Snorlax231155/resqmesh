package com.resqmesh.sim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scenario {
    private String name;
    private String graphSource;
    private List<JsonNode> agents;
    private List<ScenarioEvent> events;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGraphSource() { return graphSource; }
    public void setGraphSource(String graphSource) { this.graphSource = graphSource; }
    public List<JsonNode> getAgents() { return agents; }
    public void setAgents(List<JsonNode> agents) { this.agents = agents; }
    public List<ScenarioEvent> getEvents() { return events; }
    public void setEvents(List<ScenarioEvent> events) { this.events = events; }
}
