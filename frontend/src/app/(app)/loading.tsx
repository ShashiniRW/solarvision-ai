import { LoadingRegion, TableSkeleton, TitleSkeleton } from "@/components/page-skeletons";

// Shown while any page in (app) that has no loading.tsx of its own is loading
// (projects list, project detail, site detail)
export default function Loading() {
  return (
    <LoadingRegion label="Loading page...">
      <TitleSkeleton />
      <div className="mt-6">
        <TableSkeleton />
      </div>
    </LoadingRegion>
  );
}
