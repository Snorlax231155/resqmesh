import React, { useEffect, useRef } from 'react';
import { Radio, AlertOctagon, ArrowRightCircle } from 'lucide-react';

export default function LiveFeed({ events }) {
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [events]);

  return (
    <div className="glass-panel" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '1rem', borderBottom: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Radio className="animate-pulse" size={18} color="var(--accent-cyan)" />
        <h2 className="title-secondary">Live Event Stream</h2>
      </div>
      
      <div style={{ flex: 1, overflowY: 'auto', padding: '1rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {events.length === 0 ? (
          <p className="text-small" style={{ textAlign: 'center', marginTop: '2rem' }}>Waiting for signals...</p>
        ) : (
          events.map((ev, i) => (
            <div key={i} className="animate-slide-in glass-card" style={{ padding: '0.75rem', display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
              {ev.type === 'DISRUPTION' ? (
                <AlertOctagon size={20} color="var(--accent-amber)" style={{ flexShrink: 0, marginTop: '2px' }} />
              ) : (
                <ArrowRightCircle size={20} color="var(--accent-cyan)" style={{ flexShrink: 0, marginTop: '2px' }} />
              )}
              <div>
                <p style={{ fontSize: '0.85rem', fontWeight: 500 }}>{ev.message}</p>
                <p className="text-small" style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>
                  {new Date(ev.timestamp).toLocaleTimeString()} &bull; {ev.type}
                </p>
              </div>
            </div>
          ))
        )}
        <div ref={bottomRef} />
      </div>
    </div>
  );
}
