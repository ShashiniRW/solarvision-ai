"use client";

import { useEffect, useRef, useState } from "react";
import { usePrefersReducedMotion } from "@/lib/use-reduced-motion";

const DURATION_MS = 900;

// "2.00 kWh" -> { number: 2, decimals: 2, suffix: " kWh" }. Text without a leading number -> null.
function parse(text: string) {
  const match = /^(-?\d+(?:\.(\d+))?)(.*)$/.exec(text);
  if (!match) return null;
  return { number: Number(match[1]), decimals: match[2]?.length ?? 0, suffix: match[3] };
}

/** Counts a number up smoothly: from 0 the first time, from the old value on live updates. */
export function CountUp({ value }: { value: string }) {
  const reducedMotion = usePrefersReducedMotion();
  const parsed = parse(value);
  const target = parsed?.number ?? null;
  const [shown, setShown] = useState(0);
  const current = useRef(0);

  useEffect(() => {
    if (target === null || reducedMotion) return;

    const from = current.current;
    const start = performance.now();
    let frame = requestAnimationFrame(function step(now) {
      const progress = Math.min((now - start) / DURATION_MS, 1);
      const eased = 1 - (1 - progress) ** 3; // fast start, gentle stop
      current.current = from + (target - from) * eased;
      setShown(current.current);
      if (progress < 1) {
        frame = requestAnimationFrame(step);
      }
    });
    return () => cancelAnimationFrame(frame);
  }, [target, reducedMotion]);

  const display =
    reducedMotion || parsed === null ? value : `${shown.toFixed(parsed.decimals)}${parsed.suffix}`;

  return (
    <>
      {/* screen readers always get the real value, never the moving numbers */}
      <span aria-hidden="true">{display}</span>
      <span className="sr-only">{value}</span>
    </>
  );
}
