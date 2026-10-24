"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

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
        <button
          type="button"
          onClick={() => {
            setOpen(!open);
            setError(null);
            setSuccess(null);
          }}
          className="rounded-md bg-amber-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-amber-600"
        >
          {open ? "Close" : "Schedule inspection"}
        </button>
        {success && <span className="text-sm text-green-700">{success}</span>}
      </div>

      {open && (
        <form
          onSubmit={handleSubmit}
          className="mt-3 grid max-w-2xl grid-cols-1 gap-4 rounded-lg border border-gray-200 bg-white p-4 sm:grid-cols-2"
        >
          <label className="block text-sm font-medium text-gray-700">
            Type
            <select
              value={type}
              onChange={(event) => setType(event.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm"
            >
              {TYPES.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>

          <label className="block text-sm font-medium text-gray-700">
            Date and time (Colombo time)
            <input
              type="datetime-local"
              required
              value={scheduledAt}
              onChange={(event) => setScheduledAt(event.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm"
            />
          </label>

          <label className="block text-sm font-medium text-gray-700 sm:col-span-2">
            Related alert {type === "ALERT_FOLLOW_UP" ? "(required)" : "(optional)"}
            <select
              value={alertId}
              onChange={(event) => setAlertId(event.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm"
            >
              <option value="">None</option>
              {alertOptions.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          {error && (
            <p role="alert" className="text-sm text-red-600 sm:col-span-2">
              {error}
            </p>
          )}

          <div className="sm:col-span-2">
            <button
              type="submit"
              disabled={busy}
              className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
            >
              {busy ? "Scheduling..." : "Schedule"}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}