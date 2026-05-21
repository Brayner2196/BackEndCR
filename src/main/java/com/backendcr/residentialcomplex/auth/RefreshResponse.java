package com.backendcr.residentialcomplex.auth;

public record RefreshResponse(
		String token,
		String refreshToken
) {}
