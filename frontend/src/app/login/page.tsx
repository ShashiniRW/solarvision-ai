import { LoginForm } from "@/components/login-form";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ next?: string | string[]; expired?: string }>;
}) {
  const { next, expired } = await searchParams;

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-xl bg-white p-8 shadow">
        <h1 className="text-2xl font-semibold text-gray-900">SolarVision AI</h1>
        <p className="mt-1 text-sm text-gray-500">Sign in to your account</p>

        {expired && (
          <p role="status" className="mt-4 rounded-md bg-amber-50 p-3 text-sm text-amber-800">
            Your session has expired. Please sign in again.
          </p>
        )}

        <LoginForm next={typeof next === "string" ? next : undefined} />
      </div>
    </main>
  );
}