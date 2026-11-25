"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Logo } from "@/components/logo";
import { NAV_ITEMS } from "@/lib/navigation";

/** The page links. Shared by the desktop sidebar and the mobile menu. */
export function NavLinks({ roles, onNavigate }: { roles: string[]; onNavigate?: () => void }) {
  const pathname = usePathname();

  const items = NAV_ITEMS.filter(
    (item) => !item.roles || item.roles.some((role) => roles.includes(role)),
  );

  return (
    <nav aria-label="Main" className="px-3">
      <ul className="space-y-1">
        {items.map((item) => {
          const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                onClick={onNavigate}
                aria-current={active ? "page" : undefined}
                className={`block rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  active
                    ? "bg-amber-50 text-amber-700 dark:bg-amber-500/10 dark:text-amber-400"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
                }`}
              >
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

/** Desktop sidebar (hidden on phones, where the mobile menu is used instead). */
export function Sidebar({ roles }: { roles: string[] }) {
  return (
    <aside className="hidden w-60 shrink-0 border-r bg-card md:block">
      <Link href="/dashboard" className="flex px-6 py-5 text-lg" aria-label="SolarVision AI home">
        <Logo />
      </Link>
      <NavLinks roles={roles} />
    </aside>
  );
}
