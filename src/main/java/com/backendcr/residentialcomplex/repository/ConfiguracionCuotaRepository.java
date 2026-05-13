package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ConfiguracionCuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConfiguracionCuotaRepository extends JpaRepository<ConfiguracionCuota, Long> {

    // ─── Listados generales ──────────────────────────────────────────────

    List<ConfiguracionCuota> findAllByOrderByFechaVigenciaDesdeDesc();

    List<ConfiguracionCuota> findAllByActivoTrue();

    // ─── Resolución de monto con filtro por fecha ────────────────────────

    /** Cuota específica de una propiedad vigente en la fecha dada. */
    @Query("""
            SELECT c FROM ConfiguracionCuota c
            WHERE c.propiedadId = :propiedadId
              AND c.activo = true
              AND c.fechaVigenciaDesde <= :fecha
              AND (c.fechaVigenciaHasta IS NULL OR c.fechaVigenciaHasta >= :fecha)
            ORDER BY c.fechaVigenciaDesde DESC
            """)
    Optional<ConfiguracionCuota> findVigenteByPropiedadId(@Param("propiedadId") Long propiedadId,
                                                           @Param("fecha") LocalDate fecha);

    /** Cuotas con rango numérico para un tipo, vigentes en la fecha dada. */
    @Query("""
            SELECT c FROM ConfiguracionCuota c
            WHERE c.tipoPropiedadId = :tipoId
              AND c.numeroDesde IS NOT NULL
              AND c.activo = true
              AND c.fechaVigenciaDesde <= :fecha
              AND (c.fechaVigenciaHasta IS NULL OR c.fechaVigenciaHasta >= :fecha)
            """)
    List<ConfiguracionCuota> findVigentesByTipoIdConRango(@Param("tipoId") Long tipoId,
                                                           @Param("fecha") LocalDate fecha);

    /** Cuota general de un tipo (sin rango), vigente en la fecha dada. */
    @Query("""
            SELECT c FROM ConfiguracionCuota c
            WHERE c.tipoPropiedadId = :tipoId
              AND c.numeroDesde IS NULL
              AND c.activo = true
              AND c.fechaVigenciaDesde <= :fecha
              AND (c.fechaVigenciaHasta IS NULL OR c.fechaVigenciaHasta >= :fecha)
            ORDER BY c.fechaVigenciaDesde DESC
            """)
    Optional<ConfiguracionCuota> findVigenteByTipoIdSinRango(@Param("tipoId") Long tipoId,
                                                              @Param("fecha") LocalDate fecha);

    // ─── Validación de solapamiento de fechas (para crear sin pisar) ─────

    /**
     * Detecta si ya existe una cuota activa para la misma propiedad cuyo rango
     * de fechas se solapa con [desde, hasta].
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM ConfiguracionCuota c
            WHERE c.propiedadId = :propiedadId
              AND c.activo = true
              AND c.id <> :excludeId
              AND c.fechaVigenciaDesde <= :hasta
              AND (c.fechaVigenciaHasta IS NULL OR c.fechaVigenciaHasta >= :desde)
            """)
    boolean existeSolapamientoPorPropiedad(@Param("propiedadId") Long propiedadId,
                                           @Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta,
                                           @Param("excludeId") Long excludeId);

    /**
     * Detecta solapamiento para una cuota general de tipo (sin rango).
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM ConfiguracionCuota c
            WHERE c.tipoPropiedadId = :tipoId
              AND c.numeroDesde IS NULL
              AND c.activo = true
              AND c.id <> :excludeId
              AND c.fechaVigenciaDesde <= :hasta
              AND (c.fechaVigenciaHasta IS NULL OR c.fechaVigenciaHasta >= :desde)
            """)
    boolean existeSolapamientoGeneralPorTipo(@Param("tipoId") Long tipoId,
                                             @Param("desde") LocalDate desde,
                                             @Param("hasta") LocalDate hasta,
                                             @Param("excludeId") Long excludeId);

    /**
     * Detecta solapamiento para cuotas de tipo con rango numérico superpuesto.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM ConfiguracionCuota c
            WHERE c.tipoPropiedadId = :tipoId
              AND c.numeroDesde IS NOT NULL
              AND c.activo = true
              AND c.id <> :excludeId
              AND c.numeroDesde <= :rangoHasta
              AND c.numeroHasta >= :rangoDesde
              AND c.fechaVigenciaDesde <= :hasta
              AND (c.fechaVigenciaHasta IS NULL OR c.fechaVigenciaHasta >= :desde)
            """)
    boolean existeSolapamientoRangoPorTipo(@Param("tipoId") Long tipoId,
                                           @Param("rangoDesde") Integer rangoDesde,
                                           @Param("rangoHasta") Integer rangoHasta,
                                           @Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta,
                                           @Param("excludeId") Long excludeId);

    // ─── Compatibilidad (usados por código previo de test/legacy) ────────

    Optional<ConfiguracionCuota> findByPropiedadIdAndActivoTrue(Long propiedadId);
    Optional<ConfiguracionCuota> findByTipoPropiedadIdAndNumeroDesdeIsNullAndActivoTrue(Long tipoPropiedadId);
    List<ConfiguracionCuota> findByTipoPropiedadIdAndNumeroDesdeIsNotNullAndActivoTrue(Long tipoPropiedadId);
}
