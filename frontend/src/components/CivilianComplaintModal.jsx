import React, { useState } from 'react';

const PRESET_LOCATIONS = [
  { name: 'Times Square / Midtown (Node 474)', pickup: '474fc03b', dest: '539afc5b' },
  { name: 'Lower East Side / FDR Drive (Node 120)', pickup: '120fc03b', dest: '86b0db1a' },
  { name: 'Wall St / Financial District (Node 86B)', pickup: '86b0db1a', dest: '474fc03b' },
  { name: 'Brooklyn Bridge Approach (Node 310)', pickup: '310fc03b', dest: '120fc03b' },
  { name: 'Tribeca / Canal St (Node 539)', pickup: '539afc5b', dest: '86b0db1a' },
  { name: 'SoHo / Houston St (Node 0dd97ecc)', pickup: '0dd97ecc', dest: '8976ddfe' }
];

export default function CivilianComplaintModal({ isOpen, onClose, onSubmitComplaint, nodes }) {
  const [reporterName, setReporterName] = useState('');
  const [phone, setPhone] = useState('');
  const [priority, setPriority] = useState('HIGH');
  const [selectedPreset, setSelectedPreset] = useState(0);
  const [description, setDescription] = useState('');
  const [customPickup, setCustomPickup] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    let pickupNodeId = customPickup.trim();
    let destNodeId = '539afc5b';

    if (!pickupNodeId) {
      const preset = PRESET_LOCATIONS[selectedPreset] || PRESET_LOCATIONS[0];
      pickupNodeId = preset.pickup;
      destNodeId = preset.dest;
    }

    // Fallback if node not found
    if (nodes && nodes.length > 0) {
      const foundSrc = nodes.find(n => n.id === pickupNodeId || n.id.startsWith(pickupNodeId));
      if (foundSrc) pickupNodeId = foundSrc.id;
      else pickupNodeId = nodes[0].id;

      const foundDst = nodes.find(n => n.id === destNodeId);
      if (foundDst) destNodeId = foundDst.id;
      else destNodeId = nodes[nodes.length - 1].id;
    }

    const complaintData = {
      reporterName: reporterName || 'Anonymous Civilian',
      phone: phone || '911-DISPATCH',
      priority,
      pickupNodeId,
      destinationNodeId: destNodeId,
      description: description || 'Emergency flood rescue requested by civilian complaint.'
    };

    await onSubmitComplaint(complaintData);
    setIsSubmitting(false);
    onClose();
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      width: '100vw',
      height: '100vh',
      background: 'rgba(0,0,0,0.75)',
      zIndex: 9999,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backdropFilter: 'blur(4px)'
    }}>
      <div style={{
        background: '#14140F',
        color: '#FFF',
        border: '2px solid #E4002B',
        boxShadow: '0 8px 32px rgba(228,0,43,0.4)',
        width: '520px',
        maxWidth: '90vw',
        borderRadius: '6px',
        padding: '20px',
        fontFamily: 'var(--font-mono)'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #333', paddingBottom: '12px', marginBottom: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '18px' }}>🚨</span>
            <span style={{ color: '#E4002B', fontWeight: 'bold', fontSize: '14px', letterSpacing: '0.05em' }}>
              CIVILIAN EMERGENCY COMPLAINT & SOS INTAKE
            </span>
          </div>
          <button onClick={onClose} style={{ background: 'transparent', color: '#999', border: 'none', cursor: 'pointer', fontSize: '16px', fontWeight: 'bold' }}>✕</button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div>
              <label style={{ fontSize: '11px', color: '#AAA', display: 'block', marginBottom: '4px' }}>REPORTER NAME / VICTIM</label>
              <input 
                type="text" 
                placeholder="e.g. Maria Santos"
                value={reporterName}
                onChange={e => setReporterName(e.target.value)}
                style={{ width: '100%', background: '#222', border: '1px solid #444', color: '#FFF', padding: '6px 10px', fontSize: '12px', borderRadius: '4px' }}
              />
            </div>

            <div>
              <label style={{ fontSize: '11px', color: '#AAA', display: 'block', marginBottom: '4px' }}>CONTACT PHONE / CHANNEL</label>
              <input 
                type="text" 
                placeholder="e.g. +1 (555) 019-2834"
                value={phone}
                onChange={e => setPhone(e.target.value)}
                style={{ width: '100%', background: '#222', border: '1px solid #444', color: '#FFF', padding: '6px 10px', fontSize: '12px', borderRadius: '4px' }}
              />
            </div>
          </div>

          <div>
            <label style={{ fontSize: '11px', color: '#AAA', display: 'block', marginBottom: '4px' }}>EMERGENCY PRIORITY LEVEL</label>
            <select
              value={priority}
              onChange={e => setPriority(e.target.value)}
              style={{ width: '100%', background: '#222', border: '1px solid #E4002B', color: '#FFF', padding: '6px 10px', fontSize: '12px', fontWeight: 'bold', borderRadius: '4px' }}
            >
              <option value="CRITICAL">🔴 CRITICAL (Life Threatening / Rising Waters)</option>
              <option value="HIGH">🟠 HIGH (Trapped / Immediate Rescue Needed)</option>
              <option value="NORMAL">🔵 NORMAL (Standard Assistance / Medical Evac)</option>
              <option value="LOW">🟢 LOW (Property Risk / Non-Urgent)</option>
            </select>
          </div>

          <div>
            <label style={{ fontSize: '11px', color: '#AAA', display: 'block', marginBottom: '4px' }}>INCIDENT LOCATION / GRAPH NODE</label>
            <select
              value={selectedPreset}
              onChange={e => setSelectedPreset(Number(e.target.value))}
              style={{ width: '100%', background: '#222', border: '1px solid #444', color: '#FFF', padding: '6px 10px', fontSize: '12px', borderRadius: '4px', marginBottom: '6px' }}
            >
              {PRESET_LOCATIONS.map((loc, idx) => (
                <option key={idx} value={idx}>{loc.name}</option>
              ))}
            </select>
            <input 
              type="text" 
              placeholder="Or enter custom Graph Node ID..."
              value={customPickup}
              onChange={e => setCustomPickup(e.target.value)}
              style={{ width: '100%', background: '#111', border: '1px dashed #555', color: '#D4E82B', padding: '6px 10px', fontSize: '11px', borderRadius: '4px' }}
            />
          </div>

          <div>
            <label style={{ fontSize: '11px', color: '#AAA', display: 'block', marginBottom: '4px' }}>COMPLAINT DETAILS & SITUATION REPORT</label>
            <textarea
              rows={3}
              placeholder="Describe emergency, number of victims, water level, hazards..."
              value={description}
              onChange={e => setDescription(e.target.value)}
              style={{ width: '100%', background: '#222', border: '1px solid #444', color: '#FFF', padding: '8px 10px', fontSize: '12px', borderRadius: '4px', resize: 'vertical' }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '8px' }}>
            <button 
              type="button" 
              onClick={onClose}
              style={{ background: '#333', color: '#CCC', border: 'none', padding: '8px 16px', borderRadius: '4px', cursor: 'pointer', fontSize: '12px' }}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              disabled={isSubmitting}
              style={{ background: '#E4002B', color: '#FFF', border: 'none', padding: '8px 20px', fontWeight: 'bold', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', boxShadow: '0 0 12px rgba(228,0,43,0.6)' }}
            >
              {isSubmitting ? 'Submitting...' : '🚨 SUBMIT SOS COMPLAINT'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
