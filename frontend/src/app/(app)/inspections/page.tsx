import Link from "next/link";
import { InspectionActions } from "@/components/inspection-actions";
import { StatusBadge } from "@/components/status-badge";
import { apiGet } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { canCancelInspections, canWorkInspections } from "@/lib/permissions";
import { getCurrentUser } from "@/lib/session";
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

      {inspections.length === 0 ? (
        <p className="mt-6 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
          No inspections match this filter.
        </p>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
              <tr>
                <th className="px-4 py-3">Equipment</th>
                <th className="px-4 py-3">Type</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Scheduled</th>
                <th className="px-4 py-3">Result</th>
                <th className="px-4 py-3">Findings</th>
                {canWork && <th className="px-4 py-3">Actions</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 align-top">
              {inspections.map((inspection) => (
                <tr key={inspection.id}>
                  <td className="px-4 py-3">
                    <Link
                      href={`/equipment/${inspection.equipmentId}`}
                      className="text-amber-700 hover:underline"
                    >
                      {inspection.equipmentType} ·{" "}
                      {inspection.equipmentSerialNumber ?? inspection.equipmentId}
                    </Link>
                  </td>
                  <td className="px-4 py-3">{inspection.inspectionType.replaceAll("_", " ")}</td>
                  <td className="px-4 py-3">
                    <StatusBadge value={inspection.status} />
                  </td>
                  <td className="px-4 py-3">{formatDateTime(inspection.scheduledAt)}</td>
                  <td className="px-4 py-3">
                    {inspection.result ? <StatusBadge value={inspection.result} /> : "-"}
                  </td>
                  <td className="px-4 py-3">{inspection.findings ?? "-"}</td>
                  {canWork && (
                    <td className="px-4 py-3">
                      <InspectionActions
                        inspectionId={inspection.id}
                        status={inspection.status}
                        canCancel={canCancel}
                      />
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {(page > 0 || inspections.length === PAGE_SIZE) && (
        <div className="mt-4 flex items-center justify-between text-sm">
          {page > 0 ? (
            <Link href={hrefFor(status, page - 1)} className="text-amber-700 hover:underline">
              &larr; Previous
            </Link>
          ) : (
            <span />
          )}
          <span className="text-gray-500">Page {page + 1}</span>
          {inspections.length === PAGE_SIZE ? (
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