package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.reserva.ReservaRequest;
import com.backendcr.residentialcomplex.dto.reserva.ReservaResponse;
import com.backendcr.residentialcomplex.dto.reserva.ZonaComunResponse;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/residente")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResidenteReservasController {

    private final ReservaService reservaService;
    private final ZonaComunService zonaService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping("/zonas-comunes")
    public List<ZonaComunResponse> zonasActivas() {
        return zonaService.listarActivas();
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest req,
                                 @AuthenticationPrincipal String email) {
        return reservaService.crear(req, resolverUsuarioId(email));
    }

    @GetMapping("/reservas/me")
    public List<ReservaResponse> mias(@AuthenticationPrincipal String email) {
        return reservaService.listarPorResidente(resolverUsuarioId(email));
    }

    @PutMapping("/reservas/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id,
                                    @AuthenticationPrincipal String email) {
        return reservaService.cancelar(id, resolverUsuarioId(email));
    }

    private Long resolverUsuarioId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado para email=" + email + " tenant=" + tenantId));
    }
}
