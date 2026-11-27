import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { UsersManager } from "@/components/users-manager";
import { apiGet } from "@/lib/api";
import { getCurrentUser } from "@/lib/session";
import type { ManagedUser } from "@/lib/types";

export const metadata: Metadata = {
  title: "Users",
};

export default async function UsersPage() {
  const user = await getCurrentUser();

  // the backend enforces this too; here it just avoids showing a page that cannot work
  if (!user.roles.includes("ADMIN")) {
    notFound();
  }

  const users = await apiGet<ManagedUser[]>("/api/admin/users");

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold">Users</h1>
        <p className="text-sm text-muted-foreground">
          Create accounts, reset passwords and deactivate people who no longer need access.
        </p>
      </div>
      <UsersManager users={users} currentUserId={user.id} />
    </div>
  );
}
