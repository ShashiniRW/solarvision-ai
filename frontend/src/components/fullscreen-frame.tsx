"use client";

import { Maximize2, Minimize2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils";

/**
 * Wraps a map or 3D view and adds a full-screen button (top right).
 * Uses the browser's Fullscreen API. Where that is missing (iPhone Safari), it falls back
 * to a layer that covers the whole window. Esc exits in both cases.
 */
export function FullscreenFrame({
  label,
  className,
  children,
}: {
  label: string;
  className?: string;
  children: React.ReactNode;
}) {
  const frame = useRef<HTMLDivElement>(null);
  const [nativeFull, setNativeFull] = useState(false);
  const [fallbackFull, setFallbackFull] = useState(false);
  const full = nativeFull || fallbackFull;

  // the browser reports when real full screen starts or ends (also when the user presses Esc)
  useEffect(() => {
    const onChange = () => setNativeFull(document.fullscreenElement === frame.current);
    document.addEventListener("fullscreenchange", onChange);
    return () => document.removeEventListener("fullscreenchange", onChange);
  }, []);

  // fallback mode: Esc closes it, and the page behind does not scroll
  useEffect(() => {
    if (!fallbackFull) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setFallbackFull(false);
    };
    document.addEventListener("keydown", onKeyDown);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = "";
    };
  }, [fallbackFull]);

  async function toggle() {
    const element = frame.current;
    if (!element) return;

    if (document.fullscreenElement) {
      await document.exitFullscreen();
      return;
    }
    if (fallbackFull) {
      setFallbackFull(false);
      return;
    }
    if (document.fullscreenEnabled) {
      try {
        await element.requestFullscreen();
        return;
      } catch {
        // the browser refused: use the fallback below
      }
    }
    setFallbackFull(true);
  }

  const buttonLabel = full ? "Exit full screen" : `Show the ${label} in full screen`;

  return (
    <div
      ref={frame}
      // isolate: keeps the map's and 3D view's layers below the app's menus
      className={cn(
        "relative isolate bg-background",
        fallbackFull ? "fixed inset-0 z-50" : className,
        nativeFull && "rounded-none border-0",
      )}
    >
      {children}
      <button
        type="button"
        onClick={toggle}
        aria-label={buttonLabel}
        title={buttonLabel}
        // z-1001: above Leaflet's own controls (z-index 1000)
        className="absolute top-2 right-2 z-1001 inline-flex size-9 items-center justify-center rounded-md border bg-card/90 text-foreground shadow-sm backdrop-blur hover:bg-muted"
      >
        {full ? <Minimize2 className="size-4" /> : <Maximize2 className="size-4" />}
      </button>
    </div>
  );
}
