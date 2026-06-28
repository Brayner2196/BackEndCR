package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.BitacoraAcceso;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BitacoraAccesoRepository extends JpaRepository<BitacoraAcceso, Long> {

    List<BitacoraAcceso> findAllByOrderByCreadoEnDesc(Pageable pageable);

    List<BitacoraAcceso> findAllByCreadoEnBetweenOrderByCreadoEnDesc(Instant desde, Instant hasta);

    List<BitacoraAcceso> findAllByPropiedadIdOrderByCreadoEnDesc(Long propiedadId);
}
