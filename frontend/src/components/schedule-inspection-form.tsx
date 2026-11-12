"use client";

import { FormEvent, useId, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { brandButtonClass, selectClass, successButtonClass } from "@/lib/styles";

type AlertOption = { id: number; label: string };

const TYPES = [
  { value: "ROUTINE", label: "Routine" },
  { value: "ALERT_FOLLOW_UP", label: "Alert follow-up" },
  { value: "WARRANTY_CHECK", label: "Warranty check" },
];

export function ScheduleInspectionForm({
  equipmentId,
  alertOptions,
}: {
  equipmentId: number;
  alertOptions: AlertOption[];
}) {
  const router = useRouter();
  const id = useId(); // unique ids, so each <Label> is linked to its own input
  const [open, setOpen] = useState(false);
  const [type, setType] = useState("ROUTINE");
  const [scheduledAt, setScheduledAt] = useState("");
  const [alertId, setAlertId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (type === "ALERT_FOLLOW_UP" && !alertId) {
      setError("Choose the alert this inspection follows up on.");
      return;
    }

    setBusy(true);
    try {
      const response = await fetch("/api/backend/api/inspections", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          equipmentId,
          inspectionType: type,
          // the time typed in the form is Colombo time (UTC+05:30), like everywhere else in the app
          scheduledAt: `${scheduledAt}:00+05:30`,
          alertId: alertId ? Number(alertId) : null,
        }),
      });

      // the login is no longer valid: clear the cookie, then show the login page
      if (response.status === 401) {
        await fetch("/api/auth/logout", { method: "POST" });
        router.push("/login?expired=1");
        return;
      }

      if (!response.ok) {
        let message = "Could not schedule the inspection. Please try again.";
        try {
          const data = await response.json();
          if (data?.fieldErrors) {
            message = Object.values(data.fieldErrors as Record<string, string>).join(" ");
          } else if (data?.message) {
            message = data.message;
          }
        } catch {
          // the error body was not JSON: keep the default message
        }
        setError(message);
        return;
      }

      setSuccess("Inspection scheduled.");
      setOpen(false);
      setType("ROUTINE");
      setScheduledAt("");
      setAlertId("");
      router.refresh();
    } catch {
      setError("Cannot reach the server. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <div className="flex items-center gap-3">
        <Button
          type="button"
          aria-expanded={open}
          onClick={() => {
            setOpen(!open);
            setError(null);
            setSuccess(null);
          }}
          variant={open ? "outline" : "default"}
          className={open ? undefined : brandButtonClass}
        >
          {open ? "Close" : "Schedule inspection"}
        </Button>
        {success && (
          <span role="status" className="text-sm text-green-700 dark:text-green-400">
            {success}
          </span>
        )}
      </div>

      {open && (
        <form
          onSubmit={handleSubmit}
          className="mt-3 grid max-w-2xl grid-cols-1 gap-4 rounded-xl border bg-card p-4 sm:grid-cols-2"
        >
          <div className="space-y-1.5">
            <Label htmlFor={`${id}-type`}>Type</Label>
            <select
              id={`${id}-type`}
              value={type}
              onChange={(event) => setType(event.target.value)}
              className={selectClass}
            >
              {TYPES.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor={`${id}-when`}>Date and time (Colombo time)</Label>
            <Input
              id={`${id}-when`}
              type="datetime-local"
              required
              value={scheduledAt}
              onChange={(event) => setScheduledAt(event.target.value)}
            />
          </div>

          <div className="space-y-1.5 sm:col-span-2">
            <Label htmlFor={`${id}-alert`}>
              Related alert {type === "ALERT_FOLLOW_UP" ? "(required)" : "(optional)"}
            </Label>
            <select
              id={`${id}-alert`}
              value={alertId}
              onChange={(event) => setAlertId(event.target.value)}
              className={selectClass}
            >
              <option value="">None</option>
              {alertOptions.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          {error && (
            <p role="alert" className="text-sm text-red-600 sm:col-span-2 dark:text-red-400">
              {error}
            </p>
          )}

          <div className="sm:col-span-2">
            <Button type="submit" disabled={busy} className={successButtonClass}>
              {busy ? "Scheduling..." : "Schedule"}
            </Button>
          </div>
        </form>
      )}
    </div>
  );
}
