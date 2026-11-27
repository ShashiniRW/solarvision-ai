import { redirect } from "next/navigation";
import { Header } from "@/components/header";
import { LiveUpdates } from "@/components/live-updates";
import { Sidebar } from "@/components/sidebar";
import { getCurrentUser } from "@/lib/session";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const user = await getCurrentUser();

  // someone with a temporary password must choose their own password before anything else
  if (user.mustChangePassword) {
    redirect("/change-password");
  }

  return (
    // theme colours: light grey / dark page background, readable text in both modes
    // print: white page, no sidebar/header/live pill (see print:hidden)
    <div className="flex min-h-screen bg-muted/40 text-foreground print:bg-white">
      <Sidebar roles={user.roles} />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header user={user} />
        <main className="flex-1 p-4 md:p-6 print:p-0">{children}</main>
      </div>
      {/* one live connection for every signed-in page (not printed) */}
      <div className="print:hidden">
        <LiveUpdates />
      </div>
    </div>
  );
}
