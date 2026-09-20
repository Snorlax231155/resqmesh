import React, { useState } from 'react';
import { Zap, ShieldAlert, Rocket } from 'lucide-react';

export default function ControlPanel({ onAgentSpawn, onMissionSpawn, onDisruptionSimulate, onDispatch }) {
  const [loading, setLoading] = useState(false);

  const wrapCall = (fn) => async () => {
    setLoading(true);
    try { await fn(); } finally { setLoading(false); }
  };

  return (
    <div className="glass-panel" style={{ padding: '1.5rem' }}>
      <h2 className="title-secondary" style={{ marginBottom: '1rem' }}>Simulation Controls</h2>
      <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
        
        <button className="btn btn-primary" onClick={wrapCall(onAgentSpawn)} disabled={loading}>
          <Rocket size={16} /> Spawn Agent
        </button>

        <button className="btn btn-primary" onClick={wrapCall(onMissionSpawn)} disabled={loading}>
          <Zap size={16} /> Spawn Mission
        </button>

        <button className="btn btn-primary" style={{ background: 'linear-gradient(135deg, var(--accent-emerald), #059669)', boxShadow: '0 0 10px var(--accent-emerald-glow)' }} onClick={wrapCall(onDispatch)} disabled={loading}>
          <Zap size={16} /> Run Dispatch Cycle
        </button>

        <div style={{ flex: 1, minWidth: '20px' }}></div>

        <button className="btn btn-danger" onClick={wrapCall(onDisruptionSimulate)} disabled={loading}>
          <ShieldAlert size={16} /> Simulate Road Closure
        </button>

      </div>
    </div>
  );
}
