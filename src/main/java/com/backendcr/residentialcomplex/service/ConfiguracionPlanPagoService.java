package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.planpago.ConfiguracionPlanPagoRequest;
import com.backendcr.residentialcomplex.dto.planpago.ConfiguracionPlanPagoResponse;
import com.backendcr.residentialcomplex.entity.ConfiguracionPlanPago;
import com.backendcr.residentialcomplex.repository.ConfiguracionPlanPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ConfiguracionPlanPagoService {

    private final ConfiguracionPlanPagoRepository repo;

    /** Obtiene la configuración actual; si no existe, devuelve valores por defecto (no guardados). */
    public ConfiguracionPlanPagoResponse obtener() {
        return repo.findFirstByOrderByIdAsc()
                .map(ConfiguracionPlanPagoResponse::from)
                .orElseGet(() -> new ConfiguracionPlanPagoResponse(
                        null, false, 3, false, BigDecimal.ZERO, false, false, null));
    }

    /** Crea o actualiza la única configuración del tenant. */
    @Transactional
    public ConfiguracionPlanPagoResponse guardar(ConfiguracionPlanPagoRequest req) {
        ConfiguracionPlanPago cfg = repo.findFirstByOrderByIdAsc()
                .orElse(new ConfiguracionPlanPago());

        cfg.setActivo(req.activo());
        cfg.setMaxCuotas(req.maxCuotas());
        cfg.setRecargoFraccionamiento(req.recargoFraccionamiento());
        cfg.setPorcentajeRecargo(
                req.porcentajeRecargo() != null ? req.porcentajeRecargo() : BigDecimal.ZERO);
        cfg.setMoraCongeladaDurantePlan(req.moraCongeladaDurantePlan());
        cfg.setAprobacionAutomatica(req.aprobacionAutomatica());

        return ConfiguracionPlanPagoResponse.from(repo.save(cfg));
    }
}
