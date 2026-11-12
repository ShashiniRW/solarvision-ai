import { Skeleton } from "@/components/ui/skeleton";

// One grey placeholder shape. The pulse animation stops for users who ask for reduced motion.
export function Bone({ className }: { className: string }) {
  return <Skeleton className={`motion-reduce:animate-none ${className}`} />;
}

// Wraps a skeleton: screen readers hear the label, and the grey shapes are hidden from them.
export function LoadingRegion({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div role="status" aria-busy="true">
      <span className="sr-only">{label}</span>
      <div aria-hidden="true">{children}</div>
    </div>
  );
}

// Same height as the page <h1> (text-2xl)
export function TitleSkeleton({ width = "w-56" }: { width?: string }) {
  return <Bone className={`h-8 ${width}`} />;
}

export function BreadcrumbSkeleton() {
  return <Bone className="mb-4 h-4 w-72" />;
}

// Same size and spacing as the status filter pills on Alerts / Inspections
export function FilterPillsSkeleton({ count }: { count: number }) {
  return (
    <div className="mt-4 flex flex-wrap gap-2">
      {Array.from({ length: count }, (_, index) => (
        <Bone key={index} className="h-7 w-24 rounded-full" />
      ))}
    </div>
  );
}

// Same box as the real tables: rounded border, card background, one header row
export function TableSkeleton({ rows = 5, columns = 5 }: { rows?: number; columns?: number }) {
  return (
    <div className="rounded-xl border bg-card">
      <div className="flex gap-6 border-b px-3 py-3">
        {Array.from({ length: columns }, (_, index) => (
          <Bone key={index} className="h-4 flex-1" />
        ))}
      </div>
      {Array.from({ length: rows }, (_, row) => (
        <div key={row} className="flex gap-6 border-b px-3 py-3.5 last:border-b-0">
          {Array.from({ length: columns }, (_, column) => (
            <Bone key={column} className="h-4 flex-1" />
          ))}
        </div>
      ))}
    </div>
  );
}

// Same grid as the dashboard summary cards
export function SummaryCardsSkeleton({ count = 4 }: { count?: number }) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="space-y-3 rounded-xl border bg-card p-5">
          <Bone className="h-3 w-32" />
          <Bone className="h-8 w-20" />
          <Bone className="h-3 w-44" />
        </div>
      ))}
    </div>
  );
}

// Same grid as the "Latest reading" stat cards on the equipment page
export function StatCardsSkeleton({ count = 5 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-5">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="space-y-2 rounded-xl border bg-card p-4">
          <Bone className="h-3 w-20" />
          <Bone className="h-5 w-16" />
        </div>
      ))}
    </div>
  );
}

// Same box as InfoGrid (the facts at the top of detail pages)
export function InfoGridSkeleton({ items = 6 }: { items?: number }) {
  return (
    <div className="grid grid-cols-1 gap-4 rounded-xl border bg-card p-5 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: items }, (_, index) => (
        <div key={index} className="space-y-2">
          <Bone className="h-3 w-24" />
          <Bone className="h-4 w-36" />
        </div>
      ))}
    </div>
  );
}
