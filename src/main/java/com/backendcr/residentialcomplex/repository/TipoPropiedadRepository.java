package com.backendcr.residentialcomplex.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendcr.residentialcomplex.entity.TipoPropiedad;

public interface TipoPropiedadRepository extends JpaRepository<TipoPropiedad, Long> {

    List<TipoPropiedad> findByParentIdIsNullOrderByOrden();

    List<TipoPropiedad> findByParentIdOrderByOrden(Long parentId);
}
