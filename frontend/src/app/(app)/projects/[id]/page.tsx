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
      {project.description && (
        <p className="mt-1 text-sm text-muted-foreground">{project.description}</p>
      )}

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
      <div className="mt-3">
        {sites.length === 0 ? (
          <EmptyState>This project has no sites yet.</EmptyState>
        ) : (
          <div className="rounded-xl border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Address</TableHead>
                  <TableHead>Capacity</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {sites.map((site) => (
                  <TableRow key={site.id}>
                    <TableCell>
                      <Link href={`/sites/${site.id}`} className={linkClass}>
                        {site.name}
                      </Link>
                    </TableCell>
                    <TableCell className="min-w-48 whitespace-normal">
                      {site.address ?? "-"}
                    </TableCell>
                    <TableCell className="tabular-nums">{formatKw(site.capacityKw)}</TableCell>
                    <TableCell>
                      <StatusBadge value={site.status} />
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
