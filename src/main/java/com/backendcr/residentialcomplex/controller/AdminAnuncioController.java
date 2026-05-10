package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.anuncio.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoAnuncio;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.AnuncioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/anuncios")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminAnuncioController {

    private final AnuncioService anuncioService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping
    public List<AnuncioResponse> listar(@RequestParam(required = false) EstadoAnuncio estado) {
        return anuncioService.listarTodos(estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnuncioResponse crear(@Valid @RequestBody AnuncioRequest req,
                                 @AuthenticationPrincipal String email) {
        return anuncioService.crear(req, resolverAdminId(email));
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        anuncioService.eliminar(id);
    }

    @GetMapping("/{id}/vistas")
    public List<AnuncioVistaResponse> vistas(@PathVariable Long id) {
        return anuncioService.listarVistas(id);
    }

    private Long resolverAdminId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Admin no encontrado: " + email));
    }
}
