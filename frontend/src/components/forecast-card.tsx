import { Droplets } from "lucide-react";
import { Bone, LoadingRegion } from "@/components/page-skeletons";
import { ApiError, apiGet } from "@/lib/api";
import { isoDateInColombo } from "@/lib/format";
import { linkClass } from "@/lib/styles";
import type { SiteForecast } from "@/lib/types";
import { shortDayLabel, weatherInfo } from "@/lib/weather";

const DAYS_SHOWN = 7;

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

function ForecastFrame({ summary, children }: { summary?: string; children: React.ReactNode }) {
  return (
    <section aria-labelledby="forecast-heading">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <h2 id="forecast-heading" className="text-lg font-semibold">
          Solar forecast <span className="font-normal text-muted-foreground">· next 7 days</span>
        </h2>
        {summary && <p className="text-sm font-medium tabular-nums">{summary}</p>}
      </div>
      <div className="mt-3">{children}</div>
    </section>
  );
}

export async function ForecastCard({ siteId }: { siteId: number }) {
  const result = await loadForecast(siteId);

  if (result instanceof ApiError) {
    const message =
      result.status === 409
        ? "Add a location (latitude and longitude) to this site to see the solar forecast."
        : "The weather forecast is temporarily unavailable. Please try again later.";
    return (
      <ForecastFrame>
        <p className="rounded-xl border border-dashed p-6 text-center text-sm text-muted-foreground">
          {message}
        </p>
      </ForecastFrame>
    );
  }

  // the backend returns 7 past days too (used for "expected vs actual"); here we show today onwards
  const today = isoDateInColombo(0);
  const days = result.days.filter((day) => day.date >= today).slice(0, DAYS_SHOWN);

  const known = days.filter((day) => day.expectedEnergyKwh !== null);
  const total = known.reduce((sum, day) => sum + (day.expectedEnergyKwh ?? 0), 0);
  const summary = known.length > 0 ? `Expected: ${Math.round(total)} kWh` : undefined;

  return (
    <ForecastFrame summary={summary}>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
        {days.map((day) => {
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
    </ForecastFrame>
  );
}

// Shown while the forecast loads; same title, grid and tile size as the real card
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
    </LoadingRegion>
  );
}
