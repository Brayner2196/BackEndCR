package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.AnuncioVista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnuncioVistaRepository extends JpaRepository<AnuncioVista, Long> {
    List<AnuncioVista> findAllByAnuncioId(Long anuncioId);
    Optional<AnuncioVista> findByAnuncioIdAndResidenteId(Long anuncioId, Long residenteId);
    boolean existsByAnuncioIdAndResidenteId(Long anuncioId, Long residenteId);
    long countByAnuncioId(Long anuncioId);

    /** Total de lecturas únicas para un conjunto de anuncios */
    @Query("SELECT COUNT(v) FROM AnuncioVista v WHERE v.anuncioId IN :ids")
    long countByAnuncioIdIn(@Param("ids") Collection<Long> ids);
}
