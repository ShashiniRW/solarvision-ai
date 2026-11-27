export type Project = {
  id: number;
  organizationId: number;
  organizationName: string;
  customer: unknown;
  name: string;
  description: string | null;
  status: string;
  startDate: string | null;
  endDate: string | null;
  createdAt: string;
  updatedAt: string;
};

export type Site = {
  id: number;
  projectId: number;
  projectName: string;
  name: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  capacityKw: number | null;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type Equipment = {
  id: number;
  siteId: number;
  siteName: string;
  equipmentType: string;
  manufacturer: string | null;
  model: string | null;
  serialNumber: string | null;
  qrCode: string;
  capacityKw: number | null;
  installationDate: string | null;
  warrantyExpiryDate: string | null;
  warrantyYears: number | null;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type SensorReading = {
  id: number;
  equipmentId: number;
  recordedAt: string;
  powerKw: number | null;
  energyKwh: number | null;
  voltageV: number | null;
  currentA: number | null;
  temperatureC: number | null;
  isSimulated: boolean;
};

export type DailyEnergySummary = {
  date: string;
  totalEnergyKwh: number | null; // null = readings exist, but none had an energy value
  peakPowerKw: number | null;
  avgTemperatureC: number | null;
  readingCount: number;
};

export type Alert = {
  id: number;
  equipmentId: number;
  equipmentType: string;
  equipmentSerialNumber: string | null;
  alertType: string;
  severity: string;
  status: string;
  message: string;
  triggeredAt: string;
  acknowledgedAt: string | null;
  acknowledgedBy: number | null;
  resolvedAt: string | null;
  resolvedBy: number | null;
  readingId: number | null;
};

export type Inspection = {
  id: number;
  equipmentId: number;
  equipmentType: string;
  equipmentSerialNumber: string | null;
  alertId: number | null;
  inspectionType: string;
  status: string;
  scheduledAt: string;
  assignedTo: number | null;
  createdBy: number | null;
  startedAt: string | null;
  completedAt: string | null;
  result: string | null;
  findings: string | null;
};

export type DashboardSummary = {
  openAlerts: number;
  acknowledgedAlerts: number;
  unresolvedCriticalAlerts: number;
  scheduledInspections: number;
  inProgressInspections: number;
  overdueInspections: number;
  equipmentTotal: number;
  equipmentActive: number;
  equipmentFaulty: number;
  energyTodayKwh: number;
  currentPowerKw: number; // newest inverter readings from the last 15 minutes
  capacityKw: number; // capacity of the active inverters
};

export type ForecastDay = {
  date: string;
  weatherCode: number | null;
  temperatureMaxC: number | null;
  temperatureMinC: number | null;
  precipitationProbabilityPercent: number | null;
  sunHours: number | null;
  expectedEnergyKwh: number | null;
};

export type SiteForecast = {
  siteId: number;
  capacityKw: number | null;
  performanceRatio: number;
  attribution: string;
  days: ForecastDay[];
};

// ---------- AI assistant ----------

// "model" is the assistant (the name Gemini uses for it)
export type ChatRole = "user" | "model";

// A guide section an answer is based on
export type ChatSource = {
  document: string;
  title: string;
};

export type ChatReply = {
  reply: string;
  model: string;
  sources: ChatSource[];
};

// ---------- Site map ----------

// The worst unresolved alert decides a site's colour on the map
export type SiteHealth = "OK" | "WARNING" | "CRITICAL";

export type MapSite = {
  id: number;
  name: string;
  projectId: number;
  projectName: string;
  // null when the site has no coordinates yet
  latitude: number | null;
  longitude: number | null;
  capacityKw: number | null;
  equipmentCount: number;
  criticalAlerts: number;
  warningAlerts: number;
  health: SiteHealth;
};

export type LiveEventType = "ALERT_CREATED" | "ALERT_UPDATED" | "READINGS_RECORDED";

export type LiveEvent = {
  type: LiveEventType;
  projectId: number;
  severity: string | null;
  message: string | null;
};

// ---------- 3D digital twin ----------

export type TwinEquipment = {
  id: number;
  equipmentType: string;
  serialNumber: string | null;
  model: string | null;
  capacityKw: number | null;
  status: string;
  health: SiteHealth;
  powerKw: number | null; // null = no readings yet
  temperatureC: number | null;
  lastReadingAt: string | null;
};

export type SiteTwin = {
  siteId: number;
  siteName: string;
  equipment: TwinEquipment[];
};


// ---------- Dashboard: sites today ----------

export type SiteToday = {
  siteId: number;
  siteName: string;
  projectId: number;
  projectName: string;
  capacityKw: number | null; // installed capacity (null = unknown)
  energyTodayKwh: number; // since midnight, Colombo time
  currentPowerKw: number; // newest inverter readings from the last 15 minutes
  criticalAlerts: number;
  warningAlerts: number;
  health: SiteHealth;
};


// ---------- User management ----------

export type ManagedUser = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  status: string; // ACTIVE | INACTIVE
  roles: string[];
  organizationName: string;
  mustChangePassword: boolean;
  createdAt: string | null;
};

// What a server action returns to the page: the data, or a message to show
export type ActionResult<T = null> = { ok: true; data: T } | { ok: false; error: string };
