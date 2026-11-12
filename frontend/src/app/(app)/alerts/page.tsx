import Link from "next/link";
import { AlertActions } from "@/components/alert-actions";
import { EmptyState } from "@/components/empty-state";
import { StatusBadge } from "@/components/status-badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { apiGet } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { canManageAlerts } from "@/lib/permissions";
import { getCurrentUser } from "@/lib/session";
import { linkClass } from "@/lib/styles";
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
            aria-current={filter.value === status ? "page" : undefined}
            className={`rounded-full px-3 py-1 text-sm font-medium transition-colors ${
              filter.value === status
                ? "bg-amber-500 text-white"
                : "bg-card text-foreground ring-1 ring-border hover:bg-muted"
            }`}
          >
            {filter.label}
          </Link>
        ))}
      </div>

      <div className="mt-6">
        {alerts.length === 0 ? (
          <EmptyState>No alerts match this filter.</EmptyState>
        ) : (
          <div className="rounded-xl border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Severity</TableHead>
                  <TableHead>Equipment</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Message</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Triggered</TableHead>
                  {canManage && <TableHead>Actions</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {alerts.map((alert) => (
                  <TableRow key={alert.id}>
                    <TableCell>
                      <StatusBadge value={alert.severity} />
                    </TableCell>
                    <TableCell>
                      <Link href={`/equipment/${alert.equipmentId}`} className={linkClass}>
                        {alert.equipmentType} · {alert.equipmentSerialNumber ?? alert.equipmentId}
                      </Link>
                    </TableCell>
                    <TableCell>{alert.alertType.replaceAll("_", " ")}</TableCell>
                    <TableCell className="min-w-64 whitespace-normal">{alert.message}</TableCell>
                    <TableCell>
                      <StatusBadge value={alert.status} />
                    </TableCell>
                    <TableCell className="tabular-nums">{formatDateTime(alert.triggeredAt)}</TableCell>
                    {canManage && (
                      <TableCell>
                        <AlertActions alertId={alert.id} status={alert.status} />
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      {(page > 0 || alerts.length === PAGE_SIZE) && (
        <div className="mt-4 flex items-center justify-between text-sm">
          {page > 0 ? (
            <Link href={hrefFor(status, page - 1)} className={linkClass}>
              &larr; Previous
            </Link>
          ) : (
            <span />
          )}
          <span className="text-muted-foreground">Page {page + 1}</span>
          {alerts.length === PAGE_SIZE ? (
            <Link href={hrefFor(status, page + 1)} className={linkClass}>
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
