package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ActaReunion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActaReunionRepository extends JpaRepository<ActaReunion, Long> {

    /** Listado ordenado por fecha de reunión descendente (más recientes primero). */
    List<ActaReunion> findAllByOrderByFechaReunionDesc();
}
