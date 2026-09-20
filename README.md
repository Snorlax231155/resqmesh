# 🚑 ResQMesh

<div align="center">
  <img src="frontend/public/icons.svg" alt="ResQMesh Logo" width="100"/>
  <h3>Intelligent Emergency Response & Routing Platform</h3>
  <p>A real-time, highly scalable routing and dispatch engine built for modern emergency services.</p>
  
  <img src="frontend/public/demo.jpg" alt="ResQMesh Dashboard" width="800" style="border-radius: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.5); margin-top: 20px;"/>
</div>

---

## Overview

**ResQMesh** addresses the maximal coverage and dynamic assignment problem for emergency medical services in constrained urban environments. When road networks are disrupted by flood, fire, or debris, static routing heuristics fail and response times decay exponentially. 

This platform evaluates real-time OpenStreetMap topology (1,300+ nodes, 2,600+ edges for Lower Manhattan) against active responder units. The core dispatch engine continuously recalculates SSSP (Single-Source Shortest Path) across the live graph to optimise assignments and dynamically re-route responders around real-time edge closures.

## Architecture & Metrics

### Backend Engine
- **Java 21 & Spring Boot 3.3.3:** Core API and WebSocket broker.
- **In-Memory Traversal:** Graph traversal (Dijkstra/A*) operates strictly in-memory on sparse CSR arrays. 
  - **Dijkstra:** ~9,160 QPS (p95: 400µs, p50: 82µs)
  - **Bidirectional A*:** ~6,015 QPS (p95: 424µs, p50: 137µs)
- **PostgreSQL & Flyway:** Persistence for audit logging, initial topology seeding, and mission history.

### Real-time Telemetry
- **STOMP over WebSocket:** Sub-50ms propagation of edge closures and agent reroutes to all connected clients.

### Frontend Display
- **React + Leaflet:** Vector-based rendering of the city topology.
- **Theme:** High-contrast light/dark modes optimized for visual acuity, modeled on marine ECDIS and CAD terminal standards.

### Data Pipeline
- **Python (OSMnx):** Geographic ingestion pipeline that extracts drivable street networks and projects them into JSON graph definitions.

## 🛠️ Quick Start (Local Development)

### Prerequisites
- Java 21
- Node.js 18+
- Maven

### 1. Start the Frontend (Development)
```bash
cd frontend
npm install
npm run dev
```

### 2. Start the Backend
```bash
# From the project root
./mvnw spring-boot:run
```

The application will be accessible at `http://localhost:5173`.

## 📦 Production Deployment (AWS / Docker)

ResQMesh is designed to bundle into a single, highly portable `.jar` file for 1-click cloud deployments.

1. **Build the Production Frontend UI:**
   ```bash
   cd frontend
   npm run build
   ```
2. **Move Frontend to Backend Static Hosting:**
   ```bash
   cd ..
   mkdir -p src/main/resources/static
   cp -r frontend/dist/* src/main/resources/static/
   ```
3. **Package the Unified Application:**
   ```bash
   ./mvnw clean package -DskipTests
   ```
4. **Run on Production Server (AWS EC2):**
   ```bash
   # Make sure PostgreSQL is configured or adjust application.yml for H2
   java -jar target/resqmesh-0.0.1-SNAPSHOT.jar
   ```

## 🗺️ Updating the City Map

To generate a new city graph from OpenStreetMap:
1. Setup a python virtual environment and install `osmnx`.
2. Edit the bounding box coordinates in `fetch_city.py`.
3. Run the script: `python3 fetch_city.py`.
4. This outputs `manhattan_graph.json` directly into `src/main/resources/`.
5. Restart the Spring Boot server to seed the new graph into the database!

## 📜 License

MIT License - feel free to use this for your own hackathons and emergency services projects.
