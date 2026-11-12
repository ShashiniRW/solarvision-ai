import {
  Bone,
  BreadcrumbSkeleton,
  InfoGridSkeleton,
  LoadingRegion,
  StatCardsSkeleton,
  TableSkeleton,
  TitleSkeleton,
} from "@/components/page-skeletons";

export default function Loading() {
  return (
    <LoadingRegion label="Loading equipment...">
      <BreadcrumbSkeleton />
      <TitleSkeleton width="w-80" />
      <div className="mt-6">
        <InfoGridSkeleton />
      </div>

      <div className="mt-8">
        <Bone className="h-6 w-40" />
        <div className="mt-3">
          <StatCardsSkeleton />
        </div>
      </div>

      <div className="mt-8">
        <Bone className="h-6 w-64" />
        <div className="mt-3 rounded-xl border bg-card p-4">
          <Bone className="h-72 w-full" />
        </div>
      </div>

      <div className="mt-8">
        <Bone className="h-6 w-24" />
        <div className="mt-3">
          <TableSkeleton rows={4} />
        </div>
      </div>
    </LoadingRegion>
  );
}
