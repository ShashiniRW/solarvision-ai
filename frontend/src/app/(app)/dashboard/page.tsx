import Link from "next/link";
import { AutoRefresh } from "@/components/auto-refresh";
import { EmptyState } from "@/components/empty-state";
import { StatusBadge } from "@/components/status-badge";
import { SummaryCard } from "@/components/summary-card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { apiGet } from "@/lib/api";
import { currentTimeLabel, formatDateTime } from "@/lib/format";
import { getCurrentUser } from "@/lib/session";
import { linkClass } from "@/lib/styles";
import type { Alert, DashboardSummary } from "@/lib/types";

export default async function DashboardPage() {
  const user = await getCurrentUser();

  // ADMIN sees every project; everyone else only the projects they belong to
  const seesAllProjects = user.roles.includes("ADMIN");

  // all three requests run at the same time
  const [summary, openAlerts, acknowledgedAlerts] = await Promise.all([
    apiGet<DashboardSummary>("/api/dashboard/summary"),
    apiGet<Alert[]>("/api/alerts?status=OPEN&size=5"),
    apiGet<Alert[]>("/api/alerts?status=ACKNOWLEDGED&size=5"),
  ]);

  // "unresolved" = open + acknowledged. An acknowledged alert is not fixed yet.
  const unresolvedAlerts = summary.openAlerts + summary.acknowledgedAlerts;
  const recentAlerts = [...openAlerts, ...acknowledgedAlerts]
    .sort((a, b) => new Date(b.triggeredAt).getTime() - new Date(a.triggeredAt).getTime())
    .slice(0, 5);

  const alertTone =
    summary.unresolvedCriticalAlerts > 0 ? "danger" : unresolvedAlerts > 0 ? "warning" : "normal";
  const equipmentTone = summary.equipmentFaulty > 0 ? "danger" : "normal";
  const inspectionTone = summary.overdueInspections > 0 ? "warning" : "normal";

  return (
    <div>
      <AutoRefresh seconds={30} />

      <div className="flex flex-wrap items-end justify-between gap-2">
        <h1 className="text-2xl font-semibold">Welcome back, {user.firstName}</h1>
        <p className="text-xs text-muted-foreground">
          Updated {currentTimeLabel()} (Colombo time) · refreshes every 30 seconds
        </p>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          label="Unresolved alerts"
          value={String(unresolvedAlerts)}
          hint={`${summary.openAlerts} open · ${summary.acknowledgedAlerts} acknowledged · ${summary.unresolvedCriticalAlerts} critical`}
          href="/alerts"
          tone={alertTone}
        />
        <SummaryCard
          label="Scheduled inspections"
          value={String(summary.scheduledInspections)}
          hint={`${summary.inProgressInspections} in progress · ${summary.overdueInspections} overdue`}
          href="/inspections?status=SCHEDULED"
          tone={inspectionTone}
        />
        <SummaryCard
          label="Energy today"
          value={`${summary.energyTodayKwh.toFixed(2)} kWh`}
          hint={seesAllProjects ? "All equipment, Colombo day" : "Your equipment, Colombo day"}
        />
        <SummaryCard
          label="Active equipment"
          value={`${summary.equipmentActive} / ${summary.equipmentTotal}`}
          hint={`${summary.equipmentFaulty} faulty`}
          href="/projects"
          tone={equipmentTone}
        />
      </div>

      <section className="mt-8">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Recent unresolved alerts</h2>
          <Link href="/alerts" className={`text-sm ${linkClass}`}>
            View all alerts &rarr;
          </Link>
        </div>

        <div className="mt-3">
          {recentAlerts.length === 0 ? (
            <EmptyState>No unresolved alerts. Everything looks fine.</EmptyState>
          ) : (
            <div className="rounded-xl border bg-card">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Severity</TableHead>
                    <TableHead>Equipment</TableHead>
                    <TableHead>Message</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Triggered</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {recentAlerts.map((alert) => (
                    <TableRow key={alert.id}>
                      <TableCell>
                        <StatusBadge value={alert.severity} />
                      </TableCell>
                      <TableCell>
                        <Link href={`/equipment/${alert.equipmentId}`} className={linkClass}>
                          {alert.equipmentType} · {alert.equipmentSerialNumber ?? alert.equipmentId}
                        </Link>
                      </TableCell>
                      <TableCell className="min-w-64 whitespace-normal">{alert.message}</TableCell>
                      <TableCell>
                        <StatusBadge value={alert.status} />
                      </TableCell>
                      <TableCell className="tabular-nums">{formatDateTime(alert.triggeredAt)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
