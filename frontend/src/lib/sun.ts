// Simple sun model (the same one the backend simulator uses): up from 06:00 to 18:00 Colombo time.
const SUNRISE_HOUR = 6;
const SUNSET_HOUR = 18;

export type SunPosition = {
  isDay: boolean;
  strength: number; // 0 at sunrise/sunset and at night, 1 at midday
  position: [number, number, number];
};

/** Hours since midnight in Colombo (UTC+05:30, no daylight saving), e.g. 13.5 = 13:30. */
export function currentColomboHour(now: Date = new Date()): number {
  return (now.getUTCHours() + now.getUTCMinutes() / 60 + 5.5) % 24;
}

/**
 * Where the sun is in the 3D scene. East is +x, north is -z.
 * Sri Lanka is just north of the equator, so the sun stays slightly to the south (+z).
 */
export function sunPosition(hour: number, distance: number): SunPosition {
  const isDay = hour > SUNRISE_HOUR && hour < SUNSET_HOUR;
  const progress = (hour - SUNRISE_HOUR) / (SUNSET_HOUR - SUNRISE_HOUR);
  const strength = isDay ? Math.sin(Math.PI * progress) : 0;

  return {
    isDay,
    strength,
    position: [
      Math.cos(Math.PI * progress) * distance, // east in the morning, west in the evening
      Math.max(strength, 0.1) * distance,
      distance * 0.35,
    ],
  };
}
