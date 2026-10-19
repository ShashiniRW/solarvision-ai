import { NextResponse } from "next/server";
import { BACKEND_URL, TOKEN_COOKIE, TOKEN_MAX_AGE_SECONDS } from "@/lib/backend";

// Receives the login form, asks Spring Boot to check it, and stores the token
// in a secret (httpOnly) cookie. The token is never sent back to the browser.
export async function POST(request: Request) {
  const body = await request.text();

  const backendResponse = await fetch(`${BACKEND_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
    cache: "no-store",
  });

  // Wrong password etc.: pass the backend's answer straight through
  if (!backendResponse.ok) {
    return new NextResponse(await backendResponse.text(), {
      status: backendResponse.status,
      headers: {
        "Content-Type": backendResponse.headers.get("content-type") ?? "application/json",
      },
    });
  }

  const data = (await backendResponse.json()) as { token?: string };
  if (!data.token) {
    return NextResponse.json(
      { status: 502, error: "Bad Gateway", message: "Login response did not contain a token" },
      { status: 502 },
    );
  }

  const response = NextResponse.json({ ok: true });
  response.cookies.set({
    name: TOKEN_COOKIE,
    value: data.token,
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: TOKEN_MAX_AGE_SECONDS,
  });
  return response;
}