import type { Metadata } from "next";
import Link from "next/link";
import { SiteMap } from "@/components/site-map";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { apiGet } from "@/lib/api";
import { HEALTH_COLORS, HEALTH_LABELS, HEALTH_ORDER } from "@/lib/site-health";
import { linkClass } from "@/lib/styles";
import type { MapSite, SiteHealth } from "@/lib/types";

export const metadata: Metadata = {
  title: "Site map",
};

function HealthBadge({ health }: { health: SiteHealth }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-sm">
      <span
        aria-hidden="true"
        className="size-2.5 rounded-full"
        style={{ backgroundColor: HEALTH_COLORS[health] }}
      />
      {HEALTH_LABELS[health]}
    </span>
  );
}

export default async function MapPage() {
  const sites = await apiGet<MapSite[]>("/api/sites/map");

  // worst sites first, then by name
  const sorted = [...sites].sort(
    (a, b) =>
      HEALTH_ORDER.indexOf(a.health) - HEALTH_ORDER.indexOf(b.health) ||
      a.name.localeCompare(b.name),
  );
  const withoutLocation = sites.filter((site) => site.latitude === null || site.longitude === null);
  const countOf = (health: SiteHealth) => sites.filter((site) => site.health === health).length;

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-3xl font-bold tracking-tight">Site map</h1>
        <p className="text-muted-foreground">
          Every site you can access. The colour shows its most serious unresolved alert.
        </p>
      </div>

      {sites.length === 0 ? (
        <p className="rounded-xl border bg-card p-6 text-muted-foreground">
          No sites to show yet. Sites appear here once you have access to a project.
        </p>
      ) : (
        <>
          <ul aria-label="Legend" className="flex flex-wrap gap-4">
            {HEALTH_ORDER.map((health) => (
              <li key={health} className="flex items-center gap-2 text-sm">
                <HealthBadge health={health} />
                <span className="text-muted-foreground">({countOf(health)})</span>
              </li>
            ))}
          </ul>

          <SiteMap sites={sites} />

          {withoutLocation.length > 0 && (
            <p role="note" className="text-sm text-muted-foreground">
              Not on the map because the coordinates are missing:{" "}
              {withoutLocation.map((site) => site.name).join(", ")}
            </p>
          )}

          <section aria-labelledby="site-list-heading" className="space-y-3">
            <h2 id="site-list-heading" className="text-xl font-semibold">
              All sites
            </h2>
            <div className="rounded-xl border bg-card">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Site</TableHead>
                    <TableHead>Project</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Critical</TableHead>
                    <TableHead className="text-right">Warning</TableHead>
                    <TableHead className="text-right">Capacity</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {sorted.map((site) => (
                    <TableRow key={site.id}>
                      <TableCell>
                        <Link href={`/sites/${site.id}`} className={linkClass}>
                          {site.name}
                        </Link>
                      </TableCell>
                      <TableCell>{site.projectName}</TableCell>
                      <TableCell>
                        <HealthBadge health={site.health} />
                      </TableCell>
                      <TableCell className="text-right">{site.criticalAlerts}</TableCell>
                      <TableCell className="text-right">{site.warningAlerts}</TableCell>
                      <TableCell className="text-right">
                        {site.capacityKw === null ? "-" : `${site.capacityKw} kW`}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
