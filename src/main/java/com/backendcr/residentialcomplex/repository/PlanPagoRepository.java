package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.PlanPago;
import com.backendcr.residentialcomplex.entity.enums.EstadoPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanPagoRepository extends JpaRepository<PlanPago, Long> {

    /** Todos los planes filtrados por estado, del más reciente al más antiguo. */
    List<PlanPago> findByEstadoOrderByCreadoEnDesc(EstadoPlan estado);

    /** Todos los planes sin filtro, del más reciente al más antiguo. */
    List<PlanPago> findAllByOrderByCreadoEnDesc();

    /** Plan activo de un residente (puede haber solo uno activo a la vez). */
    Optional<PlanPago> findFirstByResidenteIdAndEstadoOrderByCreadoEnDesc(
            Long residenteId, EstadoPlan estado);

    /** Todos los planes de un residente. */
    List<PlanPago> findByResidenteIdOrderByCreadoEnDesc(Long residenteId);

    /** Verifica si un residente ya tiene un plan activo o pendiente. */
    boolean existsByResidenteIdAndEstadoIn(Long residenteId, List<EstadoPlan> estados);
}
