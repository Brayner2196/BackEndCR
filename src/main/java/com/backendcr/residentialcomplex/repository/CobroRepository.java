package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Cobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface CobroRepository extends JpaRepository<Cobro, Long> {
    List<Cobro> findAllByPeriodoId(Long periodoId);
    List<Cobro> findAllByUsuarioId(Long usuarioId);
    List<Cobro> findAllByUsuarioIdAndEstado(Long usuarioId, EstadoCobro estado);
    List<Cobro> findAllByEstado(EstadoCobro estado);
    boolean existsByPeriodoIdAndPropiedadId(Long periodoId, Long propiedadId);
    List<Cobro> findAllByEstadoAndFechaLimitePagoBefore(EstadoCobro estado, LocalDate fecha);
    List<Cobro> findAllByEstadoInAndFechaLimitePagoBefore(List<EstadoCobro> estados, LocalDate fecha);

    /** Cobros pendientes de pago para una propiedad, ordenados de más antiguo a más nuevo (FIFO). */
    List<Cobro> findAllByPropiedadIdAndEstadoInOrderByFechaGeneracionAsc(
            Long propiedadId, List<EstadoCobro> estados);
}
