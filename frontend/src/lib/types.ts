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
  totalEnergyKwh: number;
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
};