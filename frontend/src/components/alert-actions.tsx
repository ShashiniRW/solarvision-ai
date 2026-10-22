"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

type Action = "acknowledge" | "resolve";

export function AlertActions({ alertId, status }: { alertId: number; status: string }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function run(action: Action) {
    setBusy(true);
    setError(null);

    try {
      const response = await fetch(`/api/backend/api/alerts/${alertId}/${action}`, {
        method: "POST",
      });

      // the login is no longer valid: clear the cookie, then show the login page
      if (response.status === 401) {
        await fetch("/api/auth/logout", { method: "POST" });
        router.push("/login?expired=1");
        return;
      }

      if (!response.ok) {
        let message = "The action failed. Please try again.";
        try {
          const body = await response.json();
          if (body?.message) {
            message = body.message;
          }
        } catch {
          // the error body was not JSON: keep the default message
        }
        setError(message);
      }

      // reload the table either way: after an error the alert may have changed meanwhile
      router.refresh();
    } catch {
      setError("Cannot reach the server. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  if (status === "RESOLVED") {
    return <span className="text-gray-400">-</span>;
  }

  return (
    <div className="flex flex-col items-start gap-1">
      <div className="flex gap-2">
        {status === "OPEN" && (
          <button
            onClick={() => run("acknowledge")}
            disabled={busy}
            className="rounded-md border border-amber-500 px-2.5 py-1 text-xs font-medium text-amber-700 hover:bg-amber-50 disabled:opacity-50"
          >
            Acknowledge
          </button>
        )}
        <button
          onClick={() => run("resolve")}
          disabled={busy}
          className="rounded-md bg-green-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
        >
          Resolve
        </button>
      </div>
      {error && (
        <p role="alert" className="max-w-xs text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}