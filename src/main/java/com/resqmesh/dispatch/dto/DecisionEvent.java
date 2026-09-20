package com.resqmesh.dispatch.dto;

public class DecisionEvent {
    private String missionCode;
    private String trace;

    public DecisionEvent(String missionCode, String trace) {
        this.missionCode = missionCode;
        this.trace = trace;
    }

    public String getMissionCode() { return missionCode; }
    public String getTrace() { return trace; }
}
