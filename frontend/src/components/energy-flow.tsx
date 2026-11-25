import { LayoutGrid, Moon, Sun, UtilityPole, Zap, type LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

type Tone = "sun" | "on" | "fault" | "idle";

const NODE_TONES: Record<Tone, string> = {
  sun: "bg-amber-100 text-amber-600 ring-amber-300 dark:bg-amber-500/15 dark:text-amber-400 dark:ring-amber-500/40",
  on: "bg-sky-100 text-sky-700 ring-sky-300 dark:bg-sky-500/15 dark:text-sky-300 dark:ring-sky-500/40",
  fault: "bg-red-100 text-red-600 ring-red-300 dark:bg-red-500/15 dark:text-red-400 dark:ring-red-500/40",
  idle: "bg-muted text-muted-foreground ring-border",
};

// Narrow card (phones): nodes stacked top to bottom, icon on the left.
// Wide card (@xl = 576 px and more): nodes in one row, left to right.
function FlowNode({
  icon: Icon,
  label,
  value,
  tone,
}: {
  icon: LucideIcon;
  label: string;
  value: string;
  tone: Tone;
}) {
  return (
    <div className="flex items-center gap-3 @xl:w-28 @xl:shrink-0 @xl:flex-col @xl:gap-1.5 @xl:text-center">
      <span
        className={cn(
          "flex size-12 shrink-0 items-center justify-center rounded-full ring-2",
          NODE_TONES[tone],
          tone === "sun" && "motion-safe:animate-pulse",
        )}
      >
        <Icon className="size-6" />
      </span>
      <span className="flex flex-col @xl:items-center @xl:gap-1.5">
        <span className="text-sm font-medium @xl:text-xs">{label}</span>
        <span className="text-xs tabular-nums text-muted-foreground">{value}</span>
      </span>
    </div>
  );
}

const DOTS = "radial-gradient(circle, currentColor 2.5px, transparent 3px)";

/** A line with glowing dots moving along it. No dots = no power flowing. */
function FlowLine({ seconds, fault }: { seconds: number; fault: boolean }) {
  const color = fault ? "text-red-500" : "text-amber-500";
  const animationDuration = `${seconds}s`;

  return (
    // narrow: a short vertical line under the centre of the icon (48 px icon, 6 px line: 21 px in)
    // wide: a horizontal line between the icons
    <div className="relative my-1 ml-5.25 h-8 w-1.5 overflow-hidden rounded-full bg-muted @xl:my-0 @xl:mt-5.25 @xl:ml-0 @xl:h-1.5 @xl:w-auto @xl:min-w-6 @xl:flex-1">
      {seconds > 0 && (
        <>
          {/* dots falling down */}
          <div
            className={cn("absolute inset-0 animate-flow-down @xl:hidden", color)}
            style={{
              backgroundImage: DOTS,
              backgroundSize: "6px 16px",
              backgroundPositionX: "center",
              animationDuration,
            }}
          />
          {/* dots moving right */}
          <div
            className={cn("absolute inset-0 hidden animate-flow @xl:block", color)}
            style={{
              backgroundImage: DOTS,
              backgroundSize: "16px 6px",
              backgroundPositionY: "center",
              animationDuration,
            }}
          />
        </>
      )}
    </div>
  );
}

type Props = {
  powerKw: number;
  capacityKw: number;
  energyTodayKwh: number;
  criticalAlerts: number;
  isDay: boolean;
};

/** Sun -> Panels -> Inverters -> Grid, animated with the real live power. */
export function EnergyFlow({ powerKw, capacityKw, energyTodayKwh, criticalAlerts, isDay }: Props) {
  const flowing = powerKw > 0.005;
  const load = capacityKw > 0 ? Math.min(powerKw / capacityKw, 1) : 0;
  // more power = faster dots: 1.6 s per step at almost nothing, 0.4 s at full capacity
  const seconds = flowing ? 1.6 - 1.2 * load : 0;
  const fault = criticalAlerts > 0;

  const status = flowing
    ? `${Math.round(load * 100)}% of ${capacityKw.toFixed(1)} kW capacity`
    : isDay
      ? "No power right now"
      : "Night: the panels are resting";

  // what a screen reader hears instead of the animation
  const summary =
    `Live energy flow: ${powerKw.toFixed(2)} kW now (${status}). ` +
    `${energyTodayKwh.toFixed(2)} kWh today. ` +
    (fault ? `${criticalAlerts} critical alerts.` : "No critical alerts.");

  return (
    // @container: the layout follows the width of this card, not the whole screen
    <section aria-labelledby="energy-flow-title" className="@container rounded-xl border bg-card p-5">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="energy-flow-title" className="text-lg font-semibold">
          Live energy flow
        </h2>
        <p className="text-sm tabular-nums text-muted-foreground">{status}</p>
      </div>
      <p className="sr-only">{summary}</p>

      <div className="mt-5 flex flex-col @xl:flex-row @xl:items-start" aria-hidden="true">
        <FlowNode
          icon={isDay ? Sun : Moon}
          label={isDay ? "Sun" : "Night"}
          value={isDay ? "Daylight" : "No sunlight"}
          tone={isDay ? "sun" : "idle"}
        />
        <FlowLine seconds={seconds} fault={false} />
        <FlowNode
          icon={LayoutGrid}
          label="Panels"
          value={`${powerKw.toFixed(2)} kW`}
          tone={flowing ? "on" : "idle"}
        />
        <FlowLine seconds={seconds} fault={fault} />
        <FlowNode
          icon={Zap}
          label="Inverters"
          value={fault ? `${criticalAlerts} critical` : "Normal"}
          tone={fault ? "fault" : flowing ? "on" : "idle"}
        />
        <FlowLine seconds={seconds} fault={fault} />
        <FlowNode
          icon={UtilityPole}
          label="Grid"
          value={`${energyTodayKwh.toFixed(2)} kWh today`}
          tone={flowing ? "on" : "idle"}
        />
      </div>
    </section>
  );
}
