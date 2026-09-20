package com.resqmesh.sim;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class SimulationClock {
    private static SimulationClock instance;

    private Instant virtualStartTime = Instant.parse("2026-09-21T08:00:00Z");
    private Instant realStartTime = Instant.now();
    
    private boolean isRunning = false;
    private double speed = 1.0;
    
    private Instant lastUpdatedRealTime = Instant.now();
    private Instant currentVirtualTime = virtualStartTime;

    public SimulationClock() {
        instance = this;
    }

    public static Instant now() {
        if (instance != null) {
            return instance.getCurrentTime();
        }
        return Instant.now();
    }

    public synchronized Instant getCurrentTime() {
        if (!isRunning) {
            return currentVirtualTime;
        }
        Instant realNow = Instant.now();
        Duration elapsedReal = Duration.between(lastUpdatedRealTime, realNow);
        long elapsedVirtualMillis = (long) (elapsedReal.toMillis() * speed);
        currentVirtualTime = currentVirtualTime.plusMillis(elapsedVirtualMillis);
        lastUpdatedRealTime = realNow;
        return currentVirtualTime;
    }

    public synchronized void play() {
        if (!isRunning) {
            lastUpdatedRealTime = Instant.now();
            isRunning = true;
        }
    }

    public synchronized void pause() {
        if (isRunning) {
            getCurrentTime(); // update currentVirtualTime
            isRunning = false;
        }
    }

    public synchronized void setSpeed(double newSpeed) {
        getCurrentTime(); // bake in elapsed time at old speed
        this.speed = newSpeed;
        this.lastUpdatedRealTime = Instant.now();
    }

    public synchronized void reset() {
        this.currentVirtualTime = virtualStartTime;
        this.lastUpdatedRealTime = Instant.now();
        this.isRunning = false;
        this.speed = 1.0;
    }
}
