package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.RestriccionEstado;
import com.backendcr.residentialcomplex.entity.enums.AccionRestringible;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestriccionEstadoRepository extends JpaRepository<RestriccionEstado, Long> {

    Optional<RestriccionEstado> findByEstadoCarteraIdAndAccion(Long estadoCarteraId, AccionRestringible accion);

    List<RestriccionEstado> findByEstadoCarteraId(Long estadoCarteraId);
}
