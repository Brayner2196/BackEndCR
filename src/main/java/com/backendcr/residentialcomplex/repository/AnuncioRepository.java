package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Anuncio;
import com.backendcr.residentialcomplex.entity.enums.EstadoAnuncio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
    List<Anuncio> findAllByOrderByCreadoEnDesc();
    List<Anuncio> findAllByEstadoOrderByCreadoEnDesc(EstadoAnuncio estado);
}
