package com.backendcr.residentialcomplex.service.pasarela;

import com.backendcr.residentialcomplex.entity.TenantPasarela;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;
import com.backendcr.residentialcomplex.repository.TenantPasarelaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory que resuelve la implementación correcta de PasarelaService
 * y la configuración del tenant para una pasarela específica.
 *
 * Se usa junto con TenantPasarelaRepository para obtener las credenciales.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasarelaFactory {

    private final TenantPasarelaRepository pasarelaRepo;
    private final List<PasarelaService> pasarelas;

    /** Map lazy construido a partir de los beans registrados */
    private Map<TipoPasarela, PasarelaService> pasarelaMap;

    /** Obtiene la implementación para un tipo específico */
    public PasarelaService getServicio(TipoPasarela tipo) {
        return getMap().getOrDefault(tipo, null);
    }

    /**
     * Obtiene la configuración del tenant para una pasarela específica.
     * Lanza 412 si el tenant no tiene esa pasarela configurada o inactiva.
     */
    public TenantPasarela getConfigTenant(String tenantSchema, TipoPasarela tipo) {
        return pasarelaRepo.findByTenantSchemaAndTipo(tenantSchema, tipo)
                .filter(TenantPasarela::isActiva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                        "La pasarela " + tipo.name() + " no está configurada o activa para este conjunto"));
    }

    /**
     * Obtiene la implementación + config de tenant en un solo paso.
     * Usa null como config para MP con fallback global si no está en BD.
     */
    public PasarelaConConfig resolver(String tenantSchema, TipoPasarela tipo) {
        PasarelaService servicio = getMap().get(tipo);
        if (servicio == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pasarela no soportada: " + tipo);
        }

        // Para MP: si no está en BD, permite continuar con config null (usa globales)
        TenantPasarela config = pasarelaRepo.findByTenantSchemaAndTipo(tenantSchema, tipo)
                .filter(TenantPasarela::isActiva)
                .orElse(null);

        if (config == null && tipo != TipoPasarela.MERCADO_PAGO) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "La pasarela " + tipo.name() + " no está configurada para este conjunto");
        }

        return new PasarelaConConfig(servicio, config);
    }

    private Map<TipoPasarela, PasarelaService> getMap() {
        if (pasarelaMap == null) {
            pasarelaMap = pasarelas.stream()
                    .collect(Collectors.toMap(PasarelaService::getTipo, Function.identity()));
        }
        return pasarelaMap;
    }

    /** Wrapper inmutable de servicio + configuración del tenant */
    public record PasarelaConConfig(PasarelaService servicio, TenantPasarela config) {}
}
