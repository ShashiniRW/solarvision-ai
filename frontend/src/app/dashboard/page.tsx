import { LogoutButton } from "@/components/logout-button";

export default function DashboardPage() {
  return (
    <main className="p-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Dashboard</h1>
        <LogoutButton />
      </div>
      <p className="mt-4 opacity-70">You are signed in. Dashboard content comes next.</p>
    </main>
  );
}