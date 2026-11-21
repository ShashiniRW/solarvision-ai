import { cookies } from "next/headers";
import { BACKEND_URL, TOKEN_COOKIE } from "@/lib/backend";

export const dynamic = "force-dynamic";

// Streams live events (Server-Sent Events) from the backend to the browser.
// The token stays in the httpOnly cookie; the browser never sees it.
export async function GET(request: Request) {
  const token = (await cookies()).get(TOKEN_COOKIE)?.value;
  if (!token) {
    return new Response(null, { status: 401 });
  }

  let backendResponse: Response;
  try {
    backendResponse = await fetch(`${BACKEND_URL}/api/events`, {
      headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" },
      cache: "no-store",
      signal: request.signal, // browser closes the tab -> the backend connection closes too
    });
  } catch {
    return new Response(null, { status: 502 });
  }

  if (!backendResponse.ok || !backendResponse.body) {
    return new Response(null, { status: backendResponse.status === 401 ? 401 : 502 });
  }

  // pass the stream through piece by piece (never wait for the "end" - a stream has none)
  return new Response(backendResponse.body, {
    headers: {
      "Content-Type": "text/event-stream; charset=utf-8",
      "Cache-Control": "no-cache, no-transform", // no caching, no compression buffering
      "X-Accel-Buffering": "no", // tells nginx-style proxies not to buffer
    },
  });
}
