import Link from "next/link";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { HEALTH_COLORS, HEALTH_LABELS } from "@/lib/site-health";
import { linkClass } from "@/lib/styles";
import type { SiteToday } from "@/lib/types";

/** Live power as a % of the site's installed capacity (null when the capacity is unknown). */
function percentOfCapacity(site: SiteToday) {
  if (site.capacityKw === null || site.capacityKw <= 0) return null;
  return Math.round((site.currentPowerKw / site.capacityKw) * 100);
}

/** One row per site: energy today, live power and health. Problem sites come first (sorted by the API). */
export function SitesToday({ sites }: { sites: SiteToday[] }) {
  return (
    <section aria-labelledby="sites-today-title">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="sites-today-title" className="text-lg font-semibold">
          Sites today
        </h2>
        <p className="text-xs text-muted-foreground">Problem sites first · live power vs installed capacity</p>
      </div>

      <div className="mt-3 rounded-xl border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Site</TableHead>
              <TableHead>Project</TableHead>
              <TableHead className="text-right">Live power</TableHead>
              <TableHead className="text-right">Energy today</TableHead>
              <TableHead>Health</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {sites.map((site) => {
              const percent = percentOfCapacity(site);
              return (
                <TableRow key={site.siteId}>
                  <TableCell>
                    <Link href={`/sites/${site.siteId}`} className={linkClass}>
                      {site.siteName}
                    </Link>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{site.projectName}</TableCell>
                  <TableCell className="text-right tabular-nums">
                    <div className="flex items-center justify-end gap-2">
                      {percent !== null && (
                        // small bar: how hard the site is working right now
                        <span aria-hidden="true" className="h-1.5 w-12 overflow-hidden rounded-full bg-muted">
                          <span
                            className="block h-full rounded-full bg-amber-500"
                            style={{ width: `${Math.min(percent, 100)}%` }}
                          />
                        </span>
                      )}
                      <span>
                        {site.currentPowerKw.toFixed(1)} kW
                        {percent !== null && <span className="text-muted-foreground"> · {percent}%</span>}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell className="text-right tabular-nums">
                    {site.energyTodayKwh.toFixed(2)} kWh
                  </TableCell>
                  <TableCell>
                    <span className="inline-flex items-center gap-1.5 text-sm">
                      <span
                        aria-hidden="true"
                        className="size-2.5 rounded-full"
                        style={{ backgroundColor: HEALTH_COLORS[site.health] }}
                      />
                      {HEALTH_LABELS[site.health]}
                    </span>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>
    </section>
  );
}
