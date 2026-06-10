package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.MiembroConsejo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MiembroConsejoRepository extends JpaRepository<MiembroConsejo, Long> {

    /** Membresía activa de un usuario (máximo una a la vez). */
    Optional<MiembroConsejo> findByUsuarioIdAndActivoTrue(Long usuarioId);

    /** Verifica si el usuario tiene membresía activa (usado en PermisoValidator). */
    boolean existsByUsuarioIdAndActivoTrue(Long usuarioId);

    /** Lista todos los consejeros activos del conjunto. */
    List<MiembroConsejo> findAllByActivoTrue();

    /** Historial completo de un usuario en el consejo. */
    List<MiembroConsejo> findAllByUsuarioId(Long usuarioId);
}
