"use client";

import dynamic from "next/dynamic";

// three.js needs the browser (WebGL) and is large, so it is loaded only when this view is shown
export const SiteTwinView = dynamic(
  () => import("@/components/site-twin").then((module) => module.SiteTwin),
  {
    ssr: false,
    loading: () => <div className="h-full w-full animate-pulse bg-muted" />,
  },
);
