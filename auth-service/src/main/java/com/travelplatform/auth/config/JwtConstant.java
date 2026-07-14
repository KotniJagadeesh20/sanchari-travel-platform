package com.travelplatform.auth.config;

public class JwtConstant {

	public static final String SECRET_KEY = "bvvrysgguveuuggynuugrsyfnngyuwgnygnsgyngnugycutjngjhnss";

	public static final String JWT_HEADER = "Authorization";

	/** Access token (JWT) lifetime: 15 minutes. */
	public static final long ACCESS_TOKEN_EXPIRY_MS = 15 * 60 * 1000L;

	/** Refresh token lifetime: 7 days. */
	public static final long REFRESH_TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L;

}
