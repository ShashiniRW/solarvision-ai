import { Breadcrumbs } from "@/components/breadcrumbs";
import { EmptyState } from "@/components/empty-state";
import { EnergyChart } from "@/components/energy-chart";
import { InfoGrid } from "@/components/info-grid";
import { ScheduleInspectionForm } from "@/components/schedule-inspection-form";
import { StatCard } from "@/components/stat-card";
import { StatusBadge } from "@/components/status-badge";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { apiGet, apiGetOrNotFound, apiGetOrNull } from "@/lib/api";
import {
  formatDate,
  formatDateTime,
  formatKw,
  formatMeasure,
  isOlderThanMinutes,
  isoDateInColombo,
} from "@/lib/format";
import { parseId } from "@/lib/params";
import { canScheduleInspections } from "@/lib/permissions";
import { getCurrentUser } from "@/lib/session";
import type {
  Alert,
  DailyEnergySummary,
  Equipment,
  Inspection,
  SensorReading,
} from "@/lib/types";

const CHART_DAYS = 14;
const STALE_AFTER_MINUTES = 30;

export default async function EquipmentDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const equipmentId = parseId((await params).id);
  const equipment = await apiGetOrNotFound<Equipment>(`/api/equipment/${equipmentId}`);

  const from = isoDateInColombo(-(CHART_DAYS - 1));
  const to = isoDateInColombo(0);

  // all four requests run at the same time
  const [latest, summary, alerts, inspections] = await Promise.all([
    apiGetOrNull<SensorReading>(`/api/readings/equipment/${equipmentId}/latest`),
    apiGet<DailyEnergySummary[]>(
      `/api/readings/equipment/${equipmentId}/daily-summary?from=${from}&to=${to}`,
    ),
    apiGet<Alert[]>(`/api/alerts?equipmentId=${equipmentId}&size=10`),
    apiGet<Inspection[]>(`/api/inspections?equipmentId=${equipmentId}&size=10`),
  ]);

  const stale = latest ? isOlderThanMinutes(latest.recordedAt, STALE_AFTER_MINUTES) : false;
  const title = [equipment.equipmentType, equipment.serialNumber].filter(Boolean).join(" · ");

  const user = await getCurrentUser();
  const canSchedule = canScheduleInspections(user.roles);

  // alerts that an inspection can still be linked to
  const alertOptions = alerts
    .filter((alert) => alert.status !== "RESOLVED")
    .map((alert) => ({
      id: alert.id,
      label: `#${alert.id} · ${alert.alertType.replaceAll("_", " ")} (${alert.status})`,
    }));

  // one entry for each of the last 14 days; days without readings get null (no bar)
  const energyByDate = new Map(summary.map((day) => [day.date, day.totalEnergyKwh]));
  const chartData = Array.from({ length: CHART_DAYS }, (_, index) => {
    const date = isoDateInColombo(-(CHART_DAYS - 1 - index));
    return { date, totalEnergyKwh: energyByDate.get(date) ?? null };
  });

  return (
    <div>
      <Breadcrumbs
        items={[
          { label: "Projects", href: "/projects" },
          { label: equipment.siteName, href: `/sites/${equipment.siteId}` },
          { label: title },
        ]}
      />

      <h1 className="text-2xl font-semibold">{title}</h1>

      <div className="mt-6">
        <InfoGrid
          items={[
            {
              label: "Manufacturer / model",
              value: [equipment.manufacturer, equipment.model].filter(Boolean).join(" ") || "-",
            },
            { label: "QR code", value: equipment.qrCode },
            { label: "Capacity", value: formatKw(equipment.capacityKw) },
            { label: "Installed", value: formatDate(equipment.installationDate) },
            { label: "Warranty until", value: formatDate(equipment.warrantyExpiryDate) },
            { label: "Status", value: <StatusBadge value={equipment.status} /> },
          ]}
        />
      </div>

      {/* ---------- Latest reading ---------- */}
      <section className="mt-8">
        <div className="flex flex-wrap items-center gap-3">
          <h2 className="text-lg font-semibold">Latest reading</h2>
          {stale && (
            <Badge
              variant="secondary"
              className="bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-400"
            >
              Stale data
            </Badge>
          )}
          {latest?.isSimulated && <Badge variant="secondary">Simulated</Badge>}
        </div>

        {latest ? (
          <>
            <p className="mt-1 text-sm text-muted-foreground">
              Recorded {formatDateTime(latest.recordedAt)} (Colombo time)
            </p>
            <div className="mt-3 grid grid-cols-2 gap-4 md:grid-cols-5">
              <StatCard label="Power" value={formatMeasure(latest.powerKw, "kW")} />
              <StatCard label="Energy (interval)" value={formatMeasure(latest.energyKwh, "kWh", 3)} />
              <StatCard label="Voltage" value={formatMeasure(latest.voltageV, "V", 1)} />
              <StatCard label="Current" value={formatMeasure(latest.currentA, "A", 1)} />
              <StatCard label="Temperature" value={formatMeasure(latest.temperatureC, "°C", 1)} />
            </div>
          </>
        ) : (
          <div className="mt-3">
            <EmptyState>No readings have been recorded for this equipment yet.</EmptyState>
          </div>
        )}
      </section>

      {/* ---------- Daily energy chart ---------- */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold">Daily energy in kWh (last {CHART_DAYS} days)</h2>
        <div className="mt-3">
          {summary.length === 0 ? (
            <EmptyState>No energy data for this period.</EmptyState>
          ) : (
            <div className="rounded-xl border bg-card p-4">
              <EnergyChart data={chartData} />
              <p className="mt-2 text-xs text-muted-foreground">Days without readings have no bar.</p>
            </div>
          )}
        </div>
      </section>

      {/* ---------- Alerts ---------- */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold">Alerts</h2>
        <div className="mt-3">
          {alerts.length === 0 ? (
            <EmptyState>No alerts for this equipment.</EmptyState>
          ) : (
            <div className="rounded-xl border bg-card">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Severity</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Message</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Triggered</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {alerts.map((alert) => (
                    <TableRow key={alert.id}>
                      <TableCell>
                        <StatusBadge value={alert.severity} />
                      </TableCell>
                      <TableCell>{alert.alertType.replaceAll("_", " ")}</TableCell>
                      <TableCell className="min-w-64 whitespace-normal">{alert.message}</TableCell>
                      <TableCell>
                        <StatusBadge value={alert.status} />
                      </TableCell>
                      <TableCell className="tabular-nums">{formatDateTime(alert.triggeredAt)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </div>
      </section>

      {/* ---------- Inspections ---------- */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold">Inspections</h2>
        {canSchedule && (
          <div className="mt-3">
            <ScheduleInspectionForm equipmentId={equipmentId} alertOptions={alertOptions} />
          </div>
        )}
        <div className="mt-3">
          {inspections.length === 0 ? (
            <EmptyState>No inspections for this equipment.</EmptyState>
          ) : (
            <div className="rounded-xl border bg-card">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Type</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Scheduled</TableHead>
                    <TableHead>Result</TableHead>
                    <TableHead>Findings</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {inspections.map((inspection) => (
                    <TableRow key={inspection.id}>
                      <TableCell>{inspection.inspectionType.replaceAll("_", " ")}</TableCell>
                      <TableCell>
                        <StatusBadge value={inspection.status} />
                      </TableCell>
                      <TableCell className="tabular-nums">
                        {formatDateTime(inspection.scheduledAt)}
                      </TableCell>
                      <TableCell>
                        {inspection.result ? <StatusBadge value={inspection.result} /> : "-"}
                      </TableCell>
                      <TableCell className="min-w-48 whitespace-normal">
                        {inspection.findings ?? "-"}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
