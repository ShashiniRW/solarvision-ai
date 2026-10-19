import { NextRequest, NextResponse } from "next/server";
import { TOKEN_COOKIE } from "@/lib/backend";

// Runs before every PAGE request (API routes are excluded below).
// This only improves the experience. The real security check is in the Spring Boot API,
// which rejects every request without a valid token.
export function proxy(request: NextRequest) {
  const hasToken = request.cookies.has(TOKEN_COOKIE);
  const { pathname, search } = request.nextUrl;

  if (pathname === "/login") {
    // already signed in: no need to see the login page
    if (hasToken) {
      return NextResponse.redirect(new URL("/dashboard", request.url));
    }
    return NextResponse.next();
  }

  if (!hasToken) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", pathname + search); // remember where the visitor wanted to go
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  // run for pages only: skip API routes, static files and images
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};