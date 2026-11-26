"use client";

import { Briefcase, HardHat, House, Wrench, type LucideIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { cn } from "@/lib/utils";

type DemoAccount = {
  role: "MANAGER" | "ENGINEER" | "TECHNICIAN" | "CUSTOMER";
  label: string;
  hint: string;
  icon: LucideIcon;
};

// ADMIN is not offered here, and the backend refuses it too
const DEMO_ACCOUNTS: DemoAccount[] = [
  { role: "MANAGER", label: "Manager", hint: "All company sites", icon: Briefcase },
  { role: "ENGINEER", label: "Engineer", hint: "Alerts & equipment", icon: Wrench },
  { role: "TECHNICIAN", label: "Technician", hint: "Inspections", icon: HardHat },
  { role: "CUSTOMER", label: "Customer", hint: "Own sites only", icon: House },
];

/** One-click sign-in as a demo account (shown only when demo mode is on). */
export function DemoLogin() {
  const router = useRouter();
  const [pendingRole, setPendingRole] = useState<DemoAccount["role"] | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function signInAs(role: DemoAccount["role"]) {
    setError(null);
    setPendingRole(role);
    try {
      const response = await fetch("/api/auth/demo-login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ role }),
      });
      if (response.ok) {
        router.push("/dashboard");
        router.refresh();
        return;
      }
      setError("The demo is not available right now. Please try again later.");
    } catch {
      setError("Cannot reach the server. Please try again.");
    }
    setPendingRole(null);
  }

  return (
    <section aria-labelledby="demo-title">
      <div className="flex items-center gap-3 text-xs text-muted-foreground">
        <span className="h-px flex-1 bg-border" />
        <h2 id="demo-title">or explore the demo</h2>
        <span className="h-px flex-1 bg-border" />
      </div>

      <div className="mt-4 grid grid-cols-2 gap-2">
        {DEMO_ACCOUNTS.map(({ role, label, hint, icon: Icon }) => (
          <button
            key={role}
            type="button"
            onClick={() => signInAs(role)}
            disabled={pendingRole !== null}
            aria-busy={pendingRole === role}
            className={cn(
              "flex items-start gap-2.5 rounded-lg border bg-card p-3 text-left transition-colors",
              "hover:border-amber-400 hover:bg-amber-50 disabled:opacity-60 dark:hover:bg-amber-500/10",
            )}
          >
            <Icon aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-amber-600 dark:text-amber-400" />
            <span>
              <span className="block text-sm font-medium">
                {pendingRole === role ? "Signing in..." : label}
              </span>
              <span className="block text-xs text-muted-foreground">{hint}</span>
            </span>
          </button>
        ))}
      </div>

      {error && (
        <p role="alert" className="mt-3 text-sm text-red-600 dark:text-red-400">
          {error}
        </p>
      )}

      <p className="mt-3 text-xs text-muted-foreground">
        Sample company with demo data. No password needed.
      </p>
    </section>
  );
}
