package com.backendcr.residentialcomplex.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
		@NotBlank String refreshToken
) {}
