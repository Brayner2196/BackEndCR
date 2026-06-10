package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.anuncio.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoAnuncio;
import com.backendcr.residentialcomplex.service.AnuncioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/anuncios")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','CONSEJERO')")
@RequiredArgsConstructor
public class AdminAnuncioController {

    private final AnuncioService anuncioService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<AnuncioResponse> listar(@RequestParam(required = false) EstadoAnuncio estado) {
        return anuncioService.listarTodos(estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnuncioResponse crear(@Valid @RequestBody AnuncioRequest req,
                                 @AuthenticationPrincipal String email) {
        return anuncioService.crear(req, securityUtils.resolverUsuarioId(email));
    }

    @PutMapping("/{id}")
    public AnuncioResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody AnuncioRequest req) {
        return anuncioService.actualizar(id, req);
    }

    @PutMapping("/{id}/estado")
    public AnuncioResponse cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoAnuncioRequest req) {
        return anuncioService.cambiarEstado(id, req);
    }

    /** Solo TENANT_ADMIN puede eliminar anuncios. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        anuncioService.eliminar(id);
    }

    /** Solo TENANT_ADMIN ve estadísticas de vistas. */
    @GetMapping("/{id}/vistas")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<AnuncioVistaResponse> vistas(@PathVariable Long id) {
        return anuncioService.listarVistas(id);
    }
}
