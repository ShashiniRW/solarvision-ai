"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { NAV_ITEMS } from "@/lib/navigation";

export function Sidebar({ roles }: { roles: string[] }) {
  const pathname = usePathname();

  const items = NAV_ITEMS.filter(
    (item) => !item.roles || item.roles.some((role) => roles.includes(role)),
  );

  return (
    <aside className="hidden w-60 shrink-0 border-r border-gray-200 bg-white md:block">
      <div className="px-6 py-5 text-lg font-semibold text-amber-600">SolarVision AI</div>
      <nav className="px-3">
        <ul className="space-y-1">
          {items.map((item) => {
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={`block rounded-md px-3 py-2 text-sm font-medium ${
                    active ? "bg-amber-50 text-amber-700" : "text-gray-700 hover:bg-gray-100"
                  }`}
                >
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </aside>
  );
}