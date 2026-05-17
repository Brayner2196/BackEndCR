package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.reserva.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoReserva;
import com.backendcr.residentialcomplex.service.ReservaService;
import com.backendcr.residentialcomplex.service.ZonaComunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminReservasController {

    private final ReservaService reservaService;
    private final ZonaComunService zonaService;
    private final SecurityUtils securityUtils;

    // ─── Reservas ─────────────────────────────────────────────

    @GetMapping("/reservas")
    public List<ReservaResponse> listar(
            @RequestParam(required = false) EstadoReserva estado) {
        return estado != null
                ? reservaService.listarPorEstado(estado)
                : reservaService.listarTodas();
    }

    @PutMapping("/reservas/{id}/aprobar")
    public ReservaResponse aprobar(@PathVariable Long id,
                                   @RequestBody(required = false) ReservaDecisionRequest req,
                                   @AuthenticationPrincipal String email) {
        return reservaService.aprobar(id, req, securityUtils.resolverUsuarioId(email));
    }

    @PutMapping("/reservas/{id}/rechazar")
    public ReservaResponse rechazar(@PathVariable Long id,
                                    @Valid @RequestBody ReservaDecisionRequest req,
                                    @AuthenticationPrincipal String email) {
        return reservaService.rechazar(id, req, securityUtils.resolverUsuarioId(email));
    }

    // ─── Zonas comunes — CRUD ──────────────────────────────────

    @GetMapping("/zonas-comunes")
    public List<ZonaComunResponse> listarZonas() {
        return zonaService.listarTodas();
    }

    @PostMapping("/zonas-comunes")
    @ResponseStatus(HttpStatus.CREATED)
    public ZonaComunResponse crearZona(@Valid @RequestBody ZonaComunRequest req) {
        return zonaService.crear(req);
    }

    @PutMapping("/zonas-comunes/{id}")
    public ZonaComunResponse actualizarZona(@PathVariable Long id,
                                            @Valid @RequestBody ZonaComunRequest req) {
        return zonaService.actualizar(id, req);
    }

    // ─── Zonas comunes — Suspensión ────────────────────────────

    @PutMapping("/zonas-comunes/{id}/suspender")
    public ZonaComunResponse suspender(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        return zonaService.suspender(id, body.get("motivo"));
    }

    @PutMapping("/zonas-comunes/{id}/reactivar")
    public ZonaComunResponse reactivar(@PathVariable Long id) {
        return zonaService.reactivar(id);
    }

    // ─── Zonas comunes — Excepciones ──────────────────────────

    @GetMapping("/zonas-comunes/{id}/excepciones")
    public List<ExcepcionZonaComunResponse> listarExcepciones(@PathVariable Long id) {
        return zonaService.listarExcepciones(id);
    }

    @PostMapping("/zonas-comunes/{id}/excepciones")
    @ResponseStatus(HttpStatus.CREATED)
    public ExcepcionZonaComunResponse agregarExcepcion(
            @PathVariable Long id,
            @Valid @RequestBody ExcepcionZonaComunRequest req) {
        return zonaService.agregarExcepcion(id, req);
    }

    @DeleteMapping("/zonas-comunes/{id}/excepciones/{excId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarExcepcion(@PathVariable Long id, @PathVariable Long excId) {
        zonaService.eliminarExcepcion(id, excId);
    }

}
