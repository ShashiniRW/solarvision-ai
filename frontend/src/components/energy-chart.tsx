"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

// totalEnergyKwh is null on days without any readings (no bar is drawn)
type Point = { date: string; totalEnergyKwh: number | null };

// "2026-10-14" becomes "14 Oct"
function shortDate(value: string): string {
  return new Date(`${value}T00:00:00Z`).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    timeZone: "UTC",
  });
}

// Theme colours (CSS variables from globals.css), so the chart follows light/dark mode
const AXIS_TICK = { fontSize: 11, fill: "var(--muted-foreground)" };
const BRAND_AMBER = "#f59e0b"; // amber-500, the brand colour used for bars

export function EnergyChart({ data }: { data: Point[] }) {
  return (
    <div className="h-72 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
          <XAxis
            dataKey="date"
            tickFormatter={shortDate}
            interval={0}
            tick={AXIS_TICK}
            stroke="var(--border)"
          />
          <YAxis width={48} tick={AXIS_TICK} stroke="var(--border)" />
          <Tooltip
            cursor={{ fill: "var(--muted)" }}
            contentStyle={{
              backgroundColor: "var(--popover)",
              borderColor: "var(--border)",
              borderRadius: 8,
              color: "var(--popover-foreground)",
            }}
            labelStyle={{ color: "var(--popover-foreground)" }}
            labelFormatter={(label) => shortDate(String(label))}
            formatter={(value) =>
              typeof value === "number" ? `${value.toFixed(2)} kWh` : `${value} kWh`
            }
          />
          <Bar dataKey="totalEnergyKwh" name="Energy" fill={BRAND_AMBER} radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
