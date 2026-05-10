package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.AnuncioVista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnuncioVistaRepository extends JpaRepository<AnuncioVista, Long> {
    List<AnuncioVista> findAllByAnuncioId(Long anuncioId);
    Optional<AnuncioVista> findByAnuncioIdAndResidenteId(Long anuncioId, Long residenteId);
    boolean existsByAnuncioIdAndResidenteId(Long anuncioId, Long residenteId);
    long countByAnuncioId(Long anuncioId);
}
