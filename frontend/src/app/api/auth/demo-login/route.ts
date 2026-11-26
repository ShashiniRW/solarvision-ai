import { NextResponse } from "next/server";
import { BACKEND_URL } from "@/lib/backend";
import { signedInResponse } from "@/lib/token-cookie";

// One-click demo sign-in: forwards { role } to Spring Boot and stores the token
// in the same secret cookie as a normal login.
export async function POST(request: Request) {
  const body = await request.text();

  const backendResponse = await fetch(`${BACKEND_URL}/api/auth/demo-login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
    cache: "no-store",
  });

  // demo mode off (404), unknown role (400) etc.: pass the answer through
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
      { status: 502, error: "Bad Gateway", message: "Demo login response did not contain a token" },
      { status: 502 },
    );
  }

  return signedInResponse(data.token);
}
