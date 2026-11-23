"use client";

import { Html, OrbitControls } from "@react-three/drei";
import { Canvas, useFrame, type ThreeEvent } from "@react-three/fiber";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import type { Mesh } from "three";
import { HEALTH_COLORS, HEALTH_LABELS } from "@/lib/site-health";
import { sunPosition, type SunPosition } from "@/lib/sun";
import type { SiteHealth, TwinEquipment } from "@/lib/types";

// Layout (1 unit = about 1 metre)
const PANEL_GAP_X = 3.4;
const PANEL_GAP_Z = 3.2;
const DEVICE_GAP_X = 2.4;
const DEVICE_ROW_Z = 4; // inverters, batteries, meters and sensors stand in a row in front of the panels
const PANEL_TILT = (10 * Math.PI) / 180; // about 10 degrees, facing south

// height of the status light above each type of equipment
const LIGHT_HEIGHT: Record<string, number> = {
  PANEL: 1.9,
  INVERTER: 1.5,
  BATTERY: 1.7,
  METER: 1.8,
  SENSOR: 2.1,
};

type Placed = { item: TwinEquipment; x: number; z: number };

/** Panels in a square-ish grid, all other equipment in one row in front of them. */
function layoutEquipment(items: TwinEquipment[]) {
  const panels = items.filter((item) => item.equipmentType === "PANEL");
  const devices = items.filter((item) => item.equipmentType !== "PANEL");

  const perRow = Math.max(1, Math.ceil(Math.sqrt(panels.length)));
  const rows = Math.ceil(panels.length / perRow);

  const placed: Placed[] = panels.map((item, index) => ({
    item,
    x: ((index % perRow) - (perRow - 1) / 2) * PANEL_GAP_X,
    z: -Math.floor(index / perRow) * PANEL_GAP_Z,
  }));
  devices.forEach((item, index) => {
    placed.push({ item, x: (index - (devices.length - 1) / 2) * DEVICE_GAP_X, z: DEVICE_ROW_Z });
  });

  const width = Math.max(perRow * PANEL_GAP_X, devices.length * DEVICE_GAP_X);
  const depth = Math.max(rows - 1, 0) * PANEL_GAP_Z + DEVICE_ROW_Z;
  return {
    placed,
    size: Math.max(width, depth, 8),
    centerZ: (DEVICE_ROW_Z - Math.max(rows - 1, 0) * PANEL_GAP_Z) / 2,
  };
}

function equipmentLabel(item: TwinEquipment) {
  return `${item.equipmentType} · ${item.serialNumber ?? item.id}`;
}

function readingText(item: TwinEquipment) {
  if (item.powerKw === null && item.temperatureC === null) {
    return "No readings yet";
  }
  const power = item.powerKw !== null ? `${item.powerKw.toFixed(2)} kW` : "- kW";
  const temperature = item.temperatureC !== null ? `${item.temperatureC.toFixed(1)} °C` : "- °C";
  return `${power} · ${temperature}`;
}

/** The 3D shape of each equipment type. */
function Body({ type, muted }: { type: string; muted: boolean }) {
  const metal = muted ? "#9ca3af" : "#d1d5db";
  const dark = "#4b5563";

  switch (type) {
    case "PANEL":
      return (
        <group>
          {[-1.1, 1.1].map((legX) => (
            <mesh key={legX} position={[legX, 0.5, 0]} castShadow>
              <boxGeometry args={[0.08, 1, 0.08]} />
              <meshStandardMaterial color={dark} />
            </mesh>
          ))}
          <mesh position={[0, 1.05, 0]} rotation={[PANEL_TILT, 0, 0]} castShadow receiveShadow>
            <boxGeometry args={[2.8, 0.06, 1.7]} />
            <meshStandardMaterial color={muted ? "#475569" : "#1e3a8a"} metalness={0.5} roughness={0.25} />
          </mesh>
        </group>
      );
    case "INVERTER":
      return (
        <mesh position={[0, 0.55, 0]} castShadow>
          <boxGeometry args={[0.9, 1.1, 0.4]} />
          <meshStandardMaterial color={metal} metalness={0.3} roughness={0.5} />
        </mesh>
      );
    case "BATTERY":
      return (
        <mesh position={[0, 0.65, 0]} castShadow>
          <boxGeometry args={[1.2, 1.3, 0.7]} />
          <meshStandardMaterial color={muted ? "#9ca3af" : "#f3f4f6"} roughness={0.6} />
        </mesh>
      );
    case "METER":
      return (
        <group>
          <mesh position={[0, 0.6, 0]} castShadow>
            <boxGeometry args={[0.08, 1.2, 0.08]} />
            <meshStandardMaterial color={dark} />
          </mesh>
          <mesh position={[0, 1.25, 0]} castShadow>
            <boxGeometry args={[0.45, 0.55, 0.2]} />
            <meshStandardMaterial color={metal} />
          </mesh>
        </group>
      );
    case "SENSOR":
      return (
        <group>
          <mesh position={[0, 0.8, 0]} castShadow>
            <cylinderGeometry args={[0.03, 0.03, 1.6, 12]} />
            <meshStandardMaterial color={dark} />
          </mesh>
          <mesh position={[0, 1.65, 0]} castShadow>
            <sphereGeometry args={[0.15, 16, 16]} />
            <meshStandardMaterial color={metal} />
          </mesh>
        </group>
      );
    default:
      return (
        <mesh position={[0, 0.4, 0]} castShadow>
          <boxGeometry args={[0.8, 0.8, 0.8]} />
          <meshStandardMaterial color={metal} />
        </mesh>
      );
  }
}

/** Coloured light above the equipment. Critical = pulsing. */
function StatusLight({ health, height }: { health: SiteHealth; height: number }) {
  const ref = useRef<Mesh>(null);

  useFrame(({ clock }) => {
    if (!ref.current) return;
    const scale = health === "CRITICAL" ? 1 + 0.3 * Math.sin(clock.elapsedTime * 4) : 1;
    ref.current.scale.setScalar(scale);
  });

  const color = HEALTH_COLORS[health];
  return (
    <mesh ref={ref} position={[0, height, 0]}>
      <sphereGeometry args={[0.16, 20, 20]} />
      <meshStandardMaterial color={color} emissive={color} emissiveIntensity={0.9} />
    </mesh>
  );
}

function EquipmentModel({
  placed,
  hovered,
  onHover,
  onOpen,
}: {
  placed: Placed;
  hovered: boolean;
  onHover: (id: number | null) => void;
  onOpen: (id: number) => void;
}) {
  const { item, x, z } = placed;
  const color = HEALTH_COLORS[item.health];
  const muted = item.status === "INACTIVE" || item.status === "DECOMMISSIONED";
  const lightHeight = LIGHT_HEIGHT[item.equipmentType] ?? 1.3;

  return (
    <group
      position={[x, 0, z]}
      onPointerOver={(event: ThreeEvent<PointerEvent>) => {
        event.stopPropagation();
        onHover(item.id);
      }}
      onPointerOut={() => onHover(null)}
      onClick={(event: ThreeEvent<MouseEvent>) => {
        event.stopPropagation();
        onOpen(item.id);
      }}
    >
      {/* coloured circle on the ground = health at a glance */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.01, 0]} receiveShadow>
        <circleGeometry args={[hovered ? 1.7 : 1.5, 40]} />
        <meshStandardMaterial color={color} transparent opacity={hovered ? 0.5 : 0.25} />
      </mesh>

      <Body type={item.equipmentType} muted={muted} />
      <StatusLight health={item.health} height={lightHeight} />

      {hovered && (
        <Html position={[0, lightHeight + 0.5, 0]} center style={{ pointerEvents: "none" }}>
          <div className="whitespace-nowrap rounded-md border bg-card px-2 py-1 text-xs text-card-foreground shadow-md">
            <p className="font-semibold">{equipmentLabel(item)}</p>
            <p>
              {HEALTH_LABELS[item.health]} · {item.status}
            </p>
            <p className="tabular-nums">{readingText(item)}</p>
          </div>
        </Html>
      )}
    </group>
  );
}

function SceneLights({ sun, size }: { sun: SunPosition; size: number }) {
  const half = size * 1.5; // the area the sun's shadows cover

  return (
    <>
      <color attach="background" args={[sun.isDay ? "#cfe3f7" : "#0b1220"]} />
      <hemisphereLight args={["#ffffff", "#4b5563", sun.isDay ? 0.6 : 0.15]} />
      <ambientLight intensity={sun.isDay ? 0.35 : 0.15} />
      {sun.isDay ? (
        <directionalLight
          position={sun.position}
          intensity={0.6 + 1.6 * sun.strength}
          castShadow
          shadow-mapSize={[1024, 1024]}
          shadow-camera-left={-half}
          shadow-camera-right={half}
          shadow-camera-top={half}
          shadow-camera-bottom={-half}
          shadow-camera-far={size * 6}
        />
      ) : (
        // moonlight
        <directionalLight position={[-size, size * 1.5, -size]} intensity={0.35} color="#93c5fd" />
      )}
    </>
  );
}

export function SiteTwin({
  equipment,
  colomboHour,
}: {
  equipment: TwinEquipment[];
  colomboHour: number;
}) {
  const router = useRouter();
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  const { placed, size, centerZ } = layoutEquipment(equipment);
  const sun = sunPosition(colomboHour, size * 2);
  const hasCritical = equipment.some((item) => item.health === "CRITICAL");

  // hand cursor while pointing at equipment
  useEffect(() => {
    document.body.style.cursor = hoveredId !== null ? "pointer" : "auto";
    return () => {
      document.body.style.cursor = "auto";
    };
  }, [hoveredId]);

  return (
    <Canvas
      shadows="percentage" // PCF shadows: the soft type was removed from three.js
      // "demand" = only redraw when something changes (saves battery). Pulsing lights need "always".
      frameloop={hasCritical ? "always" : "demand"}
      dpr={[1, 2]}
      camera={{ position: [size * 0.5, size * 0.6, centerZ + size * 1.1], fov: 45 }}
      fallback={<p className="p-4 text-sm text-muted-foreground">The 3D view is not supported in this browser.</p>}
    >
      <SceneLights sun={sun} size={size} />

      {/* ground */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0, centerZ]} receiveShadow>
        <planeGeometry args={[size * 4, size * 4]} />
        <meshStandardMaterial color={sun.isDay ? "#a3b18a" : "#2f3a2c"} />
      </mesh>

      {placed.map((entry) => (
        <EquipmentModel
          key={entry.item.id}
          placed={entry}
          hovered={hoveredId === entry.item.id}
          onHover={setHoveredId}
          onOpen={(id) => router.push(`/equipment/${id}`)}
        />
      ))}

      <OrbitControls
        makeDefault
        target={[0, 0.6, centerZ]}
        maxPolarAngle={Math.PI / 2.1} // never look from under the ground
        minDistance={4}
        maxDistance={size * 3}
      />
    </Canvas>
  );
}
