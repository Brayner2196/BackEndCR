package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.votacion.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoVotacion;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.VotacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/votaciones")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminVotacionController {

    private final VotacionService votacionService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping
    public List<VotacionResponse> listar(@RequestParam(required = false) EstadoVotacion estado) {
        return votacionService.listarTodas(estado);
    }

    @GetMapping("/{id}")
    public VotacionResponse detalle(@PathVariable Long id) {
        return votacionService.obtenerDetalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VotacionResponse crear(@Valid @RequestBody VotacionRequest req,
                                  @AuthenticationPrincipal String email) {
        return votacionService.crear(req, resolverAdminId(email));
    }

    @PutMapping("/{id}")
    public VotacionResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody VotacionRequest req) {
        return votacionService.actualizar(id, req);
    }

    @PutMapping("/{id}/estado")
    public VotacionResponse cambiarEstado(@PathVariable Long id,
                                          @Valid @RequestBody CambiarEstadoVotacionRequest req) {
        return votacionService.cambiarEstado(id, req);
    }

    @GetMapping("/{id}/resultados")
    public VotacionResponse resultados(@PathVariable Long id) {
        return votacionService.resultadosAdmin(id);
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
