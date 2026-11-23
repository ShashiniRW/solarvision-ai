"use client";

import { OrbitControls, Stars } from "@react-three/drei";
import { Canvas, useFrame } from "@react-three/fiber";
import { useRef } from "react";
import { Color, MathUtils, type DirectionalLight, type Group } from "three";
import { usePrefersReducedMotion } from "@/lib/use-reduced-motion";

const DAY_SECONDS = 24; // one full day + night
const DAY_SHARE = 0.65; // the sun is up for 65% of the cycle
const SUN_DISTANCE = 40;

const ROWS = 4;
const PER_ROW = 7;
const GAP_X = 2.8;
const GAP_Z = 3.2;
const TRACKERS: [number, number][] = Array.from({ length: ROWS * PER_ROW }, (_, index) => [
  ((index % PER_ROW) - (PER_ROW - 1) / 2) * GAP_X,
  -Math.floor(index / PER_ROW) * GAP_Z,
]);

const NIGHT = new Color("#0b1026");
const DUSK = new Color("#f28c5b");
const DAY = new Color("#7cc4fa");
const SKY = new Color("#7cc4fa"); // reused every frame (no garbage)

type SunState = { x: number; y: number; z: number; elevation: number };

// Where the sun is right now. Shared by every part of the scene; updated once per frame by SunClock.
// (There is only one login scene on the page, so one shared object is enough.)
const SUN: SunState = { x: SUN_DISTANCE, y: 0, z: 0, elevation: 0 };

/** Moves the sun along its path. Runs first every frame, so everything else sees the same sun. */
function SunClock({ still }: { still: boolean }) {
  useFrame(({ clock }) => {
    const t = still ? 0.25 : (clock.elapsedTime / DAY_SECONDS + 0.02) % 1;
    const angle =
      t < DAY_SHARE
        ? Math.PI * (t / DAY_SHARE) // day: east -> west across the sky
        : Math.PI * (1 + (t - DAY_SHARE) / (1 - DAY_SHARE)); // night: below the horizon

    SUN.x = Math.cos(angle) * SUN_DISTANCE;
    SUN.y = Math.sin(angle) * SUN_DISTANCE;
    SUN.z = -SUN_DISTANCE * 1.2; // behind the field, so the sun crosses the visible sky
    SUN.elevation = Math.sin(angle);
  });
  return null;
}

/** Sky colour, sunlight, the visible sun and the stars. */
function SkyAndSun() {
  const light = useRef<DirectionalLight>(null);
  const sunBody = useRef<Group>(null);
  const stars = useRef<Group>(null);

  useFrame((state) => {
    const { x, y, z, elevation } = SUN;

    // night -> sunrise orange -> day blue, depending on how high the sun is
    SKY.copy(NIGHT)
      .lerp(DUSK, MathUtils.smoothstep(elevation, -0.25, 0.02))
      .lerp(DAY, MathUtils.smoothstep(elevation, 0.05, 0.5));
    state.scene.background = SKY;
    state.scene.fog?.color.copy(SKY);

    if (light.current) {
      light.current.position.set(x, y, z);
      light.current.intensity = Math.max(elevation, 0) * 2.2 + 0.05;
    }
    if (sunBody.current) {
      sunBody.current.position.set(x, y, z);
      sunBody.current.visible = elevation > -0.05;
    }
    if (stars.current) {
      stars.current.visible = elevation < -0.02;
    }
  });

  return (
    <>
      <fog attach="fog" args={["#7cc4fa", 35, 95]} />
      <hemisphereLight args={["#dbeafe", "#334155", 0.55]} />
      <ambientLight intensity={0.15} />
      <directionalLight
        ref={light}
        castShadow
        shadow-mapSize={[1024, 1024]}
        shadow-camera-left={-22}
        shadow-camera-right={22}
        shadow-camera-top={22}
        shadow-camera-bottom={-22}
        shadow-camera-far={100}
      />
      <group ref={sunBody}>
        <mesh>
          <sphereGeometry args={[2.2, 32, 32]} />
          <meshBasicMaterial color="#ffd166" fog={false} />
        </mesh>
        <mesh>
          <sphereGeometry args={[4, 32, 32]} />
          <meshBasicMaterial color="#ffb703" transparent opacity={0.25} fog={false} />
        </mesh>
      </group>
      <group ref={stars}>
        <Stars radius={90} depth={30} count={1500} factor={3} saturation={0} fade speed={0.5} />
      </group>
    </>
  );
}

/** Rows of single-axis solar trackers that turn to face the sun and lie flat at night. */
function SolarField() {
  const panels = useRef<(Group | null)[]>([]);

  useFrame((_, delta) => {
    const { x, y, elevation } = SUN;
    const target = elevation > 0 ? MathUtils.clamp(-Math.atan2(x, y), -0.9, 0.9) : 0;
    for (const panel of panels.current) {
      if (panel) {
        panel.rotation.z = MathUtils.damp(panel.rotation.z, target, 2, delta);
      }
    }
  });

  return (
    <>
      <mesh rotation={[-Math.PI / 2, 0, 0]} receiveShadow>
        <circleGeometry args={[60, 64]} />
        <meshStandardMaterial color="#5b7a4a" />
      </mesh>
      {TRACKERS.map(([x, z], index) => (
        <group key={index} position={[x, 0, z]}>
          <mesh position={[0, 0.6, 0]} castShadow>
            <cylinderGeometry args={[0.06, 0.06, 1.2, 8]} />
            <meshStandardMaterial color="#64748b" />
          </mesh>
          <group
            position={[0, 1.25, 0]}
            ref={(element) => {
              panels.current[index] = element;
            }}
          >
            <mesh castShadow receiveShadow>
              <boxGeometry args={[2.2, 0.06, 1.4]} />
              <meshStandardMaterial color="#1d3b8f" metalness={0.6} roughness={0.25} />
            </mesh>
            <mesh position={[0, 0.035, 0]}>
              <boxGeometry args={[2.22, 0.005, 0.03]} />
              <meshStandardMaterial color="#93c5fd" />
            </mesh>
          </group>
        </group>
      ))}
    </>
  );
}

export function LoginHero() {
  const still = usePrefersReducedMotion();

  return (
    <Canvas
      shadows="percentage"
      dpr={[1, 1.5]}
      frameloop={still ? "demand" : "always"}
      camera={{ position: [12, 4.5, 16], fov: 42 }}
      fallback={<div className="h-full w-full bg-linear-to-br from-sky-400 to-amber-300" />}
    >
      {/* SunClock comes first, so the sun has moved before the sky and panels read it */}
      <SunClock still={still} />
      <SkyAndSun />
      <SolarField />
      <OrbitControls
        target={[0, 2, -6]}
        autoRotate={!still}
        autoRotateSpeed={0.35}
        enableZoom={false}
        enablePan={false}
        minPolarAngle={Math.PI / 2.8} // keep a low view, so the sky (and the sun) stay in the picture
        maxPolarAngle={Math.PI / 2.2}
      />
    </Canvas>
  );
}
