package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.pqr.PQRRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponse;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
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
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResidentePQRController {

    private final PQRService pqrService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PQRResponse crear(@Valid @RequestBody PQRRequest req,
                             @AuthenticationPrincipal String email) {
        return pqrService.crear(req, resolverUsuarioId(email));
    }

    @GetMapping("/me")
    public List<PQRResponse> mias(@AuthenticationPrincipal String email) {
        return pqrService.listarPorResidente(resolverUsuarioId(email));
    }

    private Long resolverUsuarioId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow();
    }
}
