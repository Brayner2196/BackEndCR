package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.publicacion.PublicacionRequest;
import com.backendcr.residentialcomplex.dto.publicacion.PublicacionResponse;
import com.backendcr.residentialcomplex.entity.enums.CategoriaPublicacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoPublicacion;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.PublicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/residente")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResidentePublicacionController {

    private final PublicacionService publicacionService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    // ── Marketplace ───────────────────────────────────────

    @GetMapping("/marketplace")
    public List<PublicacionResponse> marketplace(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) CategoriaPublicacion categoria,
            @AuthenticationPrincipal String email) {
        return publicacionService.marketplace(resolverUsuarioId(email), busqueda, categoria);
    }

    // ── Mis publicaciones ─────────────────────────────────

    @GetMapping("/publicaciones/me")
    public List<PublicacionResponse> misPublicaciones(@AuthenticationPrincipal String email) {
        return publicacionService.misPublicaciones(resolverUsuarioId(email));
    }

    @PostMapping("/publicaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionResponse crear(@Valid @RequestBody PublicacionRequest req,
                                     @AuthenticationPrincipal String email) {
        return publicacionService.crear(resolverUsuarioId(email), req);
    }

    @PutMapping("/publicaciones/{id}")
    public PublicacionResponse actualizar(@PathVariable Long id,
                                          @Valid @RequestBody PublicacionRequest req,
                                          @AuthenticationPrincipal String email) {
        return publicacionService.actualizar(id, resolverUsuarioId(email), req);
    }

    @PatchMapping("/publicaciones/{id}/estado")
    public PublicacionResponse cambiarEstado(@PathVariable Long id,
                                              @RequestBody Map<String, String> body,
                                              @AuthenticationPrincipal String email) {
        EstadoPublicacion estado;
        try {
            estado = EstadoPublicacion.valueOf(body.getOrDefault("estado", ""));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido");
        }
        return publicacionService.cambiarEstado(id, resolverUsuarioId(email), estado);
    }

    @DeleteMapping("/publicaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal String email) {
        publicacionService.eliminar(id, resolverUsuarioId(email));
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
