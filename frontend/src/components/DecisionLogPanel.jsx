import React, { useState, useEffect, useRef } from 'react';

export default function DecisionLogPanel({ decisions }) {
  const [filter, setFilter] = useState('ALL');
  const scrollRef = useRef(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [decisions]);

  const filteredDecisions = decisions.filter(d => {
    if (filter === 'ALL') return true;
    const msg = (d.message || '').toUpperCase();
    if (filter === 'ASSIGN') return msg.includes('ASSIGN') || msg.includes('DISPATCH');
    if (filter === 'RISK') return msg.includes('RISK') || msg.includes('ESCALAT') || msg.includes('WARN');
    return true;
  });

  const getBadgeStyle = (msg) => {
    const m = (msg || '').toUpperCase();
    if (m.includes('ASSIGN') || m.includes('DISPATCH')) return { bg: '#0B4FA8', text: '#FFFFFF', label: 'ASSIGNED' };
    if (m.includes('RISK') || m.includes('ESCALAT')) return { bg: '#E4002B', text: '#FFFFFF', label: 'AT RISK' };
    if (m.includes('DISRUPT') || m.includes('BLOCK')) return { bg: '#FF8A00', text: '#000000', label: 'BLOCKAGE' };
    if (m.includes('SYSTEM') || m.includes('REGISTER')) return { bg: '#2E7D32', text: '#FFFFFF', label: 'SYSTEM' };
    return { bg: 'var(--rule)', text: 'var(--ink)', label: 'DECISION' };
  };

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, background: 'var(--panel)' }}>
      {/* Header & Filter Bar */}
      <div style={{ padding: '8px 12px', borderBottom: '1px solid var(--rule)', background: 'var(--bg)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="title" style={{ fontSize: '0.95rem', letterSpacing: '0.05em' }}>
          DECISION LOG <span className="mono" style={{ fontSize: '0.8rem', color: 'var(--ink-muted)' }}>({decisions.length})</span>
        </div>
        <div style={{ display: 'flex', gap: '4px' }}>
          {['ALL', 'ASSIGN', 'RISK'].map(f => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              style={{
                fontSize: '9px',
                padding: '1px 5px',
                background: filter === f ? 'var(--ink)' : 'transparent',
                color: filter === f ? 'var(--bg)' : 'var(--ink)',
                border: '1px solid var(--rule)'
              }}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      {/* Decision Entries */}
      <div 
        ref={scrollRef}
        style={{ flex: 1, overflowY: 'auto', padding: '8px 12px', display: 'flex', flexDirection: 'column', gap: '6px' }} 
        className="mono"
      >
        {filteredDecisions.map(d => {
          const badge = getBadgeStyle(d.message);
          const timeStr = new Date(d.timestamp).toISOString().substring(11, 19);
          return (
            <div 
              key={d.id} 
              style={{ 
                fontSize: '11px', 
                lineHeight: '1.35', 
                borderBottom: '1px solid var(--rule)', 
                paddingBottom: '6px',
                display: 'flex',
                flexDirection: 'column',
                gap: '2px'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '10px', color: 'var(--ink-muted)', fontWeight: 'bold' }}>[{timeStr}]</span>
                <span style={{ 
                  background: badge.bg, 
                  color: badge.text, 
                  fontSize: '9px', 
                  padding: '1px 4px', 
                  borderRadius: '2px', 
                  fontWeight: 'bold',
                  letterSpacing: '0.05em'
                }}>
                  {badge.label}
                </span>
              </div>
              <div style={{ color: 'var(--ink)', wordBreak: 'break-word' }}>
                {d.message}
              </div>
            </div>
          );
        })}
        {filteredDecisions.length === 0 && (
          <div style={{ color: 'var(--ink-muted)', fontSize: '11px', textAlign: 'center', marginTop: '20px' }}>
            STANDING BY — NO DECISION LOGS YET
          </div>
        )}
      </div>
    </div>
  );
}

