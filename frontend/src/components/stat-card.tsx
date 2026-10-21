export function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4">
      <p className="text-xs uppercase text-gray-500">{label}</p>
      <p className="mt-1 text-xl font-semibold">{value}</p>
    </div>
  );
}