'use client';

import { FormEvent, useState } from 'react';

const signals = [
  { label: 'Baseline builds', value: '0', detail: 'Awaiting first repository' },
  { label: 'Optimization runs', value: '0', detail: 'No active experiments' },
  { label: 'Critical findings', value: '—', detail: 'Evidence appears after analysis' },
];

export default function Home() {
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [sessionName, setSessionName] = useState('');
  const [url, setUrl] = useState('');
  const [message, setMessage] = useState('');
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function connectRepository(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setMessage('Registering and inspecting source...');
    setInspection(null);

    try {
      const api = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
      const repositoryResponse = await fetch(`${api}/api/v1/repositories`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, url }),
      });
      if (!repositoryResponse.ok) throw new Error('Repository registration failed.');

      const repository = await repositoryResponse.json() as { repositoryId: string };
      const inspectionResponse = await fetch(`${api}/api/v1/repositories/${repository.repositoryId}/inspect`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionName }),
      });
      const result = await inspectionResponse.json() as { inspection: Inspection } | { error: string };
      if (!inspectionResponse.ok) throw new Error('error' in result ? result.error : 'Inspection failed.');

      setInspection((result as { inspection: Inspection }).inspection);
      setMessage('Inspection complete.');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Something went wrong.');
    } finally {
      setIsSubmitting(false);
    }
  }

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
          <button className="primary-action" onClick={() => setShowForm((visible) => !visible)}>
            {showForm ? 'Close connection' : 'Connect a repository'} <span>→</span>
          </button>
          {showForm && (
            <form className="repository-form" onSubmit={connectRepository}>
              <label>Analysis session<input required value={sessionName} onChange={(event) => setSessionName(event.target.value)} placeholder="baseline review" /></label>
              <label>Source name<input required value={name} onChange={(event) => setName(event.target.value)} placeholder="public-project" /></label>
              <label>Public Git URL<input required type="url" value={url} onChange={(event) => setUrl(event.target.value)} placeholder="https://github.com/org/project" /></label>
              <button className="submit-action" disabled={isSubmitting}>{isSubmitting ? 'Inspecting...' : 'Inspect Dockerfiles'}</button>
              {message && <p className="form-message">{message}</p>}
            </form>
          )}
        </div>
        <div className="orbit" aria-hidden="true">
          <div className="orbit-ring ring-one" /><div className="orbit-ring ring-two" />
          <div className="orbit-core"><span>0</span><small>RUNS</small></div>
          <div className="orbit-node node-top">BUILD</div><div className="orbit-node node-right">SCAN</div><div className="orbit-node node-bottom">PROVE</div>
        </div>
      </section>
      {inspection && (
        <section className="inspection" aria-live="polite">
          <div className="inspection-heading"><p className="eyebrow">{inspection.sessionName} / repository evidence</p><h2>{inspection.summary.fileCount} files mapped</h2><p className="inspection-meta">{inspection.summary.lineCount.toLocaleString()} lines · {inspection.summary.directoryCount} directories</p></div>
          <div className="analysis-grid">
            <div className="analysis-block"><span className="block-label">Technology map</span><div className="category-list">{inspection.categories.map((category) => <article className="category" key={category.name}><strong>{category.name}</strong><div>{category.details.map((detail) => <span key={detail}>{detail}</span>)}</div></article>)}</div></div>
            <div className="analysis-block"><span className="block-label">Container surface</span><div className="dockerfile-list">{inspection.dockerAssets.map((asset) => <article className="dockerfile" key={asset}><strong>{asset}</strong></article>)}</div></div>
            <div className="analysis-block tree-block"><span className="block-label">Repository constellation</span><Tree node={inspection.tree} /></div>
          </div>
        </section>
      )}
      <section className="signal-grid" aria-label="System signals">
        {signals.map((signal) => <article className="signal" key={signal.label}><span>{signal.label}</span><strong>{signal.value}</strong><small>{signal.detail}</small></article>)}
      </section>
      <footer><span>TOOL-GROUNDED REASONING</span><span>BUILDKIT · TRIVY · HADOLINT · DIVE</span></footer>
    </main>
  );
}

type Inspection = {
  sessionName: string;
  summary: { fileCount: number; lineCount: number; directoryCount: number };
  categories: Array<{ name: string; details: string[] }>;
  dockerAssets: string[];
  dockerfiles: Array<{ path: string; lineCount: number; instructions: string[] }>;
  tree: TreeNode;
};

type TreeNode = { name: string; type: string; children: TreeNode[] };

function Tree({ node }: { node: TreeNode }) {
  return <details className="tree-node" open={node.type === 'directory'}><summary><span className={node.type}>{node.name}</span>{node.children.length > 0 && <small>{node.children.length}</small>}</summary>{node.children.length > 0 && <div className="tree-children">{node.children.map((child) => <Tree key={`${node.name}/${child.name}`} node={child} />)}</div>}</details>;
}
