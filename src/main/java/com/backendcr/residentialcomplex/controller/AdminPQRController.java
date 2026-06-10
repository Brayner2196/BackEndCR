package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.pqr.PQRCambiarEstadoRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRHistorialResponse;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponderRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponse;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import com.backendcr.residentialcomplex.service.PQRService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pqrs")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','CONSEJERO')")
@RequiredArgsConstructor
public class AdminPQRController {

    private final PQRService pqrService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<PQRResponse> listar(@RequestParam(required = false) EstadoPQR estado) {
        return estado != null ? pqrService.listarPorEstado(estado) : pqrService.listarTodas();
    }

    @GetMapping("/{id}/historial")
    public List<PQRHistorialResponse> historial(@PathVariable Long id) {
        return pqrService.listarHistorial(id);
    }

    @PutMapping("/{id}/responder")
    public PQRResponse responder(@PathVariable Long id,
                                 @Valid @RequestBody PQRResponderRequest req,
                                 @AuthenticationPrincipal String email) {
        return pqrService.responder(id, req, securityUtils.resolverUsuarioId(email));
    }

    @PutMapping("/{id}/estado")
    public PQRResponse cambiarEstado(@PathVariable Long id,
                                     @Valid @RequestBody PQRCambiarEstadoRequest req,
                                     @AuthenticationPrincipal String email) {
        return pqrService.cambiarEstado(id, req, securityUtils.resolverUsuarioId(email));
    }
}
