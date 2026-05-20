package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.TenantPasarela;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // ─── Queries nativas para rotación de clave (bypass al AttributeConverter) ──

    /** Lee el valor cifrado RAW de public_key sin pasar por el converter */
    @Query(value = "SELECT public_key FROM public.tenant_pasarelas WHERE id = :id", nativeQuery = true)
    String findRawPublicKey(@Param("id") Long id);

    /** Lee el valor cifrado RAW de private_key sin pasar por el converter */
    @Query(value = "SELECT private_key FROM public.tenant_pasarelas WHERE id = :id", nativeQuery = true)
    String findRawPrivateKey(@Param("id") Long id);

    /** Lee el valor cifrado RAW de webhook_secret sin pasar por el converter */
    @Query(value = "SELECT webhook_secret FROM public.tenant_pasarelas WHERE id = :id", nativeQuery = true)
    String findRawWebhookSecret(@Param("id") Long id);

    /** Escribe los valores re-cifrados directamente, sin pasar por el converter */
    @Modifying
    @Query(value = """
            UPDATE public.tenant_pasarelas
               SET public_key = :publicKey,
                   private_key = :privateKey,
                   webhook_secret = :webhookSecret
             WHERE id = :id
            """, nativeQuery = true)
    void updateCredencialesRaw(
            @Param("id")            Long   id,
            @Param("publicKey")     String publicKey,
            @Param("privateKey")    String privateKey,
            @Param("webhookSecret") String webhookSecret
    );
}
