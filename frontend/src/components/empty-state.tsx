// The "nothing to show here" box used on lists and tables.
export function EmptyState({ children }: { children: React.ReactNode }) {
  return (
    <p className="rounded-xl border border-dashed p-8 text-center text-sm text-muted-foreground">
      {children}
    </p>
  );
}
