package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ConfiguracionMora;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfiguracionMoraRepository extends JpaRepository<ConfiguracionMora, Long> {
    Optional<ConfiguracionMora> findFirstByActivoTrueOrderByFechaVigenciaDesc();
}
