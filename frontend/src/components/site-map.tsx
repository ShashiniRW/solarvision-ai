"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import type { Map as LeafletMap } from "leaflet";
import "leaflet/dist/leaflet.css";
import { HEALTH_COLORS, HEALTH_LABELS } from "@/lib/site-health";
import type { MapSite } from "@/lib/types";

// Map tiles: OpenStreetMap by default. A production deployment can switch to a commercial
// tile provider by setting these two variables, without code changes.
const TILE_URL =
  process.env.NEXT_PUBLIC_MAP_TILE_URL ?? "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const TILE_ATTRIBUTION =
  process.env.NEXT_PUBLIC_MAP_ATTRIBUTION ??
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

// Sri Lanka: shown when no site has coordinates yet
const DEFAULT_CENTER: [number, number] = [7.87, 80.77];
const DEFAULT_ZOOM = 7;

type LocatedSite = MapSite & { latitude: number; longitude: number };

function isLocated(site: MapSite): site is LocatedSite {
  return site.latitude !== null && site.longitude !== null;
}

// Popup content is built from DOM nodes with textContent, never from an HTML string,
// so a site name can never inject HTML or scripts (XSS).
function buildPopup(site: MapSite, navigate: (href: string) => void): HTMLElement {
  const root = document.createElement("div");
  root.className = "space-y-1 text-sm";

  const title = document.createElement("p");
  title.className = "font-semibold";
  title.textContent = site.name;

  const project = document.createElement("p");
  project.className = "text-xs text-gray-500";
  project.textContent = site.projectName;

  const status = document.createElement("p");
  status.textContent =
    `${HEALTH_LABELS[site.health]}: ${site.criticalAlerts} critical, ` +
    `${site.warningAlerts} warning alert(s)`;

  const details = document.createElement("p");
  const capacity = site.capacityKw === null ? "Capacity unknown" : `${site.capacityKw} kW`;
  details.textContent = `${capacity} · ${site.equipmentCount} equipment`;

  const link = document.createElement("a");
  link.href = `/sites/${site.id}`;
  link.className = "font-medium text-amber-600 underline underline-offset-2";
  link.textContent = "Open site";
  // navigate inside the app (no full page reload)
  link.addEventListener("click", (event) => {
    event.preventDefault();
    navigate(`/sites/${site.id}`);
  });

  root.append(title, project, status, details, link);
  return root;
}

function buildTooltip(site: MapSite): HTMLElement {
  const span = document.createElement("span");
  span.textContent = site.name;
  return span;
}

export function SiteMap({ sites }: { sites: MapSite[] }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }
    let map: LeafletMap | undefined;
    let cancelled = false;

    // Leaflet needs the browser (window), so it is loaded here only, never on the server
    import("leaflet").then(({ default: L }) => {
      if (cancelled) {
        return;
      }
      map = L.map(container, { scrollWheelZoom: false }); // the page still scrolls normally
      L.tileLayer(TILE_URL, { attribution: TILE_ATTRIBUTION, maxZoom: 19 }).addTo(map);

      const located = sites.filter(isLocated);
      for (const site of located) {
        L.circleMarker([site.latitude, site.longitude], {
          radius: 10,
          color: "#ffffff",
          weight: 2,
          fillColor: HEALTH_COLORS[site.health],
          fillOpacity: 0.9,
        })
          .bindTooltip(buildTooltip(site))
          .bindPopup(() => buildPopup(site, (href) => router.push(href)))
          .addTo(map);
      }

      if (located.length > 0) {
        const bounds = L.latLngBounds(
          located.map((site) => [site.latitude, site.longitude] as [number, number]),
        );
        map.fitBounds(bounds, { padding: [40, 40], maxZoom: 13 });
      } else {
        map.setView(DEFAULT_CENTER, DEFAULT_ZOOM);
      }
    });

    // remove the map when the page is left (or re-rendered), so nothing leaks
    return () => {
      cancelled = true;
      map?.remove();
    };
  }, [sites, router]);

  return (
    <div
      ref={containerRef}
      role="region"
      aria-label="Map of solar sites. The same sites are listed in the table below."
      // isolate: keeps Leaflet's layers below the app's menus and dialogs
      className="isolate h-112 w-full overflow-hidden rounded-xl border"
    />
  );
}
