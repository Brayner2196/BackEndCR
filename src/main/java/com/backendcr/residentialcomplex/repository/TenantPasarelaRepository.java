package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.TenantPasarela;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantPasarelaRepository extends JpaRepository<TenantPasarela, Long> {

    /** Todas las pasarelas activas de un tenant, ordenadas por prioridad */
    @Query("SELECT p FROM TenantPasarela p WHERE p.tenant.schemaName = :schemaName AND p.activa = true ORDER BY p.prioridad ASC")
    List<TenantPasarela> findActivasByTenantSchema(@Param("schemaName") String schemaName);

    /** Todas las pasarelas (activas e inactivas) de un tenant */
    @Query("SELECT p FROM TenantPasarela p WHERE p.tenant.schemaName = :schemaName ORDER BY p.prioridad ASC")
    List<TenantPasarela> findAllByTenantSchema(@Param("schemaName") String schemaName);

    /** Busca la configuración de una pasarela específica para un tenant */
    @Query("SELECT p FROM TenantPasarela p WHERE p.tenant.schemaName = :schemaName AND p.tipoPasarela = :tipo")
    Optional<TenantPasarela> findByTenantSchemaAndTipo(
            @Param("schemaName") String schemaName,
            @Param("tipo") TipoPasarela tipo
    );

    /** Todas las pasarelas de un tenant por ID */
    List<TenantPasarela> findByTenantIdOrderByPrioridad(Long tenantId);

    /** Existe configuración para tenant + tipo */
    @Query("SELECT COUNT(p) > 0 FROM TenantPasarela p WHERE p.tenant.schemaName = :schemaName AND p.tipoPasarela = :tipo")
    boolean existsByTenantSchemaAndTipo(
            @Param("schemaName") String schemaName,
            @Param("tipo") TipoPasarela tipo
    );
}
