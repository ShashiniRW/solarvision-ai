"use client";

export default function ErrorPage({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md rounded-lg border border-red-200 bg-white p-8 text-center">
        <h1 className="text-lg font-semibold text-red-700">Something went wrong</h1>
        <p className="mt-2 text-sm text-gray-600">
          We could not load this page. The server may be temporarily unavailable. Please try again.
        </p>
        {error.digest && <p className="mt-2 text-xs text-gray-400">Reference: {error.digest}</p>}
        <button
          onClick={reset}
          className="mt-4 rounded-md bg-amber-500 px-4 py-2 text-sm font-medium text-white hover:bg-amber-600"
        >
          Try again
        </button>
      </div>
    </main>
  );
}