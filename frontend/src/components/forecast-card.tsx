import { Droplets } from "lucide-react";
import {
  ExpectedVsActualChart,
  type PerformancePoint,
} from "@/components/expected-vs-actual-chart";
import { Bone, LoadingRegion } from "@/components/page-skeletons";
import { ApiError, apiGet } from "@/lib/api";
import { isoDateInColombo } from "@/lib/format";
import { linkClass } from "@/lib/styles";
import type { DailyEnergySummary, SiteForecast } from "@/lib/types";
import { shortDayLabel, weatherInfo } from "@/lib/weather";

const DAYS_SHOWN = 7;
// below this share of the expected energy, the card warns the user
const LOW_PERFORMANCE_PERCENT = 70;

// Returns the forecast, or the ApiError (503 / 409) so the card can show a friendly message.
// Anything else is re-thrown on purpose: redirect() to the login page works by throwing,
// and swallowing it would break the "session expired" flow.
async function loadForecast(siteId: number): Promise<SiteForecast | ApiError> {
  try {
    return await apiGet<SiteForecast>(`/api/sites/${siteId}/forecast`);
  } catch (error) {
    if (error instanceof ApiError) {
      return error;
    }
    throw error;
  }
}

// Actual energy per day for the whole site = the daily totals of all its equipment added up.
// Returns null when the data could not be loaded (the forecast is still shown).
async function loadActualEnergy(
  equipmentIds: number[],
  from: string,
  to: string,
): Promise<Map<string, number> | null> {
  try {
    const summaries = await Promise.all(
      equipmentIds.map((id) =>
        apiGet<DailyEnergySummary[]>(
          `/api/readings/equipment/${id}/daily-summary?from=${from}&to=${to}`,
        ),
      ),
    );
    const totals = new Map<string, number>();
    for (const list of summaries) {
      for (const day of list) {
        totals.set(day.date, (totals.get(day.date) ?? 0) + day.totalEnergyKwh);
      }
    }
    return totals;
  } catch (error) {
    if (error instanceof ApiError) {
      return null;
    }
    throw error;
  }
}

function SectionTitle({ id, title, detail }: { id: string; title: string; detail: string }) {
  return (
    <h2 id={id} className="text-lg font-semibold">
      {title} <span className="font-normal text-muted-foreground">· {detail}</span>
    </h2>
  );
}

export async function ForecastCard({
  siteId,
  equipmentIds,
}: {
  siteId: number;
  equipmentIds: number[];
}) {
  const result = await loadForecast(siteId);

  if (result instanceof ApiError) {
    const message =
      result.status === 409
        ? "Add a location (latitude and longitude) to this site to see the solar forecast."
        : "The weather forecast is temporarily unavailable. Please try again later.";
    return (
      <section aria-labelledby="forecast-heading">
        <SectionTitle id="forecast-heading" title="Solar forecast" detail="next 7 days" />
        <p className="mt-3 rounded-xl border border-dashed p-6 text-center text-sm text-muted-foreground">
          {message}
        </p>
      </section>
    );
  }

  const today = isoDateInColombo(0);

  // ---------- next 7 days ----------
  const upcoming = result.days.filter((day) => day.date >= today).slice(0, DAYS_SHOWN);
  const knownUpcoming = upcoming.filter((day) => day.expectedEnergyKwh !== null);
  const upcomingTotal = knownUpcoming.reduce((sum, day) => sum + (day.expectedEnergyKwh ?? 0), 0);

  // ---------- last 7 days: expected vs actual ----------
  const pastDays = result.days.filter((day) => day.date < today);
  const actuals =
    pastDays.length > 0 && equipmentIds.length > 0
      ? await loadActualEnergy(equipmentIds, pastDays[0].date, pastDays[pastDays.length - 1].date)
      : null;

  const points: PerformancePoint[] = pastDays.map((day) => ({
    date: day.date,
    expectedKwh: day.expectedEnergyKwh,
    actualKwh: actuals?.get(day.date) ?? null,
  }));

  // only days where BOTH values exist are compared, otherwise the percentage would be misleading
  const compared = points.filter((p) => p.expectedKwh !== null && p.actualKwh !== null);
  const expectedSum = compared.reduce((sum, p) => sum + (p.expectedKwh ?? 0), 0);
  const actualSum = compared.reduce((sum, p) => sum + (p.actualKwh ?? 0), 0);
  const percent = expectedSum > 0 ? Math.round((actualSum / expectedSum) * 100) : null;

  return (
    <div>
      <section aria-labelledby="forecast-heading">
        <div className="flex flex-wrap items-end justify-between gap-2">
          <SectionTitle id="forecast-heading" title="Solar forecast" detail="next 7 days" />
          {knownUpcoming.length > 0 && (
            <p className="text-sm font-medium tabular-nums">Expected: {Math.round(upcomingTotal)} kWh</p>
          )}
        </div>

        <ul className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
          {upcoming.map((day) => {
            const { label, Icon, iconClass } = weatherInfo(day.weatherCode);
            return (
              <li key={day.date} className="rounded-xl border bg-card p-3 text-center">
                <p className="text-xs font-medium text-muted-foreground">
                  {day.date === today ? "Today" : shortDayLabel(day.date)}
                </p>
                <Icon aria-hidden="true" className={`mx-auto mt-2 size-8 ${iconClass}`} />
                <p className="mt-1 text-xs text-muted-foreground">{label}</p>
                <p className="mt-2 text-sm tabular-nums">
                  {day.temperatureMaxC !== null ? `${Math.round(day.temperatureMaxC)}°` : "-"}
                  <span className="text-muted-foreground">
                    {" / "}
                    {day.temperatureMinC !== null ? `${Math.round(day.temperatureMinC)}°` : "-"}
                  </span>
                </p>
                <p className="mt-1 flex items-center justify-center gap-1 text-xs text-muted-foreground tabular-nums">
                  <Droplets aria-hidden="true" className="size-3" />
                  <span className="sr-only">Chance of rain</span>
                  {day.precipitationProbabilityPercent !== null
                    ? `${day.precipitationProbabilityPercent}%`
                    : "-"}
                </p>
                <p className="mt-2 text-sm font-semibold tabular-nums">
                  {day.expectedEnergyKwh !== null ? `${day.expectedEnergyKwh.toFixed(1)} kWh` : "-"}
                </p>
              </li>
            );
          })}
        </ul>

        <p className="mt-3 text-xs text-muted-foreground">
          Expected energy = capacity × sun hours × {result.performanceRatio.toFixed(2)} (losses).
          Weather data by{" "}
          <a href="https://open-meteo.com/" target="_blank" rel="noopener noreferrer" className={linkClass}>
            Open-Meteo.com
          </a>{" "}
          (CC BY 4.0).
        </p>
      </section>

      {equipmentIds.length > 0 && (
        <section aria-labelledby="performance-heading" className="mt-8">
          <SectionTitle id="performance-heading" title="Expected vs actual" detail="last 7 days" />

          {actuals === null ? (
            <p className="mt-3 rounded-xl border border-dashed p-6 text-center text-sm text-muted-foreground">
              Production data could not be loaded.
            </p>
          ) : (
            <div className="mt-3 rounded-xl border bg-card p-4">
              <ExpectedVsActualChart data={points} />
              {percent === null ? (
                <p className="mt-2 text-sm text-muted-foreground">
                  No production data for the last 7 days yet, so nothing can be compared.
                </p>
              ) : (
                <>
                  <p className="mt-2 text-sm tabular-nums">
                    Last {compared.length} day{compared.length === 1 ? "" : "s"} with data:{" "}
                    <span className="font-semibold">{Math.round(actualSum)} kWh</span> produced of{" "}
                    {Math.round(expectedSum)} kWh expected (
                    <span className="font-semibold">{percent}%</span>)
                  </p>
                  {percent < LOW_PERFORMANCE_PERCENT && (
                    <p role="status" className="mt-1 text-sm text-amber-700 dark:text-amber-400">
                      Production is well below the forecast. Check for dirty panels, shading or a fault.
                    </p>
                  )}
                </>
              )}
            </div>
          )}
        </section>
      )}
    </div>
  );
}

// Shown while the forecast loads; same layout as the real card (tiles + chart)
export function ForecastCardSkeleton() {
  return (
    <LoadingRegion label="Loading solar forecast...">
      <Bone className="h-7 w-64" />
      <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
        {Array.from({ length: DAYS_SHOWN }, (_, index) => (
          <div key={index} className="space-y-2 rounded-xl border bg-card p-3">
            <Bone className="mx-auto h-3 w-12" />
            <Bone className="mx-auto size-8 rounded-full" />
            <Bone className="mx-auto h-3 w-16" />
            <Bone className="mx-auto h-4 w-14" />
            <Bone className="mx-auto h-4 w-16" />
          </div>
        ))}
      </div>
      <Bone className="mt-8 h-7 w-56" />
      <div className="mt-3 rounded-xl border bg-card p-4">
        <Bone className="h-64 w-full" />
      </div>
    </LoadingRegion>
  );
}
