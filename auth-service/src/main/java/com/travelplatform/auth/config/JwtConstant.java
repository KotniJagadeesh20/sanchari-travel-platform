package com.travelplatform.auth.config;

/**
 * Non-secret JWT constants. The signing secret itself is NOT here — it's
 * externalized via the `jwt.secret` property (bound to env var JWT_SECRET,
 * no default — see application.properties and JwtProvider/JwtValidator).
 * A hardcoded fallback here would defeat the point of externalizing it.
 */
public class JwtConstant {

	public static final String JWT_HEADER = "Authorization";

	/** Access token (JWT) lifetime: 15 minutes. */
	public static final long ACCESS_TOKEN_EXPIRY_MS = 15 * 60 * 1000L;

	/** Refresh token lifetime: 7 days. */
	public static final long REFRESH_TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L;

}
