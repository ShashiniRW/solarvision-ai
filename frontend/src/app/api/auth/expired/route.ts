import { NextResponse } from "next/server";
import { TOKEN_COOKIE } from "@/lib/backend";

// The layout sends users here when the backend rejects their token.
// Deletes the cookie, then shows the login page with an explanation.
export async function GET(request: Request) {
  const response = NextResponse.redirect(new URL("/login?expired=1", request.url));
  response.cookies.set({
    name: TOKEN_COOKIE,
    value: "",
    httpOnly: true,
    path: "/",
    maxAge: 0,
  });
  return response;
}