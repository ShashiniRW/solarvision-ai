import Link from "next/link";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { InfoGrid } from "@/components/info-grid";
import { StatusBadge } from "@/components/status-badge";
import { apiGet, apiGetOrNotFound } from "@/lib/api";
import { formatDate, formatKw } from "@/lib/format";
import { parseId } from "@/lib/params";
import type { Project, Site } from "@/lib/types";

export default async function ProjectDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const projectId = parseId((await params).id);
  const project = await apiGetOrNotFound<Project>(`/api/projects/${projectId}`);
  const sites = await apiGet<Site[]>(`/api/sites?projectId=${projectId}`);

  return (
    <div>
      <Breadcrumbs items={[{ label: "Projects", href: "/projects" }, { label: project.name }]} />

      <h1 className="text-2xl font-semibold">{project.name}</h1>
      {project.description && <p className="mt-1 text-sm text-gray-600">{project.description}</p>}

      <div className="mt-6">
        <InfoGrid
          items={[
            { label: "Organization", value: project.organizationName },
            { label: "Status", value: <StatusBadge value={project.status} /> },
            { label: "Start", value: formatDate(project.startDate) },
            { label: "End", value: formatDate(project.endDate) },
          ]}
        />
      </div>

      <h2 className="mt-8 text-lg font-semibold">Sites</h2>
      {sites.length === 0 ? (
        <p className="mt-3 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
          This project has no sites yet.
        </p>
      ) : (
        <div className="mt-3 overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
              <tr>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">Address</th>
                <th className="px-4 py-3">Capacity</th>
                <th className="px-4 py-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {sites.map((site) => (
                <tr key={site.id}>
                  <td className="px-4 py-3 font-medium">
                    <Link href={`/sites/${site.id}`} className="text-amber-700 hover:underline">
                      {site.name}
                    </Link>
                  </td>
                  <td className="px-4 py-3">{site.address ?? "-"}</td>
                  <td className="px-4 py-3">{formatKw(site.capacityKw)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge value={site.status} />
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