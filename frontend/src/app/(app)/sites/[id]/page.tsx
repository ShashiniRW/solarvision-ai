import Link from "next/link";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { InfoGrid } from "@/components/info-grid";
import { StatusBadge } from "@/components/status-badge";
import { apiGet, apiGetOrNotFound } from "@/lib/api";
import { formatDate, formatKw } from "@/lib/format";
import { parseId } from "@/lib/params";
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
      {equipment.length === 0 ? (
        <p className="mt-3 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
          No equipment is installed at this site yet.
        </p>
      ) : (
        <div className="mt-3 overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
              <tr>
                <th className="px-4 py-3">Type</th>
                <th className="px-4 py-3">Manufacturer / model</th>
                <th className="px-4 py-3">Serial number</th>
                <th className="px-4 py-3">QR code</th>
                <th className="px-4 py-3">Capacity</th>
                <th className="px-4 py-3">Warranty until</th>
                <th className="px-4 py-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {equipment.map((item) => (
                <tr key={item.id}>
                  <td className="px-4 py-3 font-medium">
                    <Link href={`/equipment/${item.id}`} className="text-amber-700 hover:underline">
                      {item.equipmentType}
                    </Link>
                  </td>
                  <td className="px-4 py-3">
                    {[item.manufacturer, item.model].filter(Boolean).join(" ") || "-"}
                  </td>
                  <td className="px-4 py-3">{item.serialNumber ?? "-"}</td>
                  <td className="px-4 py-3">{item.qrCode}</td>
                  <td className="px-4 py-3">{formatKw(item.capacityKw)}</td>
                  <td className="px-4 py-3">{formatDate(item.warrantyExpiryDate)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge value={item.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}