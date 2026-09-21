import React, { useState, useEffect, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import NetworkMap from './components/NetworkMap';
import EventTape from './components/EventTape';
import DecisionLogPanel from './components/DecisionLogPanel';
import PolicyComparisonView from './components/PolicyComparisonView';
import CommandPalette from './components/CommandPalette';
import CivilianComplaintModal from './components/CivilianComplaintModal';
import FleetDrawer from './components/FleetDrawer';

const API_BASE = import.meta.env.PROD ? '/api/v1' : 'http://localhost:8080/api/v1';
const WS_URL = import.meta.env.PROD ? '/ws' : 'http://localhost:8080/ws';

function App() {
  const [demoBanner, setDemoBanner] = useState(null);
  const [showGuide, setShowGuide] = useState(false);
  const [isComplaintModalOpen, setIsComplaintModalOpen] = useState(false);
  const [isFleetDrawerOpen, setIsFleetDrawerOpen] = useState(false);

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
      try {
        const polData = await polRes.json();
        if (polData && polData.active) setActivePolicy(polData.active);
      } catch (e) {}
    } catch (e) {
      console.error("Failed to load initial data", e);
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
      const nextRunning = !simState.running;
      setSimState(prev => ({ ...prev, running: nextRunning }));
      addEvent(`AUTOPILOT MODE -> ${nextRunning ? 'ENABLED (AUTO DISPATCH)' : 'DISABLED (MANUAL MODE)'}`, "SYSTEM");
      addDecision(`[AUTOPILOT_STATUS] Autopilot switched to ${nextRunning ? 'AUTOMATIC' : 'MANUAL'} mode.`);
    } catch (e) {
      console.error(e);
    }
  };

  const runDispatchCycle = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/dispatch/run`, { method: 'POST' });
      const assignments = await res.json();
      if (assignments && assignments.length > 0) {
        addEvent(`DISPATCH CYCLE -> ${assignments.length} assignments made`, "SYSTEM");
      }
      fetchInitialData();
    } catch (e) {
      console.error(e);
    }
  }, []);

  // Automatic Dispatch Interval when Autopilot is ON
  useEffect(() => {
    let interval = null;
    if (simState.running) {
      // Run once immediately when Autopilot turns ON
      runDispatchCycle();
      interval = setInterval(() => {
        runDispatchCycle();
      }, 3000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [simState.running, runDispatchCycle]);

  const handleManualAssign = async (missionId, agentId) => {
    try {
      const res = await fetch(`${API_BASE}/dispatch/assign?missionId=${missionId}&agentId=${agentId}`, { method: 'POST' });
      if (res.ok) {
        addEvent(`👉 MANUALLY ASSIGNED UNIT TO MISSION`, "SYSTEM");
        await fetchInitialData();
      } else {
        const errText = await res.text();
        addEvent(`MANUAL ASSIGN ERR: ${errText}`, "ERROR");
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleCompleteMission = async (missionId) => {
    try {
      const res = await fetch(`${API_BASE}/missions/${missionId}/complete`, { method: 'POST' });
      if (res.ok) {
        const m = await res.json();
        addEvent(`✅ MISSION COMPLETED & UNIT FREED: ${m.missionCode}`, "SYSTEM");
        addDecision(`[MISSION_COMPLETED] Mission ${m.missionCode} marked COMPLETED. Assigned rescue unit freed & set to AVAILABLE status.`);
        await fetchInitialData();
        if (simState.running) await runDispatchCycle();
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleReleaseAgent = async (agentId) => {
    try {
      const res = await fetch(`${API_BASE}/agents/${agentId}/release`, { method: 'POST' });
      if (res.ok) {
        const a = await res.json();
        addEvent(`🔓 RESCUE UNIT FORCIBLY RELEASED: ${a.agentCode}`, "AGENT");
        addDecision(`[UNIT_RELEASED] Rescue unit ${a.agentCode} forcibly released to AVAILABLE status.`);
        await fetchInitialData();
        if (simState.running) await runDispatchCycle();
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleSubmitComplaint = async (complaintData) => {
    const code = `CIV-${Math.floor(100 + Math.random() * 900)}`;
    const deadlineIso = new Date(Date.now() + 40 * 60 * 1000).toISOString();

    try {
      const res = await fetch(`${API_BASE}/missions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          missionCode: code,
          priority: complaintData.priority,
          pickupNodeId: complaintData.pickupNodeId,
          destinationNodeId: complaintData.destinationNodeId,
          deadline: deadlineIso
        })
      });
      if (!res.ok) {
        const errText = await res.text();
        addEvent(`CIVILIAN SOS ERR: ${errText}`, "ERROR");
        return;
      }
      const mission = await res.json();
      addEvent(`📢 CIVILIAN COMPLAINT LOGGED: ${code} by ${complaintData.reporterName}`, "MISSION");
      addDecision(`[CIVILIAN_COMPLAINT] Emergency complaint from ${complaintData.reporterName} (${complaintData.phone}): "${complaintData.description}". Priority ${complaintData.priority} mission ${code} created at Graph Node ${complaintData.pickupNodeId.substring(0, 8)}.`);
      await fetchInitialData();
      if (simState.running) await runDispatchCycle();
    } catch (e) {
      console.error(e);
    }
  };

  const handleCompleteRandomMission = async () => {
    const activeMissions = missions.filter(m => m.status !== 'COMPLETED');
    if (activeMissions.length === 0) return;
    const randomMission = activeMissions[Math.floor(Math.random() * activeMissions.length)];
    await handleCompleteMission(randomMission.id);
  };

  const runAutomatedHackathonDemo = async () => {
    try {
      // Step 1: Load Scenario
      setDemoBanner({ step: '1/5', title: 'DISASTER INITIALIZATION', desc: 'Loading Lower Manhattan OpenStreetMap graph & deploying rescue squads...' });
      await fetch(`${API_BASE}/sim/load?scenario=urban_flood`, { method: 'POST' });
      addEvent(`[DEMO] SCENARIO LOADED: URBAN FLOOD`, "SYSTEM");
      addDecision(`[HACKATHON_DEMO] Step 1: Loaded Urban Flood scenario across 1,315 road nodes.`);
      await fetchInitialData();

      // Step 2: Report Disruption
      setTimeout(async () => {
        setDemoBanner({ step: '2/5', title: 'DISASTER EVENT TRIGGERED', desc: 'Flash flood washouts reported on FDR Drive & Manhattan Bridge! Recalculating graph weights via Bidirectional A*...' });
        await spawnRandomDisruption();
      }, 3000);

      // Step 3: Spawn Inbound SOS
      setTimeout(async () => {
        setDemoBanner({ step: '3/5', title: 'CRITICAL INBOUND SOS CALLS', desc: 'Multiple priority SOS medical emergency requests registered across flood grid...' });
        await spawnRandomMission();
      }, 6000);

      // Step 4: Execute Optimal Dispatch
      setTimeout(async () => {
        setDemoBanner({ step: '4/5', title: 'HUNGARIAN OPTIMAL DISPATCH CYCLE', desc: 'Solving Bipartite Graph Matching to minimize arrival times and maximize lives saved...' });
        await runDispatchCycle();
      }, 9000);

      // Step 5: Demo Resolution
      setTimeout(() => {
        setDemoBanner({ step: '5/5', title: 'DISPATCH OPTIMIZATION COMPLETE', desc: 'Rescue units deployed along real-time re-routed flood paths. Zero critical missions missed!' });
        setTimeout(() => setDemoBanner(null), 5000);
      }, 12000);
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
      if (simState.running) await runDispatchCycle();
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
      if (simState.running) await runDispatchCycle();
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

  const activeMissionsCount = missions.filter(m => m.status !== 'COMPLETED').length;

  return (
    <div style={{ display: 'grid', gridTemplateRows: 'auto 1fr auto', height: '100vh', width: '100vw', overflow: 'hidden' }}>
      {/* Top Header & Integrated Toolbar */}
      <header className="panel" style={{ borderBottom: '1px solid var(--rule)', padding: '8px 16px', display: 'flex', flexDirection: 'column', gap: '8px', background: 'var(--panel)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
            <div className="title" style={{ fontSize: '1.25rem', letterSpacing: '0.12em', color: '#000', fontWeight: 'bold' }}>
              RESQMESH TACTICAL DISPATCH
            </div>
            <div className="mono" style={{ fontSize: '0.85rem', color: 'var(--ink-muted)' }}>
              UNITS: <strong style={{ color: '#2E7D32' }}>{summary.availableAgents}</strong>/{summary.totalAgents} | 
              MISSIONS: <strong style={{ color: '#E4002B' }}>{summary.pendingMissions}</strong>/{summary.totalMissions} | 
              DISRUPTIONS: <strong style={{ color: '#FF8A00' }}>{summary.activeDisruptions}</strong>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <button 
              onClick={() => setIsComplaintModalOpen(true)}
              style={{
                background: '#E4002B',
                color: '#FFF',
                fontSize: '11px',
                padding: '5px 12px',
                fontWeight: 'bold',
                border: 'none',
                boxShadow: '0 2px 8px rgba(228,0,43,0.5)',
                cursor: 'pointer',
                borderRadius: '3px'
              }}
            >
              📢 LOG COMPLAINT / SOS
            </button>

            <button 
              onClick={() => setIsFleetDrawerOpen(o => !o)}
              style={{
                background: '#0B4FA8',
                color: '#FFF',
                fontSize: '11px',
                padding: '5px 12px',
                fontWeight: 'bold',
                border: 'none',
                boxShadow: '0 2px 8px rgba(11,79,168,0.5)',
                cursor: 'pointer',
                borderRadius: '3px'
              }}
            >
              ⚡ FLEET CONTROL ({activeMissionsCount})
            </button>

            <button 
              onClick={runAutomatedHackathonDemo}
              style={{
                background: 'linear-gradient(135deg, #0B4FA8 0%, #E4002B 100%)',
                color: '#FFF',
                fontSize: '11px',
                padding: '5px 12px',
                fontWeight: 'bold',
                border: 'none',
                boxShadow: '0 2px 8px rgba(11, 79, 168, 0.4)',
                cursor: 'pointer',
                borderRadius: '3px'
              }}
            >
              🚀 1-CLICK HACKATHON DEMO
            </button>

            <button 
              onClick={() => setShowGuide(g => !g)} 
              className="mono" 
              style={{ fontSize: '11px', padding: '4px 8px', background: showGuide ? 'var(--ink)' : 'var(--panel)', color: showGuide ? 'var(--bg)' : 'var(--ink)' }}
            >
              {showGuide ? '✖ HIDE GUIDE' : '❓ GUIDE'}
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span className="mono" style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>POLICY:</span>
              <select
                value={activePolicy}
                onChange={e => handlePolicyChange(e.target.value)}
                className="mono"
                style={{ background: 'var(--bg)', color: 'var(--signal)', border: '1px solid var(--rule)', fontWeight: 'bold', padding: '3px 8px', fontSize: '11px' }}
              >
                <option value="ExpectedLivesSaved">ExpectedLivesSaved (Hungarian)</option>
                <option value="FirstComeFirstServed">FirstComeFirstServed (FCFS)</option>
                <option value="EarliestDeadlineFirst">EarliestDeadlineFirst (EDF)</option>
                <option value="SeverityWeighted">SeverityWeighted (Priority)</option>
              </select>
            </div>
          </div>
        </div>

        {/* Action Controls Toolbar */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '4px', borderTop: '1px solid var(--rule)' }}>
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <button 
              onClick={toggleSimPlay}
              style={{ 
                background: simState.running ? '#0A5C0D' : 'var(--hi-vis)', 
                color: simState.running ? '#fff' : '#000',
                fontWeight: 'bold',
                minWidth: '180px',
                padding: '4px 12px',
                border: '1px solid #000',
                borderRadius: '3px',
                cursor: 'pointer',
                boxShadow: simState.running ? '0 0 10px rgba(10,92,13,0.6)' : 'none'
              }}
            >
              {simState.running ? '⚡ AUTOPILOT: ON (AUTO DISPATCH)' : '▶ AUTOPILOT: OFF (MANUAL)'}
            </button>

            <button onClick={runDispatchCycle} style={{ background: '#0B4FA8', color: '#fff', padding: '4px 12px' }}>
              ⚡ RUN DISPATCH CYCLE
            </button>
          </div>

          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <span className="mono" style={{ fontSize: '11px', color: 'var(--ink-muted)', fontWeight: 'bold' }}>TACTICAL ACTIONS:</span>
            <button onClick={spawnRandomAgent} style={{ fontSize: '11px', padding: '3px 8px' }}>+ UNIT</button>
            <button onClick={spawnRandomMission} style={{ fontSize: '11px', padding: '3px 8px', background: '#E4002B', color: '#FFF' }}>+ MISSION (SOS)</button>
            <button onClick={spawnRandomDisruption} style={{ fontSize: '11px', padding: '3px 8px', color: '#FF8A00' }}>+ FLOOD ROAD</button>
            <button 
              onClick={handleCompleteRandomMission} 
              disabled={activeMissionsCount === 0}
              style={{ 
                fontSize: '11px', 
                padding: '3px 8px', 
                background: activeMissionsCount > 0 ? '#0A5C0D' : '#333', 
                color: activeMissionsCount > 0 ? '#FFF' : '#666',
                fontWeight: 'bold',
                cursor: activeMissionsCount > 0 ? 'pointer' : 'not-allowed'
              }}
            >
              ✅ COMPLETE TASK & FREE UNIT
            </button>
          </div>
        </div>
      </header>

      {/* Floating Overlay Modal: Hackathon Judge's Explainer Guide */}
      {showGuide && (
        <div style={{
          position: 'fixed',
          top: '80px',
          left: '50%',
          transform: 'translateX(-50%)',
          width: '850px',
          maxWidth: '90vw',
          zIndex: 9999,
          background: '#14140F',
          color: '#FFF',
          padding: '16px 20px',
          border: '2px solid #0B4FA8',
          boxShadow: '0 8px 32px rgba(0,0,0,0.6)',
          borderRadius: '4px'
        }} className="mono">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', borderBottom: '1px solid #333', paddingBottom: '8px' }}>
            <span style={{ color: '#D4E82B', fontWeight: 'bold', fontSize: '14px' }}>📖 RESQMESH HACKATHON PRESENTATION GUIDE</span>
            <button onClick={() => setShowGuide(false)} style={{ background: 'transparent', color: '#FFF', border: 'none', fontSize: '14px', cursor: 'pointer' }}>✖ CLOSE</button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px', fontSize: '11px', lineHeight: '1.4' }}>
            <div>
              <div style={{ color: '#D4E82B', fontWeight: 'bold', marginBottom: '4px' }}>🎯 WHAT IS RESQMESH?</div>
              <div style={{ color: '#CCC' }}>
                An AI-driven emergency dispatch & routing system for natural disasters. It ingests real OpenStreetMap street graphs and uses the <strong>Hungarian Algorithm</strong> to maximize lives saved under strict deadlines.
              </div>
            </div>
            <div>
              <div style={{ color: '#D4E82B', fontWeight: 'bold', marginBottom: '4px' }}>💡 WHAT IS HAPPENING ON SCREEN?</div>
              <div style={{ color: '#CCC' }}>
                • 🟢 <strong>Green Circle:</strong> Available Rescue Squad<br/>
                • 🔵 <strong>Blue Line & Square:</strong> Assigned Unit en route<br/>
                • 🔴 <strong>Red Pin:</strong> Emergency Victim SOS<br/>
                • ❌ <strong>Red Dotted Road:</strong> Flooded Road Segment
              </div>
            </div>
            <div>
              <div style={{ color: '#D4E82B', fontWeight: 'bold', marginBottom: '4px' }}>🚀 HOW TO PRESENT TO JUDGES?</div>
              <div style={{ color: '#CCC' }}>
                Click <strong>"📢 LOG COMPLAINT"</strong> to lodge a custom emergency, or click <strong>"⚡ FLEET CONTROL"</strong> to end tasks and free units dynamically!
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Live Demo Floating HUD Toast Banner */}
      {demoBanner && (
        <div style={{ 
          position: 'fixed',
          top: '85px',
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 9999,
          background: 'rgba(20, 20, 15, 0.95)',
          color: '#FFF', 
          padding: '6px 16px', 
          borderRadius: '24px',
          border: '1.5px solid #D4E82B',
          boxShadow: '0 4px 20px rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          whiteSpace: 'nowrap',
          maxWidth: '90vw'
        }} className="mono">
          <span style={{ background: '#D4E82B', color: '#000', fontWeight: 'bold', padding: '2px 8px', borderRadius: '12px', fontSize: '10px' }}>
            STEP {demoBanner.step}
          </span>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: '12px', fontWeight: 'bold', color: '#FFF' }}>{demoBanner.title}</span>
            <span style={{ fontSize: '10px', color: '#CCC' }}>{demoBanner.desc}</span>
          </div>
          <span style={{ color: '#D4E82B', fontSize: '14px' }}>⚡</span>
        </div>
      )}

      {/* Main Map & Right Side Panel */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', minHeight: 0, height: '100%' }}>
        <div style={{ position: 'relative', background: 'var(--bg)', height: '100%' }}>
          <NetworkMap 
            nodes={nodes} 
            roads={roads} 
            agents={agents} 
            missions={missions} 
            disruptions={disruptions} 
            coverage={coverage} 
            repositioningRoutes={repositioningRoutes}
            onManualAssign={handleManualAssign}
            onCompleteMission={handleCompleteMission}
            onReleaseAgent={handleReleaseAgent}
          />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', borderLeft: '1px solid var(--rule)', height: '100%' }}>
          <DecisionLogPanel decisions={decisions} />
          <PolicyComparisonView currentScenario="urban_flood" apiBase={API_BASE} />
        </div>
      </div>

      {/* Telemetry Event Tape */}
      <EventTape events={events} />

      {/* Civilian Emergency Complaint Form Modal */}
      <CivilianComplaintModal
        isOpen={isComplaintModalOpen}
        onClose={() => setIsComplaintModalOpen(false)}
        onSubmitComplaint={handleSubmitComplaint}
        nodes={nodes}
      />

      {/* Tactical Fleet & Task Control Drawer */}
      <FleetDrawer
        isOpen={isFleetDrawerOpen}
        onClose={() => setIsFleetDrawerOpen(false)}
        agents={agents}
        missions={missions}
        onCompleteMission={handleCompleteMission}
        onReleaseAgent={handleReleaseAgent}
        onManualAssign={handleManualAssign}
      />

      {cmdOpen && <CommandPalette onClose={() => setCmdOpen(false)} onExecute={execCommand} />}
    </div>
  );
}

export default App;
