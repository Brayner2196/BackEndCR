package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ConfiguracionCuota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConfiguracionCuotaRepository extends JpaRepository<ConfiguracionCuota, Long> {
    List<ConfiguracionCuota> findAllByActivoTrue();
    Optional<ConfiguracionCuota> findByPropiedadIdAndActivoTrue(Long propiedadId);
    Optional<ConfiguracionCuota> findByTipoPropiedadIdAndActivoTrue(Long tipoPropiedadId);
}
