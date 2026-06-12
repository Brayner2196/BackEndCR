package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.EstadoCarteraPropiedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoCarteraPropiedadRepository extends JpaRepository<EstadoCarteraPropiedad, Long> {

    Optional<EstadoCarteraPropiedad> findByPropiedadId(Long propiedadId);
}
