package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.CategoriaPresupuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaPresupuestoRepository extends JpaRepository<CategoriaPresupuesto, Long> {

    List<CategoriaPresupuesto> findByPresupuestoIdOrderByNombreAsc(Long presupuestoId);

    void deleteByPresupuestoId(Long presupuestoId);

    boolean existsByPresupuestoIdAndNombre(Long presupuestoId, String nombre);
}
