package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByUsuarioIdAndPlataforma(Long usuarioId, String plataforma);

    List<DeviceToken> findByUsuarioId(Long usuarioId);

    List<DeviceToken> findByUsuarioIdIn(List<Long> usuarioIds);

    @Query("SELECT dt FROM DeviceToken dt WHERE dt.tenantId = :tenantId")
    List<DeviceToken> findByTenantId(@Param("tenantId") String tenantId);

    void deleteByUsuarioId(Long usuarioId);

    /**
     * Upsert atómico: inserta o actualiza el token si ya existe (usuario_id, plataforma).
     * Evita la race condition del findByX + save.
     */
    @Modifying
    @Query(value = """
            INSERT INTO public.device_tokens (usuario_id, tenant_id, token, plataforma, actualizado_en)
            VALUES (:usuarioId, :tenantId, :token, :plataforma, NOW())
            ON CONFLICT (usuario_id, plataforma)
            DO UPDATE SET token = EXCLUDED.token,
                          tenant_id = EXCLUDED.tenant_id,
                          actualizado_en = NOW()
            """, nativeQuery = true)
    void upsertToken(@Param("usuarioId") Long usuarioId,
                     @Param("tenantId") String tenantId,
                     @Param("token") String token,
                     @Param("plataforma") String plataforma);
}
