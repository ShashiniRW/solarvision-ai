// Roles allowed to acknowledge and resolve alerts. The backend enforces the same rule.
const ALERT_MANAGER_ROLES = ["ADMIN", "MANAGER", "ENGINEER", "TECHNICIAN"];

export function canManageAlerts(roles: string[]): boolean {
  return roles.some((role) => ALERT_MANAGER_ROLES.includes(role));
}