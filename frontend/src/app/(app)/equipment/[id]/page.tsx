import { Breadcrumbs } from "@/components/breadcrumbs";
import { canScheduleInspections } from "@/lib/permissions";
import { EnergyChart } from "@/components/energy-chart";
import { getCurrentUser } from "@/lib/session";
import { InfoGrid } from "@/components/info-grid";
import { ScheduleInspectionForm } from "@/components/schedule-inspection-form";
import { StatCard } from "@/components/stat-card";
import { StatusBadge } from "@/components/status-badge";
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
            <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-800">
              Stale data
            </span>
          )}
          {latest?.isSimulated && (
            <span className="rounded-full bg-gray-200 px-2.5 py-0.5 text-xs font-medium text-gray-700">
              Simulated
            </span>
          )}
        </div>

        {latest ? (
          <>
            <p className="mt-1 text-sm text-gray-500">
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
          <p className="mt-3 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
            No readings have been recorded for this equipment yet.
          </p>
        )}
      </section>

      {/* ---------- Daily energy chart ---------- */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold">Daily energy in kWh (last {CHART_DAYS} days)</h2>
        {summary.length === 0 ? (
          <p className="mt-3 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
            No energy data for this period.
          </p>
        ) : (
          <div className="mt-3 rounded-lg border border-gray-200 bg-white p-4">
            <EnergyChart data={chartData} />
            <p className="mt-2 text-xs text-gray-500">Days without readings have no bar.</p>
          </div>
        )}
      </section>

      {/* ---------- Alerts ---------- */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold">Alerts</h2>
        {alerts.length === 0 ? (
          <p className="mt-3 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
            No alerts for this equipment.
          </p>
        ) : (
          <div className="mt-3 overflow-x-auto rounded-lg border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
                <tr>
                  <th className="px-4 py-3">Severity</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Message</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Triggered</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {alerts.map((alert) => (
                  <tr key={alert.id}>
                    <td className="px-4 py-3">
                      <StatusBadge value={alert.severity} />
                    </td>
                    <td className="px-4 py-3">{alert.alertType.replaceAll("_", " ")}</td>
                    <td className="px-4 py-3">{alert.message}</td>
                    <td className="px-4 py-3">
                      <StatusBadge value={alert.status} />
                    </td>
                    <td className="px-4 py-3">{formatDateTime(alert.triggeredAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* ---------- Inspections ---------- */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold">Inspections</h2>
        {canSchedule && (
          <div className="mt-3">
            <ScheduleInspectionForm equipmentId={equipmentId} alertOptions={alertOptions} />
          </div>
        )}
        {inspections.length === 0 ? (
          <p className="mt-3 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500">
            No inspections for this equipment.
          </p>
        ) : (
          <div className="mt-3 overflow-x-auto rounded-lg border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
                <tr>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Scheduled</th>
                  <th className="px-4 py-3">Result</th>
                  <th className="px-4 py-3">Findings</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {inspections.map((inspection) => (
                  <tr key={inspection.id}>
                    <td className="px-4 py-3">{inspection.inspectionType.replaceAll("_", " ")}</td>
                    <td className="px-4 py-3">
                      <StatusBadge value={inspection.status} />
                    </td>
                    <td className="px-4 py-3">{formatDateTime(inspection.scheduledAt)}</td>
                    <td className="px-4 py-3">
                      {inspection.result ? <StatusBadge value={inspection.result} /> : "-"}
                    </td>
                    <td className="px-4 py-3">{inspection.findings ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}