import { cn } from "@/lib/utils";

/** The SolarVision AI mark: a sun rising over a solar panel, with an AI spark. */
export function LogoMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" fill="none" aria-hidden="true" className={className}>
      {/* sun and rays */}
      <circle cx="20" cy="11" r="6" fill="#f59e0b" />
      <path
        d="M20 1.5v2.2M28.5 11h2M9.5 11h2M26.7 4.3l-1.5 1.5M13.3 4.3l1.5 1.5"
        stroke="#f59e0b"
        strokeWidth="1.6"
        strokeLinecap="round"
      />
      {/* tilted solar panel with cell lines */}
      <path d="M5 17h20l-3 11H2z" fill="#1e40af" stroke="#60a5fa" strokeWidth="0.6" />
      <path d="M10 17l-3 11M15 17l-3 11M20 17l-3 11M3.5 22.5h20" stroke="#93c5fd" strokeWidth="0.9" />
      {/* AI spark */}
      <path d="M28 20l.9 2.1L31 23l-2.1.9L28 26l-.9-2.1L25 23l2.1-.9z" fill="#22d3ee" />
    </svg>
  );
}

/** Logo mark + name, used in the sidebar and on the login page. */
export function Logo({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-2 font-semibold tracking-tight", className)}>
      <LogoMark className="size-7 shrink-0" />
      <span>
        SolarVision <span className="text-amber-600 dark:text-amber-500">AI</span>
      </span>
    </span>
  );
}
