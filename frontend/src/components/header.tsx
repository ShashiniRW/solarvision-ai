import { LogoutButton } from "@/components/logout-button";
import type { CurrentUser } from "@/lib/session";

export function Header({ user }: { user: CurrentUser }) {
  return (
    <header className="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-3">
      <span className="font-semibold text-amber-600 md:hidden">SolarVision AI</span>
      <span className="hidden md:block" />
      <div className="flex items-center gap-4">
        <div className="text-right">
          <p className="text-sm font-medium">
            {user.firstName} {user.lastName}
          </p>
          <p className="text-xs text-gray-500">{user.roles.join(" · ")}</p>
        </div>
        <LogoutButton />
      </div>
    </header>
  );
}