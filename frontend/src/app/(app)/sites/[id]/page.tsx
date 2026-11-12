import Link from "next/link";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { EmptyState } from "@/components/empty-state";
import { InfoGrid } from "@/components/info-grid";
import { StatusBadge } from "@/components/status-badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { apiGet, apiGetOrNotFound } from "@/lib/api";
import { formatDate, formatKw } from "@/lib/format";
import { parseId } from "@/lib/params";
import { linkClass } from "@/lib/styles";
import type { Equipment, Site } from "@/lib/types";

export default async function SiteDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const siteId = parseId((await params).id);
  const site = await apiGetOrNotFound<Site>(`/api/sites/${siteId}`);
  const equipment = await apiGet<Equipment[]>(`/api/equipment?siteId=${siteId}`);

  return (
    <div>
      <Breadcrumbs
        items={[
          { label: "Projects", href: "/projects" },
          { label: site.projectName, href: `/projects/${site.projectId}` },
          { label: site.name },
        ]}
      />

      <h1 className="text-2xl font-semibold">{site.name}</h1>

      <div className="mt-6">
        <InfoGrid
          items={[
            { label: "Address", value: site.address ?? "-" },
            { label: "Capacity", value: formatKw(site.capacityKw) },
            { label: "Status", value: <StatusBadge value={site.status} /> },
            {
              label: "Location",
              value:
                site.latitude !== null && site.longitude !== null
                  ? `${site.latitude}, ${site.longitude}`
                  : "-",
            },
          ]}
        />
      </div>

      <h2 className="mt-8 text-lg font-semibold">Equipment</h2>
      <div className="mt-3">
        {equipment.length === 0 ? (
          <EmptyState>No equipment is installed at this site yet.</EmptyState>
        ) : (
          <div className="rounded-xl border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Type</TableHead>
                  <TableHead>Manufacturer / model</TableHead>
                  <TableHead>Serial number</TableHead>
                  <TableHead>QR code</TableHead>
                  <TableHead>Capacity</TableHead>
                  <TableHead>Warranty until</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {equipment.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>
                      <Link href={`/equipment/${item.id}`} className={linkClass}>
                        {item.equipmentType}
                      </Link>
                    </TableCell>
                    <TableCell>
                      {[item.manufacturer, item.model].filter(Boolean).join(" ") || "-"}
                    </TableCell>
                    <TableCell>{item.serialNumber ?? "-"}</TableCell>
                    <TableCell className="font-mono text-xs">{item.qrCode}</TableCell>
                    <TableCell className="tabular-nums">{formatKw(item.capacityKw)}</TableCell>
                    <TableCell className="tabular-nums">{formatDate(item.warrantyExpiryDate)}</TableCell>
                    <TableCell>
                      <StatusBadge value={item.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
    </div>
  );
}
