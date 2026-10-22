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
  { label: "Alerts", href: "/alerts" },
];