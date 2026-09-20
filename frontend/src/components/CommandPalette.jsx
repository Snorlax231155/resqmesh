import React, { useState, useEffect, useRef } from 'react';

export default function CommandPalette({ onClose, onExecute }) {
  const [input, setInput] = useState("");
  const inputRef = useRef(null);

  useEffect(() => {
    if (inputRef.current) inputRef.current.focus();
  }, []);

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      onClose();
    } else if (e.key === 'Enter') {
      onExecute(input);
      onClose();
    }
  };

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', background: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div className="panel" style={{ width: '500px', background: 'var(--bg)', border: '2px solid var(--ink)' }}>
        <div className="title" style={{ borderBottom: '1px solid var(--rule)', paddingBottom: '8px', marginBottom: '8px' }}>COMMAND TERMINAL</div>
        <input 
          ref={inputRef}
          type="text" 
          className="mono"
          style={{ width: '100%', background: 'var(--panel)', border: '1px solid var(--rule)', padding: '8px', color: 'var(--ink)', outline: 'none', fontSize: '1rem', textTransform: 'uppercase' }}
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="e.g. load urban_flood | theme night | speed 10x | policy els"
        />
        <div className="mono" style={{ fontSize: '10px', color: 'var(--ink-muted)', marginTop: '8px' }}>Press ESC to close, Enter to execute.</div>
      </div>
    </div>
  );
}
