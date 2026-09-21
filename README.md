# ResQMesh - Tactical Emergency Dispatch & Logistics Platform

**Live Demo:** [http://ec2-13-203-196-186.ap-south-1.compute.amazonaws.com:8080](http://ec2-13-203-196-186.ap-south-1.compute.amazonaws.com:8080)

ResQMesh is a high-performance tactical dispatch platform designed to coordinate emergency medical services (EMS) and rescue fleets during severe urban disasters. 

---

## 🚨 What Problem Does It Solve?
During natural disasters—such as flash floods, earthquakes, or severe storms—road networks become unpredictable. Traditional GPS and standard 911 dispatch systems fail because they assume city streets are static. When 911 call volumes spike and roads are dynamically blocked, standard dispatch systems send ambulances into dead-ends, forcing them to backtrack and lose critical life-saving minutes. Furthermore, standard "First-Come, First-Served" dispatch logic collapses under mass-casualty events where optimizing resources is the only way to minimize fatalities.

**ResQMesh solves this logistical nightmare.** It ingests live road disruptions and instantly recalculates routes across the city grid. Instead of naive dispatching, ResQMesh uses algorithmic triage policies (like *Expected Lives Saved*) combined with A* routing to mathematically match the right rescue units to the right emergencies, completely avoiding blocked roads. 

## 🎯 Who Is It For?
*   **Emergency Dispatchers (911 / EMS / Fire):** It gives operators a high-density, real-time tactical dashboard to seamlessly transition between algorithmic "Autopilot" dispatching and manual overrides.
*   **Disaster Response Agencies (e.g., FEMA, National Guard):** To coordinate fleet logistics and rescue operations in devastated areas where standard infrastructure has broken down.
*   **City Planners & Researchers:** To simulate disasters (like a Manhattan flood) to stress-test road resilience and vehicle fleet sizes.

---

## ✨ Key Capabilities & Features

### ⚡ 1. Autopilot vs. Manual Dispatch Modes
- **`⚡ AUTOPILOT: ON`**: Runs automated dispatch cycles (~every 3s) using optimal bipartite graph matching to pair incoming SOS requests with available rescue squads.
- **`▶ AUTOPILOT: OFF (MANUAL MODE)`**: Stops automatic background dispatching, enabling dispatchers to retain full manual control.

### 🗺 2. ATC-Style Tactical UI & Visualization
- **HTML5 Canvas Network Map:** Renders a 2,600-edge Manhattan road vector graph natively, bypassing standard DOM limits for lag-free performance with thousands of moving entities.
- **Visual Tethering & Ghost Routes:** Uses permanent straight cyan dashed lines to visually link assigned units to missions, while flashing **solid blue physical A* routes** on dispatch to show the exact calculated street path.
- **Interactive UI:** Clickable markers, dynamic map legends, and a high-density glass-free dashboard design.

### 🛣️ 3. Dynamic Pathfinding & 5-Rung Rerouting Ladder
- **Bidirectional A* Algorithm**: Custom in-memory graph traversal across sparse Compressed Sparse Row (CSR) arrays.
- **5-Rung Escalation Ladder**:
  1. *Rung 1*: Reroute same unit around blocked roads via alternate path.
  2. *Rung 2*: Reassign to a closer available unit.
  3. *Rung 3*: Reshuffle unit off a lower-priority mission.
  4. *Rung 4*: Capacity split simulation.
  5. *Rung 5*: Escalate to human operator (`AT_RISK`).

---

## 🏗 Architecture & Tech Stack

- **Backend:** Java 21, Spring Boot 3, PostgreSQL
  - Features a custom `SimulationClock` to run faster/slower than real-time, and a native in-memory graph routing engine.
- **Frontend:** React 18, Vite, Leaflet, HTML5 Canvas
- **Real-Time Layer:** WebSocket (STOMP/SockJS) for sub-second telemetry streaming.
- **Infrastructure:** Docker, Docker Compose, Nginx

---

## ☁️ AWS Cloud Infrastructure

The production environment is securely deployed on **AWS (Amazon Web Services)**:
- **Amazon EC2 (Elastic Compute Cloud):** A single scalable compute instance hosts the entire multi-container stack.
- **Docker Compose Orchestration:** Spins up isolated containers for the PostgreSQL Database, Java Backend API, and React/Nginx Frontend Server.
- **AWS Security Groups:** Acts as a virtual firewall, strictly managing inbound/outbound traffic by opening Port 22 (SSH) for administration and Port 80/8080 for web and WebSocket traffic.

---

## 🛠 Quick Start (Run Locally)

The entire project is heavily containerized. You can run the entire stack (Database, Backend, Frontend) with a single command:

### Prerequisites
- **Docker** and **Docker Compose** installed.

### Start the Stack
```bash
# Clone the repository
git clone https://github.com/Snorlax231155/resqmesh.git
cd resqmesh

# Build and start all containers
docker compose up --build -d
```
Once the containers spin up, simply open `http://localhost:8080` in your browser to access the tactical dashboard!
