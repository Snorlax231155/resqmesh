import React, { useMemo, useEffect } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap, CircleMarker } from 'react-leaflet';
import L from 'leaflet';

// Create custom icons for agents and missions
const agentIcon = L.divIcon({
  className: 'custom-icon',
  html: '<div style="background-color: var(--accent-cyan); width: 14px; height: 14px; border-radius: 50%; box-shadow: 0 0 10px var(--accent-cyan-glow); border: 2px solid white;"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7]
});

const missionIcon = L.divIcon({
  className: 'custom-icon',
  html: '<div style="background-color: var(--accent-amber); width: 14px; height: 14px; border-radius: 2px; box-shadow: 0 0 10px var(--accent-amber-glow); border: 2px solid white;"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7]
});

// A component to automatically fit the map bounds to the nodes
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

export default function NetworkMap({ nodes, roads, agents, missions, disruptions, coverage, repositioningRoutes }) {
  const nodeMap = useMemo(() => {
    const map = {};
    nodes.forEach(n => map[n.id] = n);
    return map;
  }, [nodes]);

  const disruptedEdgeIds = useMemo(() => {
    return new Set(disruptions.filter(d => d.active).map(d => d.affectedRoadId));
  }, [disruptions]);

  const coverageOverlay = useMemo(() => {
    if (!coverage) return null;
    const makeCircles = (nodeIds, color) => {
      return (nodeIds || []).map(id => {
        const node = nodeMap[id];
        if (!node) return null;
        return <CircleMarker key={`cov-${id}`} center={[node.latitude, node.longitude]} radius={30} pathOptions={{ color: 'transparent', fillColor: color, fillOpacity: 0.15 }} />;
      });
    };
    return (
      <>
        {makeCircles(coverage.band0to4, '#2E7D32')}
        {makeCircles(coverage.band4to8, '#D4E82B')}
        {makeCircles(coverage.band8to15, '#FF8A00')}
        {makeCircles(coverage.band15Plus, '#E4002B')}
        {makeCircles(coverage.unreachable, '#5C584E')}
      </>
    );
  }, [coverage, nodeMap]);

  const repositioningOverlay = useMemo(() => {
    if (!repositioningRoutes) return null;
    return repositioningRoutes.map((route, i) => {
      const positions = (route.routeNodeIds || []).map(id => {
        const n = nodeMap[id];
        return n ? [n.latitude, n.longitude] : null;
      }).filter(Boolean);
      
      return (
        <Polyline key={`repo-${i}`} positions={positions} pathOptions={{ color: '#0B4FA8', weight: 2, dashArray: '5, 10', opacity: 0.8 }} />
      );
    });
  }, [repositioningRoutes, nodeMap]);

  if (nodes.length === 0) {
    return <div className="glass-panel" style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Loading Map...</div>;
  }

  // Find center (average of all nodes as fallback)
  const centerLat = nodes.reduce((sum, n) => sum + parseFloat(n.latitude), 0) / nodes.length;
  const centerLon = nodes.reduce((sum, n) => sum + parseFloat(n.longitude), 0) / nodes.length;

  return (
    <div className="glass-panel" style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <div style={{ padding: '1rem 1.5rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between' }}>
        <h2 className="title-secondary">Live Operations Map (New York City)</h2>
        <span className="badge badge-cyan">{nodes.length} Nodes</span>
      </div>
      
      <div style={{ flex: 1, position: 'relative' }}>
        <MapContainer 
          center={[centerLat, centerLon]} 
          zoom={14} 
          style={{ height: '100%', width: '100%', backgroundColor: '#0f172a' }}
          zoomControl={false}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            className="map-tiles"
          />
          <FitBounds nodes={nodes} />

          {/* Coverage Overlay */}
          {coverageOverlay}

          {/* Repositioning Overlay */}
          {repositioningOverlay}

          {/* Render Roads */}
          {roads.map(road => {
            const src = nodeMap[road.sourceNodeId];
            const dst = nodeMap[road.destinationNodeId];
            if (!src || !dst) return null;

            const isDisrupted = disruptedEdgeIds.has(road.id) || road.blocked;
            const positions = [
              [src.latitude, src.longitude],
              [dst.latitude, dst.longitude]
            ];

            return (
              <Polyline 
                key={road.id} 
                positions={positions} 
                pathOptions={{ 
                  color: isDisrupted ? '#e11d48' : '#334155', 
                  weight: isDisrupted ? 4 : 2,
                  opacity: 0.8
                }} 
              />
            );
          })}

          {/* Render Missions */}
          {missions.map(m => {
            const locNode = m.status === 'COMPLETED' ? nodeMap[m.destinationNodeId] : nodeMap[m.pickupNodeId];
            if (!locNode) return null;
            return (
              <Marker key={m.id} position={[locNode.latitude, locNode.longitude]} icon={missionIcon}>
                <Popup>Mission: {m.missionCode} <br/> Status: {m.status}</Popup>
              </Marker>
            );
          })}

          {/* Render Agents */}
          {agents.map(a => {
            const locNode = nodeMap[a.currentNodeId];
            if (!locNode) return null;
            return (
              <Marker key={a.id} position={[locNode.latitude, locNode.longitude]} icon={agentIcon}>
                <Popup>Agent: {a.agentCode} <br/> Status: {a.status}</Popup>
              </Marker>
            );
          })}

        </MapContainer>
      </div>
    </div>
  );
}
