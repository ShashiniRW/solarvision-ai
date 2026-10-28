import Link from "next/link";
import { AutoRefresh } from "@/components/auto-refresh";
import { StatusBadge } from "@/components/status-badge";
import { SummaryCard } from "@/components/summary-card";
import { apiGet } from "@/lib/api";
import { currentTimeLabel, formatDateTime } from "@/lib/format";
import { getCurrentUser } from "@/lib/session";
import type { Alert, DashboardSummary } from "@/lib/types";

export default async function DashboardPage() {
  const user = await getCurrentUser();

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
        <p className="text-xs text-gray-500">
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
          hint="All equipment, Colombo day"
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
          <Link href="/alerts" className="text-sm text-amber-700 hover:underline">
            View all alerts &rarr;
          </Link>
        </div>

        {recentAlerts.length === 0 ? (
          <p className="mt-3 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
            No unresolved alerts. Everything looks fine.
          </p>
        ) : (
          <div className="mt-3 overflow-x-auto rounded-lg border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
                <tr>
                  <th className="px-4 py-3">Severity</th>
                  <th className="px-4 py-3">Equipment</th>
                  <th className="px-4 py-3">Message</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Triggered</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {recentAlerts.map((alert) => (
                  <tr key={alert.id}>
                    <td className="px-4 py-3">
                      <StatusBadge value={alert.severity} />
                    </td>
                    <td className="px-4 py-3">
                      <Link
                        href={`/equipment/${alert.equipmentId}`}
                        className="text-amber-700 hover:underline"
                      >
                        {alert.equipmentType} · {alert.equipmentSerialNumber ?? alert.equipmentId}
                      </Link>
                    </td>
                    <td className="px-4 py-3">{alert.message}</td>
                    <td className="px-4 py-3">
                      <StatusBadge value={alert.status} />
                    </td>
                    <td className="px-4 py-3">{formatDateTime(alert.triggeredAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}