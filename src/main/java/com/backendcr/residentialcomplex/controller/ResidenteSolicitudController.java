package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.solicitud.ActualizarEstadoSolicitudRequest;
import com.backendcr.residentialcomplex.dto.solicitud.SolicitudRequest;
import com.backendcr.residentialcomplex.dto.solicitud.SolicitudResponse;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/residente/solicitudes")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResidenteSolicitudController {

    private final SolicitudService solicitudService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    /**
     * Crea una nueva solicitud de pedido (comprador).
     * POST /api/residente/solicitudes
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse crear(@Valid @RequestBody SolicitudRequest req,
                                   @AuthenticationPrincipal String email) {
        return solicitudService.crear(resolverUsuarioId(email), req);
    }

    /**
     * Solicitudes que yo (comprador) he enviado.
     * GET /api/residente/solicitudes/enviadas
     */
    @GetMapping("/enviadas")
    public List<SolicitudResponse> misSolicitudesEnviadas(@AuthenticationPrincipal String email) {
        return solicitudService.misSolicitudesEnviadas(resolverUsuarioId(email));
    }

    /**
     * Solicitudes que yo (vendedor) he recibido.
     * GET /api/residente/solicitudes/recibidas
     */
    @GetMapping("/recibidas")
    public List<SolicitudResponse> misSolicitudesRecibidas(@AuthenticationPrincipal String email) {
        return solicitudService.misSolicitudesRecibidas(resolverUsuarioId(email));
    }

    /**
     * Actualiza el estado de una solicitud.
     * - Vendedor: ACEPTADA | RECHAZADA
     * - Comprador: CANCELADA
     * PATCH /api/residente/solicitudes/{id}/estado
     */
    @PatchMapping("/{id}/estado")
    public SolicitudResponse actualizarEstado(@PathVariable Long id,
                                               @Valid @RequestBody ActualizarEstadoSolicitudRequest req,
                                               @AuthenticationPrincipal String email) {
        return solicitudService.actualizarEstado(id, resolverUsuarioId(email), req);
    }

    // ── Helper ────────────────────────────────────────────

    private Long resolverUsuarioId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado para email=" + email));
    }
}
