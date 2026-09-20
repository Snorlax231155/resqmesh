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
         await fetch(`${API_BASE}/policies/active`, { 
            method: 'POST', 
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: pol }) 
         });
         setActivePolicy(pol);
         addEvent(`POLICY -> ${pol}`, "SYSTEM");
      } else if (action === "speed" && parts[1]) {
         const s = parts[1].replace('x','');
         await fetch(`${API_BASE}/sim/speed?x=${s}`, { method: 'POST' });
         addEvent(`SIM SPEED -> ${s}x`, "SYSTEM");
      } else if (action === "load" && parts[1]) {
         await fetch(`${API_BASE}/sim/load?scenario=${parts[1]}`, { method: 'POST' });
         fetchInitialData();
         addEvent(`LOAD SCENARIO -> ${parts[1]}`, "SYSTEM");
      } else if (action === "play") {
         await fetch(`${API_BASE}/sim/play`, { method: 'POST' });
         addEvent(`SIM -> PLAY`, "SYSTEM");
      } else if (action === "pause") {
         await fetch(`${API_BASE}/sim/pause`, { method: 'POST' });
         addEvent(`SIM -> PAUSE`, "SYSTEM");
      }
    } catch(e) {
      console.error(e);
      addEvent(`CMD ERR: ${cmd}`, "ERROR");
    }
  };

  return (
    <div style={{ display: 'grid', gridTemplateRows: 'auto 1fr auto', height: '100%', width: '100%' }}>
      <header className="panel" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--rule)' }}>
        <div style={{ display: 'flex', gap: '20px' }}>
          <div className="title" style={{ fontSize: '1.2rem' }}>RESQMESH TACTICAL</div>
          <div className="mono" style={{ fontSize: '0.9rem', color: 'var(--ink-muted)' }}>
            UNITS: {summary.availableAgents}/{summary.totalAgents} | 
            MSNS: {summary.pendingMissions}/{summary.totalMissions} | 
            DISR: {summary.activeDisruptions}
          </div>
        </div>
        <div className="title" style={{ color: 'var(--signal)', fontSize: '0.9rem' }}>POLICY: {activePolicy}</div>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', minHeight: 0 }}>
        <div style={{ position: 'relative', background: 'var(--bg)' }}>
          <NetworkMap nodes={nodes} roads={roads} agents={agents} missions={missions} disruptions={disruptions} coverage={coverage} repositioningRoutes={repositioningRoutes} />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', borderLeft: '1px solid var(--rule)' }}>
          <DecisionLogPanel decisions={decisions} />
          <PolicyComparisonView currentScenario="urban_flood" apiBase={API_BASE} />
        </div>
      </div>

      <EventTape events={events} />
      {cmdOpen && <CommandPalette onClose={() => setCmdOpen(false)} onExecute={execCommand} />}
    </div>
  );
}

export default App;
