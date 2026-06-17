package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.AvisoCobranza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvisoCobranzaRepository extends JpaRepository<AvisoCobranza, Long> {

    /** Historial de avisos de una propiedad, del más reciente al más antiguo. */
    List<AvisoCobranza> findByPropiedadIdOrderByCreadoEnDesc(Long propiedadId);
}
