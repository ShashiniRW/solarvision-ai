// Address of the Spring Boot API. Read on the server only.
export const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

// Name of the secret cookie that holds the login token.
export const TOKEN_COOKIE = "sv_token";

// How long the cookie lives. Matches app.jwt.expiration-ms in the backend (24 hours).
export const TOKEN_MAX_AGE_SECONDS = 60 * 60 * 24;