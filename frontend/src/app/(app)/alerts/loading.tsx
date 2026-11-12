import {
  FilterPillsSkeleton,
  LoadingRegion,
  TableSkeleton,
  TitleSkeleton,
} from "@/components/page-skeletons";

export default function Loading() {
  return (
    <LoadingRegion label="Loading alerts...">
      <TitleSkeleton width="w-32" />
      <FilterPillsSkeleton count={4} />
      <div className="mt-6">
        <TableSkeleton rows={6} columns={7} />
      </div>
    </LoadingRegion>
  );
}
