import { KeyRound } from "lucide-react";
import Link from "next/link";
import { Logo } from "@/components/logo";
import { LogoutButton } from "@/components/logout-button";
import { MobileNav } from "@/components/mobile-nav";
import { ModeToggle } from "@/components/mode-toggle";
import type { CurrentUser } from "@/lib/session";

export function Header({ user }: { user: CurrentUser }) {
  return (
    <header className="flex items-center justify-between gap-3 border-b bg-card px-4 py-2 md:px-6 md:py-3">
      {/* phones: menu button + logo (desktop shows the logo in the sidebar) */}
      <div className="flex items-center gap-1 md:hidden">
        <MobileNav roles={user.roles} />
        <Link href="/dashboard" aria-label="SolarVision AI home">
          <Logo />
        </Link>
      </div>
      <span className="hidden md:block" />

      <div className="flex items-center gap-2 sm:gap-3">
        <div className="hidden text-right sm:block">
          <p className="text-sm font-medium">
            {user.firstName} {user.lastName}
          </p>
          <p className="text-xs text-muted-foreground">{user.roles.join(" · ")}</p>
        </div>
        <Link
          href="/change-password"
          aria-label="Change password"
          title="Change password"
          className="inline-flex size-9 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground"
        >
          <KeyRound className="size-4" />
        </Link>
        <ModeToggle />
        <LogoutButton />
      </div>
    </header>
  );
}
