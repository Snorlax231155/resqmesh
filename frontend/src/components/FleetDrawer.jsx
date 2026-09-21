import React, { useState } from 'react';

export default function FleetDrawer({ isOpen, onClose, agents, missions, onCompleteMission, onReleaseAgent, onManualAssign, onFocusMapNode }) {
  const [activeTab, setActiveTab] = useState('missions');
  const [filterText, setFilterText] = useState('');
  const [selectedAgentMap, setSelectedAgentMap] = useState({});

  if (!isOpen) return null;

  const activeMissions = missions.filter(m => m.status !== 'COMPLETED');
  const completedMissions = missions.filter(m => m.status === 'COMPLETED');
  const availableAgents = agents.filter(a => a.status === 'AVAILABLE');

  const filteredMissions = activeMissions.filter(m => 
    m.missionCode.toLowerCase().includes(filterText.toLowerCase()) ||
    m.priority.toLowerCase().includes(filterText.toLowerCase()) ||
    m.status.toLowerCase().includes(filterText.toLowerCase())
  );

  const filteredAgents = agents.filter(a =>
    a.agentCode.toLowerCase().includes(filterText.toLowerCase()) ||
    a.name.toLowerCase().includes(filterText.toLowerCase()) ||
    a.status.toLowerCase().includes(filterText.toLowerCase())
  );

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      right: 0,
      width: '420px',
      maxWidth: '90vw',
      height: '100vh',
      background: 'rgba(14, 14, 10, 0.98)',
      borderLeft: '2px solid #0B4FA8',
      zIndex: 9000,
      boxShadow: '-8px 0 24px rgba(0,0,0,0.6)',
      display: 'flex',
      flexDirection: 'column',
      color: '#FFF',
      fontFamily: 'var(--font-mono)'
    }}>
      {/* Drawer Header */}
      <div style={{ padding: '14px 16px', borderBottom: '1px solid #333', background: '#090906', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ fontSize: '13px', fontWeight: 'bold', color: '#D4E82B', letterSpacing: '0.08em' }}>
            ⚡ TACTICAL FLEET & TASK CONTROL
          </div>
          <div style={{ fontSize: '10px', color: '#888' }}>
            {activeMissions.length} Active Missions • {agents.length} Deployed Rescue Squads
          </div>
        </div>
        <button onClick={onClose} style={{ background: '#222', color: '#FFF', border: '1px solid #444', padding: '4px 8px', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', fontWeight: 'bold' }}>✕ CLOSE</button>
      </div>

      {/* Tabs & Search */}
      <div style={{ padding: '10px 16px', background: '#111', borderBottom: '1px solid #222', display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={() => setActiveTab('missions')}
            style={{
              flex: 1,
              padding: '6px',
              fontSize: '11px',
              fontWeight: 'bold',
              background: activeTab === 'missions' ? '#0B4FA8' : '#222',
              color: '#FFF',
              border: 'none',
              borderRadius: '3px',
              cursor: 'pointer'
            }}
          >
            🚨 ACTIVE MISSIONS ({activeMissions.length})
          </button>
          <button
            onClick={() => setActiveTab('agents')}
            style={{
              flex: 1,
              padding: '6px',
              fontSize: '11px',
              fontWeight: 'bold',
              background: activeTab === 'agents' ? '#0B4FA8' : '#222',
              color: '#FFF',
              border: 'none',
              borderRadius: '3px',
              cursor: 'pointer'
            }}
          >
            🚑 RESCUE UNITS ({agents.length})
          </button>
        </div>

        <input 
          type="text" 
          placeholder="Filter code, status, priority..."
          value={filterText}
          onChange={e => setFilterText(e.target.value)}
          style={{ background: '#1C1C16', border: '1px solid #333', color: '#FFF', padding: '6px 10px', fontSize: '11px', borderRadius: '3px' }}
        />
      </div>

      {/* Drawer Content */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {activeTab === 'missions' && (
          filteredMissions.length === 0 ? (
            <div style={{ color: '#666', fontSize: '12px', textAlign: 'center', marginTop: '30px' }}>No active missions found.</div>
          ) : (
            filteredMissions.map(m => {
              const assignedAgent = agents.find(a => String(a.id) === String(m.assignedAgentId));
              return (
                <div key={m.id} style={{ background: '#1A1A14', border: '1px solid #333', borderRadius: '4px', padding: '10px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: 'bold', fontSize: '12px', color: '#E4002B' }}>🚨 {m.missionCode}</span>
                    <span style={{
                      fontSize: '10px',
                      padding: '2px 6px',
                      borderRadius: '2px',
                      fontWeight: 'bold',
                      background: m.priority === 'CRITICAL' ? '#E4002B' : m.priority === 'HIGH' ? '#FF8A00' : '#0B4FA8',
                      color: '#FFF'
                    }}>
                      {m.priority}
                    </span>
                  </div>

                  <div style={{ fontSize: '11px', color: '#AAA', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4px' }}>
                    <div>Status: <span style={{ color: '#FFF', fontWeight: 'bold' }}>{m.status}</span></div>
                    <div>Assigned: <span style={{ color: '#00E5FF', fontWeight: 'bold' }}>{assignedAgent ? assignedAgent.agentCode : 'UNASSIGNED'}</span></div>
                    <div>Pickup Node: <span style={{ color: '#D4E82B' }}>{m.pickupNodeId ? m.pickupNodeId.substring(0, 10) : 'N/A'}</span></div>
                  </div>

                  {/* Manual Assignment Controls when Unassigned */}
                  {!assignedAgent && availableAgents.length > 0 && onManualAssign && (
                    <div style={{ display: 'flex', gap: '6px', marginTop: '4px', alignItems: 'center', background: '#222218', padding: '6px', borderRadius: '3px', border: '1px dashed #0B4FA8' }}>
                      <select
                        value={selectedAgentMap[m.id] || availableAgents[0].id}
                        onChange={e => setSelectedAgentMap(prev => ({ ...prev, [m.id]: e.target.value }))}
                        style={{ flex: 1, background: '#111', color: '#D4E82B', border: '1px solid #444', padding: '4px', fontSize: '10px', borderRadius: '3px', fontWeight: 'bold' }}
                      >
                        {availableAgents.map(ag => (
                          <option key={ag.id} value={ag.id}>🚑 {ag.agentCode} ({ag.name})</option>
                        ))}
                      </select>
                      <button
                        onClick={() => {
                          const agId = selectedAgentMap[m.id] || availableAgents[0].id;
                          if (agId) onManualAssign(m.id, agId);
                        }}
                        style={{ background: '#0B4FA8', color: '#FFF', border: 'none', padding: '4px 8px', fontSize: '10px', fontWeight: 'bold', borderRadius: '3px', cursor: 'pointer' }}
                      >
                        👉 ASSIGN
                      </button>
                    </div>
                  )}

                  <div style={{ display: 'flex', gap: '6px', marginTop: '6px' }}>
                    <button
                      onClick={() => onCompleteMission(m.id)}
                      style={{
                        flex: 1,
                        background: '#0A5C0D',
                        color: '#FFF',
                        border: 'none',
                        padding: '6px',
                        fontSize: '10px',
                        fontWeight: 'bold',
                        borderRadius: '3px',
                        cursor: 'pointer',
                        boxShadow: '0 2px 6px rgba(10,92,13,0.4)'
                      }}
                    >
                      ✅ END TASK & FREE UNIT
                    </button>
                    {onFocusMapNode && (
                      <button
                        onClick={() => onFocusMapNode(m.pickupNodeId)}
                        style={{
                          background: '#222',
                          color: '#CCC',
                          border: '1px solid #444',
                          padding: '6px 10px',
                          fontSize: '10px',
                          borderRadius: '3px',
                          cursor: 'pointer'
                        }}
                      >
                        📍 LOCATE
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )
        )}

        {activeTab === 'agents' && (
          filteredAgents.length === 0 ? (
            <div style={{ color: '#666', fontSize: '12px', textAlign: 'center', marginTop: '30px' }}>No rescue units found.</div>
          ) : (
            filteredAgents.map(a => {
              const isAvailable = a.status === 'AVAILABLE';
              return (
                <div key={a.id} style={{ background: '#1A1A14', border: '1px solid #333', borderRadius: '4px', padding: '10px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: 'bold', fontSize: '12px', color: '#D4E82B' }}>🚑 {a.agentCode} ({a.name})</span>
                    <span style={{
                      fontSize: '10px',
                      padding: '2px 6px',
                      borderRadius: '2px',
                      fontWeight: 'bold',
                      background: isAvailable ? '#0A5C0D' : '#0B4FA8',
                      color: '#FFF'
                    }}>
                      {a.status}
                    </span>
                  </div>

                  <div style={{ fontSize: '11px', color: '#AAA' }}>
                    Current Node: <span style={{ color: '#FFF' }}>{a.currentNodeId ? a.currentNodeId.substring(0, 10) : 'N/A'}</span>
                  </div>

                  <div style={{ display: 'flex', gap: '6px', marginTop: '6px' }}>
                    <button
                      onClick={() => onReleaseAgent(a.id)}
                      disabled={isAvailable}
                      style={{
                        flex: 1,
                        background: isAvailable ? '#333' : '#FF8A00',
                        color: isAvailable ? '#666' : '#000',
                        border: 'none',
                        padding: '6px',
                        fontSize: '10px',
                        fontWeight: 'bold',
                        borderRadius: '3px',
                        cursor: isAvailable ? 'not-allowed' : 'pointer'
                      }}
                    >
                      {isAvailable ? '✓ ALREADY AVAILABLE' : '🔓 FORCE RELEASE UNIT'}
                    </button>
                    {onFocusMapNode && (
                      <button
                        onClick={() => onFocusMapNode(a.currentNodeId)}
                        style={{
                          background: '#222',
                          color: '#CCC',
                          border: '1px solid #444',
                          padding: '6px 10px',
                          fontSize: '10px',
                          borderRadius: '3px',
                          cursor: 'pointer'
                        }}
                      >
                        📍 LOCATE
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )
        )}
      </div>

      {/* Drawer Footer */}
      <div style={{ padding: '10px 16px', background: '#090906', borderTop: '1px solid #222', fontSize: '10px', color: '#666', display: 'flex', justifyContent: 'space-between' }}>
        <span>RESQMESH DISPATCH PROTOCOL v2.4</span>
        <span>{completedMissions.length} Missions Completed</span>
      </div>
    </div>
  );
}
