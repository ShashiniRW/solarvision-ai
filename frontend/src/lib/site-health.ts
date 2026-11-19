import type { SiteHealth } from "@/lib/types";

export const HEALTH_LABELS: Record<SiteHealth, string> = {
  CRITICAL: "Critical",
  WARNING: "Warning",
  OK: "OK",
};

// Marker colours (Tailwind red-600, amber-500, green-600)
export const HEALTH_COLORS: Record<SiteHealth, string> = {
  CRITICAL: "#dc2626",
  WARNING: "#f59e0b",
  OK: "#16a34a",
};

// Worst first, for sorting lists
export const HEALTH_ORDER: SiteHealth[] = ["CRITICAL", "WARNING", "OK"];
