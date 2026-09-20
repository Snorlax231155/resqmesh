import React, { useState } from 'react';

export default function PolicyComparisonView({ currentScenario, apiBase }) {
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCompare = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${apiBase}/policies/compare?scenario=${currentScenario}`);
      const data = await res.json();
      setMetrics(data);
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  };

  return (
    <div style={{ background: 'var(--panel)', borderTop: '1px solid var(--rule)' }}>
      <div className="title" style={{ padding: '8px 12px', borderBottom: '1px solid var(--rule)', fontSize: '1rem', background: 'var(--bg)', display: 'flex', justifyContent: 'space-between' }}>
        <span>POLICY EVAL</span>
        <button onClick={handleCompare}>{loading ? 'RUNNING...' : 'COMPARE'}</button>
      </div>
      <div style={{ padding: '8px 12px', overflowX: 'auto' }}>
        <table className="mono" style={{ width: '100%', fontSize: '10px', textAlign: 'left', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--rule)' }}>
              <th style={{ paddingBottom: '4px' }}>POLICY</th>
              <th>AVG(m)</th>
              <th>P95(m)</th>
              <th>CRIT_MISS</th>
              <th>SEV_DEL</th>
            </tr>
          </thead>
          <tbody>
            {metrics ? Object.entries(metrics).map(([pol, m]) => (
              <tr key={pol} style={{ borderBottom: '1px dashed var(--rule)' }}>
                <td style={{ padding: '4px 0', color: 'var(--signal)' }}>{pol.substring(0,4)}</td>
                <td>{m.avgResponseMins.toFixed(1)}</td>
                <td>{m.p95ResponseMins.toFixed(1)}</td>
                <td style={{ color: m.criticalMissed > 0 ? 'var(--alert)' : 'var(--ink)' }}>{m.criticalMissed}</td>
                <td>{m.severityWeightedDelay.toFixed(0)}</td>
              </tr>
            )) : (
              <tr><td colSpan="5" style={{ padding: '4px 0', color: 'var(--ink-muted)' }}>No data</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
