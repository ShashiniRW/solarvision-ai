"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { shortDayLabel } from "@/lib/weather";

// null = no data for that day (no bar is drawn)
export type PerformancePoint = {
  date: string;
  expectedKwh: number | null;
  actualKwh: number | null;
};

const AXIS_TICK = { fontSize: 11, fill: "var(--muted-foreground)" };

export function ExpectedVsActualChart({ data }: { data: PerformancePoint[] }) {
  return (
    <div className="h-64 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
          <XAxis dataKey="date" tickFormatter={shortDayLabel} tick={AXIS_TICK} stroke="var(--border)" />
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
            labelFormatter={(label) => shortDayLabel(String(label))}
            formatter={(value) => (typeof value === "number" ? `${value.toFixed(2)} kWh` : "no data")}
          />
          <Legend wrapperStyle={{ fontSize: 12, color: "var(--muted-foreground)" }} />
          <Bar dataKey="expectedKwh" name="Expected" fill="#fcd34d" radius={[4, 4, 0, 0]} />
          <Bar dataKey="actualKwh" name="Actual" fill="#d97706" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
