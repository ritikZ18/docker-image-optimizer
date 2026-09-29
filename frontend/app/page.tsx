const signals = [
  { label: 'Baseline builds', value: '0', detail: 'Awaiting first repository' },
  { label: 'Optimization runs', value: '0', detail: 'No active experiments' },
  { label: 'Critical findings', value: '—', detail: 'Evidence appears after analysis' },
];

export default function Home() {
  return (
    <main className="shell">
      <nav className="topbar">
        <div className="brand"><span className="brand-mark">IS</span><span>ImageSmith</span></div>
        <span className="environment">CONTROL ROOM <i /></span>
      </nav>
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Container intelligence / v1</p>
          <h1>Make every image<br /><em>earn its bytes.</em></h1>
          <p className="lede">Measure first. Generate focused hypotheses. Validate every change against the build, security, and runtime evidence.</p>
          <button className="primary-action">Connect a repository <span>→</span></button>
        </div>
        <div className="orbit" aria-hidden="true">
          <div className="orbit-ring ring-one" /><div className="orbit-ring ring-two" />
          <div className="orbit-core"><span>0</span><small>RUNS</small></div>
          <div className="orbit-node node-top">BUILD</div><div className="orbit-node node-right">SCAN</div><div className="orbit-node node-bottom">PROVE</div>
        </div>
      </section>
      <section className="signal-grid" aria-label="System signals">
        {signals.map((signal) => <article className="signal" key={signal.label}><span>{signal.label}</span><strong>{signal.value}</strong><small>{signal.detail}</small></article>)}
      </section>
      <footer><span>TOOL-GROUNDED REASONING</span><span>BUILDKIT · TRIVY · HADOLINT · DIVE</span></footer>
    </main>
  );
}
