"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { linkClass } from "@/lib/styles";
import type { LiveEvent } from "@/lib/types";

type Status = "connecting" | "live" | "offline";

const MIN_REFRESH_GAP_MS = 5_000; // at most one page refresh every 5 seconds
const RETRY_MS = 15_000; // if the browser gives up, try again after 15 seconds
const NOTICE_MS = 10_000; // how long a critical-alert notice stays visible

const STATUS_STYLES: Record<Status, { label: string; dot: string; title: string }> = {
  live: {
    label: "Live",
    dot: "bg-emerald-500",
    title: "Pages update automatically when something changes",
  },
  connecting: {
    label: "Connecting…",
    dot: "bg-amber-500 animate-pulse",
    title: "Connecting to live updates",
  },
  offline: {
    label: "Offline",
    dot: "bg-muted-foreground",
    title: "Live updates are paused. Trying to reconnect.",
  },
};

// One live connection for the whole app. On every event the current page re-loads its data.
export function LiveUpdates() {
  const router = useRouter();
  const [status, setStatus] = useState<Status>("connecting");
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    let source: EventSource | null = null;
    let retryTimer: ReturnType<typeof setTimeout> | undefined;
    let refreshTimer: ReturnType<typeof setTimeout> | undefined;
    let lastRefresh = 0;
    let missedWhileHidden = false;
    let wasConnected = false;

    // many events in a short time -> only one refresh (throttle)
    function refreshSoon() {
      if (document.hidden) {
        missedWhileHidden = true; // refresh when the user comes back to the tab
        return;
      }
      if (refreshTimer) return; // a refresh is already planned
      const wait = Math.max(0, lastRefresh + MIN_REFRESH_GAP_MS - Date.now());
      refreshTimer = setTimeout(() => {
        refreshTimer = undefined;
        lastRefresh = Date.now();
        router.refresh();
      }, wait);
    }

    function handleMessage(message: MessageEvent<string>) {
      let event: LiveEvent;
      try {
        event = JSON.parse(message.data) as LiveEvent;
      } catch {
        return; // ignore anything that is not our JSON
      }
      if (event.type === "ALERT_CREATED" && event.severity === "CRITICAL") {
        setNotice(event.message ?? "A new critical alert was raised.");
      }
      refreshSoon();
    }

    function connect() {
      source = new EventSource("/api/events");
      source.onopen = () => {
        setStatus("live");
        if (wasConnected) refreshSoon(); // catch up on anything missed while disconnected
        wasConnected = true;
      };
      source.onmessage = handleMessage;
      source.onerror = () => {
        if (source?.readyState === EventSource.CLOSED) {
          // the browser stopped trying (e.g. backend down): we try again ourselves
          setStatus("offline");
          retryTimer = setTimeout(connect, RETRY_MS);
        } else {
          setStatus("connecting"); // the browser is reconnecting by itself
        }
      };
    }

    function handleVisibility() {
      if (!document.hidden && missedWhileHidden) {
        missedWhileHidden = false;
        refreshSoon();
      }
    }

    connect();
    document.addEventListener("visibilitychange", handleVisibility);

    return () => {
      source?.close();
      clearTimeout(retryTimer);
      clearTimeout(refreshTimer);
      document.removeEventListener("visibilitychange", handleVisibility);
    };
  }, [router]);

  // hide the notice automatically after a few seconds
  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(() => setNotice(null), NOTICE_MS);
    return () => clearTimeout(timer);
  }, [notice]);

  const style = STATUS_STYLES[status];

  return (
    <>
      <div
        role="status"
        title={style.title}
        className="fixed bottom-4 right-4 z-50 flex items-center gap-2 rounded-full border bg-card px-3 py-1 text-xs text-muted-foreground shadow-sm"
      >
        <span className={`size-2 rounded-full ${style.dot}`} aria-hidden="true" />
        {style.label}
      </div>

      {notice && (
        <div
          role="alert"
          className="fixed right-4 top-4 z-50 w-80 rounded-xl border border-red-300 bg-card p-4 shadow-lg dark:border-red-900"
        >
          <p className="text-sm font-semibold text-red-600 dark:text-red-400">New critical alert</p>
          <p className="mt-1 text-sm">{notice}</p>
          <div className="mt-3 flex gap-4 text-sm">
            <Link href="/alerts" className={linkClass} onClick={() => setNotice(null)}>
              View alerts
            </Link>
            <button
              type="button"
              className="text-muted-foreground hover:text-foreground"
              onClick={() => setNotice(null)}
            >
              Dismiss
            </button>
          </div>
        </div>
      )}
    </>
  );
}
