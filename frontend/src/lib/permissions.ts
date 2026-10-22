// These lists match the @PreAuthorize rules in the backend, which enforces them.
const FIELD_ROLES = ["ADMIN", "MANAGER", "ENGINEER", "TECHNICIAN"];
const COORDINATOR_ROLES = ["ADMIN", "MANAGER", "ENGINEER"];

function hasAny(roles: string[], allowed: string[]): boolean {
  return roles.some((role) => allowed.includes(role));
}

// acknowledge and resolve alerts
export function canManageAlerts(roles: string[]): boolean {
  return hasAny(roles, FIELD_ROLES);
}

// start and complete inspections
export function canWorkInspections(roles: string[]): boolean {
  return hasAny(roles, FIELD_ROLES);
}

// cancel inspections
export function canCancelInspections(roles: string[]): boolean {
  return hasAny(roles, COORDINATOR_ROLES);
}