import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Docker: build a small self-contained server
  output: "standalone",
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
