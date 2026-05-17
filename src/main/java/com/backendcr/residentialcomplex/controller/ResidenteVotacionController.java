package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.votacion.*;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.VotacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/residente/votaciones")
@PreAuthorize("hasAnyRole('RESIDENTE', 'PROPIETARIO', 'INQUILINO')")
@RequiredArgsConstructor
public class ResidenteVotacionController {

    private final VotacionService votacionService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping
    public List<VotacionResponse> listar(@AuthenticationPrincipal String email) {
        return votacionService.listarParaResidente(resolverResidenteId(email));
    }

    @GetMapping("/{id}")
    public VotacionResponse detalle(@PathVariable Long id,
                                    @AuthenticationPrincipal String email) {
        return votacionService.detalleParaResidente(id, resolverResidenteId(email));
    }

    @PostMapping("/{id}/votar")
    public VotacionResponse votar(@PathVariable Long id,
                                  @RequestBody RegistrarVotoRequest req,
                                  @AuthenticationPrincipal String email) {
        return votacionService.votar(id, resolverResidenteId(email), req);
    }

    private Long resolverResidenteId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Residente no encontrado: " + email));
    }
}
