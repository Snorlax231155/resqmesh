import React, { useState, useEffect, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import NetworkMap from './components/NetworkMap';
import EventTape from './components/EventTape';
import DecisionLogPanel from './components/DecisionLogPanel';
import PolicyComparisonView from './components/PolicyComparisonView';
import CommandPalette from './components/CommandPalette';

const API_BASE = import.meta.env.PROD ? '/api/v1' : 'http://localhost:8080/api/v1';
const WS_URL = import.meta.env.PROD ? '/ws' : 'http://localhost:8080/ws';

function App() {
  const [summary, setSummary] = useState({ totalAgents: 0, availableAgents: 0, totalMissions: 0, pendingMissions: 0, activeDisruptions: 0 });
  const [agents, setAgents] = useState([]);
  const [missions, setMissions] = useState([]);
  const [disruptions, setDisruptions] = useState([]);
  const [events, setEvents] = useState([]);
  const [decisions, setDecisions] = useState([]);
  const [coverage, setCoverage] = useState(null);
  const [repositioningRoutes, setRepositioningRoutes] = useState([]);
  const [nodes, setNodes] = useState([]);
  const [roads, setRoads] = useState([]);
  const [cmdOpen, setCmdOpen] = useState(false);
  const [activePolicy, setActivePolicy] = useState("ExpectedLivesSaved");
  const [simState, setSimState] = useState({ running: false, speed: '1x' });

  const addEvent = useCallback((msg, type) => {
    setEvents(prev => [...prev.slice(-100), { id: Math.random(), message: msg, type, timestamp: Date.now() }]);
  }, []);

  const addDecision = useCallback((msg) => {
    setDecisions(prev => [...prev.slice(-49), { id: Math.random(), message: msg, timestamp: Date.now() }]);
  }, []);

  const refreshTimer = React.useRef(null);
  const scheduleSummaryRefresh = useCallback(() => {
    if (refreshTimer.current) clearTimeout(refreshTimer.current);
    refreshTimer.current = setTimeout(() => {
      fetch(`${API_BASE}/dashboard/summary`).then(r => r.json()).then(setSummary).catch(() => {});
    }, 500);
  }, []);

  const fetchInitialData = async () => {
    try {
      const [sumRes, agentRes, missRes, disRes, nodesRes, roadsRes, polRes] = await Promise.all([
        fetch(`${API_BASE}/dashboard/summary`),
        fetch(`${API_BASE}/agents`),
        fetch(`${API_BASE}/missions`),
        fetch(`${API_BASE}/disruptions`),
        fetch(`${API_BASE}/network/nodes`),
        fetch(`${API_BASE}/network/roads`),
        fetch(`${API_BASE}/policies/active`).catch(() => ({ json: () => ({ active: "ExpectedLivesSaved" }) }))
      ]);
      setSummary(await sumRes.json());
      setAgents(await agentRes.json());
      setMissions(await missRes.json());
      setDisruptions(await disRes.json());
      setNodes(await nodesRes.json());
      setRoads(await roadsRes.json());
      
      const p = await polRes.json();
      if (p.active) setActivePolicy(p.active);
    } catch (err) {
      console.error("Failed to fetch initial data", err);
    }
  };

  useEffect(() => {
    fetchInitialData();
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      onConnect: () => {
        client.subscribe('/topic/agents', (msg) => {
          const agent = JSON.parse(msg.body);
          setAgents(prev => {
            const idx = prev.findIndex(a => a.id === agent.id);
            if (idx >= 0) { const next = [...prev]; next[idx] = agent; return next; }
            return [...prev, agent];
          });
          addEvent(`UNIT ${agent.agentCode} -> ${agent.status}`, "AGENT");
          scheduleSummaryRefresh();
        });
        client.subscribe('/topic/missions', (msg) => {
          const mission = JSON.parse(msg.body);
          setMissions(prev => {
            const idx = prev.findIndex(m => m.id === mission.id);
            if (idx >= 0) { const next = [...prev]; next[idx] = mission; return next; }
            return [...prev, mission];
          });
          addEvent(`MSN ${mission.missionCode} -> ${mission.status}`, "MISSION");
          scheduleSummaryRefresh();
        });
        client.subscribe('/topic/disruptions', (msg) => {
          const disruption = JSON.parse(msg.body);
          setDisruptions(prev => {
             const idx = prev.findIndex(d => d.id === disruption.id);
             if (idx >= 0) { const next = [...prev]; next[idx] = disruption; return next; }
             return [...prev, disruption];
          });
          addEvent(`WARN: ${disruption.description}`, "DISRUPTION");
          scheduleSummaryRefresh();
        });
        client.subscribe('/topic/coverage', (msg) => setCoverage(JSON.parse(msg.body)));
        client.subscribe('/topic/repositioning', (msg) => {
          const event = JSON.parse(msg.body);
          const routeId = Math.random();
          setRepositioningRoutes(prev => [...prev, { ...event, _id: routeId }]);
          
          setTimeout(() => {
             setRepositioningRoutes(prev => prev.filter(r => r._id !== routeId));
          }, 2500);
        });
        client.subscribe('/topic/decisions', (msg) => {
          const event = JSON.parse(msg.body);
          addDecision(`[${event.missionCode}] ${event.trace}`);
          addEvent(`DECISION: ${event.missionCode}`, "DECISION");
        });
      }
    });
    client.activate();
    
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setCmdOpen(o => !o);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => { client.deactivate(); window.removeEventListener('keydown', handleKeyDown); };
  }, [addEvent, addDecision, scheduleSummaryRefresh]);

  const toggleSimPlay = async () => {
    try {
      const endpoint = simState.running ? `${API_BASE}/sim/pause` : `${API_BASE}/sim/play`;
      await fetch(endpoint, { method: 'POST' });
      setSimState(prev => ({ ...prev, running: !prev.running }));
      addEvent(`SIMULATION -> ${!simState.running ? 'RUNNING' : 'PAUSED'}`, "SYSTEM");
    } catch (e) {
      console.error(e);
    }
  };

  const runDispatchCycle = async () => {
    try {
      const res = await fetch(`${API_BASE}/dispatch/run`, { method: 'POST' });
      const assignments = await res.json();
      addEvent(`MANUAL DISPATCH CYCLE -> ${assignments.length} assignments`, "SYSTEM");
      fetchInitialData();
    } catch (e) {
      console.error(e);
    }
  };

  const loadUrbanFlood = async () => {
    try {
      await fetch(`${API_BASE}/sim/load?scenario=urban_flood`, { method: 'POST' });
      addEvent(`SCENARIO LOADED: URBAN FLOOD`, "SYSTEM");
      addDecision(`[SCENARIO] Loaded Urban Flood disaster scenario. Seeding initial rescue teams & emergency requests.`);
      await fetchInitialData();
      await runDispatchCycle();
    } catch (e) {
      console.error(e);
    }
  };

  const spawnRandomAgent = async () => {
    if (nodes.length === 0) return;
    const randNode = nodes[Math.floor(Math.random() * nodes.length)];
    const code = `UNIT-${Math.floor(100 + Math.random() * 900)}`;
    try {
      const res = await fetch(`${API_BASE}/agents`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          agentCode: code,
          name: `Rescue Squad ${code}`,
          capacity: 1,
          currentNodeId: randNode.id
        })
      });
      if (!res.ok) {
        const errText = await res.text();
        addEvent(`SPAWN UNIT ERR: ${errText}`, "ERROR");
        return;
      }
      const agent = await res.json();
      addEvent(`+ REGISTERED RESCUE UNIT ${agent.agentCode}`, "AGENT");
      addDecision(`[AGENT_REGISTERED] Rescue Unit ${agent.agentCode} deployed to Graph Node ${randNode.id.substring(0, 8)}.`);
      await fetchInitialData();
      await runDispatchCycle();
    } catch (e) {
      console.error(e);
    }
  };

  const spawnRandomMission = async () => {
    if (nodes.length < 2) return;
    const srcNode = nodes[Math.floor(Math.random() * nodes.length)];
    let dstNode = nodes[Math.floor(Math.random() * nodes.length)];
    while (dstNode.id === srcNode.id) {
      dstNode = nodes[Math.floor(Math.random() * nodes.length)];
    }
    const priorities = ['CRITICAL', 'HIGH', 'NORMAL'];
    const p = priorities[Math.floor(Math.random() * priorities.length)];
    const code = `MED-${Math.floor(100 + Math.random() * 900)}`;
    const deadlineIso = new Date(Date.now() + 35 * 60 * 1000).toISOString();

    try {
      const res = await fetch(`${API_BASE}/missions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          missionCode: code,
          priority: p,
          pickupNodeId: srcNode.id,
          destinationNodeId: dstNode.id,
          deadline: deadlineIso
        })
      });
      if (!res.ok) {
        const errText = await res.text();
        addEvent(`SPAWN MISSION ERR: ${errText}`, "ERROR");
        return;
      }
      const mission = await res.json();
      addEvent(`+ EMERGENCY SOS CREATED: ${mission.missionCode} (${p})`, "MISSION");
      addDecision(`[EMERGENCY_SOS] Priority ${p} mission ${mission.missionCode} logged at Pickup Node ${srcNode.id.substring(0, 8)} -> Dest Node ${dstNode.id.substring(0, 8)}.`);
      await fetchInitialData();
      await runDispatchCycle();
    } catch (e) {
      console.error(e);
    }
  };

  const spawnRandomDisruption = async () => {
    if (roads.length === 0) return;
    const randRoad = roads[Math.floor(Math.random() * roads.length)];
    try {
      const res = await fetch(`${API_BASE}/disruptions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          type: 'FLOOD',
          affectedRoadId: randRoad.id,
          description: `Flash Flood on Road Segment ${randRoad.id.substring(0, 8)}`
        })
      });
      if (!res.ok) {
        const errText = await res.text();
        addEvent(`DISRUPTION ERR: ${errText}`, "ERROR");
        return;
      }
      addEvent(`🚨 ROAD BLOCKAGE REPORTED`, "DISRUPTION");
      addDecision(`[DISRUPTION] Road segment ${randRoad.id.substring(0, 8)} blocked by FLASH_FLOOD. Evaluating active mission reroutes.`);
      await fetchInitialData();
      await runDispatchCycle();
    } catch (e) {
      console.error(e);
    }
  };

  const handlePolicyChange = async (pol) => {
    try {
      await fetch(`${API_BASE}/policies/active`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: pol })
      });
      setActivePolicy(pol);
      addEvent(`ACTIVE POLICY -> ${pol}`, "SYSTEM");
    } catch (e) {
      console.error(e);
    }
  };

  const execCommand = async (cmd) => {
    try {
      const parts = cmd.trim().split(" ");
      const action = parts[0].toLowerCase();
      if (action === "theme" && parts[1]) {
        document.documentElement.setAttribute('data-theme', parts[1]);
        addEvent(`THEME -> ${parts[1].toUpperCase()}`, "SYSTEM");
      } else if (action === "policy" && parts[1]) {
         let pol = parts[1];
         if (pol === 'eld') pol = 'EarliestDeadlineFirst';
         else if (pol === 'els') pol = 'ExpectedLivesSaved';
         else if (pol === 'fcfs') pol = 'FirstComeFirstServed';
         else if (pol === 'sw') pol = 'SeverityWeighted';
         await handlePolicyChange(pol);
      } else if (action === "speed" && parts[1]) {
         const s = parts[1].replace('x','');
         await fetch(`${API_BASE}/sim/speed?x=${s}`, { method: 'POST' });
         setSimState(prev => ({ ...prev, speed: `${s}x` }));
         addEvent(`SIM SPEED -> ${s}x`, "SYSTEM");
      } else if (action === "load" && parts[1]) {
         await fetch(`${API_BASE}/sim/load?scenario=${parts[1]}`, { method: 'POST' });
         fetchInitialData();
         addEvent(`LOAD SCENARIO -> ${parts[1]}`, "SYSTEM");
      } else if (action === "play") {
         await fetch(`${API_BASE}/sim/play`, { method: 'POST' });
         setSimState(prev => ({ ...prev, running: true }));
         addEvent(`SIM -> PLAY`, "SYSTEM");
      } else if (action === "pause") {
         await fetch(`${API_BASE}/sim/pause`, { method: 'POST' });
         setSimState(prev => ({ ...prev, running: false }));
         addEvent(`SIM -> PAUSE`, "SYSTEM");
      }
    } catch(e) {
      console.error(e);
      addEvent(`CMD ERR: ${cmd}`, "ERROR");
    }
  };

  return (
    <div style={{ display: 'grid', gridTemplateRows: 'auto auto 1fr auto', height: '100%', width: '100%' }}>
      {/* Top Header */}
      <header className="panel" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--rule)' }}>
        <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
          <div className="title" style={{ fontSize: '1.2rem', letterSpacing: '0.1em' }}>RESQMESH TACTICAL</div>
          <div className="mono" style={{ fontSize: '0.85rem', color: 'var(--ink-muted)' }}>
            UNITS: <strong style={{ color: 'var(--ink)' }}>{summary.availableAgents}/{summary.totalAgents}</strong> | 
            MISSIONS: <strong style={{ color: 'var(--ink)' }}>{summary.pendingMissions}/{summary.totalMissions}</strong> | 
            DISRUPTIONS: <strong style={{ color: 'var(--alert)' }}>{summary.activeDisruptions}</strong>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '15px', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span className="mono" style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>POLICY:</span>
            <select
              value={activePolicy}
              onChange={e => handlePolicyChange(e.target.value)}
              className="mono"
              style={{ background: 'var(--bg)', color: 'var(--signal)', border: '1px solid var(--rule)', fontWeight: 'bold', padding: '2px 6px', fontSize: '11px' }}
            >
              <option value="ExpectedLivesSaved">ExpectedLivesSaved (Hungarian)</option>
              <option value="FirstComeFirstServed">FirstComeFirstServed (FCFS)</option>
              <option value="EarliestDeadlineFirst">EarliestDeadlineFirst (EDF)</option>
              <option value="SeverityWeighted">SeverityWeighted (Priority)</option>
            </select>
          </div>

          <button onClick={() => setCmdOpen(true)} className="mono" style={{ fontSize: '11px', padding: '2px 8px' }}>
            CMD (⌘K)
          </button>
        </div>
      </header>

      {/* Interactive Simulation Toolbar */}
      <div style={{ background: 'var(--panel)', borderBottom: '1px solid var(--rule)', padding: '6px 12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <button 
            onClick={toggleSimPlay}
            style={{ 
              background: simState.running ? 'var(--alert)' : 'var(--hi-vis)', 
              color: simState.running ? '#fff' : '#000',
              fontWeight: 'bold',
              minWidth: '80px'
            }}
          >
            {simState.running ? '⏸ PAUSE' : '▶ PLAY'}
          </button>

          <button onClick={runDispatchCycle} style={{ background: 'var(--signal)', color: '#fff' }}>
            ⚡ RUN DISPATCH CYCLE
          </button>

          <button onClick={loadUrbanFlood} style={{ background: 'var(--bg)', color: 'var(--ink)' }}>
            🌊 LOAD URBAN FLOOD DEMO
          </button>
        </div>

        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <span className="mono" style={{ fontSize: '11px', color: 'var(--ink-muted)', fontWeight: 'bold' }}>SIMULATE ACTIONS:</span>
          <button onClick={spawnRandomAgent} style={{ fontSize: '11px' }}>+ UNIT</button>
          <button onClick={spawnRandomMission} style={{ fontSize: '11px' }}>+ MISSION</button>
          <button onClick={spawnRandomDisruption} style={{ fontSize: '11px', color: 'var(--alert)' }}>+ DISRUPTION</button>
        </div>
      </div>

      {/* Map & Right Side Panel */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', minHeight: 0 }}>
        <div style={{ position: 'relative', background: 'var(--bg)' }}>
          <NetworkMap nodes={nodes} roads={roads} agents={agents} missions={missions} disruptions={disruptions} coverage={coverage} repositioningRoutes={repositioningRoutes} />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', borderLeft: '1px solid var(--rule)' }}>
          <DecisionLogPanel decisions={decisions} />
          <PolicyComparisonView currentScenario="urban_flood" apiBase={API_BASE} />
        </div>
      </div>

      {/* Telemetry Event Tape */}
      <EventTape events={events} />
      {cmdOpen && <CommandPalette onClose={() => setCmdOpen(false)} onExecute={execCommand} />}
    </div>
  );
}

export default App;

