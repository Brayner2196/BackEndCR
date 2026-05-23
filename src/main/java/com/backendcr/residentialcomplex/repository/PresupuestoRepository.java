package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    List<Presupuesto> findAllByOrderByAnioDesc();

    Optional<Presupuesto> findByAnio(int anio);

    Optional<Presupuesto> findFirstByActivoTrueOrderByAnioDesc();
}
