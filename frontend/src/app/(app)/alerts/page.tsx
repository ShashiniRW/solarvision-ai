import Link from "next/link";
import { AlertActions } from "@/components/alert-actions";
import { StatusBadge } from "@/components/status-badge";
import { apiGet } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { canManageAlerts } from "@/lib/permissions";
import { getCurrentUser } from "@/lib/session";
import type { Alert } from "@/lib/types";

const PAGE_SIZE = 20;
const VALID_STATUSES = ["OPEN", "ACKNOWLEDGED", "RESOLVED"];
const FILTERS = [
  { label: "All", value: "" },
  { label: "Open", value: "OPEN" },
  { label: "Acknowledged", value: "ACKNOWLEDGED" },
  { label: "Resolved", value: "RESOLVED" },
];

export default async function AlertsPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: string; page?: string }>;
}) {
  const { status: rawStatus, page: rawPage } = await searchParams;

  // only accept known values from the address bar
  const status = rawStatus && VALID_STATUSES.includes(rawStatus) ? rawStatus : "";
  const page = rawPage && /^\d+$/.test(rawPage) ? Number(rawPage) : 0;

  const user = await getCurrentUser();
  const canManage = canManageAlerts(user.roles);

  const query = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
  if (status) {
    query.set("status", status);
  }
  const alerts = await apiGet<Alert[]>(`/api/alerts?${query.toString()}`);

  function hrefFor(targetStatus: string, targetPage: number): string {
    const params = new URLSearchParams();
    if (targetStatus) {
      params.set("status", targetStatus);
    }
    if (targetPage > 0) {
      params.set("page", String(targetPage));
    }
    const text = params.toString();
    return text ? `/alerts?${text}` : "/alerts";
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold">Alerts</h1>

      <div className="mt-4 flex flex-wrap gap-2">
        {FILTERS.map((filter) => (
          <Link
            key={filter.label}
            href={hrefFor(filter.value, 0)}
            className={`rounded-full px-3 py-1 text-sm font-medium ${
              filter.value === status
                ? "bg-amber-500 text-white"
                : "bg-white text-gray-700 ring-1 ring-gray-200 hover:bg-gray-100"
            }`}
          >
            {filter.label}
          </Link>
        ))}
      </div>

      {alerts.length === 0 ? (
        <p className="mt-6 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
          No alerts match this filter.
        </p>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
              <tr>
                <th className="px-4 py-3">Severity</th>
                <th className="px-4 py-3">Equipment</th>
                <th className="px-4 py-3">Type</th>
                <th className="px-4 py-3">Message</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Triggered</th>
                {canManage && <th className="px-4 py-3">Actions</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {alerts.map((alert) => (
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
                  <td className="px-4 py-3">{alert.alertType.replaceAll("_", " ")}</td>
                  <td className="px-4 py-3">{alert.message}</td>
                  <td className="px-4 py-3">
                    <StatusBadge value={alert.status} />
                  </td>
                  <td className="px-4 py-3">{formatDateTime(alert.triggeredAt)}</td>
                  {canManage && (
                    <td className="px-4 py-3">
                      <AlertActions alertId={alert.id} status={alert.status} />
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {(page > 0 || alerts.length === PAGE_SIZE) && (
        <div className="mt-4 flex items-center justify-between text-sm">
          {page > 0 ? (
            <Link href={hrefFor(status, page - 1)} className="text-amber-700 hover:underline">
              &larr; Previous
            </Link>
          ) : (
            <span />
          )}
          <span className="text-gray-500">Page {page + 1}</span>
          {alerts.length === PAGE_SIZE ? (
            <Link href={hrefFor(status, page + 1)} className="text-amber-700 hover:underline">
              Next &rarr;
            </Link>
          ) : (
            <span />
          )}
        </div>
      )}
    </div>
  );
}