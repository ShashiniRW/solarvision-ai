import { Header } from "@/components/header";
import { Sidebar } from "@/components/sidebar";
import { getCurrentUser } from "@/lib/session";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const user = await getCurrentUser();

  return (
    // theme colours: light grey / dark page background, readable text in both modes
    <div className="flex min-h-screen bg-muted/40 text-foreground">
      <Sidebar roles={user.roles} />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header user={user} />
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
