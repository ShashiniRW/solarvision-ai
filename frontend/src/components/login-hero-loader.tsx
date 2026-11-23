"use client";

import dynamic from "next/dynamic";

// The 3D scene needs the browser (WebGL) and is large: load it only on the login page, only in the browser
export const LoginHeroView = dynamic(
  () => import("@/components/login-hero").then((module) => module.LoginHero),
  {
    ssr: false,
    loading: () => <div className="h-full w-full bg-linear-to-br from-sky-400 to-amber-300" />,
  },
);
