package com.backendcr.residentialcomplex.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.EstadoUsuario;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByIdentidadId(Long identidadId);

    boolean existsByIdentidadId(Long identidadId);

    List<Usuario> findAllByEstado(EstadoUsuario estado);

}
