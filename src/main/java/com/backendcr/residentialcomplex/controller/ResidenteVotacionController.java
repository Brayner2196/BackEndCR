package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.votacion.*;
import com.backendcr.residentialcomplex.service.VotacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/residente/votaciones")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'INQUILINO')")
@RequiredArgsConstructor
public class ResidenteVotacionController {

    private final VotacionService votacionService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<VotacionResponse> listar(@AuthenticationPrincipal String email) {
        return votacionService.listarParaResidente(securityUtils.resolverUsuarioId(email));
    }

    @GetMapping("/{id}")
    public VotacionResponse detalle(@PathVariable Long id,
                                    @AuthenticationPrincipal String email) {
        return votacionService.detalleParaResidente(id, securityUtils.resolverUsuarioId(email));
    }

    @PostMapping("/{id}/votar")
    @PreAuthorize("hasRole('PROPIETARIO') or @permisoValidator.tienePermiso(#email, 'VOTAR')")
    public VotacionResponse votar(@PathVariable Long id,
                                  @RequestBody RegistrarVotoRequest req,
                                  @AuthenticationPrincipal String email) {
        return votacionService.votar(id, securityUtils.resolverUsuarioId(email), req);
    }
}
