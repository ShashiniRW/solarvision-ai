import { cookies } from "next/headers";
import { BACKEND_URL, TOKEN_COOKIE } from "@/lib/backend";
import type { ActionResult } from "@/lib/types";

/**
 * Calls the Spring Boot API from a server action, with the signed-in user's token.
 * Runs on the server only: the token never reaches the browser.
 */
export async function callBackend<T>(
  path: string,
  init: { method: string; body?: string },
): Promise<ActionResult<T>> {
  const token = (await cookies()).get(TOKEN_COOKIE)?.value;
  if (!token) {
    return { ok: false, error: "Your session has expired. Please sign in again." };
  }

  try {
    const response = await fetch(`${BACKEND_URL}${path}`, {
      method: init.method,
      body: init.body,
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      cache: "no-store",
    });

    if (!response.ok) {
      const body = (await response.json().catch(() => null)) as { message?: string } | null;
      return { ok: false, error: body?.message ?? `The request failed (status ${response.status}).` };
    }

    const text = await response.text(); // empty for "204 No Content"
    return { ok: true, data: (text ? JSON.parse(text) : null) as T };
  } catch {
    return { ok: false, error: "Cannot reach the server. Please try again." };
  }
}
