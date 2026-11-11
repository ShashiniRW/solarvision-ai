import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const GREEN = "bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300";
const BLUE = "bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300";
const AMBER = "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300";
const RED = "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300";
const GREY = "bg-muted text-muted-foreground";

// One colour per meaning: green = good, blue = planned, amber = attention, red = problem.
const STYLES: Record<string, string> = {
  ACTIVE: GREEN,
  COMPLETED: GREEN,
  PASSED: GREEN,
  RESOLVED: GREEN,
  PLANNED: BLUE,
  SCHEDULED: BLUE,
  IN_PROGRESS: AMBER,
  ACKNOWLEDGED: AMBER,
  WARNING: AMBER,
  NEEDS_FOLLOW_UP: AMBER,
  OPEN: RED,
  CRITICAL: RED,
  FAILED: RED,
  FAULTY: RED,
};

// A small coloured label for any status value, e.g. PLANNED or IN_PROGRESS.
export function StatusBadge({ value }: { value: string }) {
  return (
    <Badge variant="secondary" className={cn("border-transparent", STYLES[value] ?? GREY)}>
      {value.replaceAll("_", " ")}
    </Badge>
  );
}
