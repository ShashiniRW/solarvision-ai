"use client";

import { useId, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { brandButtonClass, selectClass, successButtonClass } from "@/lib/styles";

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
  const id = useId(); // unique per table row, so labels point to the right input
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
    return <span className="text-muted-foreground">-</span>;
  }

  return (
    <div className="flex flex-col items-start gap-2">
      <div className="flex flex-wrap gap-2">
        {status === "SCHEDULED" && (
          <Button
            type="button"
            size="sm"
            onClick={() => send("start")}
            disabled={busy}
            className={brandButtonClass}
          >
            Start
          </Button>
        )}
        {status === "IN_PROGRESS" && (
          <Button
            type="button"
            size="sm"
            aria-expanded={panel === "complete"}
            onClick={() => togglePanel("complete")}
            disabled={busy}
            className={successButtonClass}
          >
            Complete
          </Button>
        )}
        {canCancel && (
          <Button
            type="button"
            size="sm"
            variant="outline"
            aria-expanded={panel === "cancel"}
            onClick={() => togglePanel("cancel")}
            disabled={busy}
          >
            Cancel...
          </Button>
        )}
      </div>

      {panel === "complete" && (
        <div className="w-64 space-y-3 rounded-md border bg-muted/50 p-3">
          <div className="space-y-1.5">
            <Label htmlFor={`${id}-result`}>Result</Label>
            <select
              id={`${id}-result`}
              value={result}
              onChange={(event) => setResult(event.target.value)}
              className={selectClass}
            >
              {RESULTS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor={`${id}-findings`}>Findings</Label>
            <Textarea
              id={`${id}-findings`}
              value={findings}
              onChange={(event) => setFindings(event.target.value)}
              rows={3}
              maxLength={5000}
            />
          </div>
          <Button
            type="button"
            size="sm"
            onClick={submitComplete}
            disabled={busy}
            className={successButtonClass}
          >
            Save
          </Button>
        </div>
      )}

      {panel === "cancel" && (
        <div className="w-64 space-y-3 rounded-md border bg-muted/50 p-3">
          <div className="space-y-1.5">
            <Label htmlFor={`${id}-reason`}>Reason (optional)</Label>
            <Input
              id={`${id}-reason`}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              maxLength={1000}
            />
          </div>
          <Button
            type="button"
            size="sm"
            variant="destructive"
            onClick={() => send("cancel", { reason: reason.trim() })}
            disabled={busy}
          >
            Confirm cancellation
          </Button>
        </div>
      )}

      {error && (
        <p role="alert" className="max-w-xs text-xs text-red-600 dark:text-red-400">
          {error}
        </p>
      )}
    </div>
  );
}
