package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.reserva.DisponibilidadZonaResponse;
import com.backendcr.residentialcomplex.dto.reserva.ReservaRequest;
import com.backendcr.residentialcomplex.dto.reserva.ReservaResponse;
import com.backendcr.residentialcomplex.dto.reserva.ZonaComunResponse;
import com.backendcr.residentialcomplex.service.ReservaService;
import com.backendcr.residentialcomplex.service.ZonaComunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/residente")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'INQUILINO')")
@RequiredArgsConstructor
public class ResidenteReservasController {

    private final ReservaService reservaService;
    private final ZonaComunService zonaService;
    private final SecurityUtils securityUtils;

    @GetMapping("/zonas-comunes")
    public List<ZonaComunResponse> zonasActivas() {
        return zonaService.listarActivas();
    }

    @GetMapping("/zonas-comunes/{id}/disponibilidad")
    public DisponibilidadZonaResponse disponibilidad(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reservaService.disponibilidad(id, fecha);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROPIETARIO') or @permisoValidator.tienePermiso(#email, 'RESERVAS')")
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest req,
                                 @AuthenticationPrincipal String email) {
        return reservaService.crear(req, securityUtils.resolverUsuarioId(email));
    }

    @GetMapping("/reservas/me")
    public List<ReservaResponse> mias(@AuthenticationPrincipal String email) {
        return reservaService.listarPorResidente(securityUtils.resolverUsuarioId(email));
    }

    @PutMapping("/reservas/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id,
                                    @AuthenticationPrincipal String email) {
        return reservaService.cancelar(id, securityUtils.resolverUsuarioId(email));
    }
}
