import React, { useEffect, useRef } from 'react';

export default function EventTape({ events }) {
  const tapeRef = useRef(null);

  useEffect(() => {
    if (tapeRef.current) {
      tapeRef.current.scrollLeft = tapeRef.current.scrollWidth;
    }
  }, [events]);

  return (
    <div className="panel" style={{ height: '80px', display: 'flex', flexDirection: 'column', borderTop: '1px solid var(--rule)' }}>
      <div className="title" style={{ fontSize: '0.8rem', color: 'var(--ink-muted)', marginBottom: '4px' }}>TELEMETRY TAPE</div>
      <div ref={tapeRef} style={{ flex: 1, display: 'flex', alignItems: 'center', overflowX: 'auto', gap: '2px', paddingBottom: '4px' }}>
        {events.map((e, i) => {
          let color = 'var(--ink)';
          if (e.type === 'DISRUPTION') color = 'var(--alert)';
          else if (e.type === 'MISSION') color = 'var(--caution)';
          else if (e.type === 'AGENT') color = 'var(--signal)';
          else if (e.type === 'DECISION') color = 'var(--hi-vis)';
          
          return (
            <div key={e.id} style={{ height: '30px', width: '3px', background: color, flexShrink: 0, position: 'relative' }} title={e.message}>
              {i % 10 === 0 && <div className="mono" style={{ position: 'absolute', top: '-15px', left: 0, fontSize: '8px', color: 'var(--ink-muted)' }}>{new Date(e.timestamp).getSeconds()}s</div>}
            </div>
          );
        })}
      </div>
    </div>
  );
}
