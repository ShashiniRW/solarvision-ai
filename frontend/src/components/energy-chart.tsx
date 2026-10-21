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

type Point = { date: string; totalEnergyKwh: number };

// "2026-10-14" becomes "14 Oct"
function shortDate(value: string): string {
  return new Date(`${value}T00:00:00Z`).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    timeZone: "UTC",
  });
}

export function EnergyChart({ data }: { data: Point[] }) {
  return (
    <div className="h-72 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis dataKey="date" tickFormatter={shortDate} />
          <YAxis unit=" kWh" width={70} />
          <Tooltip
            labelFormatter={(label) => shortDate(String(label))}
            formatter={(value) => `${value} kWh`}
          />
          <Bar dataKey="totalEnergyKwh" name="Energy" fill="#f59e0b" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}