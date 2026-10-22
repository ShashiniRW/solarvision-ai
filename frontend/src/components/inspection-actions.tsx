"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

type Props = {
  inspectionId: number;
  status: string;
  canCancel: boolean;
};

type Panel = "complete" | "cancel" | null;
type Action = "start" | "complete" | "cancel";

const RESULTS = [
  { value: "PASSED", label: "Passed" },
  { value: "FAILED", label: "Failed" },
  { value: "NEEDS_FOLLOW_UP", label: "Needs follow-up" },
];

export function InspectionActions({ inspectionId, status, canCancel }: Props) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [panel, setPanel] = useState<Panel>(null);
  const [result, setResult] = useState("PASSED");
  const [findings, setFindings] = useState("");
  const [reason, setReason] = useState("");

  async function send(action: Action, body?: object) {
    setBusy(true);
    setError(null);

    try {
      const response = await fetch(`/api/backend/api/inspections/${inspectionId}/${action}`, {
        method: "POST",
        headers: body ? { "Content-Type": "application/json" } : undefined,
        body: body ? JSON.stringify(body) : undefined,
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
          const data = await response.json();
          if (data?.message) {
            message = data.message;
          }
        } catch {
          // the error body was not JSON: keep the default message
        }
        setError(message);
        router.refresh(); // the inspection may have changed meanwhile
        return;
      }

      setPanel(null);
      router.refresh();
    } catch {
      setError("Cannot reach the server. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  function submitComplete() {
    if (result !== "PASSED" && findings.trim() === "") {
      setError("Findings are required unless the inspection passed.");
      return;
    }
    send("complete", { result, findings: findings.trim() || null });
  }

  function togglePanel(target: Exclude<Panel, null>) {
    setError(null);
    setPanel(panel === target ? null : target);
  }

  if (status === "COMPLETED" || status === "CANCELLED") {
    return <span className="text-gray-400">-</span>;
  }

  return (
    <div className="flex flex-col items-start gap-2">
      <div className="flex flex-wrap gap-2">
        {status === "SCHEDULED" && (
          <button
            onClick={() => send("start")}
            disabled={busy}
            className="rounded-md bg-amber-500 px-2.5 py-1 text-xs font-medium text-white hover:bg-amber-600 disabled:opacity-50"
          >
            Start
          </button>
        )}
        {status === "IN_PROGRESS" && (
          <button
            onClick={() => togglePanel("complete")}
            disabled={busy}
            className="rounded-md bg-green-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
          >
            Complete
          </button>
        )}
        {canCancel && (
          <button
            onClick={() => togglePanel("cancel")}
            disabled={busy}
            className="rounded-md border border-gray-300 px-2.5 py-1 text-xs font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50"
          >
            Cancel...
          </button>
        )}
      </div>

      {panel === "complete" && (
        <div className="w-64 space-y-2 rounded-md border border-gray-200 bg-gray-50 p-3">
          <label className="block text-xs font-medium text-gray-700">
            Result
            <select
              value={result}
              onChange={(event) => setResult(event.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-2 py-1 text-sm"
            >
              {RESULTS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs font-medium text-gray-700">
            Findings
            <textarea
              value={findings}
              onChange={(event) => setFindings(event.target.value)}
              rows={3}
              maxLength={5000}
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-2 py-1 text-sm"
            />
          </label>
          <button
            onClick={submitComplete}
            disabled={busy}
            className="rounded-md bg-green-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
          >
            Save
          </button>
        </div>
      )}

      {panel === "cancel" && (
        <div className="w-64 space-y-2 rounded-md border border-gray-200 bg-gray-50 p-3">
          <label className="block text-xs font-medium text-gray-700">
            Reason (optional)
            <input
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              maxLength={1000}
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-2 py-1 text-sm"
            />
          </label>
          <button
            onClick={() => send("cancel", { reason: reason.trim() })}
            disabled={busy}
            className="rounded-md bg-red-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
          >
            Confirm cancellation
          </button>
        </div>
      )}

      {error && (
        <p role="alert" className="max-w-xs text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}