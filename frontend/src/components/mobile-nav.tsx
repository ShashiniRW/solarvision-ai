"use client";

import { Menu, X } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { Logo } from "@/components/logo";
import { NavLinks } from "@/components/sidebar";
import { cn } from "@/lib/utils";

/** The ☰ button and slide-in menu for phones (md and larger use the sidebar). */
export function MobileNav({ roles }: { roles: string[] }) {
  const [open, setOpen] = useState(false);
  const openButton = useRef<HTMLButtonElement>(null);
  const closeButton = useRef<HTMLButtonElement>(null);

  // while open: focus the close button, stop the page behind from scrolling, Esc closes
  useEffect(() => {
    if (!open) return;
    const trigger = openButton.current;
    closeButton.current?.focus();
    document.body.style.overflow = "hidden";

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("keydown", onKeyDown);

    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = "";
      trigger?.focus(); // keyboard users land back on the ☰ button
    };
  }, [open]);

  const close = () => setOpen(false);

  return (
    <>
      <button
        ref={openButton}
        type="button"
        onClick={() => setOpen(true)}
        aria-label="Open menu"
        aria-expanded={open}
        aria-controls="mobile-menu"
        className="-ml-2 inline-flex size-10 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground md:hidden"
      >
        <Menu className="size-5" />
      </button>

      <div
        id="mobile-menu"
        role="dialog"
        aria-modal="true"
        aria-label="Menu"
        inert={!open}
        className={cn(
          "fixed inset-0 z-50 md:hidden motion-safe:transition-[visibility] motion-safe:duration-200",
          open ? "visible" : "invisible",
        )}
      >
        {/* dark background: tap to close */}
        <div
          aria-hidden="true"
          onClick={close}
          className={cn(
            "absolute inset-0 bg-black/50 motion-safe:transition-opacity motion-safe:duration-200",
            open ? "opacity-100" : "opacity-0",
          )}
        />

        {/* the sliding panel */}
        <div
          className={cn(
            "absolute inset-y-0 left-0 flex w-72 max-w-[85%] flex-col border-r bg-card shadow-xl motion-safe:transition-transform motion-safe:duration-200",
            open ? "translate-x-0" : "-translate-x-full",
          )}
        >
          <div className="flex items-center justify-between px-6 py-4">
            <Link href="/dashboard" onClick={close} className="text-lg" aria-label="SolarVision AI home">
              <Logo />
            </Link>
            <button
              ref={closeButton}
              type="button"
              onClick={close}
              aria-label="Close menu"
              className="-mr-2 inline-flex size-10 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground"
            >
              <X className="size-5" />
            </button>
          </div>
          <div className="flex-1 overflow-y-auto pb-6">
            <NavLinks roles={roles} onNavigate={close} />
          </div>
        </div>
      </div>
    </>
  );
}
