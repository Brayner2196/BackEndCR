package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.pqr.PQRRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponse;
import com.backendcr.residentialcomplex.service.PQRService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/residente/pqrs")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'INQUILINO')")
@RequiredArgsConstructor
public class ResidentePQRController {

    private final PQRService pqrService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROPIETARIO') or @permisoValidator.tienePermiso(#email, 'PQRS')")
    public PQRResponse crear(@Valid @RequestBody PQRRequest req,
                             @AuthenticationPrincipal String email) {
        return pqrService.crear(req, securityUtils.resolverUsuarioId(email));
    }

    @GetMapping("/me")
    public List<PQRResponse> mias(@AuthenticationPrincipal String email) {
        return pqrService.listarPorResidente(securityUtils.resolverUsuarioId(email));
    }
}
