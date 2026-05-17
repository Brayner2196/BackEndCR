package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.InquilinoPermiso;
import com.backendcr.residentialcomplex.entity.enums.PermisoInquilino;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquilinoPermisoRepository extends JpaRepository<InquilinoPermiso, Long> {

    List<InquilinoPermiso> findAllByInquilinoId(Long inquilinoId);

    boolean existsByInquilinoIdAndPermiso(Long inquilinoId, PermisoInquilino permiso);

    void deleteByInquilinoId(Long inquilinoId);
}
