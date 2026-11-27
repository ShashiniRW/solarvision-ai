"use server";

import { revalidatePath } from "next/cache";
import { callBackend } from "@/lib/backend-action";
import type { ActionResult, ManagedUser } from "@/lib/types";

// Server actions: run on the server, called from the Users page.
// The backend checks that the caller is an ADMIN; these only forward the request.

export async function createUser(input: {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: string;
}): Promise<ActionResult<{ user: ManagedUser; temporaryPassword: string }>> {
  const result = await callBackend<{ user: ManagedUser; temporaryPassword: string }>("/api/admin/users", {
    method: "POST",
    body: JSON.stringify({ ...input, phone: input.phone.trim() || null }),
  });
  if (result.ok) revalidatePath("/users");
  return result;
}

export async function updateUser(
  id: number,
  input: { firstName: string; lastName: string; phone: string; role: string | null },
): Promise<ActionResult<ManagedUser>> {
  if (!Number.isInteger(id)) return { ok: false, error: "Invalid user." };
  const result = await callBackend<ManagedUser>(`/api/admin/users/${id}`, {
    method: "PUT",
    body: JSON.stringify({ ...input, phone: input.phone.trim() || null }),
  });
  if (result.ok) revalidatePath("/users");
  return result;
}

export async function deactivateUser(id: number): Promise<ActionResult<ManagedUser>> {
  return changeStatus(id, "deactivate");
}

export async function activateUser(id: number): Promise<ActionResult<ManagedUser>> {
  return changeStatus(id, "activate");
}

export async function resetUserPassword(id: number): Promise<ActionResult<{ temporaryPassword: string }>> {
  if (!Number.isInteger(id)) return { ok: false, error: "Invalid user." };
  const result = await callBackend<{ temporaryPassword: string }>(`/api/admin/users/${id}/reset-password`, {
    method: "POST",
  });
  if (result.ok) revalidatePath("/users");
  return result;
}

async function changeStatus(id: number, action: "activate" | "deactivate"): Promise<ActionResult<ManagedUser>> {
  if (!Number.isInteger(id)) return { ok: false, error: "Invalid user." };
  const result = await callBackend<ManagedUser>(`/api/admin/users/${id}/${action}`, { method: "POST" });
  if (result.ok) revalidatePath("/users");
  return result;
}
