package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Cobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
