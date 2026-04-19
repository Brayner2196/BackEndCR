package com.backendcr.residentialcomplex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendcr.residentialcomplex.entity.Propiedad;

public interface PropiedadRepository extends JpaRepository<Propiedad, Long> {

    Optional<Propiedad> findByTipoIdAndIdentificadorAndParentId(
            Long tipoId, String identificador, Long parentId);

    List<Propiedad> findByParentIdIsNull();

    List<Propiedad> findByParentId(Long parentId);
}
