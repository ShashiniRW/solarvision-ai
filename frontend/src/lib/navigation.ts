export type NavItem = {
  label: string;
  href: string;
  // if set, only users with at least one of these roles see the item
  roles?: string[];
};

// Items are added here as each page is built.
export const NAV_ITEMS: NavItem[] = [
  { label: "Dashboard", href: "/dashboard" },
  { label: "Projects", href: "/projects" },
  { label: "Map", href: "/map" },
  { label: "Alerts", href: "/alerts" },
  { label: "Inspections", href: "/inspections" },
  // everyone: each user only gets reports for the projects they may see
  { label: "Reports", href: "/reports" },
  // no roles: every signed-in user may use the assistant (same rule as the backend)
  { label: "AI Assistant", href: "/assistant" },
  // user management: administrators only (the backend enforces this too)
  { label: "Users", href: "/users", roles: ["ADMIN"] },
];
