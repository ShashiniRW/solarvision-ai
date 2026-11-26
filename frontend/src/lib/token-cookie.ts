import { NextResponse } from "next/server";
import { TOKEN_COOKIE, TOKEN_MAX_AGE_SECONDS } from "@/lib/backend";

/**
 * The response after a successful sign-in: stores the JWT in a secret (httpOnly) cookie.
 * JavaScript in the browser can never read this cookie, so the token cannot be stolen by XSS.
 */
export function signedInResponse(token: string) {
  const response = NextResponse.json({ ok: true });
  response.cookies.set({
    name: TOKEN_COOKIE,
    value: token,
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: TOKEN_MAX_AGE_SECONDS,
  });
  return response;
}
