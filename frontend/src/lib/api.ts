import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";
import { BACKEND_URL, TOKEN_COOKIE } from "@/lib/backend";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

// Server-side GET to the Spring Boot API, using the login token from the secret cookie.
export async function apiGet<T>(path: string): Promise<T> {
  const token = (await cookies()).get(TOKEN_COOKIE)?.value;
  if (!token) {
    redirect("/login");
  }

  const response = await fetch(`${BACKEND_URL}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  // The backend does not accept the token any more: clear it and go to the login page
  if (response.status === 401) {
    redirect("/api/auth/expired");
  }

  if (!response.ok) {
    let message = `Request failed (status ${response.status})`;
    try {
      const body = await response.json();
      if (body?.message) {
        message = body.message;
      }
    } catch {
      // the error body was not JSON: keep the default message
    }
    throw new ApiError(response.status, message);
  }

  return (await response.json()) as T;
}

// Same as apiGet, but a 404 from the backend shows the "not found" page.
export async function apiGetOrNotFound<T>(path: string): Promise<T> {
  try {
    return await apiGet<T>(path);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}


// Same as apiGet, but a 404 from the backend gives null instead of an error.
// Used for "might not exist yet" data, such as the latest reading of new equipment.
export async function apiGetOrNull<T>(path: string): Promise<T | null> {
  try {
    return await apiGet<T>(path);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}