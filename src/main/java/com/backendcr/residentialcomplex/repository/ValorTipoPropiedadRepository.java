package com.backendcr.residentialcomplex.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendcr.residentialcomplex.entity.ValorTipoPropiedad;

public interface ValorTipoPropiedadRepository extends JpaRepository<ValorTipoPropiedad, Long> {

    /** Todos los valores (activos e inactivos) de un tipo — para gestión admin. */
    List<ValorTipoPropiedad> findByTipoIdOrderByOrdenAscValorAsc(Long tipoId);

    /** Valores activos de la plantilla global de un tipo (sin padre). */
    List<ValorTipoPropiedad> findByTipoIdAndParentValorIdIsNullAndActivoTrueOrderByOrdenAscValorAsc(Long tipoId);

    /** Valores activos contextuales de un tipo bajo un valor padre concreto. */
    List<ValorTipoPropiedad> findByTipoIdAndParentValorIdAndActivoTrueOrderByOrdenAscValorAsc(
            Long tipoId, Long parentValorId);

    /** ¿El tipo tiene al menos un valor activo definido (global o contextual)? */
    boolean existsByTipoIdAndActivoTrue(Long tipoId);
}
