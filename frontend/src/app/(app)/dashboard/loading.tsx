import {
  Bone,
  LoadingRegion,
  SummaryCardsSkeleton,
  TableSkeleton,
  TitleSkeleton,
} from "@/components/page-skeletons";

export default function Loading() {
  return (
    <LoadingRegion label="Loading dashboard...">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <TitleSkeleton width="w-72" />
        <Bone className="h-3 w-64" />
      </div>
      <div className="mt-6">
        <SummaryCardsSkeleton />
      </div>
      <div className="mt-8">
        <Bone className="h-6 w-56" />
        <div className="mt-3">
          <TableSkeleton rows={3} />
        </div>
      </div>
    </LoadingRegion>
  );
}
