import { LogoutButton } from "@/components/logout-button";
import { ModeToggle } from "@/components/mode-toggle";
import type { CurrentUser } from "@/lib/session";

export function Header({ user }: { user: CurrentUser }) {
  return (
    <header className="flex items-center justify-between border-b bg-card px-6 py-3">
      <span className="font-semibold text-amber-600 md:hidden dark:text-amber-500">
        SolarVision AI
      </span>
      <span className="hidden md:block" />
      <div className="flex items-center gap-3">
        <div className="text-right">
          <p className="text-sm font-medium">
            {user.firstName} {user.lastName}
          </p>
          <p className="text-xs text-muted-foreground">{user.roles.join(" · ")}</p>
        </div>
        <ModeToggle />
        <LogoutButton />
      </div>
    </header>
  );
}
