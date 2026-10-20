import { cookies } from "next/headers";
import { redirect } from "next/navigation";
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