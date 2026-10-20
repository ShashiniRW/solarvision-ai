const STYLES: Record<string, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  COMPLETED: "bg-green-100 text-green-800",
  PASSED: "bg-green-100 text-green-800",
  RESOLVED: "bg-green-100 text-green-800",
  PLANNED: "bg-blue-100 text-blue-800",
  SCHEDULED: "bg-blue-100 text-blue-800",
  IN_PROGRESS: "bg-amber-100 text-amber-800",
  ACKNOWLEDGED: "bg-amber-100 text-amber-800",
  WARNING: "bg-amber-100 text-amber-800",
  NEEDS_FOLLOW_UP: "bg-amber-100 text-amber-800",
  OPEN: "bg-red-100 text-red-800",
  CRITICAL: "bg-red-100 text-red-800",
  FAILED: "bg-red-100 text-red-800",
  FAULTY: "bg-red-100 text-red-800",
};

const DEFAULT_STYLE = "bg-gray-200 text-gray-700";

// A small coloured label for any status value, e.g. PLANNED or IN_PROGRESS.
export function StatusBadge({ value }: { value: string }) {
  const style = STYLES[value] ?? DEFAULT_STYLE;
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${style}`}>
      {value.replaceAll("_", " ")}
    </span>
  );
}