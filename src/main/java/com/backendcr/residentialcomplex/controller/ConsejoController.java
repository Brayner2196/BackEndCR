package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.consejo.ConsejoEstadisticasResponse;
import com.backendcr.residentialcomplex.dto.consejo.MiembroConsejoResponse;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponse;
import com.backendcr.residentialcomplex.service.ConsejoEstadisticasService;
import com.backendcr.residentialcomplex.service.ConsejoService;
import com.backendcr.residentialcomplex.service.PQRService;
import com.backendcr.residentialcomplex.config.TenantClock;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints exclusivos para el rol CONSEJERO.
 * Accesibles también por TENANT_ADMIN (para pruebas y supervisión).
 */
@RestController
@RequestMapping("/api/consejo")
@RequiredArgsConstructor
public class ConsejoController {

    private final ConsejoService consejoService;
    private final PQRService pqrService;
    private final ConsejoEstadisticasService estadisticasService;

    /**
     * Directorio público del consejo — cualquier residente autenticado puede verlo.
     */
    @GetMapping("/miembros")
    @PreAuthorize("hasAnyRole('PROPIETARIO','INQUILINO','CONSEJERO','TENANT_ADMIN')")
    public List<MiembroConsejoResponse> directorio() {
        return consejoService.directorioPublico();
    }

    /**
     * Vista de todas las PQRs del conjunto (no solo las propias).
     * Permite al consejo hacer seguimiento de las peticiones de los residentes.
     */
    @GetMapping("/pqrs")
    @PreAuthorize("hasAnyRole('CONSEJERO','TENANT_ADMIN')")
    public List<PQRResponse> todasLasPqrs(
            @RequestParam(required = false) String estado) {
        return pqrService.listarTodasParaConsejo(estado);
    }

    /**
     * Estadísticas agregadas de PQRs, Anuncios y Votaciones en un rango de fechas.
     * Por defecto: últimos 30 días.
     * Parámetros: desde (yyyy-MM-dd), hasta (yyyy-MM-dd).
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('CONSEJERO','TENANT_ADMIN')")
    public ConsejoEstadisticasResponse estadisticas(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        Instant d = desde != null
                ? LocalDate.parse(desde).atStartOfDay(TenantClock.zona()).toInstant()
                : TenantClock.ahora().minus(Duration.ofDays(30));
        Instant h = hasta != null
                ? LocalDate.parse(hasta).atTime(23, 59, 59).atZone(TenantClock.zona()).toInstant()
                : TenantClock.ahora();
        return estadisticasService.calcular(d, h);
    }
}
