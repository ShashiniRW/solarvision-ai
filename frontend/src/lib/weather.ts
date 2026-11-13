import {
  Cloud,
  CloudDrizzle,
  CloudFog,
  CloudLightning,
  CloudRain,
  CloudSnow,
  CloudSun,
  Sun,
  type LucideIcon,
} from "lucide-react";

export type WeatherInfo = { label: string; Icon: LucideIcon; iconClass: string };

// WMO weather codes (used by Open-Meteo) grouped into what a user cares about
export function weatherInfo(code: number | null): WeatherInfo {
  if (code === 0) return { label: "Clear", Icon: Sun, iconClass: "text-amber-500" };
  if (code === 1 || code === 2) {
    return { label: "Partly cloudy", Icon: CloudSun, iconClass: "text-amber-500" };
  }
  if (code === 3) return { label: "Overcast", Icon: Cloud, iconClass: "text-muted-foreground" };
  if (code === 45 || code === 48) {
    return { label: "Fog", Icon: CloudFog, iconClass: "text-muted-foreground" };
  }
  if (code !== null && code >= 51 && code <= 57) {
    return { label: "Drizzle", Icon: CloudDrizzle, iconClass: "text-sky-500" };
  }
  if (code !== null && ((code >= 61 && code <= 67) || (code >= 80 && code <= 82))) {
    return { label: "Rain", Icon: CloudRain, iconClass: "text-sky-500" };
  }
  if (code !== null && ((code >= 71 && code <= 77) || code === 85 || code === 86)) {
    return { label: "Snow", Icon: CloudSnow, iconClass: "text-sky-300" };
  }
  if (code === 95 || code === 96 || code === 99) {
    return { label: "Thunderstorm", Icon: CloudLightning, iconClass: "text-violet-500" };
  }
  return { label: "Unknown", Icon: Cloud, iconClass: "text-muted-foreground" };
}

// "2026-11-14" becomes "Sat 14"
export function shortDayLabel(isoDate: string): string {
  return new Date(`${isoDate}T00:00:00Z`).toLocaleDateString("en-GB", {
    weekday: "short",
    day: "numeric",
    timeZone: "UTC",
  });
}
