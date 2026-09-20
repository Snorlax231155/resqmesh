import React from 'react';
import { Activity, Users, Truck, AlertTriangle, CheckCircle } from 'lucide-react';

export default function Header({ summary }) {
  return (
    <header className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <div>
        <h1 className="title-primary text-gradient" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Activity color="var(--accent-cyan)" /> ResQMesh
        </h1>
        <p className="text-small" style={{ marginTop: '0.25rem' }}>Live Disruption-Aware Coordination Engine</p>
      </div>

      <div style={{ display: 'flex', gap: '1.5rem' }}>
        <StatCard icon={<Users size={20} color="var(--accent-cyan)" />} title="Total Agents" value={summary.totalAgents} />
        <StatCard icon={<CheckCircle size={20} color="var(--accent-emerald)" />} title="Available" value={summary.availableAgents} />
        <StatCard icon={<Truck size={20} color="var(--accent-cyan)" />} title="Active Missions" value={summary.totalMissions - summary.pendingMissions} />
        <StatCard icon={<AlertTriangle size={20} color="var(--accent-amber)" />} title="Disruptions" value={summary.activeDisruptions} />
      </div>
    </header>
  );
}

function StatCard({ icon, title, value }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', background: 'rgba(255,255,255,0.02)', padding: '0.75rem 1rem', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
      <div style={{ background: 'rgba(0,0,0,0.2)', padding: '0.5rem', borderRadius: '50%' }}>
        {icon}
      </div>
      <div>
        <p className="text-small">{title}</p>
        <p className="title-secondary">{value}</p>
      </div>
    </div>
  );
}
