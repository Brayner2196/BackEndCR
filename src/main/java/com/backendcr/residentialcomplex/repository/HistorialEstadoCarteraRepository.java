package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.HistorialEstadoCartera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoCarteraRepository extends JpaRepository<HistorialEstadoCartera, Long> {

    List<HistorialEstadoCartera> findByPropiedadIdOrderByCreadoEnDesc(Long propiedadId);
}
