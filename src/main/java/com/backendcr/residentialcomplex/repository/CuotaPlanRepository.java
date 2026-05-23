package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.CuotaPlan;
import com.backendcr.residentialcomplex.entity.enums.EstadoCuotaPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CuotaPlanRepository extends JpaRepository<CuotaPlan, Long> {

    /** Todas las cuotas de un plan, ordenadas por número de cuota. */
    List<CuotaPlan> findByPlanIdOrderByNumeroCuotaAsc(Long planId);

    /** Cuotas vencidas (para notificación al admin). */
    List<CuotaPlan> findByEstadoAndFechaVencimientoLessThanEqual(
            EstadoCuotaPlan estado, LocalDate fecha);

    /** Cuotas de un plan en estado específico. */
    List<CuotaPlan> findByPlanIdAndEstado(Long planId, EstadoCuotaPlan estado);

    /** ¿Todas las cuotas del plan están pagadas? */
    boolean existsByPlanIdAndEstadoNot(Long planId, EstadoCuotaPlan estado);
}
