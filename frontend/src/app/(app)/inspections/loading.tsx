import {
  FilterPillsSkeleton,
  LoadingRegion,
  TableSkeleton,
  TitleSkeleton,
} from "@/components/page-skeletons";

export default function Loading() {
  return (
    <LoadingRegion label="Loading inspections...">
      <TitleSkeleton width="w-44" />
      <FilterPillsSkeleton count={5} />
      <div className="mt-6">
        <TableSkeleton rows={6} columns={7} />
      </div>
    </LoadingRegion>
  );
}
