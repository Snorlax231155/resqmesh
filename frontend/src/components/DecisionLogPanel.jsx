import React from 'react';

export default function DecisionLogPanel({ decisions }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, background: 'var(--panel)' }}>
      <div className="title" style={{ padding: '8px 12px', borderBottom: '1px solid var(--rule)', fontSize: '1rem', background: 'var(--bg)' }}>
        DECISION LOG
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: '8px 12px', display: 'flex', flexDirection: 'column', gap: '4px' }} className="mono">
        {decisions.map(d => (
          <div key={d.id} style={{ fontSize: '11px', lineHeight: '1.2', borderBottom: '1px dashed var(--rule)', paddingBottom: '4px' }}>
            <span style={{ color: 'var(--ink-muted)' }}>{new Date(d.timestamp).toISOString().substring(11,19)}</span><br/>
            {d.message}
          </div>
        ))}
        {decisions.length === 0 && <div style={{ color: 'var(--ink-muted)', fontSize: '11px' }}>STANDING BY...</div>}
      </div>
    </div>
  );
}
