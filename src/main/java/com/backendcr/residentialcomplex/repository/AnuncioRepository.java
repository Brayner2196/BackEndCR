package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Anuncio;
import com.backendcr.residentialcomplex.entity.enums.EstadoAnuncio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
    List<Anuncio> findAllByOrderByCreadoEnDesc();
    List<Anuncio> findAllByEstadoOrderByCreadoEnDesc(EstadoAnuncio estado);

    @Query("SELECT a FROM Anuncio a WHERE a.creadoEn >= :desde AND a.creadoEn <= :hasta")
    List<Anuncio> findByCreadoEnBetween(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
