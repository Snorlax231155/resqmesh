# ResQMesh — Hackathon Demo Scenario

This document outlines the step-by-step presentation script to demonstrate ResQMesh to hackathon judges.

> **Note:** All node and edge IDs are UUIDs from the real OpenStreetMap Lower Manhattan graph seeded on startup.
> The frontend UI at `http://localhost:5173` auto-discovers valid IDs — use it for the visual demo.
> The curl commands below use real graph IDs and will work against the running server.

---

## Scenario Setup
ResQMesh coordinates emergency logistics over a real road network. We will demonstrate automated routing, real-time road closure detection, and the 5-rung escalation ladder.

### 1. Start the Application
```bash
./mvnw clean package -DskipTests
java -jar target/resqmesh-0.0.1-SNAPSHOT.jar
```
*On startup, `DemoDataInitializer` seeds the real OpenStreetMap graph of Lower Manhattan — 1,315 nodes and 2,682 edges.*

---

### 2. Verify Initial Network & Dashboard
```bash
curl -s http://localhost:8080/api/v1/dashboard/summary | python3 -m json.tool
```
**Talking Point:** "Our dashboard API aggregates the operational state. The real OpenStreetMap road network of Lower Manhattan is fully loaded — over 1,300 nodes and 2,600 edges. Zero missions, zero agents yet."

---

### 3. Register a Field Agent
Pick any valid node ID from the network (example below is a real OSM node in Lower Manhattan):
```bash
curl -s -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{
    "agentCode": "UNIT-01",
    "name": "Alpha Responder",
    "currentNodeId": "e3599685-c67b-4f76-ac7a-8eabdb2a798a",
    "capacity": 5
  }' | python3 -m json.tool
```
**Talking Point:** "We register 'Alpha Responder' on East Village, near 4th St & 2nd Ave. The system immediately broadcasts this over WebSocket `/topic/agents` — the frontend map pins the agent in real time."

---

### 4. Create an Emergency Mission
```bash
curl -s -X POST http://localhost:8080/api/v1/missions \
  -H "Content-Type: application/json" \
  -d '{
    "missionCode": "MED-001",
    "pickupNodeId": "2467a056-9302-4409-ad27-f99b75576a20",
    "destinationNodeId": "64e81adb-6976-41f4-9d56-08d9e0266788",
    "priority": "CRITICAL",
    "deadline": "2030-12-31T23:59:59Z"
  }' | python3 -m json.tool
```
**Talking Point:** "A CRITICAL supply request (MED-001) is logged. Pickup is near W 14th St, destination near W 15th St & 7th Ave — real Manhattan streets."

---

### 5. Trigger the Dispatch Engine
```bash
curl -s -X POST http://localhost:8080/api/v1/dispatch/run | python3 -m json.tool
```
**Talking Point:** "The dispatch cycle runs. Our triage engine selects the active policy — by default `ExpectedLivesSaved`, which solves the global bipartite assignment problem using the Hungarian Algorithm. Bidirectional A* computes the optimal route on the real road graph. UNIT-01 is assigned to MED-001 with an accurately estimated arrival time."

---

### 6. Compare All Triage Policies (Optional Showcase)
First, tag the existing missions with a scenario ID:
```bash
# Seed a scenario scenario via the API or note the missionId from step 4 response
curl -s "http://localhost:8080/api/v1/policies/compare?scenario=demo" | python3 -m json.tool
```
**Talking Point:** "We can dry-run all four dispatch policies simultaneously — First Come First Served, Severity Weighted, Earliest Deadline First, and Expected Lives Saved — and compare them on avg response time, p95 response, critical missions missed, and severity-weighted delay. No other system shows you this comparison in real time."

---

### 7. The Disruption Event (The "Wow" Moment)
First, get a real edge ID that connects to the agent's route:
```bash
# List a few road edges
curl -s http://localhost:8080/api/v1/network/roads | python3 -c "
import json, sys
roads = json.load(sys.stdin)
for r in roads[:3]:
    print(r['id'], '->', r.get('name','?'))
"
```
Then block one:
```bash
curl -s -X POST http://localhost:8080/api/v1/disruptions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ROAD_CLOSURE",
    "affectedRoadId": "682cdd17-a0fb-4bbc-868a-47f6c6556c95",
    "description": "Bridge washed out by flash flood"
  }' | python3 -m json.tool
```
**Talking Point:** "Roads close unexpectedly in emergencies. ResQMesh intercepts the signal, immediately removes this edge from the in-memory CSR graph, and cascades a re-evaluation across all affected missions. Watch the frontend map — the road turns red and the agent rerouts in real time."

---

### 8. The 5-Rung Escalation Ladder
After the disruption, watch the `/topic/decisions` WebSocket feed (visible in the LiveFeed panel):

| Rung | What you'll see |
|---|---|
| **1** | "Rerouted same agent via alternate path. New ETA: Xm" |
| **2** | "Reassigned to different available agent" |
| **3** | "Reshuffled agent off lower-priority mission" |
| **5** | "Escalated to human (AT_RISK)" |

**Talking Point:** "Unlike a simple retry, ResQMesh has a 5-rung escalation ladder. It first tries to reroute the same agent. If blocked completely, it grabs an idle agent. If none available, it preempts a lower-priority mission. Only as a last resort does it escalate to a human — and it tells you exactly why."

---

### 9. Review Results
```bash
curl -s http://localhost:8080/api/v1/dashboard/summary | python3 -m json.tool
```

---

## Wrap Up
"ResQMesh is not a CRUD app. It is a resilient, disruption-aware dispatch engine running bidirectional A* on a real OSM road graph, with a globally-optimal triage engine and a 5-rung escalation ladder — all streaming live over WebSockets."
