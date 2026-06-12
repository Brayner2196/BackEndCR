package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.EstadoCartera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoCarteraRepository extends JpaRepository<EstadoCartera, Long> {

    /** Estados activos del más severo al menos severo (orden de evaluación). */
    List<EstadoCartera> findByActivoTrueOrderBySeveridadDesc();

    /** Estado base "al día" (es_positivo=true). */
    Optional<EstadoCartera> findFirstByEsPositivoTrueAndActivoTrue();

    Optional<EstadoCartera> findByCodigo(String codigo);

    long countByActivoTrue();
}
