package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.anuncio.AnuncioResponse;
import com.backendcr.residentialcomplex.service.AnuncioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/residente/anuncios")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'INQUILINO')")
@RequiredArgsConstructor
public class ResidenteAnuncioController {

    private final AnuncioService anuncioService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<AnuncioResponse> listar(@AuthenticationPrincipal String email) {
        return anuncioService.listarParaResidente(securityUtils.resolverUsuarioId(email));
    }

    @PostMapping("/{id}/visto")
    public AnuncioResponse marcarVisto(@PathVariable Long id,
                                       @AuthenticationPrincipal String email) {
        return anuncioService.marcarVisto(id, securityUtils.resolverUsuarioId(email));
    }
}
