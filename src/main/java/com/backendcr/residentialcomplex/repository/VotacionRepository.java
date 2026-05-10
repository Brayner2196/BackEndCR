package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Votacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoVotacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VotacionRepository extends JpaRepository<Votacion, Long> {
    List<Votacion> findAllByOrderByCreadoEnDesc();
    List<Votacion> findAllByEstadoOrderByCreadoEnDesc(EstadoVotacion estado);
}
