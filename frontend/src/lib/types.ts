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