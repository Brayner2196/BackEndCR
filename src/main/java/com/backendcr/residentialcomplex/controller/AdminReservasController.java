package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.reserva.ReservaDecisionRequest;
import com.backendcr.residentialcomplex.dto.reserva.ReservaResponse;
import com.backendcr.residentialcomplex.dto.reserva.ZonaComunRequest;
import com.backendcr.residentialcomplex.dto.reserva.ZonaComunResponse;
import com.backendcr.residentialcomplex.entity.enums.EstadoReserva;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.ReservaService;
import com.backendcr.residentialcomplex.service.ZonaComunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminReservasController {

    private final ReservaService reservaService;
    private final ZonaComunService zonaService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    // ─── Reservas ─────────────────────────────

    @GetMapping("/reservas")
    public List<ReservaResponse> listar(@RequestParam(required = false) EstadoReserva estado) {
        return estado != null ? reservaService.listarPorEstado(estado) : reservaService.listarTodas();
    }

    @PutMapping("/reservas/{id}/aprobar")
    public ReservaResponse aprobar(@PathVariable Long id,
                                   @RequestBody(required = false) ReservaDecisionRequest req,
                                   @AuthenticationPrincipal String email) {
        return reservaService.aprobar(id, req, resolverAdminId(email));
    }

    @PutMapping("/reservas/{id}/rechazar")
    public ReservaResponse rechazar(@PathVariable Long id,
                                    @Valid @RequestBody ReservaDecisionRequest req,
                                    @AuthenticationPrincipal String email) {
        return reservaService.rechazar(id, req, resolverAdminId(email));
    }

    // ─── Zonas comunes ────────────────────────────

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

    private Long resolverAdminId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow();
    }
}
