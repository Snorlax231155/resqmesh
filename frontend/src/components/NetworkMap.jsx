import React, { useMemo, useEffect, useState } from 'react';
import { MapContainer, Polyline, Marker, useMap, CircleMarker, TileLayer, Popup } from 'react-leaflet';
import L from 'leaflet';

function FitBounds({ nodes }) {
  const map = useMap();
  useEffect(() => {
    if (nodes.length > 0) {
      const bounds = L.latLngBounds(nodes.map(n => [n.latitude, n.longitude]));
      map.fitBounds(bounds, { padding: [30, 30] });
    }
  }, [nodes, map]);
  return null;
}

const getAgentShape = (status) => {
  switch (status) {
    case 'AVAILABLE': return '<div class="agent-shape shape-circle" style="background: #D4E82B; border: 2px solid #000; box-shadow: 0 0 10px #D4E82B;"></div>';
    case 'ASSIGNED': return '<div class="agent-shape shape-square" style="background: #0B4FA8; border: 2px solid #FFF; box-shadow: 0 0 10px #0B4FA8;"></div>';
    case 'EN_ROUTE': return '<div class="agent-shape shape-triangle" style="border-bottom-color: #0B4FA8;"></div>';
    case 'REPOSITIONING': return '<div class="agent-shape shape-diamond" style="background: #FF8A00; border: 1px solid #000;"></div>';
    default: return '<div class="agent-shape shape-circle" style="background: #5C584E;"></div>';
  }
};

const createAgentIcon = (agent) => L.divIcon({
  className: 'atc-marker',
  html: `
    <div style="position: relative;">
      ${getAgentShape(agent.status)}
      <div class="atc-leader-line"></div>
      <div class="atc-data-block">
        <div class="atc-header">${agent.agentCode}</div>
        <div class="atc-body" style="color: ${agent.status === 'AVAILABLE' ? '#0A5C0D' : '#0B4FA8'}; font-weight: bold;">${agent.status}</div>
      </div>
    </div>
  `,
  iconSize: [0, 0],
  iconAnchor: [0, 0]
});

const createMissionIcon = (priority, status) => L.divIcon({
  className: 'atc-marker',
  html: `
    <div style="position: relative;">
      <div class="mission-pulse-ring" style="border-color: ${priority === 'CRITICAL' ? '#E4002B' : '#FF8A00'};"></div>
      <div class="mission-shape" style="background: ${priority === 'CRITICAL' ? '#E4002B' : priority === 'HIGH' ? '#FF8A00' : '#0B4FA8'}; border: 2px solid #FFF;"></div>
      <div class="mission-label">🚨 ${priority ? priority : 'SOS'}</div>
    </div>
  `,
  iconSize: [0, 0],
  iconAnchor: [0, 0]
});

const TILE_STYLES = {
  osm: {
    name: 'OpenStreetMap (Clean)',
    url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; OpenStreetMap contributors'
  },
  esri: {
    name: 'Esri World Street Map',
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}',
    attribution: 'Tiles &copy; Esri'
  },
  carto_light: {
    name: 'CartoDB Light',
    url: 'https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png',
    attribution: '&copy; OpenStreetMap &copy; CARTO'
  }
};

export default function NetworkMap({ 
  nodes, 
  roads, 
  agents, 
  missions, 
  disruptions, 
  coverage, 
  repositioningRoutes,
  onManualAssign,
  onCompleteMission,
  onReleaseAgent 
}) {
  const [tileStyle, setTileStyle] = useState('osm');
  const [selectedMissionForAgent, setSelectedMissionForAgent] = useState({});
  const [selectedAgentForMission, setSelectedAgentForMission] = useState({});

  const nodeMap = useMemo(() => {
    const map = {};
    nodes.forEach(n => map[n.id] = n);
    return map;
  }, [nodes]);

  const disruptedEdges = useMemo(() => {
    return disruptions.filter(d => d.active);
  }, [disruptions]);

  const disruptedEdgeIds = useMemo(() => new Set(disruptedEdges.map(d => d.affectedRoadId)), [disruptedEdges]);

  const unitMissionConnections = useMemo(() => {
    const connections = [];
    const addedKeys = new Set();

    missions.forEach(m => {
      if (m.status === 'COMPLETED' || !m.assignedAgentId) return;
      const agent = agents.find(a => String(a.id) === String(m.assignedAgentId));
      if (agent) {
        const agentNode = nodeMap[agent.currentNodeId];
        const missionNode = nodeMap[m.pickupNodeId];
        if (agentNode && missionNode) {
          const key = `${m.id}-${agent.id}`;
          addedKeys.add(key);
          connections.push({
            id: key,
            from: [agentNode.latitude, agentNode.longitude],
            to: [missionNode.latitude, missionNode.longitude]
          });
        }
      }
    });

    agents.forEach(a => {
      if (a.assignedMissionId) {
        const mission = missions.find(m => String(m.id) === String(a.assignedMissionId));
        if (mission && mission.status !== 'COMPLETED') {
          const key = `${mission.id}-${a.id}`;
          if (!addedKeys.has(key)) {
            const agentNode = nodeMap[a.currentNodeId];
            const missionNode = nodeMap[mission.pickupNodeId];
            if (agentNode && missionNode) {
              addedKeys.add(key);
              connections.push({
                id: key,
                from: [agentNode.latitude, agentNode.longitude],
                to: [missionNode.latitude, missionNode.longitude]
              });
            }
          }
        }
      }
    });

    return connections;
  }, [missions, agents, nodeMap]);

  if (nodes.length === 0) {
    return <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }} className="title">Loading Road Network Graph...</div>;
  }

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      {/* Map Style Overlay Selector */}
      <div style={{
        position: 'absolute',
        top: 12,
        left: 12,
        zIndex: 1000,
        background: 'rgba(255,255,255,0.95)',
        border: '1px solid #14140F',
        padding: '6px 12px',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
        borderRadius: '4px'
      }}>
        <span className="mono" style={{ fontSize: '11px', fontWeight: 'bold', color: '#000' }}>MAP STYLE:</span>
        <select 
          value={tileStyle} 
          onChange={e => setTileStyle(e.target.value)}
          className="mono"
          style={{ background: '#fff', color: '#000', border: '1px solid #ccc', padding: '3px 8px', fontSize: '11px', fontWeight: 'bold' }}
        >
          {Object.entries(TILE_STYLES).map(([key, style]) => (
            <option key={key} value={key}>{style.name}</option>
          ))}
        </select>
      </div>

      {/* High Visibility Map Legend */}
      <div style={{
        position: 'absolute',
        top: 12,
        right: 12,
        zIndex: 1000,
        background: 'rgba(14, 14, 10, 0.95)',
        color: '#FFF',
        border: '2px solid #0B4FA8',
        padding: '10px 14px',
        fontSize: '11px',
        boxShadow: '0 4px 16px rgba(0,0,0,0.4)',
        borderRadius: '4px'
      }} className="mono">
        <div style={{ fontWeight: 'bold', borderBottom: '1px solid #444', paddingBottom: '4px', marginBottom: '8px', color: '#D4E82B', fontSize: '12px' }}>
          TACTICAL MAP LEGEND
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr', gap: '8px 12px', alignItems: 'center' }}>
          <span style={{ display: 'inline-block', width: '12px', height: '12px', borderRadius: '50%', background: '#D4E82B', border: '1px solid #000', boxShadow: '0 0 6px #D4E82B' }}></span>
          <span>Available Rescue Unit</span>
          <span style={{ display: 'inline-block', width: '12px', height: '12px', background: '#0B4FA8', border: '1px solid #FFF' }}></span>
          <span>Assigned Rescue Unit</span>
          <span style={{ display: 'inline-block', width: '12px', height: '12px', borderRadius: '50%', background: '#E4002B', boxShadow: '0 0 6px #E4002B' }}></span>
          <span>Emergency SOS Victim</span>
          <span style={{ display: 'inline-block', width: '20px', height: '4px', background: '#E4002B', borderTop: '1px dashed #FFF' }}></span>
          <span style={{ color: '#FF8A00', fontWeight: 'bold' }}>Flooded / Blocked Road</span>
          <span style={{ display: 'inline-block', width: '20px', height: '4px', background: '#0B4FA8' }}></span>
          <span>Dynamic A* Dispatch Path</span>
          <span style={{ display: 'inline-block', width: '20px', height: '4px', background: '#00E5FF', borderTop: '2px dashed #00E5FF' }}></span>
          <span style={{ color: '#00E5FF', fontWeight: 'bold' }}>Unit-to-Mission Link</span>
        </div>
      </div>

      <style>{`
        .atc-marker { overflow: visible !important; }
        .agent-shape { width: 14px; height: 14px; position: absolute; left: -7px; top: -7px; }
        .shape-circle { border-radius: 50%; }
        .shape-square { }
        .shape-diamond { transform: rotate(45deg); }
        .shape-triangle { width: 0; height: 0; background: transparent !important; border-left: 8px solid transparent; border-right: 8px solid transparent; border-bottom: 14px solid; left: -8px; top: -7px; }
        .atc-leader-line { position: absolute; left: 0; top: 0; width: 24px; height: 1px; background: #000; transform: rotate(-45deg); transform-origin: 0 0; }
        .atc-data-block { position: absolute; left: 18px; top: -32px; background: rgba(255,255,255,0.98); border: 1.5px solid #14140F; padding: 2px 6px; min-width: 65px; font-family: var(--font-mono); font-size: 10px; color: #14140F; white-space: nowrap; box-shadow: 0 2px 6px rgba(0,0,0,0.3); border-radius: 2px; }
        .atc-header { font-weight: bold; border-bottom: 1px solid #bbb; margin-bottom: 1px; color: #000; }
        .mission-shape { width: 12px; height: 12px; position: absolute; left: -6px; top: -6px; border-radius: 50%; box-shadow: 0 0 10px rgba(228,0,43,0.9); }
        .mission-pulse-ring { width: 26px; height: 26px; position: absolute; left: -13px; top: -13px; border-radius: 50%; border: 2px solid #E4002B; animation: pulseRing 1.8s infinite ease-out; }
        @keyframes pulseRing {
          0% { transform: scale(0.5); opacity: 1; }
          100% { transform: scale(1.6); opacity: 0; }
        }
        .mission-label { position: absolute; left: 9px; top: -8px; font-family: var(--font-mono); font-size: 10px; font-weight: bold; color: #E4002B; text-shadow: 0 0 4px #FFF, 0 0 4px #FFF; white-space: nowrap; }
      `}</style>

      <MapContainer center={[40.73, -73.99]} zoom={14} style={{ height: '100%', width: '100%' }} zoomControl={true} preferCanvas={true} attributionControl={true}>
        <FitBounds nodes={nodes} />
        
        {/* OpenStreetMap / Esri Clean Map Tiles */}
        <TileLayer
          url={TILE_STYLES[tileStyle].url}
          attribution={TILE_STYLES[tileStyle].attribution}
          maxZoom={19}
        />

        {/* Road Network Graph Edges */}
        {roads.map(road => {
          const src = nodeMap[road.sourceNodeId];
          const dst = nodeMap[road.destinationNodeId];
          if (!src || !dst) return null;
          const isDisrupted = disruptedEdgeIds.has(road.id) || road.blocked;
          return (
            <Polyline 
              key={road.id} 
              positions={[[src.latitude, src.longitude], [dst.latitude, dst.longitude]]} 
              pathOptions={{ 
                color: isDisrupted ? '#E4002B' : '#333330', 
                weight: isDisrupted ? 5 : 1.5, 
                opacity: isDisrupted ? 1.0 : 0.45,
                dashArray: isDisrupted ? '8, 8' : null 
              }} 
            />
          );
        })}

        {/* Graph Nodes Marks (Solid Black, Less Transparent) */}
        {nodes.map(n => (
          <CircleMarker 
            key={`n-${n.id}`} 
            center={[n.latitude, n.longitude]} 
            radius={2.0} 
            pathOptions={{ color: '#000000', fillColor: '#000000', fillOpacity: 0.95, weight: 1 }} 
          />
        ))}

        {/* Tactical Unit-to-Mission Connection Lines */}
        {unitMissionConnections.map(conn => (
          <Polyline
            key={`unit-mission-conn-${conn.id}`}
            positions={[conn.from, conn.to]}
            pathOptions={{
              color: '#00E5FF',
              weight: 3.5,
              opacity: 0.95,
              dashArray: '6, 8'
            }}
          />
        ))}

        {/* Real-time A* Repositioning & Dynamic Dispatch Paths */}
        {repositioningRoutes.map(route => {
          const positions = (route.routeNodeIds || []).map(id => {
            const n = nodeMap[id];
            return n ? [n.latitude, n.longitude] : null;
          }).filter(Boolean);
          
          return (
            <Polyline key={`repo-${route._id || Math.random()}`} positions={positions} pathOptions={{ color: '#0B4FA8', weight: 5, opacity: 0.95, dashArray: '8, 8' }} />
          );
        })}

        {/* SOS Emergency Missions */}
        {missions.filter(m => m.status !== 'COMPLETED').map(m => {
          const locNode = nodeMap[m.pickupNodeId];
          if (!locNode) return null;
          const assignedAgent = agents.find(a => String(a.id) === String(m.assignedAgentId));
          const availableAgents = agents.filter(a => a.status === 'AVAILABLE');

          return (
            <Marker key={m.id} position={[locNode.latitude, locNode.longitude]} icon={createMissionIcon(m.priority, m.status)}>
              <Popup minWidth={240}>
                <div style={{ fontFamily: 'monospace', fontSize: '11px', color: '#14140F' }}>
                  <div style={{ fontWeight: 'bold', fontSize: '12px', color: '#E4002B', borderBottom: '1.5px solid #E4002B', paddingBottom: '4px', marginBottom: '6px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>🚨 {m.missionCode}</span>
                    <span style={{ fontSize: '9px', background: m.priority === 'CRITICAL' ? '#E4002B' : '#FF8A00', color: '#FFF', padding: '1px 5px', borderRadius: '2px' }}>{m.priority}</span>
                  </div>
                  <div style={{ fontSize: '10px', color: '#555', marginBottom: '4px' }}>Status: <strong>{m.status}</strong></div>
                  <div style={{ fontSize: '10px', color: '#555', marginBottom: '4px' }}>Assigned Squad: <strong>{assignedAgent ? assignedAgent.agentCode : 'UNASSIGNED'}</strong></div>
                  <div style={{ fontSize: '10px', color: '#555', marginBottom: '8px' }}>Pickup Node: <code>{m.pickupNodeId.substring(0, 12)}</code></div>

                  {!assignedAgent ? (
                    availableAgents.length > 0 ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '6px', background: '#F4F4EE', padding: '6px', borderRadius: '4px', border: '1px dashed #E4002B' }}>
                        <label style={{ fontSize: '10px', fontWeight: 'bold', color: '#000' }}>SELECT RESCUE UNIT TO ASSIGN:</label>
                        <select
                          value={selectedAgentForMission[m.id] || availableAgents[0].id}
                          onChange={e => setSelectedAgentForMission(prev => ({ ...prev, [m.id]: e.target.value }))}
                          style={{ fontSize: '10px', padding: '4px', fontWeight: 'bold', background: '#FFF', color: '#000', border: '1px solid #999', borderRadius: '3px' }}
                        >
                          {availableAgents.map(ag => (
                            <option key={ag.id} value={ag.id}>🚑 {ag.agentCode} ({ag.name})</option>
                          ))}
                        </select>
                        <button
                          onClick={() => {
                            const agId = selectedAgentForMission[m.id] || availableAgents[0].id;
                            if (agId && onManualAssign) onManualAssign(m.id, agId);
                          }}
                          style={{ background: '#0B4FA8', color: '#FFF', border: 'none', padding: '5px 8px', fontSize: '10px', fontWeight: 'bold', cursor: 'pointer', borderRadius: '3px', marginTop: '2px', boxShadow: '0 2px 4px rgba(11,79,168,0.3)' }}
                        >
                          👉 ASSIGN SELECTED SQUAD
                        </button>
                      </div>
                    ) : (
                      <div style={{ fontSize: '10px', color: '#888', fontStyle: 'italic', marginTop: '4px' }}>No available rescue units right now.</div>
                    )
                  ) : (
                    onCompleteMission && (
                      <button
                        onClick={() => onCompleteMission(m.id)}
                        style={{ background: '#0A5C0D', color: '#FFF', border: 'none', padding: '5px 10px', fontSize: '10px', fontWeight: 'bold', cursor: 'pointer', borderRadius: '3px', width: '100%', marginTop: '6px' }}
                      >
                        ✅ END TASK & FREE UNIT
                      </button>
                    )
                  )}
                </div>
              </Popup>
            </Marker>
          );
        })}

        {/* Rescue Units / Agents */}
        {agents.map(a => {
          const locNode = nodeMap[a.currentNodeId];
          if (!locNode) return null;
          const isAvailable = a.status === 'AVAILABLE';
          const pendingMissions = missions.filter(m => m.status === 'PENDING' || !m.assignedAgentId);

          return (
            <Marker key={a.id} position={[locNode.latitude, locNode.longitude]} icon={createAgentIcon(a)} zIndexOffset={1000}>
              <Popup minWidth={240}>
                <div style={{ fontFamily: 'monospace', fontSize: '11px', color: '#14140F' }}>
                  <div style={{ fontWeight: 'bold', fontSize: '12px', color: '#0B4FA8', borderBottom: '1.5px solid #0B4FA8', paddingBottom: '4px', marginBottom: '6px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>🚑 {a.agentCode}</span>
                    <span style={{ fontSize: '9px', background: isAvailable ? '#0A5C0D' : '#0B4FA8', color: '#FFF', padding: '1px 5px', borderRadius: '2px' }}>{a.status}</span>
                  </div>
                  <div style={{ fontSize: '10px', color: '#555', marginBottom: '4px' }}>Name: <strong>{a.name}</strong></div>
                  <div style={{ fontSize: '10px', color: '#555', marginBottom: '8px' }}>Location: <code>{a.currentNodeId.substring(0, 12)}</code></div>

                  {isAvailable ? (
                    pendingMissions.length > 0 ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '6px', background: '#F4F4EE', padding: '6px', borderRadius: '4px', border: '1px dashed #0B4FA8' }}>
                        <label style={{ fontSize: '10px', fontWeight: 'bold', color: '#000' }}>SELECT UNASSIGNED SOS MISSION:</label>
                        <select
                          value={selectedMissionForAgent[a.id] || pendingMissions[0].id}
                          onChange={e => setSelectedMissionForAgent(prev => ({ ...prev, [a.id]: e.target.value }))}
                          style={{ fontSize: '10px', padding: '4px', fontWeight: 'bold', background: '#FFF', color: '#000', border: '1px solid #999', borderRadius: '3px' }}
                        >
                          {pendingMissions.map(m => (
                            <option key={m.id} value={m.id}>🚨 {m.missionCode} ({m.priority})</option>
                          ))}
                        </select>
                        <button
                          onClick={() => {
                            const mId = selectedMissionForAgent[a.id] || pendingMissions[0].id;
                            if (mId && onManualAssign) onManualAssign(mId, a.id);
                          }}
                          style={{ background: '#0B4FA8', color: '#FFF', border: 'none', padding: '5px 8px', fontSize: '10px', fontWeight: 'bold', cursor: 'pointer', borderRadius: '3px', marginTop: '2px', boxShadow: '0 2px 4px rgba(11,79,168,0.3)' }}
                        >
                          👉 ASSIGN UNIT TO MISSION
                        </button>
                      </div>
                    ) : (
                      <div style={{ fontSize: '10px', color: '#888', fontStyle: 'italic', marginTop: '4px' }}>No unassigned missions pending right now.</div>
                    )
                  ) : (
                    onReleaseAgent && (
                      <button
                        onClick={() => onReleaseAgent(a.id)}
                        style={{ background: '#FF8A00', color: '#000', border: 'none', padding: '5px 10px', fontSize: '10px', fontWeight: 'bold', cursor: 'pointer', borderRadius: '3px', width: '100%', marginTop: '6px' }}
                      >
                        🔓 FORCE RELEASE UNIT
                      </button>
                    )
                  )}
                </div>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </div>
  );
}



