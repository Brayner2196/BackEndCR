package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.anuncio.AnuncioResponse;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.AnuncioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/residente/anuncios")
@PreAuthorize("hasAnyRole('RESIDENTE', 'PROPIETARIO', 'INQUILINO')")
@RequiredArgsConstructor
public class ResidenteAnuncioController {

    private final AnuncioService anuncioService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping
    public List<AnuncioResponse> listar(@AuthenticationPrincipal String email) {
        return anuncioService.listarParaResidente(resolverResidenteId(email));
    }

    @PostMapping("/{id}/visto")
    public AnuncioResponse marcarVisto(@PathVariable Long id,
                                       @AuthenticationPrincipal String email) {
        return anuncioService.marcarVisto(id, resolverResidenteId(email));
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
