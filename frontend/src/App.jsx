import React, { useState, useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import Header from './components/Header';
import LiveFeed from './components/LiveFeed';
import NetworkMap from './components/NetworkMap';
import ControlPanel from './components/ControlPanel';

const API_BASE = import.meta.env.PROD ? '/api/v1' : 'http://localhost:8080/api/v1';
const WS_URL = import.meta.env.PROD ? '/ws' : 'http://localhost:8080/ws';

function App() {
  const [summary, setSummary] = useState({ totalAgents: 0, availableAgents: 0, totalMissions: 0, pendingMissions: 0, activeDisruptions: 0 });
  const [agents, setAgents] = useState([]);
  const [missions, setMissions] = useState([]);
  const [disruptions, setDisruptions] = useState([]);
  const [events, setEvents] = useState([]);
  
  const [coverage, setCoverage] = useState(null);
  const [repositioningRoutes, setRepositioningRoutes] = useState([]);
  
  const [nodes, setNodes] = useState([]);
  const [roads, setRoads] = useState([]);

  const addEvent = (msg, type) => {
    setEvents(prev => [...prev.slice(-49), { message: msg, type, timestamp: Date.now() }]);
  };

  // Debounced summary refresh: batch WS-triggered refreshes to avoid request storms.
  const refreshTimer = React.useRef(null);
  const scheduleSummaryRefresh = () => {
    if (refreshTimer.current) clearTimeout(refreshTimer.current);
    refreshTimer.current = setTimeout(() => {
      fetch(`${API_BASE}/dashboard/summary`)
        .then(r => r.json())
        .then(setSummary)
        .catch(() => {});
    }, 500);
  };

  const fetchInitialData = async () => {
    try {
      const [sumRes, agentRes, missRes, disRes, nodesRes, roadsRes] = await Promise.all([
        fetch(`${API_BASE}/dashboard/summary`),
        fetch(`${API_BASE}/agents`),
        fetch(`${API_BASE}/missions`),
        fetch(`${API_BASE}/disruptions`),
        fetch(`${API_BASE}/network/nodes`),
        fetch(`${API_BASE}/network/roads`)
      ]);
      setSummary(await sumRes.json());
      setAgents(await agentRes.json());
      setMissions(await missRes.json());
      setDisruptions(await disRes.json());
      
      const n = await nodesRes.json();
      const r = await roadsRes.json();
      setNodes(n);
      setRoads(r);
    } catch (err) {
      console.error("Failed to fetch initial data", err);
    }
  };

  useEffect(() => {
    fetchInitialData();
    addEvent("System initialized. OSM road network active.", "SYSTEM");

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      onConnect: () => {
        addEvent("WebSocket Connected.", "SYSTEM");
        
        client.subscribe('/topic/agents', (msg) => {
          const agent = JSON.parse(msg.body);
          setAgents(prev => {
            const idx = prev.findIndex(a => a.id === agent.id);
            if (idx >= 0) {
              const next = [...prev];
              next[idx] = agent;
              return next;
            }
            return [...prev, agent];
          });
          addEvent(`Agent ${agent.agentCode} status → ${agent.status}`, "AGENT");
          scheduleSummaryRefresh();
        });

        client.subscribe('/topic/missions', (msg) => {
          const mission = JSON.parse(msg.body);
          setMissions(prev => {
            const idx = prev.findIndex(m => m.id === mission.id);
            if (idx >= 0) {
              const next = [...prev];
              next[idx] = mission;
              return next;
            }
            return [...prev, mission];
          });
          addEvent(`Mission ${mission.missionCode} status → ${mission.status}`, "MISSION");
          scheduleSummaryRefresh();
        });

        client.subscribe('/topic/disruptions', (msg) => {
          const disruption = JSON.parse(msg.body);
          setDisruptions(prev => [...prev, disruption]);
          addEvent(`DISRUPTION: ${disruption.description} on ${disruption.affectedRoadId}`, "DISRUPTION");
          scheduleSummaryRefresh();
        });

        client.subscribe('/topic/coverage', (msg) => {
          setCoverage(JSON.parse(msg.body));
        });

        client.subscribe('/topic/repositioning', (msg) => {
          const event = JSON.parse(msg.body);
          setRepositioningRoutes(prev => [...prev, event]);
          addEvent(`Agent ${event.agentCode} repositioning → ${event.targetNodeId}`, "SYSTEM");
        });

        client.subscribe('/topic/decisions', (msg) => {
          const event = JSON.parse(msg.body);
          addEvent(`⚠️ DECISION [${event.missionCode}]: ${event.trace}`, "DISRUPTION");
        });
      }
    });

    client.activate();
    return () => client.deactivate();
  }, []);

  const handleSpawnAgent = async () => {
    if (nodes.length === 0) return;
    const randomNode = nodes[Math.floor(Math.random() * nodes.length)];
    const id = Math.floor(Math.random() * 1000);
    
    await fetch(`${API_BASE}/agents`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ agentCode: `UNIT-${id}`, name: `Responder ${id}`, currentNodeId: randomNode.id, capacity: 5 })
    });
  };

  const handleSpawnMission = async () => {
    if (nodes.length < 2) return;
    const pickupNode = nodes[Math.floor(Math.random() * nodes.length)];
    let destNode = nodes[Math.floor(Math.random() * nodes.length)];
    while(destNode.id === pickupNode.id) destNode = nodes[Math.floor(Math.random() * nodes.length)];
    const id = Math.floor(Math.random() * 1000);
    
    await fetch(`${API_BASE}/missions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ missionCode: `MED-${id}`, pickupNodeId: pickupNode.id, destinationNodeId: destNode.id, priority: 'HIGH', deadline: '2030-12-31T23:59:59Z' })
    });
  };

  const handleDispatch = async () => {
    await fetch(`${API_BASE}/dispatch/run`, { method: 'POST' });
    addEvent("Manual dispatch cycle triggered.", "SYSTEM");
  };

  const handleDisruption = async () => {
    if (roads.length === 0) return;
    // Pick a random edge from the loaded roads
    const randomEdge = roads[Math.floor(Math.random() * roads.length)];
    const edgeId = randomEdge.id;
    
    await fetch(`${API_BASE}/disruptions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: 'ROAD_CLOSURE', affectedRoadId: edgeId, description: `Unexpected blockage on ${edgeId}` })
    });
  };

  return (
    <div style={{ padding: '2rem', maxWidth: '1400px', margin: '0 auto', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Header summary={summary} />
      
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem', flex: 1, minHeight: 0 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          <div style={{ flex: 1, minHeight: 0 }}>
            <NetworkMap nodes={nodes} roads={roads} agents={agents} missions={missions} disruptions={disruptions} coverage={coverage} repositioningRoutes={repositioningRoutes} />
          </div>
          <ControlPanel 
            onAgentSpawn={handleSpawnAgent} 
            onMissionSpawn={handleSpawnMission} 
            onDisruptionSimulate={handleDisruption}
            onDispatch={handleDispatch}
          />
        </div>
        
        <LiveFeed events={events} />
      </div>
    </div>
  );
}

export default App;
