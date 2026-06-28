package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.TenantClock;
import com.backendcr.residentialcomplex.dto.vigilancia.BitacoraAccesoResponse;
import com.backendcr.residentialcomplex.dto.vigilancia.ConfigVigilanciaDto;
import com.backendcr.residentialcomplex.service.vigilancia.BitacoraService;
import com.backendcr.residentialcomplex.service.vigilancia.ConfigVigilanciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Administración del módulo de vigilancia: parametrización y reportes (minuta).
 */
@RestController
@RequestMapping("/api/admin/vigilancia")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminVigilanciaController {

    private final ConfigVigilanciaService configService;
    private final BitacoraService bitacoraService;

    // ── Parametrización ───────────────────────────────────────────────────────

    @GetMapping("/config")
    public ConfigVigilanciaDto obtenerConfig() {
        return configService.obtenerDto();
    }

    @PutMapping("/config")
    public ConfigVigilanciaDto actualizarConfig(@Valid @RequestBody ConfigVigilanciaDto dto) {
        return configService.actualizar(dto);
    }

    // ── Reportes / minuta ─────────────────────────────────────────────────────

    /**
     * Bitácora por rango. Si no se envían fechas, devuelve los últimos 7 días.
     * Las fechas se interpretan en ISO-8601 (con o sin zona) según la zona del tenant.
     */
    @GetMapping("/bitacora")
    public List<BitacoraAccesoResponse> bitacora(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        Instant fin = (hasta == null || hasta.isBlank())
                ? TenantClock.ahora() : TenantClock.aInstante(hasta);
        Instant inicio = (desde == null || desde.isBlank())
                ? fin.minus(7, ChronoUnit.DAYS) : TenantClock.aInstante(desde);
        return bitacoraService.porRango(inicio, fin);
    }
}
