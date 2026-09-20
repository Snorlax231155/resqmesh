package com.resqmesh.sim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScenarioEvent {
    private long tOffsetSeconds;
    private String type;
    private JsonNode payload;

    public long getTOffsetSeconds() { return tOffsetSeconds; }
    public void setTOffsetSeconds(long tOffsetSeconds) { this.tOffsetSeconds = tOffsetSeconds; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }
}
