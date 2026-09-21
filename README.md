# 🚑 ResQMesh

<div align="center">
  <h3>Intelligent Emergency Response & Routing Platform</h3>
  <p>A real-time, highly scalable triage dispatch & routing engine built for disaster management and modern emergency medical services.</p>

  <p>
    <a href="http://ec2-13-203-196-186.ap-south-1.compute.amazonaws.com:8080/"><strong>🌐 Live Application Showcase Demo</strong></a>
  </p>
</div>

---

## 🎯 Overview

**ResQMesh** solves the maximal coverage, dynamic assignment, and real-time routing problem for emergency rescue squads operating in disaster-affected urban environments. When urban infrastructure is disrupted by flash floods, road washouts, or debris, static routing heuristics fail and life-saving response times decay exponentially. 

ResQMesh ingests real-world **OpenStreetMap (OSM)** topology (1,315 nodes, 2,682 edges across Lower Manhattan) and models active rescue squads and emergency SOS victims as a dynamic bipartite graph. 

The core engine uses the **Hungarian Algorithm** ($O(V^3)$ bipartite matching) alongside a **5-Rung Escalation Ladder** to maximize lives saved, enforce strict medical deadlines, and dynamically re-route responders around blocked road segments in real-time.

---

## ✨ Key Capabilities & Features

### ⚡ 1. Autopilot vs. Manual Dispatch Modes
- **`⚡ AUTOPILOT: ON`**: Runs automated dispatch cycles (~every 3s) using optimal bipartite graph matching to pair incoming SOS requests with available rescue squads.
- **`▶ AUTOPILOT: OFF (MANUAL MODE)`**: Stops automatic background dispatching, enabling dispatchers to retain full manual control over unit assignments.

### 📍 2. Interactive Map Marker Unit Assignment
- **Rescue Squad Markers (🚑)**: Click any unit on the Leaflet map to inspect status, current graph node, and manually assign unassigned emergency missions from an inline popup picker.
- **Emergency Victim Markers (🚨)**: Click any SOS marker to inspect priority, pickup location, assigned squad, or pair an available squad with 1-click (`👉 ASSIGN SELECTED SQUAD`).

### 📢 3. Civilian Emergency Complaint & SOS Intake Form
- **Civilian Intake Modal (`📢 LOG COMPLAINT / SOS`)**: Allows dispatch operators or civilians to lodge custom emergency complaints.
- Includes reporter details, contact channels, priority selection (`CRITICAL`, `HIGH`, `NORMAL`, `LOW`), landmark location node presets (*Times Square*, *Wall St*, *FDR Drive*, *Brooklyn Bridge*), and situation descriptions.
- Automatically generates live missions and broadcasts STOMP events into the decision log (`[CIVILIAN_COMPLAINT]`).

### ⚡ 4. Tactical Fleet & Task Control Drawer
- **Drawer Panel (`⚡ FLEET CONTROL`)**: A slide-out panel for fleet management.
- Quick actions:
  - **`✅ END TASK & FREE UNIT`**: Completes a mission and releases the assigned rescue squad back to `AVAILABLE` status at the target node.
  - **`🔓 FORCE RELEASE UNIT`**: Forcibly frees an occupied rescue squad.
  - **`📍 LOCATE`**: Focuses the map on specific graph nodes.

### 🛣️ 5. Dynamic Pathfinding & 5-Rung Rerouting Ladder
- **Bidirectional A* Algorithm**: In-memory graph traversal across sparse Compressed Sparse Row (CSR) arrays.
- **5-Rung Escalation Ladder**:
  1. *Rung 1*: Reroute same unit around blocked roads via alternate A* path.
  2. *Rung 2*: Reassign to a closer available unit.
  3. *Rung 3*: Reshuffle unit off a lower-priority mission.
  4. *Rung 4*: Capacity split simulation.
  5. *Rung 5*: Escalate to human operator (`AT_RISK`).

---

## 🏛️ Architecture & Tech Stack

```
   ┌─────────────────────────────────────────────────────────────┐
   │                React 18 + Leaflet Web GIS                   │
   │      (Autopilot / Manual Mode, Marker Popups, Drawer)       │
   └──────────────────────────────┬──────────────────────────────┘
                                  │ STOMP over WebSocket & HTTP REST API
   ┌──────────────────────────────▼──────────────────────────────┐
   │                 Spring Boot 3.3.3 API Server                │
   │  - DispatchService (Hungarian Solver & 5-Rung Ladder)      │
   │  - NotificationService (WebSocket STOMP Broker)             │
   │  - RoutingEngine (In-Memory CSR Graph Traversal)            │
   └──────────────────────────────┬──────────────────────────────┘
                                  │ Fast In-Memory Graph Access
   ┌──────────────────────────────▼──────────────────────────────┐
   │        OpenStreetMap (OSM) Lower Manhattan Topology         │
   │               (1,315 Nodes / 2,682 Edges)                   │
   └──────────────────────────────┬──────────────────────────────┘
```

### Performance Metrics:
- **Dijkstra Traversal:** ~9,160 QPS (p95: 400µs, p50: 82µs)
- **Bidirectional A*:** ~6,015 QPS (p95: 424µs, p50: 137µs)
- **WebSocket Broadcast Latency:** Sub-50ms propagation across all clients

---

## 🛠️ Quick Start (Local Development)

### Prerequisites
- **Java 21**
- **Node.js 18+**
- **Maven**

### 1. Build and Run Frontend (Vite + React)
```bash
cd frontend
npm install
npm run dev
```

### 2. Start Backend Engine
```bash
# From project root
./mvnw spring-boot:run
```

Open `http://localhost:5173` in your browser.

---

## 📦 Single-JAR Deployment (AWS EC2 / Cloud)

ResQMesh compiles into a single, unified fat `.jar` containing static frontend assets:

```bash
# 1. Build Production Frontend Assets
cd frontend
npm run build
cd ..

# 2. Copy Static Assets to Backend
rm -rf src/main/resources/static/*
cp -r frontend/dist/* src/main/resources/static/

# 3. Package Fat JAR
./mvnw clean package -DskipTests

# 4. Launch on Production Server
java -jar target/resqmesh-0.0.1-SNAPSHOT.jar
```

---

## 📜 License

MIT License — Built for disaster relief hackathons and emergency services optimization.
