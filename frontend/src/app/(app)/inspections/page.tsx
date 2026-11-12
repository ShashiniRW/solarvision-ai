import Link from "next/link";
import { EmptyState } from "@/components/empty-state";
import { InspectionActions } from "@/components/inspection-actions";
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
import { canCancelInspections, canWorkInspections } from "@/lib/permissions";
import { getCurrentUser } from "@/lib/session";
import { linkClass } from "@/lib/styles";
import type { Inspection } from "@/lib/types";

const PAGE_SIZE = 20;
const VALID_STATUSES = ["SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"];
const FILTERS = [
  { label: "All", value: "" },
  { label: "Scheduled", value: "SCHEDULED" },
  { label: "In progress", value: "IN_PROGRESS" },
  { label: "Completed", value: "COMPLETED" },
  { label: "Cancelled", value: "CANCELLED" },
];

export default async function InspectionsPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: string; page?: string }>;
}) {
  const { status: rawStatus, page: rawPage } = await searchParams;

  // only accept known values from the address bar
  const status = rawStatus && VALID_STATUSES.includes(rawStatus) ? rawStatus : "";
  const page = rawPage && /^\d+$/.test(rawPage) ? Number(rawPage) : 0;

  const user = await getCurrentUser();
  const canWork = canWorkInspections(user.roles);
  const canCancel = canCancelInspections(user.roles);

  const query = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
  if (status) {
    query.set("status", status);
  }
  const inspections = await apiGet<Inspection[]>(`/api/inspections?${query.toString()}`);

  function hrefFor(targetStatus: string, targetPage: number): string {
    const params = new URLSearchParams();
    if (targetStatus) {
      params.set("status", targetStatus);
    }
    if (targetPage > 0) {
      params.set("page", String(targetPage));
    }
    const text = params.toString();
    return text ? `/inspections?${text}` : "/inspections";
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold">Inspections</h1>

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
        {inspections.length === 0 ? (
          <EmptyState>No inspections match this filter.</EmptyState>
        ) : (
          <div className="rounded-xl border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Equipment</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Scheduled</TableHead>
                  <TableHead>Result</TableHead>
                  <TableHead>Findings</TableHead>
                  {canWork && <TableHead>Actions</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {inspections.map((inspection) => (
                  // align-top: the Actions cell can grow tall (e.g. the "complete" form)
                  <TableRow key={inspection.id} className="[&>td]:align-top">
                    <TableCell>
                      <Link href={`/equipment/${inspection.equipmentId}`} className={linkClass}>
                        {inspection.equipmentType} ·{" "}
                        {inspection.equipmentSerialNumber ?? inspection.equipmentId}
                      </Link>
                    </TableCell>
                    <TableCell>{inspection.inspectionType.replaceAll("_", " ")}</TableCell>
                    <TableCell>
                      <StatusBadge value={inspection.status} />
                    </TableCell>
                    <TableCell className="tabular-nums">
                      {formatDateTime(inspection.scheduledAt)}
                    </TableCell>
                    <TableCell>
                      {inspection.result ? <StatusBadge value={inspection.result} /> : "-"}
                    </TableCell>
                    <TableCell className="min-w-48 whitespace-normal">
                      {inspection.findings ?? "-"}
                    </TableCell>
                    {canWork && (
                      <TableCell>
                        <InspectionActions
                          inspectionId={inspection.id}
                          status={inspection.status}
                          canCancel={canCancel}
                        />
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      {(page > 0 || inspections.length === PAGE_SIZE) && (
        <div className="mt-4 flex items-center justify-between text-sm">
          {page > 0 ? (
            <Link href={hrefFor(status, page - 1)} className={linkClass}>
              &larr; Previous
            </Link>
          ) : (
            <span />
          )}
          <span className="text-muted-foreground">Page {page + 1}</span>
          {inspections.length === PAGE_SIZE ? (
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
