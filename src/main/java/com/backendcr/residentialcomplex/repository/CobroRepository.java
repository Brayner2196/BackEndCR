package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Cobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface CobroRepository extends JpaRepository<Cobro, Long> {
	
    List<Cobro> findAllByPeriodoId(Long periodoId);
    List<Cobro> findAllByPropiedadId(Long propiedadId);
    List<Cobro> findAllByPropiedadIdIn(List<Long> propiedadIds);
    List<Cobro> findAllByPropiedadIdInAndEstado(List<Long> propiedadIds, EstadoCobro estado);
    List<Cobro> findAllByEstado(EstadoCobro estado);
    boolean existsByPeriodoIdAndPropiedadId(Long periodoId, Long propiedadId);
    List<Cobro> findAllByEstadoAndFechaLimitePagoBefore(EstadoCobro estado, LocalDate fecha);
    List<Cobro> findAllByEstadoInAndFechaLimitePagoBefore(List<EstadoCobro> estados, LocalDate fecha);

    /** Cobros pendientes de pago para una propiedad, ordenados de más antiguo a más nuevo (FIFO). */
    List<Cobro> findAllByPropiedadIdAndEstadoInOrderByFechaGeneracionAsc(
            Long propiedadId, List<EstadoCobro> estados);

    /** Cobros especiales (multas, sanciones, etc.) sin período asociado. */
    List<Cobro> findAllByPeriodoIdIsNull();

    /** Todos los cobros de un conjunto de propiedades, ordenados por fecha de generación desc.
     *  Usado para el historial paginado del residente (infinite scroll). */
    Page<Cobro> findAllByPropiedadIdInOrderByFechaGeneracionDesc(
            List<Long> propiedadIds, Pageable pageable);

    /**
     * Historial paginado con orden compuesto:
     *  1º fecha_generacion DESC  (cobros más recientes primero)
     *  2º anio DESC              (desempate por año del período — si tienen periodo)
     *  3º mes  DESC              (desempate por mes: febrero antes que enero)
     *
     * Los cobros especiales (periodo_id = null) quedan con COALESCE → 0,
     * por lo que en caso de empate de fecha van después de los de período.
     * La native query fija el ORDER BY; el Pageable solo aporta page y size.
     */
    @Query(value = """
            SELECT c.* FROM cobros c
            LEFT JOIN periodos_cobro p ON p.id = c.periodo_id
            WHERE c.propiedad_id IN (:propiedadIds)
            ORDER BY c.fecha_generacion DESC,
                     COALESCE(p.anio, 0) DESC,
                     COALESCE(p.mes,  0) DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM cobros c
            WHERE c.propiedad_id IN (:propiedadIds)
            """,
            nativeQuery = true)
    Page<Cobro> findHistorialOrdenadoPorPeriodo(
            @Param("propiedadIds") List<Long> propiedadIds,
            Pageable pageable);
}
