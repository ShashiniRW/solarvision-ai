import { LoginForm } from "@/components/login-form";
import { ModeToggle } from "@/components/mode-toggle";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ next?: string | string[]; expired?: string }>;
}) {
  const { next, expired } = await searchParams;

  return (
    <main className="relative flex min-h-screen items-center justify-center bg-muted/40 px-4 text-foreground">
      {/* theme can be chosen before signing in */}
      <div className="absolute top-4 right-4">
        <ModeToggle />
      </div>

      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-2xl text-amber-600 dark:text-amber-500">
            SolarVision AI
          </CardTitle>
          <CardDescription>Sign in to your account</CardDescription>
        </CardHeader>
        <CardContent>
          {expired && (
            <p
              role="status"
              className="mb-4 rounded-md bg-amber-50 p-3 text-sm text-amber-800 dark:bg-amber-500/10 dark:text-amber-300"
            >
              Your session has expired. Please sign in again.
            </p>
          )}

          <LoginForm next={typeof next === "string" ? next : undefined} />
        </CardContent>
      </Card>
    </main>
  );
}
