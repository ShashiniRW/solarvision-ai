// Turns "2026-09-10" (or a timestamp) into "10 Sep 2026". Empty values show a dash.
export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleDateString("en-GB", {
    year: "numeric",
    month: "short",
    day: "numeric",
    timeZone: "UTC",
  });
}

// 10.5 becomes "10.5 kW"; empty values show a dash.
export function formatKw(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "-";
  }
  return `${value} kW`;
}

// "2026-10-18T01:38:16Z" becomes "18 Oct 2026, 07:08" in Colombo time.
export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "Asia/Colombo",
  });
}

// 4.1234 with unit "kW" becomes "4.12 kW"; empty values show a dash.
export function formatMeasure(
  value: number | null | undefined,
  unit: string,
  digits = 2,
): string {
  if (value === null || value === undefined) {
    return "-";
  }
  return `${value.toFixed(digits)} ${unit}`;
}

// Today's date (or a day offset from today) as "2026-10-21", in Colombo time.
export function isoDateInColombo(daysFromToday = 0): string {
  const date = new Date(Date.now() + daysFromToday * 86_400_000);
  return date.toLocaleDateString("en-CA", { timeZone: "Asia/Colombo" });
}

// True when the timestamp is more than the given number of minutes in the past.
export function isOlderThanMinutes(value: string, minutes: number): boolean {
  return Date.now() - new Date(value).getTime() > minutes * 60_000;
}


// The current time as "12:04:23" in Colombo time (shown as "Updated ..." on the dashboard).
export function currentTimeLabel(): string {
  return new Date().toLocaleTimeString("en-GB", { timeZone: "Asia/Colombo" });
}