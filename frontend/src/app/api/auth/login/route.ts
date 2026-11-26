import { NextResponse } from "next/server";
import { BACKEND_URL } from "@/lib/backend";
import { signedInResponse } from "@/lib/token-cookie";

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

  return signedInResponse(data.token);
}
