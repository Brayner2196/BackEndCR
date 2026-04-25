package com.backendcr.residentialcomplex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendcr.residentialcomplex.entity.UsuarioPropiedad;

public interface UsuarioPropiedadRepository extends JpaRepository<UsuarioPropiedad, Long> {

    List<UsuarioPropiedad> findByUsuarioId(Long usuarioId);

    List<UsuarioPropiedad> findByPropiedadId(Long propiedadId);

    Optional<UsuarioPropiedad> findByUsuarioIdAndPropiedadId(Long usuarioId, Long propiedadId);

    boolean existsByUsuarioIdAndPropiedadId(Long usuarioId, Long propiedadId);

    void deleteByUsuarioIdAndPropiedadId(Long usuarioId, Long propiedadId);

    Optional<UsuarioPropiedad> findByPropiedadIdAndEsPrincipalTrue(Long propiedadId);
}
