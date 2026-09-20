import React, { useMemo, useEffect, useState } from 'react';
import { MapContainer, Polyline, Marker, useMap, CircleMarker, TileLayer } from 'react-leaflet';
import L from 'leaflet';

function FitBounds({ nodes }) {
  const map = useMap();
  useEffect(() => {
    if (nodes.length > 0) {
      const bounds = L.latLngBounds(nodes.map(n => [n.latitude, n.longitude]));
      map.fitBounds(bounds, { padding: [20, 20] });
    }
  }, [nodes, map]);
  return null;
}

const getAgentShape = (status) => {
  switch (status) {
    case 'AVAILABLE': return '<div class="agent-shape shape-circle" style="background: var(--hi-vis);"></div>';
    case 'ASSIGNED': return '<div class="agent-shape shape-square" style="background: var(--signal);"></div>';
    case 'EN_ROUTE': return '<div class="agent-shape shape-triangle" style="border-bottom-color: var(--signal);"></div>';
    case 'REPOSITIONING': return '<div class="agent-shape shape-diamond" style="background: var(--caution);"></div>';
    default: return '<div class="agent-shape shape-circle" style="background: var(--ink-muted);"></div>';
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
        <div class="atc-body">STS: ${agent.status.substring(0,4)}</div>
        <div class="atc-body">LOD: ${agent.status === 'EN_ROUTE' ? '1' : '0'}/${agent.capacity}</div>
        <div class="atc-body">ETA: ${agent.status === 'EN_ROUTE' ? '3m' : '--'}</div>
      </div>
    </div>
  `,
  iconSize: [0, 0],
  iconAnchor: [0, 0]
});

const createMissionIcon = (priority) => L.divIcon({
  className: 'atc-marker',
  html: `<div class="mission-shape" style="background: ${priority === 'CRITICAL' ? '#E4002B' : priority === 'HIGH' ? '#FF8A00' : '#0B4FA8'}"></div><div class="mission-label">${priority ? priority.substring(0,3) : 'MSN'}</div>`,
  iconSize: [0, 0],
  iconAnchor: [0, 0]
});

const TILE_STYLES = {
  voyager: {
    name: 'CartoDB Voyager',
    url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
    attribution: '&copy; OpenStreetMap &copy; CARTO'
  },
  dark: {
    name: 'CartoDB Dark',
    url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
    attribution: '&copy; OpenStreetMap &copy; CARTO'
  },
  osm: {
    name: 'OpenStreetMap',
    url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; OpenStreetMap contributors'
  }
};

export default function NetworkMap({ nodes, roads, agents, missions, disruptions, coverage, repositioningRoutes }) {
  const [tileStyle, setTileStyle] = useState('voyager');

  const nodeMap = useMemo(() => {
    const map = {};
    nodes.forEach(n => map[n.id] = n);
    return map;
  }, [nodes]);

  const disruptedEdgeIds = useMemo(() => new Set(disruptions.filter(d => d.active).map(d => d.affectedRoadId)), [disruptions]);

  const coverageOverlay = useMemo(() => {
    if (!coverage) return null;
    const makeCircles = (nodeIds, colorCode) => {
      return (nodeIds || []).map(id => {
        const node = nodeMap[id];
        if (!node) return null;
        return <CircleMarker key={`cov-${id}`} center={[node.latitude, node.longitude]} radius={20} pathOptions={{ color: 'transparent', fillColor: colorCode, fillOpacity: 0.2 }} />;
      });
    };
    return (
      <>
        {makeCircles(coverage.band0to4, 'var(--cov-1)')}
        {makeCircles(coverage.band4to8, 'var(--cov-2)')}
        {makeCircles(coverage.band8to15, 'var(--cov-3)')}
        {makeCircles(coverage.band15Plus, 'var(--cov-4)')}
        {makeCircles(coverage.unreachable, 'var(--cov-5)')}
      </>
    );
  }, [coverage, nodeMap]);

  if (nodes.length === 0) {
    return <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }} className="title">Loading Graph...</div>;
  }

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      {/* Map Control Bar Overlay */}
      <div style={{
        position: 'absolute',
        top: 10,
        left: 10,
        zIndex: 1000,
        background: 'var(--panel)',
        border: '1px solid var(--rule)',
        padding: '6px 10px',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        boxShadow: '0 2px 6px rgba(0,0,0,0.15)'
      }}>
        <span className="mono" style={{ fontSize: '11px', fontWeight: 'bold' }}>MAP TILES:</span>
        <select 
          value={tileStyle} 
          onChange={e => setTileStyle(e.target.value)}
          className="mono"
          style={{ background: 'var(--bg)', color: 'var(--ink)', border: '1px solid var(--rule)', padding: '2px 6px', fontSize: '11px' }}
        >
          {Object.entries(TILE_STYLES).map(([key, style]) => (
            <option key={key} value={key}>{style.name}</option>
          ))}
        </select>
      </div>

      <style>{`
        .atc-marker { overflow: visible !important; }
        .agent-shape { width: 10px; height: 10px; position: absolute; left: -5px; top: -5px; }
        .shape-circle { border-radius: 50%; }
        .shape-square { }
        .shape-diamond { transform: rotate(45deg); }
        .shape-triangle { width: 0; height: 0; background: transparent !important; border-left: 6px solid transparent; border-right: 6px solid transparent; border-bottom: 10px solid; left: -6px; top: -5px; }
        .atc-leader-line { position: absolute; left: 0; top: 0; width: 30px; height: 1px; background: var(--ink); transform: rotate(-45deg); transform-origin: 0 0; }
        .atc-data-block { position: absolute; left: 21px; top: -35px; background: var(--bg); border: 1px solid var(--rule); padding: 2px 4px; min-width: 60px; font-family: var(--font-mono); font-size: 10px; color: var(--ink); white-space: nowrap; }
        .atc-header { font-weight: bold; border-bottom: 1px solid var(--rule); margin-bottom: 1px; }
        .atc-body { line-height: 1.1; }
        .mission-shape { width: 8px; height: 8px; position: absolute; left: -4px; top: -4px; border-radius: 50%; border: 1px solid #000; box-shadow: 0 0 4px rgba(0,0,0,0.5); }
        .mission-label { position: absolute; left: 6px; top: -6px; font-family: var(--font-mono); font-size: 9px; font-weight: bold; color: var(--alert); text-shadow: 0 0 2px #fff; }
      `}</style>

      <MapContainer center={[40.73, -73.99]} zoom={14} style={{ height: '100%', width: '100%' }} zoomControl={true} preferCanvas={true} attributionControl={true}>
        <FitBounds nodes={nodes} />
        
        {/* Real OpenStreetMap / CartoDB Map Tiles */}
        <TileLayer
          url={TILE_STYLES[tileStyle].url}
          attribution={TILE_STYLES[tileStyle].attribution}
          subdomains="abcd"
          maxZoom={20}
        />

        {coverageOverlay}
        
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
                color: isDisrupted ? 'var(--alert)' : '#4A4A45', 
                weight: isDisrupted ? 3 : 1.5, 
                opacity: isDisrupted ? 0.9 : 0.4,
                dashArray: isDisrupted ? '4, 4' : null 
              }} 
            />
          );
        })}

        {/* Nodes Marks */}
        {nodes.map(n => <CircleMarker key={`n-${n.id}`} center={[n.latitude, n.longitude]} radius={1} pathOptions={{ color: '#4A4A45', fillOpacity: 0.6 }} />)}

        {/* Ghost Routes / Dynamic Repositioning */}
        {repositioningRoutes.map(route => {
          const positions = (route.routeNodeIds || []).map(id => {
            const n = nodeMap[id];
            return n ? [n.latitude, n.longitude] : null;
          }).filter(Boolean);
          
          return (
            <Polyline key={`repo-${route._id || Math.random()}`} positions={positions} pathOptions={{ color: 'var(--signal)', weight: 3, dashArray: '6, 8', opacity: 0.8 }} />
          );
        })}

        {/* Missions */}
        {missions.filter(m => m.status !== 'COMPLETED').map(m => {
          const locNode = nodeMap[m.pickupNodeId];
          if (!locNode) return null;
          return <Marker key={m.id} position={[locNode.latitude, locNode.longitude]} icon={createMissionIcon(m.priority)} />;
        })}

        {/* Rescue Units / Agents */}
        {agents.map(a => {
          const locNode = nodeMap[a.currentNodeId];
          if (!locNode) return null;
          return <Marker key={a.id} position={[locNode.latitude, locNode.longitude]} icon={createAgentIcon(a)} zIndexOffset={1000} />;
        })}
      </MapContainer>
    </div>
  );
}

