import type { Metadata } from "next";
import { EmptyState } from "@/components/empty-state";
import { EnergyChart } from "@/components/energy-chart";
import { ReportActions } from "@/components/report-actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { apiGet, apiGetOrNotFound } from "@/lib/api";
import { brandButtonClass } from "@/lib/styles";
import type { MonthlyReport, Project } from "@/lib/types";

export const metadata: Metadata = {
  title: "Reports",
};

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

/** The current month in Colombo, e.g. "2026-11". */
function currentMonth(): string {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Colombo",
    year: "numeric",
    month: "2-digit",
  }).formatToParts(new Date());
  const year = parts.find((part) => part.type === "year")?.value;
  const month = parts.find((part) => part.type === "month")?.value;
  return `${year}-${month}`;
}

/** When the report was made, shown on the printed page. */
function generatedAt(): string {
  return new Date().toLocaleString("en-GB", {
    timeZone: "Asia/Colombo",
    dateStyle: "medium",
    timeStyle: "short",
  });
}

// "2026-11" -> "November 2026"
function monthLabel(month: string) {
  return new Date(`${month}-01T00:00:00Z`).toLocaleDateString("en-GB", {
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  });
}

// "2026-11-03" -> "Tue 3 Nov"
function dayLabel(date: string) {
  return new Date(`${date}T00:00:00Z`).toLocaleDateString("en-GB", {
    weekday: "short",
    day: "numeric",
    month: "short",
    timeZone: "UTC",
  });
}

function kwh(value: number | null) {
  return value === null ? "-" : `${value.toFixed(2)} kWh`;
}

function Stat({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="rounded-xl border bg-card p-4">
      <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
      {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
    </div>
  );
}

export default async function ReportsPage({
  searchParams,
}: {
  searchParams: Promise<{ projectId?: string | string[]; month?: string | string[] }>;
}) {
  const params = await searchParams;
  // already limited to the projects this user may see
  const projects = await apiGet<Project[]>("/api/projects");

  if (projects.length === 0) {
    return (
      <div>
        <h1 className="text-2xl font-semibold">Reports</h1>
        <div className="mt-6">
          <EmptyState>No projects to report on yet.</EmptyState>
        </div>
      </div>
    );
  }

  const latestMonth = currentMonth();
  const requestedMonth = first(params.month);
  const month =
    requestedMonth && MONTH_PATTERN.test(requestedMonth) && requestedMonth <= latestMonth
      ? requestedMonth
      : latestMonth;
  // only one of the user's own projects can be chosen (the backend checks again)
  const project = projects.find((item) => String(item.id) === first(params.projectId)) ?? projects[0];

  const report = await apiGetOrNotFound<MonthlyReport>(
    `/api/reports/monthly?projectId=${project.id}&month=${month}`,
  );
  const daysWithData = report.days.filter((day) => day.energyKwh !== null).length;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Reports</h1>
          <p className="mt-1 text-sm text-muted-foreground print:hidden">
            Monthly energy, alerts and inspections for one project.
          </p>
        </div>
        <ReportActions report={report} />
      </div>

      {/* a plain GET form: the choice ends up in the URL, so a report can be bookmarked or shared */}
      <form method="get" className="flex flex-wrap items-end gap-3 rounded-xl border bg-card p-4 print:hidden">
        <div className="min-w-56 flex-1 space-y-1.5">
          <Label htmlFor="projectId">Project</Label>
          <select
            id="projectId"
            name="projectId"
            defaultValue={project.id}
            className="h-9 w-full rounded-md border bg-transparent px-3 text-sm dark:bg-input/30"
          >
            {projects.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="month">Month</Label>
          <Input id="month" name="month" type="month" defaultValue={month} max={latestMonth} required />
        </div>
        <Button type="submit" className={brandButtonClass}>
          Show report
        </Button>
      </form>

      <section aria-labelledby="report-title" className="space-y-6">
        <div>
          <h2 id="report-title" className="text-xl font-semibold">
            {report.projectName} · {monthLabel(report.month)}
          </h2>
          <p className="text-xs text-muted-foreground">
            Generated {generatedAt()} (Colombo time) by SolarVision AI
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4 print:grid-cols-4">
          <Stat
            label="Total energy"
            value={`${report.totalEnergyKwh.toFixed(2)} kWh`}
            hint={`${daysWithData} of ${report.days.length} days have readings`}
          />
          <Stat label="Average per day" value={kwh(report.averageDailyKwh)} hint="Days with readings only" />
          <Stat
            label="Best day"
            value={report.bestDay ? kwh(report.bestDay.energyKwh) : "-"}
            hint={report.bestDay ? dayLabel(report.bestDay.date) : "No readings this month"}
          />
          <Stat
            label="Alerts raised"
            value={String(report.alerts.total)}
            hint={`${report.alerts.critical} critical · ${report.alerts.warning} warning · ${report.alerts.resolved} resolved`}
          />
        </div>

        <div className="rounded-xl border bg-card p-5 print:break-inside-avoid">
          <h3 className="font-semibold">Energy per day</h3>
          <div className="mt-4">
            <EnergyChart data={report.days.map((day) => ({ date: day.date, totalEnergyKwh: day.energyKwh }))} />
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 print:grid-cols-2">
          <Stat
            label="Inspections scheduled"
            value={String(report.inspections.scheduled)}
            hint="Planned for this month"
          />
          <Stat
            label="Inspections completed"
            value={String(report.inspections.completed)}
            hint="Finished this month"
          />
        </div>

        <div className="rounded-xl border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead className="text-right">Energy</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {report.days.map((day) => (
                <TableRow key={day.date}>
                  <TableCell>{dayLabel(day.date)}</TableCell>
                  <TableCell className="text-right tabular-nums">{kwh(day.energyKwh)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </section>
    </div>
  );
}
