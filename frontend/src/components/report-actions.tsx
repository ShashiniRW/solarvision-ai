"use client";

import { Download, Printer } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { MonthlyReport } from "@/lib/types";

type Cell = string | number | null;

/**
 * One CSV cell. Text that starts with = + - @ is prefixed with ' so Excel shows it as text
 * instead of running it as a formula ("CSV injection": a project name could contain one).
 */
function csvCell(value: Cell): string {
  if (value === null) return "";
  let text = String(value);
  if (typeof value === "string" && /^[=+\-@]/.test(text)) {
    text = `'${text}`;
  }
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

function fileSlug(name: string) {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") || "project";
}

function toCsv(report: MonthlyReport): string {
  const rows: Cell[][] = [
    ["SolarVision AI - Monthly report"],
    ["Project", report.projectName],
    ["Month", report.month],
    [],
    ["Date", "Energy (kWh)"],
    ...report.days.map((day): Cell[] => [day.date, day.energyKwh === null ? null : day.energyKwh.toFixed(2)]),
    [],
    ["Total energy (kWh)", report.totalEnergyKwh.toFixed(2)],
    ["Average per day with data (kWh)", report.averageDailyKwh === null ? null : report.averageDailyKwh.toFixed(2)],
    ["Best day", report.bestDay ? report.bestDay.date : null],
    [],
    ["Alerts raised", report.alerts.total],
    ["Critical", report.alerts.critical],
    ["Warning", report.alerts.warning],
    ["Resolved", report.alerts.resolved],
    [],
    ["Inspections scheduled", report.inspections.scheduled],
    ["Inspections completed", report.inspections.completed],
  ];
  return rows.map((row) => row.map(csvCell).join(",")).join("\r\n");
}

/** "Download CSV" and "Print / PDF" (both hidden on the printed page). */
export function ReportActions({ report }: { report: MonthlyReport }) {
  function downloadCsv() {
    // \uFEFF (BOM) tells Excel the file is UTF-8, so names with special letters show correctly
    const blob = new Blob(["\uFEFF" + toCsv(report)], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `solarvision-${fileSlug(report.projectName)}-${report.month}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="flex flex-wrap gap-2 print:hidden">
      <Button type="button" variant="outline" onClick={downloadCsv}>
        <Download className="size-4" />
        Download CSV
      </Button>
      <Button type="button" variant="outline" onClick={() => window.print()}>
        <Printer className="size-4" />
        Print / PDF
      </Button>
    </div>
  );
}
