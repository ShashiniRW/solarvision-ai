import { cache } from "react";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { BACKEND_URL, TOKEN_COOKIE } from "@/lib/backend";

export type CurrentUser = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  roles: string[];
};

// Loads the logged-in user from the Spring Boot API.
// cache() makes sure the layout and the page share ONE call per request.
export const getCurrentUser = cache(async (): Promise<CurrentUser> => {
  const token = (await cookies()).get(TOKEN_COOKIE)?.value;
  if (!token) {
    redirect("/login");
  }

  const response = await fetch(`${BACKEND_URL}/api/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  // The backend does not accept this token (expired, tampered, or user removed)
  if (response.status === 401 || response.status === 404) {
    redirect("/api/auth/expired");
  }

  if (!response.ok) {
    throw new Error(`Could not load the current user (status ${response.status})`);
  }

  return (await response.json()) as CurrentUser;
});