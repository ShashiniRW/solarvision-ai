import Link from "next/link";
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
import { formatDate } from "@/lib/format";
import { linkClass } from "@/lib/styles";
import type { Project } from "@/lib/types";

export default async function ProjectsPage() {
  const projects = await apiGet<Project[]>("/api/projects");

  return (
    <div>
      <h1 className="text-2xl font-semibold">Projects</h1>
      <p className="mt-1 text-sm text-muted-foreground">{projects.length} project(s)</p>

      <div className="mt-6">
        {projects.length === 0 ? (
          <EmptyState>No projects yet.</EmptyState>
        ) : (
          <div className="rounded-xl border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Organization</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Start</TableHead>
                  <TableHead>End</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {projects.map((project) => (
                  <TableRow key={project.id}>
                    <TableCell>
                      <Link href={`/projects/${project.id}`} className={linkClass}>
                        {project.name}
                      </Link>
                    </TableCell>
                    <TableCell>{project.organizationName}</TableCell>
                    <TableCell>
                      <StatusBadge value={project.status} />
                    </TableCell>
                    <TableCell className="tabular-nums">{formatDate(project.startDate)}</TableCell>
                    <TableCell className="tabular-nums">{formatDate(project.endDate)}</TableCell>
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
