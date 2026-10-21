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