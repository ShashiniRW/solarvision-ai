import { StatusBadge } from "@/components/status-badge";
import { apiGet } from "@/lib/api";
import { formatDate } from "@/lib/format";
import type { Project } from "@/lib/types";

export default async function ProjectsPage() {
  const projects = await apiGet<Project[]>("/api/projects");

  return (
    <div>
      <h1 className="text-2xl font-semibold">Projects</h1>
      <p className="mt-1 text-sm text-gray-600">{projects.length} project(s)</p>

      {projects.length === 0 ? (
        <p className="mt-6 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
          No projects yet.
        </p>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
              <tr>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">Organization</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Start</th>
                <th className="px-4 py-3">End</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {projects.map((project) => (
                <tr key={project.id}>
                  <td className="px-4 py-3 font-medium">{project.name}</td>
                  <td className="px-4 py-3">{project.organizationName}</td>
                  <td className="px-4 py-3">
                    <StatusBadge value={project.status} />
                  </td>
                  <td className="px-4 py-3">{formatDate(project.startDate)}</td>
                  <td className="px-4 py-3">{formatDate(project.endDate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}