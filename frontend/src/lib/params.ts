import { notFound } from "next/navigation";

// Accepts only plain whole numbers from the address bar; anything else shows the "not found" page.
export function parseId(value: string): number {
  if (!/^\d+$/.test(value)) {
    notFound();
  }
  return Number(value);
}