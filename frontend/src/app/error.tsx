"use client";

import { startTransition } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { brandButtonClass } from "@/lib/styles";

export default function ErrorPage({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const router = useRouter();

  // Errors in server pages need the data to be fetched again, not just the screen re-drawn
  function retry() {
    startTransition(() => {
      router.refresh();
      reset();
    });
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 px-4 text-foreground">
      <div
        role="alert"
        className="max-w-md rounded-xl border border-red-200 bg-card p-8 text-center dark:border-red-900/60"
      >
        <h1 className="text-lg font-semibold text-red-700 dark:text-red-400">
          Something went wrong
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          We could not load this page. The server may be temporarily unavailable. Please try again.
        </p>
        {error.digest && (
          <p className="mt-2 font-mono text-xs text-muted-foreground">Reference: {error.digest}</p>
        )}
        <Button type="button" onClick={retry} className={`mt-4 ${brandButtonClass}`}>
          Try again
        </Button>
      </div>
    </main>
  );
}
