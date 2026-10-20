import { getCurrentUser } from "@/lib/session";

export default async function DashboardPage() {
  const user = await getCurrentUser();

  return (
    <div>
      <h1 className="text-2xl font-semibold">Welcome back, {user.firstName}</h1>
      <p className="mt-2 text-gray-600">
        Live readings, alerts and inspections will appear here.
      </p>
    </div>
  );
}