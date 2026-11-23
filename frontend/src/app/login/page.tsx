import { LoginForm } from "@/components/login-form";
import { LoginHeroView } from "@/components/login-hero-loader";
import { Logo } from "@/components/logo";
import { ModeToggle } from "@/components/mode-toggle";

const FEATURES = ["Live monitoring", "AI anomaly detection", "3D digital twin", "Maintenance workflow"];

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ next?: string | string[]; expired?: string }>;
}) {
  const { next, expired } = await searchParams;

  return (
    <main className="grid min-h-screen bg-background text-foreground lg:grid-cols-[1.15fr_1fr]">
      {/* Left: the living 3D solar farm (large screens only) */}
      <section className="relative hidden overflow-hidden lg:block" aria-label="Animated solar farm">
        <div className="absolute inset-0">
          <LoginHeroView />
        </div>
        <div className="pointer-events-none absolute inset-0 bg-linear-to-t from-black/70 via-black/10 to-transparent" />
        <div className="pointer-events-none absolute inset-x-0 bottom-0 p-10 text-white">
          <p className="text-sm font-medium uppercase tracking-widest text-amber-300">
            AI-powered solar operations
          </p>
          <h2 className="mt-2 max-w-lg text-4xl font-semibold leading-tight">
            Every panel, every minute, in one live view.
          </h2>
          <ul className="mt-6 flex flex-wrap gap-2 text-sm">
            {FEATURES.map((feature) => (
              <li key={feature} className="rounded-full bg-white/15 px-3 py-1 backdrop-blur">
                {feature}
              </li>
            ))}
          </ul>
        </div>
      </section>

      {/* Right: sign in */}
      <section className="relative flex items-center justify-center px-6 py-12">
        <div className="absolute top-4 right-4">
          <ModeToggle />
        </div>

        <div className="w-full max-w-sm animate-fade-up">
          <Logo className="text-xl" />
          <h1 className="mt-10 text-2xl font-semibold">Welcome back</h1>
          <p className="mt-1 text-sm text-muted-foreground">Sign in to monitor your solar sites.</p>

          {expired && (
            <p
              role="status"
              className="mt-6 rounded-md bg-amber-50 p-3 text-sm text-amber-800 dark:bg-amber-500/10 dark:text-amber-300"
            >
              Your session has expired. Please sign in again.
            </p>
          )}

          <div className="mt-6">
            <LoginForm next={typeof next === "string" ? next : undefined} />
          </div>

          <p className="mt-8 text-xs text-muted-foreground">
            Accounts are created by your administrator.
          </p>
        </div>
      </section>
    </main>
  );
}
