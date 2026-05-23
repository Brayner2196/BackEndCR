package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ConfiguracionPlanPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionPlanPagoRepository extends JpaRepository<ConfiguracionPlanPago, Long> {
    /** Devuelve el único registro de configuración del tenant (siempre es solo 1). */
    Optional<ConfiguracionPlanPago> findFirstByOrderByIdAsc();
}
