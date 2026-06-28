package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Visita;
import com.backendcr.residentialcomplex.entity.enums.EstadoVisita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitaRepository extends JpaRepository<Visita, Long> {

    Optional<Visita> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Visita> findAllByResidenteIdOrderByCreadoEnDesc(Long residenteId);

    List<Visita> findAllByPropiedadIdOrderByCreadoEnDesc(Long propiedadId);

    List<Visita> findAllByEstado(EstadoVisita estado);
}
