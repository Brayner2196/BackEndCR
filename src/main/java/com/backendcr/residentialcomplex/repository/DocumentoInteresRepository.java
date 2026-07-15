package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.DocumentoInteres;
import com.backendcr.residentialcomplex.entity.enums.CategoriaDocumento;
import com.backendcr.residentialcomplex.entity.enums.EstadoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoInteresRepository extends JpaRepository<DocumentoInteres, Long> {

    List<DocumentoInteres> findAllByOrderByCreadoEnDesc();

    List<DocumentoInteres> findAllByCategoriaOrderByCreadoEnDesc(CategoriaDocumento categoria);

    List<DocumentoInteres> findAllByEstadoOrderByCreadoEnDesc(EstadoDocumento estado);

    List<DocumentoInteres> findAllByEstadoAndCategoriaOrderByCreadoEnDesc(
            EstadoDocumento estado, CategoriaDocumento categoria);
}
