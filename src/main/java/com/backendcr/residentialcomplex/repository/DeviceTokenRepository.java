package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
