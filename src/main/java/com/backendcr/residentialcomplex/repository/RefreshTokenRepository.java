package com.backendcr.residentialcomplex.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.backendcr.residentialcomplex.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByToken(String token);

	/** Revoca todos los refresh tokens activos de un usuario (logout o seguridad). */
	@Modifying
	@Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.identidadId = :identidadId AND r.revoked = false")
	void revocarTodosPorIdentidad(Long identidadId);

	/** Limpieza periódica — borra tokens expirados o revocados. */
	@Modifying
	@Query("DELETE FROM RefreshToken r WHERE r.revoked = true OR r.expiresAt < CURRENT_TIMESTAMP")
	void eliminarObsoletos();
}
