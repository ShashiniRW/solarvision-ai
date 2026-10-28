"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

// Re-loads the data of the current page every few seconds. Does nothing while the tab is hidden.
export function AutoRefresh({ seconds }: { seconds: number }) {
  const router = useRouter();

  useEffect(() => {
    const timer = setInterval(() => {
      if (!document.hidden) {
        router.refresh();
      }
    }, seconds * 1000);

    return () => clearInterval(timer);
  }, [router, seconds]);

  return null;
}