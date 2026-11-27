import type { Metadata } from "next";
import Link from "next/link";
import { ChangePasswordForm } from "@/components/change-password-form";
import { Logo } from "@/components/logo";
import { LogoutButton } from "@/components/logout-button";
import { getCurrentUser } from "@/lib/session";
import { linkClass } from "@/lib/styles";

export const metadata: Metadata = {
  title: "Change password",
};

// Outside the (app) layout on purpose: a user who MUST change their password
// is sent here and cannot open any other page until they have done it.
export default async function ChangePasswordPage() {
  const user = await getCurrentUser();

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 px-6 py-12 text-foreground">
      <div className="w-full max-w-sm rounded-xl border bg-card p-6 shadow-sm">
        <Logo className="text-lg" />
        <h1 className="mt-8 text-xl font-semibold">
          {user.mustChangePassword ? "Choose your own password" : "Change password"}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {user.mustChangePassword
            ? `Welcome, ${user.firstName}. You signed in with a temporary password. Please choose your own password to continue.`
            : "Use at least 12 characters. A short sentence is easy to remember and hard to guess."}
        </p>

        <div className="mt-6">
          <ChangePasswordForm />
        </div>

        <div className="mt-6 flex items-center justify-between text-sm">
          {user.mustChangePassword ? (
            <span className="text-muted-foreground">Not you?</span>
          ) : (
            <Link href="/dashboard" className={linkClass}>
              &larr; Back to dashboard
            </Link>
          )}
          <LogoutButton />
        </div>
      </div>
    </main>
  );
}
