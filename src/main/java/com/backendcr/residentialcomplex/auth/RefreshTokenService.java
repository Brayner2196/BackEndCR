package com.backendcr.residentialcomplex.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.entity.RefreshToken;
import com.backendcr.residentialcomplex.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;

	@Value("${jwt.refresh-expiration-ms}")
	private long refreshExpirationMs;

	private static final SecureRandom RANDOM = new SecureRandom();

	// ─── Crear ───────────────────────────────────────────────────────────

	@Transactional
	public RefreshToken crear(Long identidadId) {
		RefreshToken rt = new RefreshToken();
		rt.setToken(generarToken());
		rt.setIdentidadId(identidadId);
		rt.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
		rt.setRevoked(false);
		return refreshTokenRepository.save(rt);
	}

	// ─── Validar y rotar (token rotation) ────────────────────────────────

	/**
	 * Valida el token recibido, lo revoca y crea uno nuevo.
	 * Lanza 401 si el token no existe, está revocado o expiró.
	 */
	@Transactional
	public RefreshToken rotar(String tokenString) {
		RefreshToken existente = refreshTokenRepository.findByToken(tokenString)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido"));

		if (existente.isRevoked()) {
			// Posible reutilización — revocar toda la familia por seguridad
			refreshTokenRepository.revocarTodosPorIdentidad(existente.getIdentidadId());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token ya fue utilizado");
		}

		if (existente.getExpiresAt().isBefore(Instant.now())) {
			existente.setRevoked(true);
			refreshTokenRepository.save(existente);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado");
		}

		// Revocar el actual y emitir uno nuevo
		existente.setRevoked(true);
		refreshTokenRepository.save(existente);
		return crear(existente.getIdentidadId());
	}

	// ─── Revocar todos (logout) ───────────────────────────────────────────

	@Transactional
	public void revocarTodos(Long identidadId) {
		refreshTokenRepository.revocarTodosPorIdentidad(identidadId);
	}

	// ─── Helpers ─────────────────────────────────────────────────────────

	private String generarToken() {
		byte[] bytes = new byte[48];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
