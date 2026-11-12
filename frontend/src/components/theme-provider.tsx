"use client";

import { ThemeProvider as NextThemesProvider } from "next-themes";

// Wraps the whole app so every page knows the current theme (light / dark / system)
export function ThemeProvider({
  children,
  ...props
}: React.ComponentProps<typeof NextThemesProvider>) {
  return <NextThemesProvider {...props}>{children}</NextThemesProvider>;
}
