// The "nothing to show here" box used on lists and tables, with a small friendly drawing.
export function EmptyState({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed p-8 text-center text-sm text-muted-foreground">
      <svg viewBox="0 0 120 64" fill="none" className="h-16 w-auto" aria-hidden="true">
        {/* sun */}
        <circle cx="88" cy="18" r="9" fill="#fbbf24" opacity="0.9" />
        <path
          d="M88 3v3M88 30v3M73 18h3M100 18h3M77.4 7.4l2.1 2.1M96.5 26.5l2.1 2.1M98.6 7.4l-2.1 2.1M79.5 26.5l-2.1 2.1"
          stroke="#fbbf24"
          strokeWidth="2"
          strokeLinecap="round"
          opacity="0.7"
        />
        {/* solar panel on a stand */}
        <path d="M30 34h34l-6 16H24z" fill="currentColor" opacity="0.18" />
        <path d="M30 34h34l-6 16H24z" stroke="currentColor" strokeOpacity="0.5" />
        <path d="M41 34l-6 16M52 34l-6 16M27 42h34" stroke="currentColor" strokeOpacity="0.4" />
        <path d="M41 50v8" stroke="currentColor" strokeOpacity="0.5" strokeWidth="2" />
        {/* ground */}
        <path d="M8 58h104" stroke="currentColor" strokeOpacity="0.3" strokeWidth="2" strokeLinecap="round" />
      </svg>
      <p>{children}</p>
    </div>
  );
}
