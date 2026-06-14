package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Votacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoVotacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface VotacionRepository extends JpaRepository<Votacion, Long> {
    List<Votacion> findAllByOrderByCreadoEnDesc();
    List<Votacion> findAllByEstadoOrderByCreadoEnDesc(EstadoVotacion estado);

    @Query("SELECT v FROM Votacion v WHERE v.creadoEn >= :desde AND v.creadoEn <= :hasta")
    List<Votacion> findByCreadoEnBetween(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta);
}
