import { cn } from "@/lib/utils";

// 8 rays around the sun's face (rounded, so server and browser draw exactly the same)
const RAYS = Array.from({ length: 8 }, (_, index) => {
  const angle = (index * Math.PI) / 4;
  const point = (radius: number) => Number((18 + radius).toFixed(2));
  return {
    x1: point(Math.cos(angle) * 12.5),
    y1: point(Math.sin(angle) * 12.5),
    x2: point(Math.cos(angle) * 16),
    y2: point(Math.sin(angle) * 16),
  };
});

/** The AI assistant's face: a friendly sun. While thinking, its rays turn and it glows. */
export function AssistantAvatar({
  thinking = false,
  className,
}: {
  thinking?: boolean;
  className?: string;
}) {
  return (
    <span aria-hidden="true" className={cn("relative inline-flex size-8 shrink-0", className)}>
      <span
        className={cn(
          "absolute inset-1 rounded-full bg-amber-400/40",
          thinking && "motion-safe:animate-ping",
        )}
      />
      <svg
        viewBox="0 0 36 36"
        className={cn(
          "absolute inset-0 size-full",
          thinking && "motion-safe:animate-[spin_4s_linear_infinite]",
        )}
      >
        {RAYS.map((ray, index) => (
          <line key={index} {...ray} stroke="#f59e0b" strokeWidth="2.2" strokeLinecap="round" />
        ))}
      </svg>
      <svg viewBox="0 0 36 36" className="absolute inset-0 size-full">
        <circle cx="18" cy="18" r="9.5" fill="#fbbf24" />
        <circle cx="14.8" cy="16.5" r="1.3" fill="#78350f" />
        <circle cx="21.2" cy="16.5" r="1.3" fill="#78350f" />
        <path d="M14.2 20.6q3.8 3.2 7.6 0" stroke="#78350f" strokeWidth="1.4" fill="none" strokeLinecap="round" />
      </svg>
    </span>
  );
}
