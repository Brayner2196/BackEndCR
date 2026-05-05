package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.pqr.PQRCambiarEstadoRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponderRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponse;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.PQRService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pqrs")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminPQRController {

    private final PQRService pqrService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping
    public List<PQRResponse> listar(@RequestParam(required = false) EstadoPQR estado) {
        return estado != null ? pqrService.listarPorEstado(estado) : pqrService.listarTodas();
    }

    @PutMapping("/{id}/responder")
    public PQRResponse responder(@PathVariable Long id,
                                 @Valid @RequestBody PQRResponderRequest req,
                                 @AuthenticationPrincipal String email) {
        return pqrService.responder(id, req, resolverAdminId(email));
    }

    @PutMapping("/{id}/estado")
    public PQRResponse cambiarEstado(@PathVariable Long id,
                                     @Valid @RequestBody PQRCambiarEstadoRequest req) {
        return pqrService.cambiarEstado(id, req);
    }

    private Long resolverAdminId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado para email=" + email + " tenant=" + tenantId));
    }
}
