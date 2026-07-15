package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ArchivoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArchivoDocumentoRepository extends JpaRepository<ArchivoDocumento, Long> {

    /** Busca un archivo asegurando que pertenece al documento indicado (evita accesos cruzados). */
    Optional<ArchivoDocumento> findByIdAndDocumentoId(Long id, Long documentoId);

    long countByDocumentoId(Long documentoId);
}
