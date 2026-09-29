'use client';

import { useEffect, useRef, useState } from 'react';
import { effect, frameLoop, init, surface, type Gpu } from 'vgpu';

const shaderSource = {
  version: 1 as const,
  wgsl: `
    struct Frame { time: f32 };
    @group(0) @binding(0) var<uniform> frame: Frame;

    @fragment
    fn fs_main(@location(0) uv: vec2f) -> @location(0) vec4f {
      let wave = sin((uv.x + frame.time * 0.04) * 9.0) * 0.08;
      let glow = smoothstep(0.9, 0.1, distance(uv, vec2f(0.72 + wave, 0.22)));
      let green = vec3f(0.48, 0.62, 0.22) * glow;
      let blue = vec3f(0.12, 0.36, 0.34) * smoothstep(0.85, 0.0, distance(uv, vec2f(0.18, 0.76)));
      return vec4f(vec3f(0.04, 0.07, 0.07) + green + blue, 1.0);
    }
  `,
};

type Props = {
  fileCount: number;
  lineCount: number;
  language: string;
};

export default function AnalysisSurface({ fileCount, lineCount, language }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [fallback, setFallback] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let disposed = false;
    let gpu: Gpu | undefined;

    const start = async () => {
      try {
        gpu = await init();
        if (disposed) return;
        const output = surface(gpu, canvas, { dpr: [1, 2] });
        const shader = effect(gpu, shaderSource, { set: { frame: { time: 0 } } });
        await shader.compile({ colors: [output.format] });
        if (disposed) return;
        const started = performance.now();
        frameLoop(gpu, (frame) => {
          shader.set({ frame: { time: (performance.now() - started) / 1000 } });
          frame.pass(output, shader);
        }, { fps: 30 });
      } catch {
        if (!disposed) setFallback(true);
      }
    };

    void start();
    return () => {
      disposed = true;
      gpu?.dispose();
    };
  }, []);

  return (
    <div className={`analysis-surface ${fallback ? 'analysis-surface-fallback' : ''}`}>
      {!fallback && <canvas ref={canvasRef} aria-hidden="true" />}
      <div className="surface-copy">
        <span>REPOSITORY SIGNAL</span>
        <strong>{lineCount.toLocaleString()}</strong>
        <small>lines / {fileCount} files / {language || 'mixed stack'}</small>
      </div>
    </div>
  );
}