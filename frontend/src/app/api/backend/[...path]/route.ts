import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL, TOKEN_COOKIE } from "@/lib/backend";

type RouteContext = { params: Promise<{ path: string[] }> };

// Forwards /api/backend/<anything> to the Spring Boot API, adding the login token.
async function forward(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;

  // Only the backend's /api/... endpoints may be reached, and no path tricks
  if (path[0] !== "api" || path.some((segment) => segment === ".." || segment === ".")) {
    return NextResponse.json(
      { status: 400, error: "Bad Request", message: "Invalid path" },
      { status: 400 },
    );
  }

  const token = (await cookies()).get(TOKEN_COOKIE)?.value;
  if (!token) {
    return NextResponse.json(
      { status: 401, error: "Unauthorized", message: "Not logged in" },
      { status: 401 },
    );
  }

  const target = new URL(`${BACKEND_URL}/${path.join("/")}`);
  target.search = request.nextUrl.search;

  const headers = new Headers();
  headers.set("Authorization", `Bearer ${token}`);
  const contentType = request.headers.get("content-type");
  if (contentType) {
    headers.set("Content-Type", contentType);
  }

  const hasBody = request.method !== "GET" && request.method !== "HEAD";
  const backendResponse = await fetch(target, {
    method: request.method,
    headers,
    body: hasBody ? await request.text() : undefined,
    cache: "no-store",
  });

  const responseHeaders = new Headers();
  const responseType = backendResponse.headers.get("content-type");
  if (responseType) {
    responseHeaders.set("Content-Type", responseType);
  }

  return new NextResponse(
    backendResponse.status === 204 ? null : await backendResponse.text(),
    { status: backendResponse.status, headers: responseHeaders },
  );
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
export const PATCH = forward;
export const DELETE = forward;